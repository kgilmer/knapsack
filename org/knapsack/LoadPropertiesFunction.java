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

import org.apache.commons.io.IOUtils;
import org.apache.felix.framework.Logger;
import org.osgi.service.log.LogService;
import org.sprinkles.Applier.Fn;

/**
 * A function that loads properties files into System properties dictionary.
 * 
 * @author kgilmer
 * 
 */
public class LoadPropertiesFunction implements Fn<File, File> {
	/**
	 * Extension that property files must have to be handled.  Example "http.properties".
	 */
	private static final String PROPERTY_FILE_EXTENSION = ".properties";
	private final Logger logger;

	/**
	 * @param logger Logger
	 */
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

	/**
	 * Parse a property file and load values into the System property dictionary.
	 * 
	 * @param file input file
	 * @throws IOException on I/O error
	 */
	private void loadPropertyFile(File file) throws IOException {
		
		for (String line : IOUtils.readLines(new FileInputStream(file))) {
			if (line.length() == 0 || line.trim().startsWith("#"))
				continue;

			if (!line.contains("="))
				throw new IOException("Invalid line in property file: " + line);
			
			String[] elems = line.split("=");		
			
			String key = elems[0].trim();
			
			String value = null;
			if (elems.length == 1)
				value = "";
			else 
				value = line.substring(elems[0].length() + 1).trim();
			
			if (System.getProperties().contains(key))
				logger.log(LogService.LOG_WARNING, "Overriding property " + key + ".  Existing value: " + System.getProperty(key) + "  New value: " + value);

			System.getProperties().put(key, value);
		}
	}

	/**
	 * Assume all files in default directory are property files.
	 * 
	 * @param f
	 * @return
	 */
	private boolean isPropertiesFile(File f) {
		return f.getName().endsWith(PROPERTY_FILE_EXTENSION);
	}
}
