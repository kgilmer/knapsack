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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.knapsack.KnapsackLogger;
import org.knapsack.Launcher;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.log.LogService;
import org.sprinkles.Applier;

/**
 * Install a file as an OSGi bundle.
 * 
 * @author kgilmer
 *
 */
class InstallBundleFunction implements Applier.Fn<File, BundleJarWrapper> {

	private Map<String, Bundle> installedBundleMap;
	private final Collection<BundleJarWrapper> installed;
	private final BundleContext context;
	private KnapsackLogger logger;

	public InstallBundleFunction(Collection<BundleJarWrapper> installed, BundleContext context) {
		this.installed = installed;
		this.context = context;
		this.logger = Launcher.getLogger();
		installedBundleMap = createLocationList(context);
	}

	public static Map<String, Bundle> createLocationList(BundleContext context) {
		Map<String, Bundle> l = new HashMap<String, Bundle>();
		for (Bundle b : Arrays.asList(context.getBundles()))
			l.put(b.getLocation(), b);
		
		return l;
	}

	@Override
	public BundleJarWrapper apply(File element) {
		if (!element.getName().toUpperCase().endsWith(".JAR")) {
			logger.log(LogService.LOG_WARNING, "Ignoring " + element.getName() + ", not a jar.");
			return null;
		}
		
		String fileUri = fileToUri(element);
		
		if (isInstalled(fileUri) && !fileChanged(element)) {
			logger.log(LogService.LOG_DEBUG, element.getName() + " is already installed.");
			return new BundleJarWrapper(element, installedBundleMap.get(fileUri));
		} else if (isInstalled(fileUri) && fileChanged(element)) {
			uninstallBundle(installedBundleMap.get(fileUri));
		}
			
		try {
			Bundle b = context.installBundle(fileUri);		
			BundleInitThread.getBundleSizeMap().put(element, element.length());
			BundleJarWrapper wrapper = new BundleJarWrapper(element, b);
			installed.add(wrapper);
			return wrapper;		
		} catch (BundleException e) {
			logger.log(LogService.LOG_ERROR, "Unable to install " + element.getName() + " as a bundle.", e);
			return null;
		}
	}

	/**
	 * Uninstall a bundle.  Will absorb any BundleException and log it but allow installation process to continue rather than aborting.
	 * 
	 * @param bundle
	 */
	private void uninstallBundle(Bundle bundle) {
		try {
			bundle.uninstall();
		} catch (BundleException e) {
			logger.log(LogService.LOG_ERROR, "An error occurred while uninstalling " + bundle.getLocation() + ".", e);
		}
	}

	/**
	 * Compares file on filesystem to internal state of file to determine if they are different.
	 * 
	 * @param element
	 * @return
	 */
	private boolean fileChanged(File element) {
		Map<File, Long> bsm = BundleInitThread.getBundleSizeMap();
		
		if (!bsm.containsKey(element))
			return true;
		
		if (bsm.get(element) != element.length())
			return true;
		
		return false;
	}

	/**
	 * Determine if a given bundle is already installed.
	 * 
	 * @param fileUri
	 * @return
	 */
	private boolean isInstalled(String fileUri) {		
		return installedBundleMap.keySet().contains(fileUri);
	}

	/**
	 * Create a string uri to compare against what the framework provides.
	 * 
	 * @param f
	 * @return
	 */
	public static String fileToUri(File f) {
		return "file://" + f.toString();
	}
	
}