package org.knapsack;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Class for knapsack and framework configuration.  
 * It also handles creating the initial default configuration if it doesn't exist upon start.
 * 
 * @author kgilmer
 *
 */
public class Config {
	
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
	 * Only one instance of Config per runtime.
	 */
	private static Config ref;
	
	/**
	 * Map that stores configuration.
	 */
	private static Map<String, String> cmap;
	
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
			cmap = createDefaultConfiguration(confFile.getParentFile());
					
			writeToFile(cmap, confFile);
			copyScripts(confFile.getParentFile());
		} else {
			//Load the map from a file.
			cmap = readFromFile(confFile);
			if (!cmap.containsKey(CONFIG_KEY_ROOT_DIR))
				cmap.put(CONFIG_KEY_ROOT_DIR, confFile.getParent());
		}
	}
	
	private void copyScripts(File parentFile) throws IOException {
		File scriptDir = new File(parentFile, "bin");
		
		if (!scriptDir.exists())
			if (!scriptDir.mkdirs())
				throw new IOException("Unable to create directories: " + scriptDir);
		
		String [] scripts = new String [] {
				"knapsack-bundles.sh",  
				"knapsack-log.sh",
				"knapsack-rescan.sh",
				"knapsack-get-all.sh",
				"knapsack-properties.sh",
				"knapsack-services.sh"
		};
		
		for (String script : Arrays.asList(scripts)) {
			File f = new File(scriptDir, script);
			writeToFile(f, Config.class.getResourceAsStream("/scripts/" + script));
			f.setExecutable(true, true);
		}
	}

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
	 */
	private Map<String, String> createDefaultConfiguration(File rootDir) {
		Map<String, String> map = new HashMap<String, String>();
		
		// Knapsack properties
		map.put(CONFIG_KEY_OUT_BUNDLE, "true");
		map.put(CONFIG_KEY_OUT_SERVICE, "true");
		map.put(CONFIG_KEY_OUT_PROPERTY, "false");
		map.put(CONFIG_KEY_OUT_CONFIG, "false");
		map.put(CONFIG_KEY_OUT_LOG, "true");
		map.put(CONFIG_KEY_BUILTIN_LOGGER, "true");
		map.put(CONFIG_KEY_BUILTIN_CONFIGADMIN, "true");
		
		map.put(CONFIG_KEY_VERBOSE, "true");
		map.put(CONFIG_KEY_ROOT_DIR, rootDir.getAbsolutePath());
		
		// Felix properties			
		map.put("org.osgi.framework.storage.clean", "onFirstInit");
		map.put("felix.log.level", "3");
		map.put("org.osgi.framework.storage", "bundleCache");
		map.put("felix.cm.dir", new File(rootDir, "configAdmin").getAbsolutePath());
		map.put("org.osgi.framework.system.packages", 
				"org.osgi.service.startlevel;uses:=\"org.osgi.framework\";version=\"1.1\",org.osgi.framework.launch;uses:=\"org.osgi.framework\";version=\"1.0\",org.osgi.util.tracker;uses:=\"org.osgi.framework\";version=\"1.4\",org.osgi.service.url;version=\"1.0\",org.osgi.framework;version=\"1.5\",org.osgi.service.packageadmin;uses:=\"org.osgi.framework\";version=\"1.2\",org.osgi.framework.hooks.service;uses:=\"org.osgi.framework\";version=\"1.0\", org.osgi.service.cm, org.osgi.service.log");
		
		return map;
	}

	/**
	 * Read a configuration from file.
	 * 
	 * @param ifile
	 * @return
	 * @throws IOException
	 */
	private Map<String, String> readFromFile(File ifile) throws IOException {
		HashMap<String, String> map = new HashMap<String, String>();
		BufferedReader br = new BufferedReader(new FileReader(ifile));
		
		String line = null;
		while ((line = br.readLine()) != null) {
			line = line.trim();
			if (!line.startsWith("#") && line.indexOf('=') > -1) {
				String []nvp = line.split("=");
				map.put(nvp[0].trim(), nvp[1].trim());
			}				
		}
		br.close();
			
		return map;
	}

	/**
	 * Write a configuration to file.
	 * 
	 * @param map
	 * @param ofile
	 * @throws IOException
	 */
	private void writeToFile(Map<String, String> map, File ofile) throws IOException {
		BufferedWriter br = new BufferedWriter(new FileWriter(ofile));
		br.write("# This is a generated default configuration for knapsack\n\n");
		for (Map.Entry<String, String> e: map.entrySet())
			br.write(e.getKey() + " = " + e.getValue() + "\n");
		
		br.close();
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
	 * Map.get(key).
	 * 
	 * @param key
	 * @return
	 */
	public String get(String key) {		
		return cmap.get(key);
	}

	/**
	 * Parse value as boolean.
	 * 
	 * @param key
	 * @return
	 */
	public boolean getBoolean(String key) {
		return Boolean.parseBoolean(cmap.get(key));
	}

	/**
	 * @return
	 */
	public Set<Entry<String, String>> entrySet() {		
		return cmap.entrySet();
	}

	/**
	 * @param key
	 * @param val
	 */
	public void put(String key, String val) {
		cmap.put(key, val);
	}

	/**
	 * @return
	 */
	public Map<String, String> asMap() {
		return cmap;
	}
}