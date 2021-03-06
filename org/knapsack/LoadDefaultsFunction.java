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
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Properties;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;
import org.sprinkles.Applier.Fn;

/**
 * A function that loads property files into configuration admin.
 * 
 * @author kgilmer
 *
 */
public class LoadDefaultsFunction implements Fn<File, File> {

	private final ConfigurationAdmin ca;
	private final KnapsackLogger log;
	private final boolean overwriteConfiguration;

	/**
	 * @param ca ConfigurationAdmin
	 * @param frameworkLogger KnapsackLogger
	 * @param overwriteConfiguration if true, pre-existing Configurations will be overwritten
	 */
	public LoadDefaultsFunction(ConfigurationAdmin ca, KnapsackLogger frameworkLogger, boolean overwriteConfiguration) {
		this.ca = ca;
		this.log = frameworkLogger;
		this.overwriteConfiguration = overwriteConfiguration;		
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

	/**
	 * Load a config admin property file, save as a ConfigAdmin Configuration.
	 * 
	 * @param file File
	 * @throws IOException on I/O error
	 */
	private void loadDefaultFile(File file) throws IOException {
		String pid = file.getName();
		
		//Only set the configuration if it does not already exist.
		if (!overwriteConfiguration && !isPIDEmpty(pid)) {
			log.log(LogService.LOG_INFO, "Ignoring defaults for PID " + pid + ", configuration has data.");
			return;
		}
		
		Properties pf = new Properties();
		pf.load(new FileInputStream(file));
		Dictionary<String, String> kvp = new Hashtable<String, String>();
		
		for (Entry<Object, Object> e : pf.entrySet()) {
			if (System.getProperties().containsKey(e.getKey())) {
				log.log(LogService.LOG_WARNING, "Ignoring property that already has a value:" + e.getKey() + ".  Existing value: " + System.getProperty(e.getKey().toString()));
				continue;
			}
			
			kvp.put(e.getKey().toString(), e.getValue().toString());
		}		
		
		if (kvp.size() > 0) {	
			Configuration config = ca.getConfiguration(pid, null);
			
			config.update(kvp);
			log.log(LogService.LOG_INFO, "Set " + kvp.size() + " properties for PID: " + pid + ".");			
		}
	}
	
	/**
	 * @param pid PID of Configuration
	 * @return true if PID is null 
	 * @throws IOException on I/O error
	 */
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
		return f.getParentFile().getName().equals(ConfigurationConstants.DEFAULT_DIRECTORY_NAME) && !f.getName().equalsIgnoreCase(".properties");
	}
}
