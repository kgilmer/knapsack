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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.felix.cm.impl.ConfigurationManager;
import org.apache.felix.framework.FrameworkFactory;
import org.apache.felix.framework.Logger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.service.log.LogService;
import org.sprinkles.Applier;
import org.sprinkles.functions.FileFunctions;

/**
 * The bootstrap class for Knapsack. Creates and starts a framework with the
 * Knapsack launcher.
 * 
 * @author kgilmer
 * 
 */
public class Bootstrap {
	/**
	 * Felix property to specify the logger instance.
	 */
	private static final String FELIX_LOGGER_INSTANCE = "felix.log.logger";
	
	/**
	 * Felix property to specify bundle instances to run with framework.
	 */
	private static final String FELIX_BUNDLE_INSTANCES = "felix.systembundle.activators";
	

	private static final int PORT_START = 12288;
	private static final int MAX_PORT_RANGE = 64;

	protected static final int DISABLE_SCRIPTS_PORT = -1;

	private static File scriptDir;

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
	public static void main(String[] args) {		
		//Record boot time.
		long time = System.currentTimeMillis();
		
		FrameworkFactory frameworkFactory = new FrameworkFactory();
		Logger logger = new Logger();
		
		int port = DISABLE_SCRIPTS_PORT;
		
		try {
			final File baseDirectory = getBaseDirectory();
			FSHelper.validateFile(baseDirectory, true, true, false, true);
			createKnapsackLayout(baseDirectory, logger);
	
			// Create initial configuration, this will load some values with defaults.
			Config config = new Config(baseDirectory);
			
			if (!config.containsKey(Config.CONFIG_DISABLE_SCRIPTS) || !config.getBoolean(Config.CONFIG_DISABLE_SCRIPTS)) {
				Random r = new Random();
				port = PORT_START + r.nextInt(MAX_PORT_RANGE);
				createKnapsackScripts(baseDirectory, port, config);
			}
			System.setProperty(Config.SYSTEM_PROPERTY_KEY_SHELL_PORT, Integer.toString(port));
	
			// Create activators that will start
			List<BundleActivator> activators = new ArrayList<BundleActivator>();
	
			if (config.getBoolean(Config.CONFIG_KEY_BUILTIN_LOGGER))
				activators.add(new org.apache.felix.log.Activator());
	
			if (config.getBoolean(Config.CONFIG_KEY_BUILTIN_CONFIGADMIN))
				activators.add(new ConfigurationManager());
	
			// Create an internal logger that will be used for log output before LogService takes over.
			activators.add(new org.knapsack.Activator(config, logger, port));
	
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
						
						FSHelper.deleteFilesInDir(scriptDir);
					} catch (Exception ex) {
						System.err.println("Error stopping framework: " + ex);
					}
				}
			});
			
			framework.init();
			framework.start();
			logger.log(LogService.LOG_INFO, "Framework started in " + ((double) (System.currentTimeMillis() - time) / 1000) + " seconds with activators: " + activators);
		} catch (Exception e) {
			logger.log(LogService.LOG_INFO, "Framework encountered a critical error and failed to start: ", e);
			System.exit(1);
		}
	}
	
	/**
	 * @return root directory that this instance of knapsack runs in.
	 */
	private static File getBaseDirectory() {
		if (System.getProperty(Config.CONFIG_KEY_ROOT_DIR) != null)
			return new File(System.getProperty(Config.CONFIG_KEY_ROOT_DIR));
		
		return new File(System.getProperty("user.dir"));
	}

	/**
	 * Create the directory layout for knapsack.
	 * 
	 * [root]/cache   	 - Default Bundle Cache directory
	 * [root]/default 	 - Default system and ConfigAdmin properties
	 * [root]/bundle  	 - Default bundle directory
	 * [root]/bin 	  	 - Default script directory
	 * [root]/felix.conf - Launch properties for Felix
	 * 
	 * @param baseDirectory
	 * @param logger
	 * @param port
	 * @throws IOException
	 */
	private static void createKnapsackLayout(File baseDirectory, Logger logger) throws IOException {
		File confFile = new File(baseDirectory, Config.CONFIGURATION_FILENAME);
		
		File defaultDir = new File(baseDirectory, Config.DEFAULT_DIRECTORY_NAME);
		FSHelper.validateFile(defaultDir, true, true, false, true);
		//Load default properties before creating/loading global conf file.  This allows for simple modification by installers, bundles.
		if (FSHelper.directoryHasFiles(defaultDir)) 
			Applier.map(Applier.map(defaultDir, FileFunctions.GET_FILES_FN), new LoadPropertiesFunction(logger));
		
		if (!confFile.exists()) {
			//Create a default configuration
			logger.log(LogService.LOG_INFO, "Creating new default configuration file: " + confFile);
			FSHelper.copyDefaultConfiguration(Config.CONFIGURATION_RESOURCE_FILENAME, confFile, baseDirectory);
		}
				
		File configAdminDir = new File(baseDirectory, Config.CONFIGADMIN_DIRECTORY_NAME);
		FSHelper.validateFile(configAdminDir, true, true, false, true);
	}
	
	/**
	 * Create the transient script symlinks used to access the knapsack shell from the native environment.
	 * 
	 * @param baseDirectory
	 * @param port
	 * @param config
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	private static void createKnapsackScripts(File baseDirectory, int port, Config config) throws IOException, URISyntaxException {
		scriptDir = new File(baseDirectory, Config.SCRIPT_DIRECTORY_NAME);
		FSHelper.validateFile(scriptDir, true, true, false, true);
		
		if (FSHelper.directoryHasFiles(scriptDir))
			FSHelper.deleteFilesInDir(scriptDir);
		
		FSHelper.copyScripts(baseDirectory, port, config.getProperty(Config.CONFIG_KEY_SHELL_COMMAND));
	}
}