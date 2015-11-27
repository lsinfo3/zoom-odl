package org.opendaylight.controller.zoom.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.flowprogrammer.IFlowProgrammerService;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.IReadService;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.slf4j.Logger;

public class ZoomBase implements IZoom {

	private ISwitchManager switchManager;
	private IFlowProgrammerService programmer;
	private IReadService reader;
	private Logger logger;
	private String sudoPassword = "Ork45_re";
	private String intf1 = "s1";
	
	public ZoomBase(ISwitchManager switchManager, IFlowProgrammerService programmer, IReadService reader) {
		this.switchManager = switchManager;
		this.programmer = programmer;
		this.reader = reader;
	}
	
	public void start(Logger logger, int eFlowCount, int eTopFlows, int eSleepTime, int eOffset, int eCycles, String path) {

		this.logger = logger;

		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

		int flowCount = eFlowCount;
		int sleepTime = eSleepTime;
		int topFlows = eTopFlows;
		int cycles = eCycles;
		int offset = eOffset;

		sleepTime = sleepTime * 1000;

		Runtime run = Runtime.getRuntime();
		try {
			run.exec(new String[] { "/bin/bash", "-c", "echo " + sudoPassword + " | sudo -S tcpreplay-edit --intf1=" + intf1 + " --fixlen=pad " + path });
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		int flowsPerTopFlow = flowCount / topFlows;

		int stepCountPerByte = 8;

		switch (flowsPerTopFlow) {
		case 2:
			stepCountPerByte = 8;
			break;
		case 4:
			stepCountPerByte = 4;
			break;
		case 16:
			stepCountPerByte = 2;
			break;
		}

		int cycleTimer = ((4 * stepCountPerByte) * (sleepTime / 1000)) + (((4 * stepCountPerByte) * (flowsPerTopFlow*topFlows))/1000) + 5;

		Runnable algorithmRunnable = new algorithmRunnable(cycles, flowCount, sleepTime, topFlows, this.switchManager, this.reader, this.programmer, this.logger, executor);
		executor.scheduleAtFixedRate(algorithmRunnable, offset, cycleTimer, TimeUnit.SECONDS);
		
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		try {
			run.exec(new String[] { "/bin/bash", "-c", "echo " + sudoPassword + " | sudo -S killall tcpreplay-edit" });
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void start(CommandInterpreter ci, Logger logger) {

		this.logger = logger;

		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

		int flowCount = 2;
		int sleepTime = 2;
		int topFlows = 1;
		int cycles = 1;
		int offset = 5;
		String path = "";

		for (String current = ci.nextArgument(); current != null; current = ci.nextArgument()) {
			if (current.equals("-nflows")) {
				flowCount = Integer.parseInt(ci.nextArgument());
			} else if (current.equals("-ntop")) {
				topFlows = Integer.parseInt(ci.nextArgument());
			} else if (current.equals("-t")) {
				sleepTime = Integer.parseInt(ci.nextArgument());
			} else if (current.equals("-ncycles")) {
				cycles = Integer.parseInt(ci.nextArgument());
			} else if (current.equals("-offset")) {
				offset = Integer.parseInt(ci.nextArgument());
			} else if (current.equals("-trace")) {
				path = ci.nextArgument();
			} else {
				ci.println("Unknown parameter " + current + ". Ignored.");
			}
		}

		sleepTime = sleepTime * 1000;

		if (!(flowCount == 2 || flowCount == 4 || flowCount == 16)) {
			ci.println("Invalid number of flows! (-nflows VALUE)\nValid values are 2 (default), 4, 16.\nExit.");
			return;
		} else if (topFlows > flowCount) {
			ci.println("The number top flows has to be smaller than the total number of flows.\nExit");
			return;
		} else if (!(flowCount / topFlows == 2 || flowCount / topFlows == 4 || flowCount / topFlows == 8 || flowCount / topFlows == 16)) {
			ci.println("flowCount / topFlows needs to equal 2, 4, 8 or 16.\nExit");
			return;
		} else if (path.equals("")) {
			ci.println("No trace file has been specified.\nExit");
			return;
		}
		
		Runtime run = Runtime.getRuntime();
		try {
			run.exec(new String[] { "/bin/bash", "-c", "echo " + sudoPassword + " | sudo -S tcpreplay-edit --intf1=" + intf1 + " --fixlen=pad " + path });
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		int flowsPerTopFlow = flowCount / topFlows;

		int stepCountPerByte = 8;

		switch (flowsPerTopFlow) {
		case 2:
			stepCountPerByte = 8;
			break;
		case 4:
			stepCountPerByte = 4;
			break;
		case 16:
			stepCountPerByte = 2;
			break;
		}

		int cycleTimer = ((4 * stepCountPerByte) * (sleepTime / 1000)) + (((4 * stepCountPerByte) * (flowsPerTopFlow*topFlows))/1000) + 5;

		Runnable algorithmRunnable = new algorithmRunnable(cycles, flowCount, sleepTime, topFlows, this.switchManager, this.reader, this.programmer, this.logger, executor);
		executor.scheduleAtFixedRate(algorithmRunnable, offset, cycleTimer, TimeUnit.SECONDS);
		
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		try {
			run.exec(new String[] { "/bin/bash", "-c", "echo " + sudoPassword + " | sudo -S killall tcpreplay-edit" });
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private class algorithmRunnable extends AbstractAlgorithmRunnable {

		public algorithmRunnable(int cycles, int flowCount, int sleepTime, int topFlows, ISwitchManager switchManager, IReadService reader, IFlowProgrammerService programmer, Logger logger, ScheduledExecutorService executor) {
			super(cycles, flowCount, sleepTime, topFlows, switchManager, reader, programmer, logger, executor);
		}

		public void run() {
			
			IPv4 localCurrentSrcIP = null, localCurrentDstIP = null;
			
			Set<Node> nodeSet = switchManager.getNodes();
			Node node = (Node) nodeSet.toArray()[0]; // TODO: Experimental!! Only works if just one node is present

			List<Flow> flowList = new LinkedList<Flow>();
			List<IPv4> srcList = new LinkedList<IPv4>();
			List<IPv4> dstList = new LinkedList<IPv4>();

			boolean firstFlag = true;
			boolean changeFlag = true;

			int topFlowCount = 1;
			this.flowCounter = 0;

			long start = (new Date().getTime() - this.creationTime.getTime()) / 1000;
			
			String resultPath = "results/ZoomBase/";
			File theDir = new File(resultPath + this.flowCount + "/" + this.topFlows + "/" + this.sleepTime / 1000 + "/");
			
			if (!theDir.exists()) {
				try {
					theDir.mkdirs();
				} catch (SecurityException se) {
					se.printStackTrace();
				}
			}
			
			debug(resultPath + "debug", "\n NEW RUN \n");

			String parameterString = "# Start: " + start;

			firstFlag = false;

			srcList.add(new IPv4("1.0.0.0", "255.0.0.0"));
			srcList.add(new IPv4("2.0.0.0", "254.0.0.0"));
			srcList.add(new IPv4("4.0.0.0", "252.0.0.0"));
			srcList.add(new IPv4("8.0.0.0", "248.0.0.0"));
			srcList.add(new IPv4("16.0.0.0", "240.0.0.0"));
			for (int i = 32; i <= 240; i += 16)
				srcList.add(new IPv4(i + ".0.0.0", "240.0.0.0"));

			dstList.add(new IPv4("1.0.0.0", "255.0.0.0"));
			dstList.add(new IPv4("2.0.0.0", "254.0.0.0"));
			dstList.add(new IPv4("4.0.0.0", "252.0.0.0"));
			dstList.add(new IPv4("8.0.0.0", "248.0.0.0"));
			dstList.add(new IPv4("16.0.0.0", "240.0.0.0"));
			for (int i = 32; i <= 240; i += 16)
				dstList.add(new IPv4(i + ".0.0.0", "240.0.0.0"));

			for (IPv4 src : srcList) {
				for (IPv4 dst : dstList) {
					Flow f = generateFlow(src.getIP(), src.getNetmask(), dst.getIP(), dst.getNetmask());
					f.setId(this.flowCounter++);
					flowList.add(f);
				}
			}

			for (Flow f : flowList) {
				programFlow(node, f);
			}
			flowProgramExecutor.shutdown();
			try {
				flowProgramExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			} catch (InterruptedException e) {
			  e.printStackTrace();
			}
			this.programmer.syncSendBarrierMessage(node);
			this.flowProgramExecutor = Executors.newFixedThreadPool(this.threadCount);

			srcList.clear();
			dstList.clear();
			flowList.clear();

			try {
				Thread.sleep(this.sleepTime);
			} catch (InterruptedException e2) {
				e2.printStackTrace();
			}

			String biggestFlowString = "";
		
			
			while (changeFlag) {
				changeFlag = false;

				flowList.clear();
				this.biggestFlowList.clear();

				List<FlowOnNode> currentFlowList = getCurrentFlows(node);
				
				biggestFlowString = "";

				for (int k = 1; k <= this.topFlows; k++) {
					if (!firstFlag) {
						topFlowCount = this.topFlows;
						FlowOnNode biggestFlow = findBiggestFlow(currentFlowList);
						if (biggestFlow == null) {
							for (FlowOnNode f : getCurrentFlows(node)) {
								w.println(f);
							}
							break;
						}

						localCurrentSrcIP = extractSrcFromFlow(biggestFlow);
						localCurrentDstIP = extractDstFromFlow(biggestFlow);
						debug(resultPath + "debug", "Biggest Flow: " + localCurrentSrcIP.getIP() + " -> " + localCurrentDstIP.getIP());
						biggestFlowString += "\n" + localCurrentSrcIP.getIP() + ";" + localCurrentSrcIP.getNetmask() + ";" + localCurrentDstIP.getIP() + ";" + localCurrentDstIP.getNetmask() + ";" + biggestFlow.getByteCount()*8 / (this.sleepTime / 1000);

					}

					int flowCountPerIP = this.flowCount / topFlowCount;
					int shiftwidth = (int) (Math.log(flowCountPerIP) / Math.log(2));

					IPv4 srcOne = localCurrentSrcIP;
					IPv4 dstOne = localCurrentDstIP;

					if (localCurrentSrcIP.getNumberOfHosts() > 1) {

						changeFlag = true;
						
						// NEW
						int numberOfFlows = flowCountPerIP; 
						
						int nmb = localCurrentSrcIP.getNetmaskByte();
						int newSrcNetmask = localCurrentSrcIP.getNumericNetmask() >> shiftwidth;
						int[] newSrcNetmaskBytes = ip2int(convertNumericIpToSymbolic(newSrcNetmask));
						debug(resultPath + "debug", "SrcBefore: " + convertNumericIpToSymbolic(newSrcNetmask));
						if (nmb < 3 && newSrcNetmaskBytes[nmb+1] != 0) {
							int[] tmpMask = new int[4];
							for (int q = 0; q < 4; q++) {
								if (q <= nmb)
									tmpMask[q] = 255;
								else
									tmpMask[q] = 0;
							}
							IPv4 tmpIP = new IPv4(localCurrentSrcIP.getIP(), ip2string(tmpMask));
							newSrcNetmask = tmpIP.getNumericNetmask();
							numberOfFlows = 256 - ip2int(localCurrentSrcIP.getNetmask())[nmb];
						}
						debug(resultPath + "debug", "SrcAfter: " + convertNumericIpToSymbolic(newSrcNetmask));
						// UNTIL HERE
						
						srcOne = new IPv4(localCurrentSrcIP.getIP(), numeric2Symbolic(newSrcNetmask));

						srcList.add(srcOne);

						int[] srcOneIP = ip2int(srcOne.getIP());
						int[] srcOneNetmask = ip2int(srcOne.getNetmask());

						int[] srcIP = srcOneIP.clone();

						boolean netmaskJumperFlag = false;
						int temp = localCurrentSrcIP.getNetmaskByte();
						for (int j = 1; j < numberOfFlows; j++) {
							for (int i = 0; i < 4; i++) {
								if (i == temp) {
									srcIP[i] = srcIP[i] + 256 - srcOneNetmask[i];
									if (!netmaskJumperFlag && srcOneNetmask[i] == 255) {
										localCurrentSrcIP.increaseNetmaskByte();
										netmaskJumperFlag = true;
									}
								}
							}
							srcList.add(new IPv4(ip2string(srcIP), numeric2Symbolic(newSrcNetmask)));
							debug(resultPath + "debug", "Src: " + ip2string(srcIP) + "/" + numeric2Symbolic(newSrcNetmask));
						}
					} else
						srcList.add(srcOne);

					if (localCurrentDstIP.getNumberOfHosts() > 1) {
						changeFlag = true;
						
						// NEW
						int numberOfFlows = flowCountPerIP; 
						
						int nmb = localCurrentDstIP.getNetmaskByte();
						int newDstNetmask = localCurrentDstIP.getNumericNetmask() >> shiftwidth;
						int[] newDstNetmaskBytes = ip2int(convertNumericIpToSymbolic(newDstNetmask));
						debug(resultPath + "debug", "DstBefore: " + convertNumericIpToSymbolic(newDstNetmask));
						if (nmb < 3 && newDstNetmaskBytes[nmb+1] != 0) {
							int[] tmpMask = new int[4];
							for (int q = 0; q < 4; q++) {
								if (q <= nmb)
									tmpMask[q] = 255;
								else
									tmpMask[q] = 0;
							}
							IPv4 tmpIP = new IPv4(localCurrentDstIP.getIP(), ip2string(tmpMask));
							newDstNetmask = tmpIP.getNumericNetmask();
							numberOfFlows = 256 - ip2int(localCurrentDstIP.getNetmask())[nmb];
						}
						debug(resultPath + "debug", "DstAfter: " + convertNumericIpToSymbolic(newDstNetmask));
						// UNTIL HERE
						
						dstOne = new IPv4(localCurrentDstIP.getIP(), numeric2Symbolic(newDstNetmask));

						dstList.add(dstOne);

						int[] dstOneIP = ip2int(dstOne.getIP());
						int[] dstOneNetmask = ip2int(dstOne.getNetmask());

						int[] dstIP = dstOneIP.clone();

						boolean netmaskJumperFlag = false;
						int temp = localCurrentDstIP.getNetmaskByte();
						for (int j = 1; j < numberOfFlows; j++) {
							for (int i = 0; i < 4; i++) {
								if (i == temp) {
									dstIP[i] = dstIP[i] + 256 - dstOneNetmask[i];
									if (!netmaskJumperFlag && dstOneNetmask[i] == 255) {
										localCurrentDstIP.increaseNetmaskByte();
										netmaskJumperFlag = true;
									}
								}
							}
							dstList.add(new IPv4(ip2string(dstIP), numeric2Symbolic(newDstNetmask)));
							debug(resultPath + "debug", "Dst: " + ip2string(dstIP) + "/" + numeric2Symbolic(newDstNetmask));
						}
					} else
						dstList.add(dstOne);

					for (IPv4 src : srcList) {
						for (IPv4 dst : dstList) {
							Flow f = generateFlow(src.getIP(), src.getNetmask(), dst.getIP(), dst.getNetmask());
							f.setId(this.flowCounter++);
							flowList.add(f);
						}
					}
					srcList.clear();
					dstList.clear();

					if (firstFlag)
						break;

				}
				
				removeAllFlowsFromNode(node);
				
				if(changeFlag) {
					for (Flow f : flowList) {
						programFlow(node, f);
					}
					flowProgramExecutor.shutdown();
					try {
						flowProgramExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
					} catch (InterruptedException e) {
					  e.printStackTrace();
					}
					this.programmer.syncSendBarrierMessage(node);
					this.flowProgramExecutor = Executors.newFixedThreadPool(this.threadCount);
	
					try {
						Thread.sleep(this.sleepTime);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				firstFlag = false;
				currentFlowList.clear();
			}

			removeAllFlowsFromNode(node);

			try {
				w = new PrintWriter(resultPath + this.flowCount + "/" + this.topFlows + "/" + this.sleepTime / 1000 + "/result_" + start, "UTF-8");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			parameterString += "\n# Stop: " + (new Date().getTime() - this.creationTime.getTime()) / 1000;
			parameterString += "\n# Total flows per cycle: " + this.flowCount;
			parameterString += "\n# Top n flows per cycle: " + this.topFlows;
			parameterString += "\n# Sleep between cycles: " + this.sleepTime;

			w.println(parameterString);
			w.println(biggestFlowString);
			w.close();

			this.currentCycle++;
			if (this.cycles == this.currentCycle) {
				this.executor.shutdown();
			}

		}
	}
}
