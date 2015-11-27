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

public class ZoomTT implements IZoom {

	private ISwitchManager switchManager;
	private IFlowProgrammerService programmer;
	private IReadService reader;
	private Logger logger;
	private String sudoPassword = "Ork45_re";
	private String intf1 = "s1";
	
	public ZoomTT(ISwitchManager switchManager, IFlowProgrammerService programmer, IReadService reader) {
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
		
		int flowsPerTopFlow = flowCount;

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
		case 256:
			stepCountPerByte = 1;
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

		int flowCount = 256;
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
			} else if (current.equals("-file")) {
				path = ci.nextArgument();
			} else {
				ci.println("Unknown parameter " + current + ". Ignored.");
			}
		}

		sleepTime = sleepTime * 1000;

		if (!(flowCount == 2 || flowCount == 4 || flowCount == 16 || flowCount == 256)) {
			ci.println("Invalid number of flows! (-nflows VALUE)\nValid values are 2 (default), 4, 16, 256.\nExit.");
			return;
		} else if (topFlows > flowCount) {
			ci.println("The number top flows has to be smaller than the total number of flows.\nExit");
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
		
		int flowsPerTopFlow = flowCount;

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
						
			IPv4 localCurrentIP = null;
			
			Set<Node> nodeSet = switchManager.getNodes();
			Node node = (Node) nodeSet.toArray()[0]; // TODO: Experimental!! Only works if just one node is present
			
			long start = (new Date().getTime() - this.creationTime.getTime()) / 1000;
			
			String resultPath = "results/ZoomTT/";
			File theDir = new File(resultPath + this.flowCount + "/" + this.topFlows + "/" + this.sleepTime / 1000 + "/");

			if (!theDir.exists()) {
				try {
					theDir.mkdirs();
				} catch (SecurityException se) {
					se.printStackTrace();
				}
			}

			List<Flow> flowList = new LinkedList<Flow>();

			boolean firstFlag = true;
			boolean changeFlag = true;

			this.flowCounter = 0;

			String parameterString = "# Start: " + start;
			
			
			firstFlag = false;
			for (int i = 1; i <= 255; i++) {
				IPv4 newIP = new IPv4(i + ".0.0.0", "255.0.0.0");
				Flow f = generateSrcFlow(newIP.getIP(), newIP.getNetmask());
				f.setId(this.flowCounter++);
				flowList.add(f);
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

				for (int k = 1; k <= this.topFlows && k <= currentFlowList.size(); k++) {
					
					if (!firstFlag) {
						FlowOnNode biggestFlow = findBiggestFlow(currentFlowList);
						if (biggestFlow == null) {
							break;
						}

						localCurrentIP = extractSrcFromFlow(biggestFlow);

						biggestFlowString += "\n" + localCurrentIP.getIP() + ";" + localCurrentIP.getNetmask() + ";" + biggestFlow.getByteCount()*8 / (this.sleepTime / 1000);

					}

					int flowCountPerIP = this.flowCount;
					int shiftwidth = (int) (Math.log(flowCountPerIP) / Math.log(2));

					IPv4 srcOne = localCurrentIP;
					
					Flow f;

					if (localCurrentIP.getNumberOfHosts() > 1) {
						
						// NEW
						int numberOfFlows = flowCountPerIP; 
						
						int nmb = localCurrentIP.getNetmaskByte();
						int newSrcNetmask = localCurrentIP.getNumericNetmask() >> shiftwidth;
						int[] newSrcNetmaskBytes = ip2int(convertNumericIpToSymbolic(newSrcNetmask));
						// debug(resultPath + "debug", "SrcBefore: " + convertNumericIpToSymbolic(newSrcNetmask));
						if (nmb < 3 && newSrcNetmaskBytes[nmb+1] != 0) {
							int[] tmpMask = new int[4];
							for (int q = 0; q < 4; q++) {
								if (q <= nmb)
									tmpMask[q] = 255;
								else
									tmpMask[q] = 0;
							}
							IPv4 tmpIP = new IPv4(localCurrentIP.getIP(), ip2string(tmpMask));
							newSrcNetmask = tmpIP.getNumericNetmask();
							numberOfFlows = 256 - ip2int(localCurrentIP.getNetmask())[nmb];
						}
						// debug(resultPath + "debug", "SrcAfter: " + convertNumericIpToSymbolic(newSrcNetmask));
						// UNTIL HERE
						srcOne = new IPv4(localCurrentIP.getIP(), numeric2Symbolic(newSrcNetmask));

						changeFlag = true;

						int[] srcOneIP = ip2int(srcOne.getIP());
						int[] srcOneNetmask = ip2int(srcOne.getNetmask());

						int[] srcIP = srcOneIP.clone();

						boolean netmaskJumperFlag = false;
						int temp = localCurrentIP.getNetmaskByte();
						for (int j = 1; j < numberOfFlows; j++) {
							for (int i = 0; i < 4; i++) {
								if (i == temp) {
									srcIP[i] = srcIP[i] + 256 - srcOneNetmask[i];
									if (!netmaskJumperFlag && srcOneNetmask[i] == 255) {
										localCurrentIP.increaseNetmaskByte();
										netmaskJumperFlag = true;
									}
								}
							}
							IPv4 newIP = new IPv4(ip2string(srcIP), numeric2Symbolic(newSrcNetmask));
							f = generateSrcFlow(newIP.getIP(), newIP.getNetmask());
							f.setId(this.flowCounter++);
							flowList.add(f);
						}
					}
					
					f = generateSrcFlow(srcOne.getIP(), srcOne.getNetmask());
					f.setId(this.flowCounter++);
					flowList.add(f);

					if (firstFlag)
						break;
					

				}
				
				removeAllFlowsFromNode(node);
				
				if (changeFlag) {
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
