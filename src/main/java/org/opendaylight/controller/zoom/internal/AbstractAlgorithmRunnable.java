package org.opendaylight.controller.zoom.internal;

import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.HwPath;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.flowprogrammer.IFlowProgrammerService;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchField;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.IReadService;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.slf4j.Logger;

public abstract class AbstractAlgorithmRunnable implements Runnable {
	
	protected ISwitchManager switchManager;
	protected IFlowProgrammerService programmer;
	protected IReadService reader;
	protected Logger logger;

	protected int flowCount;
	protected int sleepTime;
	protected int topFlows;
	protected int cycles;
	protected int currentCycle = 0;
	protected ScheduledExecutorService executor;
	
	protected int threadCount = 4;
	protected ExecutorService flowProgramExecutor;

	protected List<FlowOnNode> biggestFlowList = new LinkedList<FlowOnNode>();

	protected PrintWriter w = null;

	protected Date creationTime;

	protected long flowCounter = 0;

	protected List<Flow> currentFlows = new LinkedList<Flow>();
	
	public AbstractAlgorithmRunnable(int cycles, int flowCount, int sleepTime, int topFlows, ISwitchManager switchManager, IReadService reader, IFlowProgrammerService programmer, Logger logger, ScheduledExecutorService executor) {
		this.flowCount = flowCount;
		this.sleepTime = sleepTime;
		this.topFlows = topFlows;
		this.switchManager = switchManager;
		this.cycles = cycles;

		this.executor = executor;
		this.reader = reader;
		this.logger = logger;
		this.programmer = programmer;

		this.creationTime = new Date();
		
		this.flowProgramExecutor = Executors.newFixedThreadPool(this.threadCount);
	}
	
	protected int[] ip2int(String ip) {

		int tokens[] = new int[4];

		String[] stokens = ip.split("\\.");

		for (int i = 0; i < stokens.length; i++)
			tokens[i] = Integer.parseInt(stokens[i]);

		return tokens;
	}
	
	protected String ip2string(int[] tokens) {
		return tokens[0] + "." + tokens[1] + "." + tokens[2] + "." + tokens[3];
	}
	
	protected FlowOnNode findBiggestFlow(List<FlowOnNode> flows) {
		long count = 0;
		FlowOnNode biggestFlow = null;
		for (FlowOnNode flow : flows) {
			long currentCount = flow.getByteCount();
			boolean containerFlag = false;
			if (currentCount > count) {
				for (FlowOnNode f : this.biggestFlowList) {
					if (f.getFlow().getId() == flow.getFlow().getId()) {
						containerFlag = true;
						break;
					}
				}
				if (!containerFlag) {
					count = currentCount;
					biggestFlow = flow;
				}
			}
		}

		if (biggestFlow != null) {
			this.biggestFlowList.add(biggestFlow);
		}

		return biggestFlow;
	}
	
	protected void programFlow(Node node, Flow f) {
		this.flowProgramExecutor.execute(new FlowProgrammerRunnable(programmer, f, node));
		this.currentFlows.add(f);
	}
	
	private class FlowProgrammerRunnable implements Runnable {
		
		IFlowProgrammerService programmer;
		Flow f;
		Node node;
		
		public FlowProgrammerRunnable(IFlowProgrammerService programmer, Flow f, Node node) {
			this.programmer = programmer;
			this.f = f;
			this.node = node;
		}

		public void run() {
			programmer.addFlowAsync(node, f);
		}
		
		
	}
	
	protected String numeric2Symbolic(int ip) {
		StringBuffer sb = new StringBuffer(15);
		for (int shift = 24; shift > 0; shift -= 8) {
			sb.append(Integer.toString((ip >>> shift) & 0xff));
			sb.append('.');
		}
		sb.append(Integer.toString(ip & 0xff));
		return sb.toString();
	}
	
	protected void removeAllFlowsFromNode(Node node) {
		ExecutorService s = Executors.newFixedThreadPool(this.threadCount);
		for(Flow f : this.currentFlows) {
			s.execute(new FlowRemoverRunnable(this.programmer, f, node));
		}
		s.shutdown();
		try {
			s.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
		  e.printStackTrace();
		}
		this.programmer.syncSendBarrierMessage(node);
		this.currentFlows.clear();
	}
	
	private class FlowRemoverRunnable implements Runnable {
		
		IFlowProgrammerService programmer;
		Flow f;
		Node node;
		
		public FlowRemoverRunnable(IFlowProgrammerService programmer, Flow f, Node node) {
			this.programmer = programmer;
			this.f = f;
			this.node = node;
		}

		public void run() {
			programmer.removeFlowAsync(node, f);
		}
	}
	
	protected List<FlowOnNode> getCurrentFlows(Node node) {
		List<FlowOnNode> flows = new LinkedList<FlowOnNode>();
		for (FlowOnNode flow : this.reader.nonCachedReadAllFlows(node)) {
			if (this.currentFlows.contains(flow.getFlow())) {
				flows.add(flow);
			}
		}
		return flows;
	}
	
	protected Flow generateFlow(String sSrcIP, String sSrcMask, String sDstIP, String sDstMask) {
		InetAddress srcIP = null;
		InetAddress srcMask = null;
		InetAddress dstIP = null;
		InetAddress dstMask = null;
		Match match = new Match();

		try {
			srcIP = InetAddress.getByName(sSrcIP);
			srcMask = InetAddress.getByName(sSrcMask);
			dstIP = InetAddress.getByName(sDstIP);
			dstMask = InetAddress.getByName(sDstMask);
		} catch (UnknownHostException e) {
			logger.error("Unknown Host");
		}
		short ethertype = EtherTypes.IPv4.shortValue();
		match.setField(new MatchField(MatchType.DL_TYPE, ethertype));

		match.setField(new MatchField(MatchType.NW_SRC, srcIP, srcMask));
		match.setField(new MatchField(MatchType.NW_DST, dstIP, dstMask));

		List<Action> actions = new ArrayList<Action>();
		actions.add(new HwPath());

		Flow f = new Flow(match, actions);

		return f;
	}
	
	protected Flow generateSrcFlow(String sIP, String sMask) {
		InetAddress IP = null;
		InetAddress Mask = null;
		Match match = new Match();

		try {
			IP = InetAddress.getByName(sIP);
			Mask = InetAddress.getByName(sMask);
		} catch (UnknownHostException e) {
			logger.error("Unknown Host");
		}
		short ethertype = EtherTypes.IPv4.shortValue();
		match.setField(new MatchField(MatchType.DL_TYPE, ethertype));

		match.setField(new MatchField(MatchType.NW_SRC, IP, Mask));

		List<Action> actions = new ArrayList<Action>();
		actions.add(new HwPath());

		Flow f = new Flow(match, actions);

		return f;
	}
	
	protected Flow generateDstFlow(String sIP, String sMask) {
		InetAddress IP = null;
		InetAddress Mask = null;
		Match match = new Match();

		try {
			IP = InetAddress.getByName(sIP);
			Mask = InetAddress.getByName(sMask);
		} catch (UnknownHostException e) {
			logger.error("Unknown Host");
		}
		short ethertype = EtherTypes.IPv4.shortValue();
		match.setField(new MatchField(MatchType.DL_TYPE, ethertype));

		match.setField(new MatchField(MatchType.NW_DST, IP, Mask));

		List<Action> actions = new ArrayList<Action>();
		actions.add(new HwPath());

		Flow f = new Flow(match, actions);

		return f;
	}
	
	protected IPv4 extractSrcFromFlow(FlowOnNode flow) {

		String newStartIP = null;
		String newStartNetmask = null;
		if (flow.getFlow().getMatch().getField(MatchType.NW_SRC) != null) {
			newStartIP = flow.getFlow().getMatch().getField(MatchType.NW_SRC).getValue().toString().replace("/", "");
			newStartNetmask = flow.getFlow().getMatch().getField(MatchType.NW_SRC).getMask().toString().replace("/", "");
		} else
			return null;

		IPv4 ip = new IPv4(newStartIP, newStartNetmask);
		return ip;
	}

	protected IPv4 extractDstFromFlow(FlowOnNode flow) {

		String newStartIP = null;
		String newStartNetmask = null;
		if (flow.getFlow().getMatch().getField(MatchType.NW_DST) != null) {
			newStartIP = flow.getFlow().getMatch().getField(MatchType.NW_DST).getValue().toString().replace("/", "");
			newStartNetmask = flow.getFlow().getMatch().getField(MatchType.NW_DST).getMask().toString().replace("/", "");
		} else
			return null;

		IPv4 ip = new IPv4(newStartIP, newStartNetmask);

		return ip;
	}

	public abstract void run();

}
