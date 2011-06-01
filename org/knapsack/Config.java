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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Properties;

/**
 * Class for knapsack and framework configuration.  
 * It also handles creating the initial default configuration if it doesn't exist upon start.
 * 
 * @author kgilmer
 *
 */
public class Config extends Properties {
	private static final long serialVersionUID = -5479563157788056552L;

	/**
	 * Properties that cause behavior changes to OSGi instance.
	 * @author kgilmer
	 *
	 */
	public enum SpecialConfigKey {
		SHUTDOWN,RESTART,BOUNCE,RESCAN
	}
	
	/**
	 * Optional system property that defines location of knapsack configuration file.
	 */
	public static final String CONFIG_KEY_CONFIG_FILE = "org.knapsack.configFile";
	
	/**
	 * Optional system property that defines root directory where knapsack runs.
	 */
	public static final String CONFIG_KEY_ROOT_DIR = "org.knapsack.rootDir";
	/**
	 * Emit bundle info in pipe
	 */
	public static final String CONFIG_KEY_OUT_BUNDLE = "org.knapsack.bundle";
	/**
	 * Emit service info in pipe
	 */
	public static final String CONFIG_KEY_OUT_SERVICE = "org.knapsack.service";
	/**
	 * Emit system property info in pipe
	 */
	public static final String CONFIG_KEY_OUT_PROPERTY = "org.knapsack.property";
	/**
	 * Emit config info in pipe
	 */
	public static final String CONFIG_KEY_OUT_CONFIG = "org.knapsack.config";
	/**
	 * Emit log info in pipe
	 */
	public static final String CONFIG_KEY_OUT_LOG = "org.knapsack.log";
	
	/**
	 * Emit log events to stdout
	 */
	public static final String CONFIG_KEY_LOG_STDOUT = "org.knapsack.log.stdout";

	/**
	 * Emit verbose messages in pipe
	 */
	public static final String CONFIG_KEY_VERBOSE = "org.knapsack.output.verbose";
	
	/**
	 * Name of knapsack's configuration file
	 */
	private static final String CONFIGURATION_FILENAME = "knapsack.conf";

	/**
	 * If true the internal Logger will be started with the framework.
	 */
	public static final String CONFIG_KEY_BUILTIN_LOGGER = "org.knapsack.builtin.logger";

	/**
	 * If true the internal ConfigAdmin will be started with the framework.
	 */
	public static final String CONFIG_KEY_BUILTIN_CONFIGADMIN = "org.knapsack.builtin.configAdmin";
	
	/**
	 * A list of directory names which hold bundles that should be installed and optionally started.
	 */
	public static final String CONFIG_KEY_BUNDLE_DIRS = "org.knapsack.bundleDirs";
	
	/**
	 * Filename for bundle directory
	 */
	public static final String DEFAULT_BUNDLE_DIRECTORY = "bundle";
	
	/**
	 * Only one instance of Config per runtime.
	 */
	private static Config ref;
	
	private final static String [] shellScripts = new String [] {
			"ks-bundles.sh",  
			"ks-log.sh",
			"ks-rescan.sh",
			"ks-get-all.sh",
			"ks-properties.sh",
			"ks-services.sh",
			"ks-shutdown.sh"
	};
	
	public static Config getRef() throws IOException {
		if (ref == null)
			ref = new Config();
		
		return ref;
	}

	/**
	 * Initialize state
	 * 
	 * @throws IOException
	 */
	private Config() throws IOException {		
		File confFile = getConfigFile();
		
		if (confFile.isDirectory())
			throw new IOException("Invalid start state, " + confFile + " is a directory.");
		
		if (!confFile.exists()) {
			//Create a default configuration
			copyDefaultConfiguration(confFile);
			copyScripts(confFile.getParentFile());
		}
		
		load(new FileInputStream(confFile));
		
		//Specify the root of the knapsack install if not explicitly defined.
		if (!this.containsKey(CONFIG_KEY_ROOT_DIR))
			this.put(CONFIG_KEY_ROOT_DIR, getInitRootDirectory());
		
		//Specify a default bundle directory if not explicitly defined.
		if (!this.containsKey(CONFIG_KEY_BUNDLE_DIRS))
			this.put(CONFIG_KEY_BUNDLE_DIRS, DEFAULT_BUNDLE_DIRECTORY);
	}
	
	/**
	 * Copy shell scripts from the Jar into the deployment directory.
	 * @param parentFile
	 * @throws IOException
	 */
	private void copyScripts(File parentFile) throws IOException {
		File scriptDir = new File(parentFile, "bin");
		
		if (!scriptDir.exists())
			if (!scriptDir.mkdirs())
				throw new IOException("Unable to create directories: " + scriptDir);
		
		for (String script : Arrays.asList(shellScripts)) {
			File f = new File(scriptDir, script);
			
			if (f.exists())
				continue;
			
			InputStream istream = Config.class.getResourceAsStream("/scripts/" + script);
			if (istream == null)
				throw new IOException("Script file does not exist: " + f);
			
			writeToFile(f, istream);
			f.setExecutable(true, true);
		}
	}

	/**
	 * Write an inputstream to a file.
	 * 
	 * @param outputFile
	 * @param inputStream
	 * @throws IOException
	 */
	private void writeToFile(File outputFile, InputStream inputStream) throws IOException {
		FileOutputStream fos = new FileOutputStream(outputFile);
		byte [] buff = new byte[4096];
		
		while (inputStream.available() > 0) {
			inputStream.read(buff);
			fos.write(buff);
		}
		
		inputStream.close();
		fos.close();
	}
		

	/**
	 * Get the knapsack configuration file.
	 * 
	 * @return
	 * @throws IOException
	 */
	private File getConfigFile() throws IOException {
		if (System.getProperty(CONFIG_KEY_CONFIG_FILE) != null) {
			return new File(System.getProperty(CONFIG_KEY_CONFIG_FILE));
		}
		
		return new File(getInitRootDirectory(), CONFIGURATION_FILENAME);
	}

	/**
	 * Generate the default configuration.
	 * 
	 * @param rootDir
	 * @return
	 * @throws IOException 
	 */
	private void copyDefaultConfiguration(File rootDir) throws IOException {
		byte [] buff = new byte[4096];
		InputStream istream = Config.class.getResourceAsStream("/default.conf");
		
		OutputStream fos = new FileOutputStream(rootDir);
		
		while (istream.read(buff) > -1) {
			fos.write(buff);
		}
		
		istream.close();
		fos.close();
	}

	/**
	 * @return The root of the init folder system.
	 * @throws IOException 
	 * 
	 */
	private String getInitRootDirectory() throws IOException {

		if (System.getProperty(CONFIG_KEY_ROOT_DIR) != null) {
			return System.getProperty(CONFIG_KEY_ROOT_DIR);
		}
		
		return System.getProperty("user.dir");
	}

	/**
	 * Parse value as boolean.
	 * 
	 * @param key
	 * @return
	 */
	public boolean getBoolean(String key) {
		if (this.containsKey(key))
			return Boolean.parseBoolean(this.get(key).toString());
		
		return false;
	}

	public String getString(String key) {	
		if (this.containsKey(key))
			return this.get(key).toString();
		
		return null;
	}
}