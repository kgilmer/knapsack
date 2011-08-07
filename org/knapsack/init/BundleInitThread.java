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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knapsack.KnapsackLogger;
import org.knapsack.Launcher;
import org.osgi.service.log.LogService;
import org.sprinkles.Applier;
import org.sprinkles.functions.FileFunctions;

/**
 * A short-lived thread that scans a set of directories for files, and installs/starts/stops bundles in the framework.
 * @author kgilmer
 *
 */
public class BundleInitThread extends Thread {
	/**
	 * A list of directories that knapsack will look for bundles in.
	 */
	private final List<File> bundleDirs;
	private final KnapsackLogger logger;
	private final static Map<File, Long> bundleJarSizeMap = new HashMap<File, Long>();

	/**
	 * @param rootDir
	 * @param filenames
	 */
	public BundleInitThread(File rootDir, List<String> filenames) {	
		bundleDirs = new ArrayList<File>();
		for (String bfn : filenames) 
			bundleDirs.add(new File(rootDir, bfn.trim()));
		
		logger = Launcher.getLogger();
	}
	
	/**
	 * @param directories
	 */
	public BundleInitThread(Collection<File> directories) {	
		bundleDirs = new ArrayList<File>();
		bundleDirs.addAll(directories);		
		logger = Launcher.getLogger();
	}

	@Override
	public void run() {		
		// A collection for all bundles.
		Collection<BundleJarWrapper> all = new ArrayList<BundleJarWrapper>();
		// A collection for newly installed bundles.
		Collection<BundleJarWrapper> installed = new ArrayList<BundleJarWrapper>();
		// A collection for started bundles.
		Collection<BundleJarWrapper> started = new ArrayList<BundleJarWrapper>();
		// A collection for stopped bundles.
		Collection<BundleJarWrapper> stopped = new ArrayList<BundleJarWrapper>();
		// A collection for uninstalled bundles.
		Collection<File> uninstalled = new ArrayList<File>();
		
		for (File bundleDir : bundleDirs) {
			//Verify and setup fs
			if (bundleDir.isFile()) {
				logger.log(LogService.LOG_ERROR, "Bundle directory is a file, cannot start: " + bundleDir + ".");
				return;
			}
				
			logger.log(LogService.LOG_INFO, "Scanning bundle directory: " + bundleDir);
			
			if (!bundleDir.exists()) {
				if (!bundleDir.mkdirs()) 
					logger.log(LogService.LOG_ERROR, "Bundle directory cannot be created: " + bundleDir + ".");
				
				continue;
			}
						
			//Install bundles
			Collection<BundleJarWrapper> bundles = Applier.map(
					Applier.map(bundleDir, FileFunctions.GET_FILES_FN),
					new InstallBundleFunction(installed, Launcher.getBundleContext()));		
			
			all.addAll(bundles);
			
			//Start bundles
			started.addAll(Applier.map(
					Applier.map(
							bundles, new StartableBundleFilter()), new StartBundleFunction()));
			
			//Stop bundles
			stopped.addAll(Applier.map( 
					Applier.map(
							bundles, new StoppableBundleFilter()), new StopBundleFunction()));			
		}
		
		//Uninstall bundles
		uninstalled.addAll(Applier.map(
				Applier.map(
						bundleJarSizeMap.keySet(), new UninstallBundleFilter(all)), 
						new UninstallBundleFunction()));
	
		if (installed != null && installed.size() > 0)
			logger.log(LogService.LOG_INFO, "Installed Bundles: " + installed);
		
		if (started != null && started.size() > 0)
			logger.log(LogService.LOG_INFO, "Started Bundles: " + started);
		
		if (stopped != null && stopped.size() > 0)
			logger.log(LogService.LOG_INFO, "Stopped Bundles: " + stopped);
		
		if (uninstalled != null && uninstalled.size() > 0)
			logger.log(LogService.LOG_INFO, "Uninstalled Bundles: " + uninstalled);
	}

	/**
	 * @return Map of installed bundles that their size when installed.
	 */
	public static Map<File, Long> getBundleSizeMap() {

		return bundleJarSizeMap;
	}

}
