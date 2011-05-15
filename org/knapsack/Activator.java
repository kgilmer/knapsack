package org.knapsack;

import java.io.File;

import org.apache.felix.framework.Logger;
import org.knapsack.in.KnapsackReaderOutput;
import org.knapsack.in.PipeReaderThread;
import org.knapsack.init.InitThread;
import org.knapsack.out.KnapsackWriterInput;
import org.knapsack.out.PipeWriterThread;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 * Activator for Knapsack.  Is not typically called as a regular bundle but via the BootStrap
 * class as a system bundle.
 * 
 * @author kgilmer
 *
 */
public class Activator implements BundleActivator {
	private static final String INFO_FILENAME = "info";
	private static final String CONTROL_FILENAME = "control";
	public static final String BUNDLE_DIRECTORY = "bundle";
	
	private static BundleContext context;
	private static Config config;

	public Activator() {
		
	}
	
	public Activator(Logger logger) {
		Activator.logger = logger;
	}

	public static BundleContext getContext() {
		return context;
	}

	private PipeWriterThread writer;

	private PipeReaderThread reader;
	private static Logger logger;
	private static LogService logService;

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		
		config = Config.getRef();
		log(LogService.LOG_INFO, "Knapsack starting in " + config.get(Config.CONFIG_KEY_ROOT_DIR));
		
		writer = new PipeWriterThread(new File(config.get(Config.CONFIG_KEY_ROOT_DIR), INFO_FILENAME), new KnapsackWriterInput());
		writer.start();
		
		reader = new PipeReaderThread(new File(config.get(Config.CONFIG_KEY_ROOT_DIR), CONTROL_FILENAME), new KnapsackReaderOutput());
		reader.start();
		
		InitThread init = new InitThread(new File(config.get(Config.CONFIG_KEY_ROOT_DIR), BUNDLE_DIRECTORY));
		init.start();
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

	public static void log(int level, String message) {
		if (logService == null) {
			ServiceReference rs = context.getServiceReference(LogService.class.getName());
			
			if (rs != null) {
				logService = (LogService) context.getService(rs);			
			}
		}
		
		if (logService != null)
			logService.log(level, message);
		else if (logger != null)
			logger.log(level, message);
		else 
			System.out.println("[" + getLevelLabel(level) + "]: " + message);		
	}
	
	public static void log(int level, String message, Throwable error) {
		if (logService == null) {
			ServiceReference rs = context.getServiceReference(LogService.class.getName());
			
			if (rs != null) {
				logService = (LogService) context.getService(rs);			
			}
		}
			
		if (logService != null) {
			logService.log(level, message, error);
		} else if (logger != null)  {
			logger.log(level, message, error);
		} else {
			log(level, message);
			error.printStackTrace();
		}
	}
	
	/**
	 * @param level
	 * @return A human-readable log level string.
	 */
	public static String getLevelLabel(int level) {
		switch (level) {
		case 1:
			return "ERROR  ";
		case 2:
			return "WARNING";
		case 3:
			return "INFO   ";
		case 4:
			return "DEBUG  ";
		}

		return "UNKNOWN";
	}

	public void setLogger(Logger logger) {
		Activator.logger = logger;
	}
}
