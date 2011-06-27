/*
 *    Copyright 2011 Ken Gilmer
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.knapsack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.cm.impl.ConfigurationManager;
import org.apache.felix.framework.FrameworkFactory;
import org.apache.felix.framework.Logger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.service.log.LogService;

/**
 * The bootstrap class for Knapsack. Creates and starts a framework with the
 * Knapsack launcher.
 * 
 * @author kgilmer
 * 
 */
public class BootStrap {
	/**
	 * Felix property to specify the logger instance.
	 */
	private static final String FELIX_LOGGER_INSTANCE = "felix.log.logger";
	
	/**
	 * Felix property to specify bundle instances to run with framework.
	 */
	private static final String FELIX_BUNDLE_INSTANCES = "felix.systembundle.activators";

	/**
	 * Entry point for the knapsack.  Creates a felix framework and attaches the Log, ConfigAdmin, and Knapsack bundles.  Registers a shutdown hook to cleanup.
	 * 
	 * Based on the example provided at:
	 * http://felix.apache.org/site/apache-felix-framework-launching-and-embedding.html
	 * 
	 * @param args
	 * @throws IOException
	 * @throws BundleException
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, BundleException, InterruptedException {
		//Record boot time.
		long time = System.currentTimeMillis();
		FrameworkFactory frameworkFactory = new FrameworkFactory();

		// Create initial configuration, this will load some values with defaults.
		Config config = Config.getRef();	

		// Create activators that will start
		List<BundleActivator> activators = new ArrayList<BundleActivator>();

		if (config.getBoolean(Config.CONFIG_KEY_BUILTIN_LOGGER))
			activators.add(new org.apache.felix.log.Activator());

		if (config.getBoolean(Config.CONFIG_KEY_BUILTIN_CONFIGADMIN))
			activators.add(new ConfigurationManager());

		// Create an internal logger that will be used for log output before LogService takes over.
		Logger logger = new Logger();
		
		activators.add(new org.knapsack.Activator(logger));

		config.put(FELIX_LOGGER_INSTANCE, logger);
		config.put(FELIX_BUNDLE_INSTANCES, activators);

		final Framework framework = frameworkFactory.newFramework(config);
		Runtime.getRuntime().addShutdownHook(new Thread("Felix Shutdown Hook") {
			public void run() {
				try {
					if (framework != null) {
						framework.stop();
						framework.waitForStop(0);
					}
				} catch (Exception ex) {
					System.err.println("Error stopping framework: " + ex);
				}
			}
		});
		
		framework.init();
		framework.start();
		logger.log(LogService.LOG_INFO, "Framework started in " + ((double) (System.currentTimeMillis() - time) / 1000) + " seconds with activators: " + activators);
	}
}
