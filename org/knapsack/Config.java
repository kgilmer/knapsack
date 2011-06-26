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
	 * Emit log events to stdout
	 */
	public static final String CONFIG_KEY_LOG_STDOUT = "org.knapsack.log.stdout";

	/**
	 * Name of knapsack's configuration file
	 */
	private static final String CONFIGURATION_FILENAME = "felix.conf";

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
	private static Config ref;
	
	private final static String baseScript = ".knapsack-command.sh";

	private static final String FELIX_CONFIGURATION = "/felix.conf";
	
	public static Config getRef() throws IOException {
		if (ref == null)
			ref = new Config();
		
		return ref;
	}

	private File scriptDir;

	private File baseScriptFile;

	/**
	 * Initialize state
	 * 
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	private Config() throws IOException {		
		File confFile = getConfigFile();
		
		if (confFile.isDirectory())
			throw new IOException("Invalid start state, " + confFile + " is a directory.");
		
		String rootDir = getInitRootDirectory();
		scriptDir = new File(rootDir, "bin");
		baseScriptFile = new File(scriptDir, baseScript);
		
		if (!confFile.exists()) {
			//Create a default configuration
			copyDefaultConfiguration(FELIX_CONFIGURATION, confFile, true);
		}
			
		// Create the default dir if doesn't exist.
		getCreateDefaultDir(rootDir);			
			
		if (!scriptDir.exists()) {
			//Create script
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
	
	private File getCreateDefaultDir(String initRootDirectory) throws IOException {
		File defDir = new File(initRootDirectory, Activator.DEFAULT_FILENAME);
		
		if (!defDir.exists())
			if (!defDir.mkdirs())
				throw new IOException("Unable to create directory: " + defDir);
		
		return defDir;
	}

	/**
	 * Copy shell scripts from the Jar into the deployment directory.
	 * @param parentFile
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	private void copyScripts(File parentFile) throws IOException {
		if (scriptDir == null)
			scriptDir = new File(parentFile, "bin");
		
		if (!scriptDir.exists())
			if (!scriptDir.mkdirs())
				throw new IOException("Unable to create directories: " + scriptDir);
		
		if (baseScriptFile == null)
			baseScriptFile = new File(scriptDir, baseScript);
		
		if (!baseScriptFile.exists()) {	
			InputStream istream = Config.class.getResourceAsStream("/scripts/" + baseScript);
			if (istream == null)
				throw new IOException("Script file does not exist: " + baseScriptFile);
			
			writeToFile(baseScriptFile, istream);
			baseScriptFile.setExecutable(true, true);
		}
	}
	
	/**
	 * Generate the filesystem symlink necessary to allow a command to be called from the shell environment.
	 * @param commandName
	 * @throws IOException
	 */
	public void createFilesystemCommand(String commandName) throws IOException {
		File sf = new File(scriptDir, commandName);
		
		if (sf.exists())
			throw new IOException(commandName + " already exists in " + scriptDir);
		
		try {
			createSymlink(baseScriptFile.getAbsolutePath(), scriptDir + File.separator + commandName);
			Activator.logDebug("Created symlink to " + commandName);
		} catch (InterruptedException e) {
			throw new IOException("Process was interrupted.", e);
		}		
	}
	
	/**
	 * Delete the filesystem symlink for a command.
	 * @param commandName
	 * @throws IOException
	 */
	public void deleteFilesystemCommand(String commandName) throws IOException {
		File cmd = new File(scriptDir, commandName);
		
		if (!cmd.exists() || !cmd.isFile())
			throw new IOException("Invalid file: " + cmd);
		
		if (!cmd.delete())
			throw new IOException("Failed to delete " + cmd);
	}
	
	/**
	 * Delete the /bin dir.
	 * @throws IOException 
	 */
	public void deleteBinDir() throws IOException {
		for (File f : Arrays.asList(scriptDir.listFiles()))
			if (!f.delete())
				throw new IOException("Unable to delete: " + f);
		
		if (!scriptDir.delete())
			throw new IOException("Unable to delete: " + scriptDir);
	}

	private void createSymlink(String baseFile, String link) throws InterruptedException, IOException {
		String [] cmd = {"ln", "-s", baseFile, link};
		Runtime.getRuntime().exec(cmd).waitFor();
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
	 * @param confFile
	 * @return
	 * @throws IOException 
	 */
	private void copyDefaultConfiguration(String inFile, File confFile, boolean addCmDir) throws IOException {
		byte [] buff = new byte[4096];
		InputStream istream = Config.class.getResourceAsStream(inFile);
		
		if (istream == null)
			throw new IOException("Configuration resource is not present: " + inFile);
		
		OutputStream fos = new FileOutputStream(confFile);
		
		int len = 0;
		while ((len = istream.read(buff)) > -1) {
			fos.write(buff, 0, len);
		}
		
		if (addCmDir) {
			//Since this property is not static, create dynamically.  If multiple properties need to be set dynamically in the future, consider using a template format.
			fos.write(("\nfelix.cm.dir = " + getInitRootDirectory() + File.separator + Activator.CONFIGADMIN_FILENAME + "\n").getBytes());
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
}