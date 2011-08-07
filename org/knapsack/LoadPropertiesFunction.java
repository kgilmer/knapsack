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
import java.util.Map.Entry;
import java.util.Properties;

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
	private final KnapsackLogger logger;

	/**
	 * @param logger KnapsackLogger
	 */
	public LoadPropertiesFunction(KnapsackLogger logger) {
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
		Properties pf = new Properties();
		pf.load(new FileInputStream(file));
		
		for (Entry<Object, Object> e : pf.entrySet()) {	
			if (System.getProperties().containsKey(e.getKey())) {
				logger.log(LogService.LOG_WARNING, "Ignoring property that already has a value:" + e.getKey() + ".  Existing value: " + System.getProperty(e.getKey().toString()));
				continue;
			}
			
			String finalValue = evalSubsitutions(e.getValue().toString());
			
			System.getProperties().put(e.getKey(), finalValue);
		}
	}

	/**
	 * A recursive function to replace variables with values from System.properties.
	 * Variable is defined in ${var} style with 'var' being a system property.
	 * 
	 * @param ins
	 * @return
	 * @throws IOException
	 */
	public static String evalSubsitutions(final String ins) throws IOException {
		int si = ins.indexOf("${");
		if (si > -1) {
			int ti = ins.indexOf('}', si + 2);
			
			if (ti == -1)
				throw new IOException("Property value has invalid subsitution variable syntax: " + ins);
			
			String varName = ins.substring(si + 2, ti);
			String varVal = System.getProperty(varName);
			String varLiteral = "\\$\\{" + varName + "\\}";
			
			String subLine = ins.replaceAll(varLiteral, varVal);
			
			return evalSubsitutions(subLine);
		}
		
		return ins;
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
