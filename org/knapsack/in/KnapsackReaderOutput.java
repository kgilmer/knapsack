package org.knapsack.in;

import java.io.File;

import org.knapsack.Activator;
import org.knapsack.Config;
import org.knapsack.Config.SpecialConfigKey;
import org.knapsack.in.PipeReaderThread.ReaderOutput;
import org.knapsack.init.InitThread;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.log.LogService;

/**
 * @author kgilmer
 *
 */
public class KnapsackReaderOutput implements ReaderOutput {
	
	private Config config;
	private BundleContext context;

	public KnapsackReaderOutput() {
		config = Activator.getConfig();
		context = Activator.getContext();
	}
	
	@Override
	public void inputReceived(String input) {
		if (input.contains("=")) {
			String [] kvp = input.split("=");
			
			if (kvp.length == 2) {
				String key = getToken(input, 1, "=");
				String val = getToken(input, 2, "=");
				if (isSpecialKey(key))
					handleSpecial(key, val);
				else 
					config.put(key, val);
				
				return;
			}
		} else if (isSpecialKey(getToken(input, 1, " "))) {
			handleSpecial(getToken(input, 1, " "), getToken(input, 2, " "));
			return;
		}
		
		Activator.log(LogService.LOG_ERROR, "Invalid config line: " + input);
	}

	private String getToken(String input, int index, String delim) {
		String [] elems = input.split(delim);
		
		if (elems != null && elems.length > (index - 1))
			return elems[index - 1].trim();
		
		return null;
	}

	private void handleSpecial(String key, String val) {
		if (key.toUpperCase().equals(SpecialConfigKey.SHUTDOWN.toString()))
			shutdownFramework();
		else if (key.toUpperCase().equals(SpecialConfigKey.RESCAN.toString()))
			rescanBundles();
		else if (key.toUpperCase().equals(SpecialConfigKey.RESTART.toString()))
			restartFramework();
	}

	private void restartFramework() {
		shutdownFramework();
		try {
			context.getBundle(0).start();
		} catch (BundleException e) {
			Activator.log(LogService.LOG_ERROR, "Error occurred while starting system bundle.", e);
		}
	}

	private void rescanBundles() {
		InitThread init = new InitThread(new File(config.get(Config.CONFIG_KEY_ROOT_DIR), Activator.BUNDLE_DIRECTORY));
		init.start();
	}

	private void shutdownFramework() {
		try {
			context.getBundle(0).stop();
		} catch (BundleException e) {
			Activator.log(LogService.LOG_ERROR, "Error occurred while stopping system bundle.", e);
		}
	}

	private boolean isSpecialKey(String key) {
		if (key == null)
			return false;
		
		try {
			SpecialConfigKey.valueOf(key.toUpperCase());
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
}
