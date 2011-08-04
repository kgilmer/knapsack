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
	 * Optional system property that defines root directory where knapsack runs.
	 */
	public static final String CONFIG_KEY_ROOT_DIR = "org.knapsack.rootDir";

	/**
	 * Enable color output of shell commands in terminal.
	 */
	public static final String CONFIG_KEY_COLOR_OUTPUT = "org.knapsack.shell.color";
	/**
	 * Emit log events to stdout
	 */
	public static final String CONFIG_KEY_LOG_STDOUT = "org.knapsack.log.stdout";

	/**
	 * Name of knapsack's configuration file
	 */
	public static final String CONFIGURATION_FILENAME = "felix.conf";
	/**
	 * Path to default configuration file within the knapsack jar.
	 */
	public static final String CONFIGURATION_RESOURCE_FILENAME = "/" + CONFIGURATION_FILENAME;

	/**
	 * If enabled pre-existing config admin Configurations will be overwritten on each start.
	 */
	public static final String CONFIG_KEY_OVERWRITE_CONFIGADMIN = "org.knapsack.configAdmin.overwrite";
	
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
	
	public final static String BASE_SCRIPT_FILENAME = ".knapsack-command.sh";
	
	/**
	 * Directory for scripts
	 */
	public static final String SCRIPT_DIRECTORY_NAME = "bin";
	/**
	 * Directory name where configadmin default property files are stored.
	 */
	protected static final String DEFAULT_DIRECTORY_NAME = "default";
	
	/**
	 * Filename for configadmin directory.
	 */
	public static final String CONFIGADMIN_DIRECTORY_NAME = "configadmin";
	/**
	 * If true, the Knapsack script directory (/bin) will not be created and socket listener will not be started.
	 */
	public static final String CONFIG_DISABLE_SCRIPTS = "org.knapsack.scripts.disable";

	/**
	 * If true, a remote client can execute shell commands.  
	 */
	public static final String CONFIG_KEY_ACCEPT_ANY_HOST = "org.knapsack.scripts.acceptAnyHost";

	/**
	 * Native command to be called to proxy shell command to knapsack and get response.
	 */
	public static final String CONFIG_KEY_SHELL_COMMAND = "org.knapsack.shell.command";

	/**
	 * Port that the shell is listening on for commands, or -1 if shell is disabled.
	 */
	public static final String SYSTEM_PROPERTY_KEY_SHELL_PORT = "org.knapsack.shell.port";

	/**
	 * Base directory where knapsack instance is running.
	 */
	private final File baseDirectory;

	/**
	 * Initialize state
	 * 
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	protected Config(File baseDirectory) throws IOException {	
		Properties originalProperties = new Properties();
		for (Object key : System.getProperties().keySet())
			originalProperties.put(key, System.getProperty(key.toString()));
		
					
		this.baseDirectory = baseDirectory;
		load(new FileInputStream(getConfigFile()));
		
		// Overwrite any pre-existing properties that may have been reset by load().  
		// This makes the default/*.properties property files take precedence over felix.conf, 
		// allowing for simple overrides.
		for (Object key : originalProperties.keySet())
			this.put(key, originalProperties.get(key.toString()));
		
		//Specify the root of the knapsack install if not explicitly defined.
		if (!this.containsKey(CONFIG_KEY_ROOT_DIR))
			this.put(CONFIG_KEY_ROOT_DIR, baseDirectory);
		
		//Specify a default bundle directory if not explicitly defined.
		if (!this.containsKey(CONFIG_KEY_BUNDLE_DIRS))
			this.put(CONFIG_KEY_BUNDLE_DIRS, DEFAULT_BUNDLE_DIRECTORY);
	}
	
	/**
	 * Get the knapsack configuration file.
	 * 
	 * @return
	 * @throws IOException
	 */
	private File getConfigFile() throws IOException {	
		return new File(baseDirectory, CONFIGURATION_FILENAME);
	}

	/**
	 * Parse value as boolean.
	 * 
	 * @param key
	 * @return true if key value is 'true', false otherwise.
	 */
	public boolean getBoolean(String key) {
		if (this.containsKey(key))
			return Boolean.parseBoolean(this.get(key).toString());
		
		return false;
	}

	/**
	 * Convenience method to return value as a string or null if key doesn't exist.
	 * @param key
	 * @return
	 */
	public String getString(String key) {	
		if (this.containsKey(key))
			return this.get(key).toString();
		
		return null;
	}
	
	/* (non-Javadoc)
	 * @see java.util.Hashtable#contains(java.lang.Object)
	 */
	public boolean contains(Object key) {
		return this.get(key) != null;
	}
	
}