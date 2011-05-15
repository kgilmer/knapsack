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
