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
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.knapsack.init.pub.KnapsackInitService;
import org.knapsack.shell.CommandParser;
import org.knapsack.shell.ConsoleSocketListener;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;
import org.sprinkles.Applier;
import org.sprinkles.functions.FileFunctions;

/**
 * Activator for Knapsack.  Is not typically called as a regular bundle but via the BootStrap
 * class as part of system bundle.
 * 
 * @author kgilmer
 *
 */
public class Activator implements BundleActivator, ManagedService, LogService {	
	/**
	 * Store all the bundle sizes at time of install to compare later for updates.
	 */
	private static HashMap<File, Long> sizeMap;
	
	/**
	 * This should be in sync with manifest version.
	 */
	public static final String KNAPSACK_VERSION = "0.8.4";
	public static final String KNAPSACK_PID = "org.knapsack";
	
	/**
	 * Static reference to BundleContext
	 */
	private static BundleContext context;
	/**
	 * Static reference to Config
	 */
	private static Config config;
	/**
	 * This is set to true by the BootStrap class.  This allows the knapsack bundle to know if it's part of the bootstrap or being started as a regular OSGi bundle.
	 */
	private boolean embeddedMode = false;

	private final int port;
	/**
	 * Static self reference for logging.
	 */
	private static Activator ref;

	/**
	 * Default constructor for running as a normal bundle.
	 * 
	 * @throws IOException
	 */
	public Activator() throws IOException {
		ref = this;
		Activator.frameworkLogger = null;
		this.port = Bootstrap.DISABLE_SCRIPTS_PORT;
		Activator.config = null;
	}
	
	/**
	 * Constructor for running as an embedded bundle in the Knapsack bootstrap.
	 * 
	 * @param logger instance of framework logger.
	 * @throws IOException Upon configuration error.
	 * @throws InterruptedException Upon interruption.
	 */
	public Activator(Config config, Logger logger, int port) throws IOException, InterruptedException {
		ref = this;
		this.port = port;
		Activator.frameworkLogger = logger;
		embeddedMode  = true;
		Activator.config = config;
	}

	/**
	 * @return BundleContext
	 */
	public static BundleContext getContext() {
		return context;
	}

	private ServiceRegistration managedServiceRef;
	/**
	 * Socket for shell interface.
	 */
	private ConsoleSocketListener shell;

	private ServiceRegistration initSR;
	/**
	 * Instance of framework logger.
	 */
	private static Logger frameworkLogger;
	/**
	 * Instance of LogService from OSGi service registry.
	 */
	private static LogService logService;

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		
		//Register knapsack for configAdmin updates
		managedServiceRef = bundleContext.registerService(ManagedService.class.getName(), this, getManagedServiceProperties());
			
		//Load configuration admin with defaults if no pre-existing state exists.
		if (embeddedMode) {
			ServiceReference sr = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
			if (sr != null) {
				ConfigurationAdmin ca = (ConfigurationAdmin) bundleContext.getService(sr);
				loadDefaults(new File(config.get(Config.CONFIG_KEY_ROOT_DIR).toString(), Config.CONFIGADMIN_DIRECTORY_NAME), ca);
			}
		}
		
		if (config.getBoolean(Config.CONFIG_KEY_LOG_STDOUT)) {
			int logLevel = 4;
			if (config.contains("felix.log.level"))
				logLevel = Integer.parseInt(config.getProperty("felix.log.level"));
			
			new LogPrinter(context, logLevel);
			
		}
		
		sizeMap = new HashMap<File, Long>();
		File baseDir = new File(config.getString(Config.CONFIG_KEY_ROOT_DIR));
		
		FSHelper.validateFile(baseDir, false, true, false, true);
	
		if (port != Bootstrap.DISABLE_SCRIPTS_PORT) {
			shell = new ConsoleSocketListener(config, port, context, this, new CommandParser(context, new File(baseDir, Config.SCRIPT_DIRECTORY_NAME)));
			shell.start();
		} else {
			log(LogService.LOG_INFO, "Knapsack shell is disabled.");
		}
		
		KnapsackInitServiceImpl serviceImpl = new KnapsackInitServiceImpl(baseDir, config);
		serviceImpl.updateBundlesSync();
		
		initSR = context.registerService(KnapsackInitService.class.getName(), serviceImpl, null);

		log(LogService.LOG_INFO, "Knapsack " + KNAPSACK_VERSION + " running in " + config.get(Config.CONFIG_KEY_ROOT_DIR));
	
	}
	
	/**
	 * @return The dictionary to bind Knapsack's configuration into configadmin.
	 */
	private Dictionary getManagedServiceProperties() {
		Dictionary d = new Properties();
		d.put(Constants.SERVICE_PID, KNAPSACK_PID);
		return d;
	}

	/**
	 * Load defaults for ConfigAdmin.
	 * 
	 * @param defaultDir Directory where default configurations are stored.
	 * @param ca Instance of ConfigAdmin
	 */
	private void loadDefaults(File defaultDir, ConfigurationAdmin ca) {

		Applier.map(
				Applier.map(defaultDir, FileFunctions.GET_FILES_FN)
				, new LoadDefaultsFunction(ca, frameworkLogger, config.getBoolean(Config.CONFIG_KEY_OVERWRITE_CONFIGADMIN)));
	}


	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {	
		if (initSR != null)
			initSR.unregister();
		
		if (shell != null)
			shell.shutdown();
			
		managedServiceRef.unregister();
	
		Activator.context = null;
	}
	
	@Override
	public void log(int level, String message) {
		if (logService == null) {
			ServiceReference rs = context.getServiceReference(LogService.class.getName());
			
			if (rs != null) {
				logService = (LogService) context.getService(rs);			
			}
		}
		
		if (logService != null)
			logService.log(level, message);
		else if (frameworkLogger != null)
			frameworkLogger.log(level, message);
		else 
			Logger.doKnapsackLog(null, null, level, message, null);		
	}
	
	/**
	 * Log an error.  Will log in order of availability: LogService, Framework Logger, System.out.
	 * @param level
	 * @param message
	 * @param error
	 */
	@Override
	public void log(int level, String message, Throwable error) {
		if (logService == null) {
			ServiceReference rs = context.getServiceReference(LogService.class.getName());
			
			if (rs != null) {
				logService = (LogService) context.getService(rs);			
			}
		}
			
		if (logService != null) 
			logService.log(level, message, error);
		else if (frameworkLogger != null)  
			frameworkLogger.log(level, message, error);
		else 
			Logger.doKnapsackLog(null, null, level, message, error);
	}

	public void setLogger(Logger logger) {
		Activator.frameworkLogger = logger;
	}
	
	/**
	 * @return Map of cache of loaded bundle sizes.
	 */
	public static Map<File, Long> getBundleSizeMap() {
		return sizeMap;
	}

	@Override
	public void updated(Dictionary properties) throws ConfigurationException {
		if (properties == null)
			return;
		
		Enumeration i = properties.keys();
			
		while (i.hasMoreElements()) {
			Object key = i.nextElement();
			String val = properties.get(key).toString();
			
			config.put(key, val);
		}
	}

	@Override
	public void log(ServiceReference servicereference, int level, String message) {
		log(level, message);
	}

	@Override
	public void log(ServiceReference servicereference, int level, String message, Throwable exception) {
		log(level,message, exception);
	}

	/**
	 * Convenience method to log a an error.
	 * 
	 * @param message
	 */
	public static void logError(String message) {
		ref.log(LogService.LOG_ERROR, message);
	}

	/**
	 * Convenience method to log a warning.
	 * 
	 * @param message
	 */
	public static void logInfo(String message) {
		ref.log(LogService.LOG_INFO, message);
	}

	/**
	 * Convenience method to log a warning.
	 * 
	 * @param message
	 */
	public static void logWarning(String message) {
		ref.log(LogService.LOG_WARNING, message);
	}

	/**
	 * Convenience method to log a debug message.
	 * 
	 * @param message
	 */
	public static void logDebug(String message) {
		ref.log(LogService.LOG_DEBUG, message);
	}

	/**
	 * Convenience method to log an error.
	 * 
	 * @param message
	 */
	public static void logError(String message, Exception e) {
		ref.log(LogService.LOG_ERROR, message, e);
	}

	public static Config getConfig() {
		return config;
	}
}
