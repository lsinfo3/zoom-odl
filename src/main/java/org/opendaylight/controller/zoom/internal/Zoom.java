package org.opendaylight.controller.zoom.internal;

import java.util.List;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.flowprogrammer.IFlowProgrammerService;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.IReadService;
import org.opendaylight.controller.sal.reader.IReadServiceListener;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.reader.NodeDescription;
import org.opendaylight.controller.sal.reader.NodeTableStatistics;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Zoom implements IReadServiceListener, CommandProvider {
	private static final Logger logger = LoggerFactory
			.getLogger(Zoom.class);
	private ISwitchManager switchManager = null;
	private IFlowProgrammerService programmer = null;
	private IReadService reader = null;
	private IDataPacketService dataPacketService = null;
	
	private IZoom algorithm = null;

	private void registerWithOSGIConsole() {
		BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass())
				.getBundleContext();
		bundleContext.registerService(CommandProvider.class.getName(), this,
				null);
	}

	void setDataPacketService(IDataPacketService s) {
		this.dataPacketService = s;
	}

	void unsetDataPacketService(IDataPacketService s) {
		if (this.dataPacketService == s) {
			this.dataPacketService = null;
		}
	}

	public void setFlowProgrammerService(IFlowProgrammerService s) {
		this.programmer = s;
	}

	public void unsetFlowProgrammerService(IFlowProgrammerService s) {
		if (this.programmer == s) {
			this.programmer = null;
		}
	}

	void setSwitchManager(ISwitchManager s) {
		logger.debug("SwitchManager set");
		this.switchManager = s;
	}

	void unsetSwitchManager(ISwitchManager s) {
		if (this.switchManager == s) {
			logger.debug("SwitchManager removed!");
			this.switchManager = null;
		}
	}

	public void setReaderService(IReadService service) {
		logger.debug("Got inventory service set request {}", service);
		this.reader = service;
	}

	public void unsetReaderService(IReadService service) {
		logger.debug("Got a service UNset request {}", service);
		this.reader = null;
	}

	/**
	 * Function called by the dependency manager when all the required
	 * dependencies are satisfied
	 *
	 */
	void init() {
		logger.info("Initialized");
	}

	/**
	 * Function called by the dependency manager when at least one dependency
	 * become unsatisfied or when the component is shutting down because for
	 * example bundle is being stopped.
	 *
	 */
	void destroy() {
	}

	/**
	 * Function called by dependency manager after "init ()" is called and after
	 * the services provided by the class are registered in the service registry
	 *
	 */
	void start() {
		logger.info("Started");
		this.registerWithOSGIConsole();
	}

	/**
	 * Function called by the dependency manager before the services exported by
	 * the component are unregistered, this will be followed by a "destroy ()"
	 * calls
	 *
	 */
	void stop() {
		logger.info("Stopped");
	}

	public String getHelp() {
		return null;
	}
	
	public void _zoomBaseInteractive(CommandInterpreter ci) {
		this.algorithm = new ZoomBase(switchManager, programmer, reader);
		this.algorithm.start(ci, logger);
	}
	
	public void _zoomTTInteractive(CommandInterpreter ci) {
		this.algorithm = new ZoomTT(switchManager, programmer, reader);
		this.algorithm.start(ci, logger);
	}

	public void _zoomAutomated(CommandInterpreter ci) {
		
		// Path to a pcap trace file that is used for traffic replay
		String path = "path/to/trace/file.pcap";
		
		// The following loops will run all defined tests in sequence
		// This will take a LONG time
		this.algorithm = new ZoomTT(switchManager, programmer, reader);
		{
		int[] nflows = {256};
		int[] ntop = {5, 10, 20, 30, 50};
		int[] twait = {1, 2, 5};
		int[] offset = {5, 10, 20, 30, 40, 50};
		
		for (int _nflows : nflows)
			for (int _ntop : ntop)
				for (int _twait : twait)
					for (int _offset: offset)
						this.algorithm.start(logger, _nflows, _ntop, _twait, _offset, 10, path);
		}
		
		this.algorithm = new ZoomBase(switchManager, programmer, reader);
		{
		int[] nflows = {2};
		int[] ntop = {1};
		int[] twait = {1, 2, 5};
		int[] offset = {5, 10, 20, 30, 40, 50};
		
		for (int _nflows : nflows)
			for (int _ntop : ntop)
				for (int _twait : twait)
					for (int _offset: offset)
						this.algorithm.start(logger, _nflows, _ntop, _twait, _offset, 10, path);
		}
		{
		int[] nflows = {4};
		int[] ntop = {1, 2};
		int[] twait = {1, 2, 5};
		int[] offset = {5, 10, 20, 30, 40, 50};
		
		for (int _nflows : nflows)
			for (int _ntop : ntop)
				for (int _twait : twait)
					for (int _offset: offset)
						this.algorithm.start(logger, _nflows, _ntop, _twait, _offset, 10, path);
		}
		{
		int[] nflows = {16};
		int[] ntop = {1, 4, 8};
		int[] twait = {1, 2, 5};
		int[] offset = {5, 10, 20, 30, 40, 50};
		
		for (int _nflows : nflows)
			for (int _ntop : ntop)
				for (int _twait : twait)
					for (int _offset: offset)
						this.algorithm.start(logger, _nflows, _ntop, _twait, _offset, 10, path);
		}
	}

	public void nodeFlowStatisticsUpdated(Node node,
			List<FlowOnNode> flowStatsList) {
	}

	public void nodeConnectorStatisticsUpdated(Node node,
			List<NodeConnectorStatistics> ncStatsList) {
	}

	public void nodeTableStatisticsUpdated(Node node,
			List<NodeTableStatistics> tableStatsList) {
	}

	public void descriptionStatisticsUpdated(Node node,
			NodeDescription nodeDescription) {
	}
}
