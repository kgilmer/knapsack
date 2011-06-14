package org.knapsack;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.framework.Logger;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;
import org.sprinkles.Fn.Function;

/**
 * A function that loads property files into configuration admin.
 * 
 * @author kgilmer
 *
 */
public class LoadDefaultsFunction implements Function<File, File> {

	private final ConfigurationAdmin ca;
	private final Logger log;

	public LoadDefaultsFunction(ConfigurationAdmin ca, Logger frameworkLogger) {
		this.ca = ca;
		this.log = frameworkLogger;		
	}

	@Override
	public File apply(File f) {
		
		if (isDefaultFile(f))
			try {
				loadDefaultFile(f);
				return f;
			} catch (IOException e) {
				log.log(LogService.LOG_ERROR, "Unable to load default properties for " + f, e);
			}
		else
			log.log(LogService.LOG_WARNING, "Ignoring file named " + f);
		
		return null;
	}

	private void loadDefaultFile(File f) throws IOException {
		String pid = f.getName();
		
		//Only set the configuration if it does not already exist.
		if (!isPIDEmpty(pid)) {
			log.log(LogService.LOG_INFO, "Ignoring defaults for PID " + pid + ", configuration has data.");
			return;
		}
		
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
		String line = null;
		Dictionary<String, String> kvp = new Hashtable<String, String>();
		
		while ((line = br.readLine()) != null) {
			line = line.trim();
			
			if (line.length() == 0 || line.startsWith("#"))
				continue;
			
			String [] elems = line.split("=");
			
			if (elems.length != 2)
				throw new IOException("Invalid line in config admin property file: " + line);
			
			kvp.put(elems[0].trim(), elems[1].trim());
		}
		br.close();
		
		if (kvp.size() > 0) {	
			Configuration config = ca.getConfiguration(pid, null);
			
			config.update(kvp);
			log.log(LogService.LOG_INFO, "Set " + kvp.size() + " properties for PID: " + pid + ".");			
		}
	}
	
	private boolean isPIDEmpty(String pid) throws IOException {	
		Configuration config = ca.getConfiguration(pid, null);
		
		//Only set the configuration if it does not already exist.
		return config.getProperties() == null;
	}

	/**
	 * Assume all files in default directory are property files.
	 * 
	 * @param f
	 * @return
	 */
	private boolean isDefaultFile(File f) {
		return f.getParentFile().getName().equals(Activator.DEFAULT_FILENAME);
	}
}
