package org.opendaylight.controller.zoom.internal;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.slf4j.Logger;

public interface IZoom {
	
	public void start(CommandInterpreter ci, Logger logger);
	public void start(Logger logger, int eFlowCount, int eTopFlows, int eSleepTime, int eOffset, int eCycles, String path);
	
}
