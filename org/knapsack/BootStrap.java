package org.knapsack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.cm.impl.ConfigurationManager;
import org.apache.felix.framework.FrameworkFactory;
import org.apache.felix.framework.Logger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.service.log.LogService;

/**
 * The bootstrap class for Knapsack.  Creates and starts a framework with the Knapsack launcher.
 * 
 * @author kgilmer
 *
 */
public class BootStrap {

	public static void main(String[] args) throws IOException, BundleException {
		long time = System.currentTimeMillis();
		FrameworkFactory frameworkFactory = new FrameworkFactory();
		
		//Create initial configuration
		Map<String, Object> config = new HashMap<String, Object>(Config.getRef().asMap());
		
		Logger logger = new Logger();
		
		//Create activators that will start
		List<BundleActivator> activators = new ArrayList<BundleActivator>();
		
		if (config.containsKey(Config.CONFIG_KEY_BUILTIN_LOGGER) && Config.getRef().getBoolean(Config.CONFIG_KEY_BUILTIN_LOGGER)) 
			activators.add(new org.apache.felix.log.Activator());
		
		if (config.containsKey(Config.CONFIG_KEY_BUILTIN_CONFIGADMIN) && Config.getRef().getBoolean(Config.CONFIG_KEY_BUILTIN_CONFIGADMIN))
			activators.add(new ConfigurationManager());
		
		activators.add(new org.knapsack.Activator(logger));
		
		config.put("felix.log.logger", logger);
		config.put("felix.systembundle.activators", activators);
		
		Framework framework = frameworkFactory.newFramework(config);
		framework.init();
		framework.start();
		logger.log(LogService.LOG_INFO, "Framework started in " + ((double) (System.currentTimeMillis() - time) / 1000) + " seconds with activators: " + activators);		
	}
}
