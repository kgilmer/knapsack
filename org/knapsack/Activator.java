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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.framework.Logger;
import org.knapsack.in.KnapsackReaderOutput;
import org.knapsack.in.PipeReaderThread;
import org.knapsack.init.InitThread;
import org.knapsack.out.KnapsackWriterInput;
import org.knapsack.out.PipeWriterThread;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;
import org.sprinkles.Fn;
import org.sprinkles.functions.ReturnFilesFunction;

/**
 * Activator for Knapsack.  Is not typically called as a regular bundle but via the BootStrap
 * class as part of system bundle.
 * 
 * @author kgilmer
 *
 */
public class Activator implements BundleActivator, FrameworkListener {
	/**
	 * Filename for read-only pipe.
	 */
	protected static final String INFO_FILENAME = "info";
	/**
	 * Filename for write-only pipe.
	 */
	protected static final String CONTROL_FILENAME = "control";
	/**
	 * Filename for defaults directory.
	 */
	protected static final String DEFAULT_FILENAME = "default";
	
	/**
	 * Filename for config admin directory.
	 */
	public static final String CONFIGADMIN_FILENAME = "configAdmin";
	
	/**
	 * Store all the bundle sizes at time of install to compare later for updates.
	 */
	private static HashMap<File, Long> sizeMap;
	
	/**
	 * This should be in sync with manifest version.
	 */
	public static final String KNAPSACK_VERSION = "0.3.0";
	
	private static BundleContext context;
	private static Config config;
	private boolean embeddedMode = false;

	public Activator() throws IOException {
		Activator.frameworkLogger = null;
	}
	
	public Activator(Logger logger) throws IOException {
		Activator.frameworkLogger = logger;
		embeddedMode  = true;
		config = Config.getRef();
		if (getInfoFile().exists() || getControlFile().exists())
			throw new IOException("Pipe already exists in " + config.getString(Config.CONFIG_KEY_ROOT_DIR) + ".  This means a framework is already running or has crashed in the same directory.  Shutdown existing framework or manually remove " + getInfoFile() + " and " +  getControlFile() + ", then run again.");	
	}

	public static BundleContext getContext() {
		return context;
	}

	/**
	 * Thread for write-only pipe.
	 */
	private PipeWriterThread writer;

	/**
	 * Thread for read-only pipe.
	 */
	private PipeReaderThread reader;
	/**
	 * Non-persistent thread for bundle initialization.
	 */
	private InitThread init;
	private static Logger frameworkLogger;
	private static LogService logService;

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		
		if (config == null)
			config = Config.getRef();
		
		if (config.getBoolean(Config.CONFIG_KEY_LOG_STDOUT))
			new LogPrinter(bundleContext);
		
		sizeMap = new HashMap<File, Long>();
		bundleContext.addFrameworkListener(this);
		
		writer = new PipeWriterThread(getInfoFile(), new KnapsackWriterInput());
		reader = new PipeReaderThread(getControlFile(), new KnapsackReaderOutput());
		init = new InitThread(new File(config.getString(Config.CONFIG_KEY_ROOT_DIR)), Arrays.asList(config.getString(Config.CONFIG_KEY_BUNDLE_DIRS).split(",")));
		
		if (embeddedMode && defaultDirExists()) {
			ServiceReference sr = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
			if (sr != null) {
				ConfigurationAdmin ca = (ConfigurationAdmin) bundleContext.getService(sr);
				loadDefaults(getDefaultDir(), ca);
			}
		}
	}
	
	private void loadDefaults(File defaultDir, ConfigurationAdmin ca) {

		Fn.map(new LoadDefaultsFunction(ca, frameworkLogger), Fn.map(
				ReturnFilesFunction.GET_FILES_FN, defaultDir));
	}

	/**
	 * @return File (that may or may not exist) of default dir.
	 */
	private File getDefaultDir() {
		return new File(config.getString(Config.CONFIG_KEY_ROOT_DIR), DEFAULT_FILENAME);
	}

	/**
	 * @return true if 'default' directory exists
	 */
	private boolean defaultDirExists() {
		File d = getDefaultDir();
		return d != null && d.isDirectory();
	}

	/**
	 * @return A file that represents the pipe file used for info (out) pipe.
	 */
	public static File getInfoFile() {
		return new File(config.getString(Config.CONFIG_KEY_ROOT_DIR), INFO_FILENAME);
	}
	
	/**
	 * @return A file that represents the pipe file used for control (in) pipe.
	 */
	public static File getControlFile() {
		return new File(config.getString(Config.CONFIG_KEY_ROOT_DIR), CONTROL_FILENAME);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		if (writer != null) 
			writer.shutdown();
		
		if (reader != null);
			reader.shutdown();
	
		Activator.context = null;
	}
	
	public static Config getConfig() {
		return config;
	}

	/**
	 * Will log in order of availability: LogService, Framework Logger, System.out.
	 * 
	 * @param level LogService log level
	 * @param message message to log
	 */
	public static void log(int level, String message) {
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
			LogPrinter.doLog(null, null, level, message, null);		
	}
	
	/**
	 * Log an error.  Will log in order of availability: LogService, Framework Logger, System.out.
	 * @param level
	 * @param message
	 * @param error
	 */
	public static void log(int level, String message, Throwable error) {
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
			LogPrinter.doLog(null, null, level, message, error);
	}

	public void setLogger(Logger logger) {
		Activator.frameworkLogger = logger;
	}

	@Override
	public void frameworkEvent(FrameworkEvent event) {
		if (event.getType() == FrameworkEvent.STARTED) {
			log(LogService.LOG_INFO, "Knapsack " + KNAPSACK_VERSION + " starting in " + config.get(Config.CONFIG_KEY_ROOT_DIR));
			writer.start();
			reader.start();
			init.start();
		}
	}
	
	/**
	 * @return
	 */
	public static Map<File, Long> getBundleSizeMap() {
		return sizeMap;
	}
}
