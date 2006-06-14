package org.apache.felix.ipojo.plugin;

import java.util.logging.Level;
import java.util.logging.Logger;

public class IpojoPluginConfiguration {

	private static Logger logger;
	
	public static Level logLevel = Level.WARNING; 
	
	public static Logger getLogger() {
		if (logger == null) {
			String name = "org.apache.felix.ipojo.tools.plugin";
			logger = Logger.getLogger(name);
			logger.setLevel(logLevel);
		}
		return logger;
	}
	
}
