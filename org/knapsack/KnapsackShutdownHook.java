package org.knapsack;

import java.io.File;

import org.knapsack.shell.ConsoleSocketListener;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.launch.Framework;

public class KnapsackShutdownHook extends Thread {
	private final Framework framework;
	private final File scriptDirectory;
	private final ConsoleSocketListener shell;
	private final ServiceRegistration initSR;

	public KnapsackShutdownHook(Framework framework, File scriptDirectory, ConsoleSocketListener shell, ServiceRegistration initSR) {
		super("Knapsack Shutdown Hook");
		this.framework = framework;
		this.scriptDirectory = scriptDirectory;
		this.shell = shell;
		this.initSR = initSR;
	}

	public void run() {
		try {
			shell.shutdown();
			initSR.unregister();
			
			if (framework != null) {
				framework.stop();
				framework.waitForStop(0);
			}

			FSHelper.deleteFilesInDir(scriptDirectory);
		} catch (Exception ex) {
			System.err.println("Error stopping framework: " + ex);
		}
	}
}
