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

import org.apache.felix.framework.Logger;
import org.knapsack.shell.ConsoleSocketListener;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.launch.Framework;
import org.osgi.service.log.LogService;

/**
 * A shutdown hook to shutdown Knapsack and Felix gracefully before exiting the JVM.
 * 
 * @author kgilmer
 *
 */
public class ShutdownHook extends Thread {
	private final Framework framework;
	private final File scriptDirectory;
	private final ConsoleSocketListener shell;
	private final ServiceRegistration initSR;
	private final Logger logger;

	/**
	 * @param framework
	 * @param scriptDirectory
	 * @param shell
	 * @param initSR
	 */
	public ShutdownHook(Framework framework, File scriptDirectory, ConsoleSocketListener shell, ServiceRegistration initSR, Logger logger) {
		super("Knapsack Shutdown Hook");
		this.framework = framework;
		this.scriptDirectory = scriptDirectory;
		this.shell = shell;
		this.initSR = initSR;
		this.logger = logger;
	}

	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		try {
			logger.log(LogService.LOG_INFO, "Shutdown started...");
						
			//These must be shutdown because they were created outside of the OSGi context.
			if (shell != null)
				shell.shutdown();
			
			if (initSR != null)
				initSR.unregister();
			
			if (framework != null) {
				framework.stop();
				framework.waitForStop(0);
			}

			FSHelper.deleteFilesInDir(scriptDirectory);
			logger.log(LogService.LOG_INFO, "Shutdown complete.");
		} catch (Exception ex) {
			System.err.println("Error during felix shutdown: " + ex);
		}
	}
}
