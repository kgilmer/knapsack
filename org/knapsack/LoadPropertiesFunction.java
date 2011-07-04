package org.knapsack;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.felix.framework.Logger;
import org.osgi.service.log.LogService;
import org.sprinkles.Fn.Function;

/**
 * A function that loads properties files into System properties dictionary.
 * 
 * @author kgilmer
 * 
 */
public class LoadPropertiesFunction implements Function<File, File> {
	private final Logger logger;

	public LoadPropertiesFunction(Logger logger) {
		this.logger = logger;
	}

	@Override
	public File apply(File f) {

		if (isPropertiesFile(f)) {
			try {
				loadPropertyFile(f);
				return f;
			} catch (IOException e) {
				logger.log(LogService.LOG_ERROR, "Failed to process properties for " + f, e);
			}
			
		}

		return null;
	}

	private void loadPropertyFile(File f) throws IOException {
		
		for (String line : IOUtils.readLines(new FileInputStream(f))) {
			if (line.length() == 0 || line.trim().startsWith("#"))
				continue;

			String[] elems = line.split("=");

			if (elems.length < 2)
				throw new IOException("Invalid line in property file: " + line);

			System.getProperties().put(elems[0].trim(), line.substring(elems[0].length() + 1).trim());
		}
	}

	/**
	 * Assume all files in default directory are property files.
	 * 
	 * @param f
	 * @return
	 */
	private boolean isPropertiesFile(File f) {
		return f.getName().endsWith(".properties");
	}
}
