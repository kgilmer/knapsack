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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.apache.felix.cm.impl.ConfigurationManager;
import org.apache.felix.framework.FrameworkFactory;
import org.knapsack.init.KnapsackInitServiceImpl;
import org.knapsack.init.pub.KnapsackInitService;
import org.knapsack.shell.CommandParser;
import org.knapsack.shell.ConsoleSocketListener;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.launch.Framework;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.sprinkles.Applier;
import org.sprinkles.functions.FileFunctions;

/**
 * Entry point to Knapsack.  The static main() method runs the knapsack startup process and initializes Felix.
 * 
 * @author kgilmer
 *
 */
public class Launcher {
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

	private static KnapsackLogger logger;

	private static BundleContext context;

	/**
	 * Main entry point into knapsack.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		List<String> argList = Arrays.asList(args);
		
		if (argList.contains("-v") || argList.contains("--version")) {
			printVersion();
			return;
		}
		
		if (argList.contains("-h") || argList.contains("--help") || argList.size() > 1) {
			printHelp();
			return;
		}
		
		if (argList.size() == 1) {
			System.setProperty(ConfigurationConstants.CONFIG_KEY_ROOT_DIR, argList.get(0));
		}
			
		// Record boot time.
		final long time = System.currentTimeMillis();

		// Create the pre-OSGi logger instance for logging outside of the OGSi
		// context.
		logger = new KnapsackLogger();

		// Determine the root directory from where we run.
		final File baseDirectory = getBaseDirectory();
		try {
			FSHelper.validateFile(baseDirectory, true, true, false, true);

			// Set initial directories
			final File defaultDirectory = new File(baseDirectory, ConfigurationConstants.DEFAULT_DIRECTORY_NAME);
			final File scriptDirectory = new File(baseDirectory, ConfigurationConstants.SCRIPT_DIRECTORY_NAME);

			if (baseDirectoryUninitialized(baseDirectory)) 
				createDefaultConfigurationFiles(baseDirectory);

			// Load the system properties files from files within the /default
			// directory.
			loadProperties(defaultDirectory, logger);		

			// From this point we consider all the relevant system properties
			// have been loaded for Felix.
			// Set the logger output level if configured
			if (System.getProperties().containsKey("felix.log.level"))
				logger.setLogLevel(Integer.parseInt(System.getProperties().getProperty("felix.log.level")));
			
			logger.setLogStdout(PropertyHelper.getBoolean(ConfigurationConstants.CONFIG_KEY_LOG_STDOUT));

			// Create activators that will start
			final List<BundleActivator> activators = createBundles();

			// Create the Properties file used to initialize Felix
			final Properties felixConfig = createFelixProperties(defaultDirectory, logger, activators);

			// Create and initialize the Felix framework
			final Framework felix = (new FrameworkFactory()).newFramework(felixConfig);
			felix.init();
			
			context = felix.getBundleContext();
			
			// LogService should now be loaded, setup logger so all log output goes to stdout
			if (PropertyHelper.getBoolean(ConfigurationConstants.CONFIG_KEY_LOG_STDOUT)) 				
				addLogReadersToLogger(logger, context);
			
			// ConfigAdmin should now be loaded, setup defaults.
			if (PropertyHelper.getBoolean(ConfigurationConstants.CONFIG_KEY_BUILTIN_CONFIGADMIN))
				initializeConfigAdmin(felix.getBundleContext(), getConfigAdminDirectory(baseDirectory), logger);
			
			// Create the scripts for access from the native shell.
			ConsoleSocketListener shell = null;
			if (!PropertyHelper.getBoolean(ConfigurationConstants.CONFIG_DISABLE_SCRIPTS)) {
				int port = generatePort();
				shell = new ConsoleSocketListener(
						port, context, logger, new CommandParser(context, scriptDirectory));
				shell.start();
				createKnapsackScripts(scriptDirectory, port);
			}
			
			KnapsackInitServiceImpl serviceImpl = new KnapsackInitServiceImpl(baseDirectory);
			serviceImpl.updateBundlesSync();
			
			ServiceRegistration initSR = context.registerService(KnapsackInitService.class.getName(), serviceImpl, null);
			Runtime.getRuntime().addShutdownHook(new KnapsackShutdownHook(felix, scriptDirectory, shell, initSR));
			
			felix.start();
			
			logger.log(LogService.LOG_INFO, "Knapsack " + getKnapsackVersion() + " for Apache Felix " + getFelixVersion(context) + " (" + baseDirectory + ") started in " + ((double) (System.currentTimeMillis() - time) / 1000) + " seconds.");
		} catch (Exception e) {
			logger.log(LogService.LOG_ERROR, "Unable to start knapsack.", e);
			System.exit(1);
		}
	}
	
	// /***************** Private helper methods
	
	/**
	 * Print command usage information.
	 */
	private static void printHelp() {
		System.out.println("Usage: knapsack.jar [-v|--version] [-h|--help] [root directory]");
	}

	/**
	 * Print knapsack version.
	 */
	private static void printVersion() {
		System.out.println("Knapsack version " + getKnapsackVersion());
	}

	/**
	 * Get the version string from the build of the version of Knapsack.
	 * @param context2
	 * @return
	 */
	private static String getKnapsackVersion() {
		try {
			InputStream istream = Launcher.class.getResourceAsStream("knapsack.version");
			
			Properties p = new Properties();
			p.load(istream);
			
			return p.getProperty("knapsack.version");
		} catch (Exception e) {
			//To be consistent with Felix
			return "0.0.0";
		}
	}

	/**
	 * The the version of Apache Felix from the system bundle.
	 * @param c
	 * @return
	 */
	private static String getFelixVersion(BundleContext c) {
		return c.getBundle(0).getVersion().toString();
	}

	/**
	 * Attach the KnapsackLogger to all LogReader services available.
	 * 
	 * @param logger
	 * @param context
	 */
	private static void addLogReadersToLogger(final KnapsackLogger logger, final BundleContext context) {
		ServiceTracker st = new ServiceTracker(context, LogReaderService.class.getName(), new ServiceTrackerCustomizer() {
			
			@Override
			public void removedService(ServiceReference reference, Object service) {
				logger.removeLogReader((LogReaderService) service);
			}
			
			@Override
			public void modifiedService(ServiceReference reference, Object service) {				
			}
			
			@Override
			public Object addingService(ServiceReference reference) {
				Object svc = context.getService(reference);
				
				logger.addLogReader((LogReaderService) svc);
				
				return svc;
			}
		});		
		st.open();
	}

	/**
	 * Get the directory configured to store config admin data.
	 * 
	 * @param baseDirectory
	 * @return
	 */
	private static File getConfigAdminDirectory(File baseDirectory) {
		if (System.getProperty("felix.cm.dir") == null)
			System.setProperty("felix.cm.dir", System.getProperty(ConfigurationConstants.CONFIG_KEY_ROOT_DIR) + ConfigurationConstants.CONFIGADMIN_DIRECTORY_NAME);
		
		return new File(System.getProperty("felix.cm.dir"));
	}

	/**
	 * Initialize the ConfigAdmin Configurations with any PID files stored in the properties directory.
	 * 
	 * @param bundleContext
	 * @param configAdminDir
	 * @param logger
	 */
	private static void initializeConfigAdmin(BundleContext bundleContext, File configAdminDir, KnapsackLogger logger) {
		ServiceReference sr = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
		
		if (sr != null) {
			ConfigurationAdmin ca = (ConfigurationAdmin) bundleContext.getService(sr);
			
			if (ca != null) {
				Applier.map(
						Applier.map(configAdminDir, FileFunctions.GET_FILES_FN)
							, new LoadDefaultsFunction(ca, logger, PropertyHelper.getBoolean(ConfigurationConstants.CONFIG_KEY_OVERWRITE_CONFIGADMIN)));
				
				return;
			}			
		}
		
		logger.log(LogService.LOG_WARNING, "Unable to access ConfigurationAdmin.");
	}


	/**
	 * Create the Properties file for Felix to launch.
	 * 
	 * @param defaultDirectory
	 * @param logger
	 * @param activators
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static Properties createFelixProperties(File defaultDirectory, KnapsackLogger logger, List<BundleActivator> activators) throws FileNotFoundException, IOException {
		Properties felixConfig = new Properties();
		felixConfig.load(getFelixConfigFileInputStream(defaultDirectory));
		felixConfig.put(FELIX_LOGGER_INSTANCE, logger);
		felixConfig.put(FELIX_BUNDLE_INSTANCES, activators);

		return felixConfig;
	}

	/**
	 * Get the Felix configuration file as an InputStream.
	 * 
	 * @param defaultDirectory
	 * @return
	 * @throws FileNotFoundException
	 */
	private static InputStream getFelixConfigFileInputStream(File defaultDirectory) throws FileNotFoundException {
		return new FileInputStream(new File(defaultDirectory, ConfigurationConstants.CONFIGURATION_FILENAME[0]));
	}

	/**
	 * Create the bundled bundles that will start along with the Framework.
	 * 
	 * @return
	 */
	private static List<BundleActivator> createBundles() {
		List<BundleActivator> activators = new ArrayList<BundleActivator>();

		if (PropertyHelper.getBoolean(ConfigurationConstants.CONFIG_KEY_BUILTIN_LOGGER))
			activators.add(new org.apache.felix.log.Activator());

		if (PropertyHelper.getBoolean(ConfigurationConstants.CONFIG_KEY_BUILTIN_CONFIGADMIN))
			activators.add(new ConfigurationManager());

		return activators;
	}

	/**
	 * Create the transient script symlinks used to access the knapsack shell
	 * from the native environment.
	 * 
	 * @param baseDirectory
	 * @param port
	 * @param config
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	private static void createKnapsackScripts(File scriptDir, int port) throws IOException, URISyntaxException {
		FSHelper.validateFile(scriptDir, true, true, false, true);

		if (FSHelper.directoryHasFiles(scriptDir))
			FSHelper.deleteFilesInDir(scriptDir);

		FSHelper.copyScripts(scriptDir, port, System.getProperties().getProperty(ConfigurationConstants.CONFIG_KEY_SHELL_COMMAND));
		System.setProperty(ConfigurationConstants.SYSTEM_PROPERTY_KEY_SHELL_PORT, Integer.toString(port));
	}

	/**
	 * Generate a random port within a specified range for the knapsack shell to work with.
	 * 
	 * @return
	 */
	private static int generatePort() {
		Random r = new Random();
		return PORT_START + r.nextInt(MAX_PORT_RANGE);
	}

	/**
	 * Load default properties.
	 * 
	 * @param baseDirectory
	 * @param logger
	 * @throws IOException
	 */
	private static void loadProperties(File baseDirectory, KnapsackLogger logger) throws IOException {
		Applier.map(Applier.map(baseDirectory, FileFunctions.GET_FILES_FN), new LoadPropertiesFunction(logger));
	}

	/**
	 * Create the default configuration files for knapsack, felix, and starting bundles.
	 * 
	 * @param baseDirectory
	 * @throws IOException
	 */
	private static void createDefaultConfigurationFiles(File baseDirectory) throws IOException {
		File defaultDir = new File(baseDirectory, ConfigurationConstants.DEFAULT_DIRECTORY_NAME);

		FSHelper.validateFile(defaultDir, true, true, false, true);

		for (String filename : Arrays.asList(ConfigurationConstants.CONFIGURATION_FILENAME))
			FSHelper.copyResourceToFile("/" + filename, new File(defaultDir, filename));
	}

	/**
	 * @param baseDirectory
	 * @return true if the knapsack configuration files and directory layout
	 *         should be created.
	 */
	private static boolean baseDirectoryUninitialized(File baseDirectory) {

		return !(new File(baseDirectory, ConfigurationConstants.DEFAULT_DIRECTORY_NAME)).exists();
	}

	/**
	 * @return root directory that this instance of knapsack runs in.
	 */
	private static File getBaseDirectory() {
		if (System.getProperty(ConfigurationConstants.CONFIG_KEY_ROOT_DIR) == null)
			System.setProperty(ConfigurationConstants.CONFIG_KEY_ROOT_DIR, System.getProperty("user.dir"));		

		return new File(System.getProperty(ConfigurationConstants.CONFIG_KEY_ROOT_DIR));
	}

	/**
	 * @return instance of internal Logger class.
	 */
	public static KnapsackLogger getLogger() {	
		return logger;
	}

	/**
	 * @return bundle context or throw RuntimeException() if does not exist.
	 */
	public static BundleContext getBundleContext() {
		if (context == null)
			throw new RuntimeException("BundleContext is not available.");
		
		return context;
	}
}
