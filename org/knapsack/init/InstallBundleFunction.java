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
package org.knapsack.init;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.knapsack.Activator;
import org.knapsack.out.KnapsackWriterInput;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.log.LogService;
import org.sprinkles.Fn;

/**
 * Install a file as an OSGi bundle.
 * @author kgilmer
 *
 */
class InstallBundleFunction implements Fn.Function<File, BundleJarWrapper> {

	private final BundleContext context;
	private Map<String, Bundle> locationList;

	public InstallBundleFunction() {
		this.context = Activator.getContext();
		locationList = createLocationList();
	}

	private Map<String, Bundle> createLocationList() {
		Map<String, Bundle> l = new HashMap<String, Bundle>();
		for (Bundle b : Arrays.asList(context.getBundles()))
			l.put(b.getLocation(), b);
		
		return l;
	}

	@Override
	public BundleJarWrapper apply(File element) {
		if (!element.getName().toUpperCase().endsWith(".JAR")) {
			Activator.log(LogService.LOG_WARNING, "Ignoring fine " + element.getName() + ", not a jar.");
			return null;
		}
		
		String fileUri = fileToUri(element);
		
		if (isInstalled(fileUri)) {
			Activator.log(LogService.LOG_DEBUG, "Not installing " + element.getName() + ", already installed.");
			return new BundleJarWrapper(element, locationList.get(fileUri));
		}
			
		try {
			Bundle b = this.context.installBundle(fileUri);			
			return new BundleJarWrapper(element, b);
		} catch (BundleException e) {
			Activator.log(LogService.LOG_ERROR, "Unable to install " + element.getName() + " as a bundle.", e);
			return null;
		}
	}

	private boolean isInstalled(String fileUri) {		
		return locationList.keySet().contains(fileUri);
	}

	private String fileToUri(File f) {
		return "file://" + f.toString();
	}
	
}