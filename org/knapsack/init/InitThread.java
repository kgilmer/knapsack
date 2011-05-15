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
import java.util.Collection;

import org.knapsack.Activator;
import org.osgi.service.log.LogService;
import org.sprinkles.Fn;
import org.sprinkles.functions.ReturnFilesFunction;

public class InitThread extends Thread {

	private final File rootDir;

	public InitThread(File rootDir) {
		this.rootDir = rootDir;
	}

	@Override
	public void run() {
		//Verify and setup fs
		if (rootDir.isFile()) {
			Activator.log(LogService.LOG_ERROR, "Init directory is a file, cannot start: " + rootDir + ".");
			return;
		}
			
		if (!rootDir.exists())
			if (!rootDir.mkdirs()) {
				Activator.log(LogService.LOG_ERROR, "Init directory cannot be created: " + rootDir + ".");
				return;
			}
				
		
		//Install bundles
		Collection<BundleJarWrapper> bundles = Fn.map(new InstallBundleFunction(), 
				Fn.map(ReturnFilesFunction.GET_FILES_FN, rootDir));
		
		//Start bundles
		Collection<BundleJarWrapper> started = Fn.map(new StartBundleFunction(), 
				Fn.map(new StartableBundleFilter(), bundles));
		
		//Stop bundles
		Collection<BundleJarWrapper> stopped = Fn.map(new StopBundleFunction(), 
				Fn.map(new StoppableBundleFilter(), bundles));
		
		if (started != null && started.size() > 0)
			Activator.log(LogService.LOG_INFO, "Started Bundles: " + started);
		
		if (stopped != null && stopped.size() > 0)
			Activator.log(LogService.LOG_INFO, "Stopped Bundles: " + stopped);
	}
}
