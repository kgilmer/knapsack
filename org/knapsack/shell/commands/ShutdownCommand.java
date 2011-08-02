package org.knapsack.shell.commands;

import org.knapsack.shell.AbstractKnapsackCommand;
import org.osgi.service.log.LogService;

/**
 * A command to exit the OSGi framework.
 * 
 * @author kgilmer
 * 
 */
public class ShutdownCommand extends AbstractKnapsackCommand {
	
	private final LogService log;
	public ShutdownCommand(LogService log) {
		this.log = log;
	}

	private static final String MSG = "OSGi framework is shutting down due to user request via shell.";
	public String execute() throws Exception {
		log.log(LogService.LOG_INFO, MSG);
		//If force option enabled, kill the jvm after 4 seconds regardless if framework shutdown is complete.
		if (arguments.contains("-f"))
			(new Thread() {
				@Override
				public void run() {
					try {
						Thread.sleep(4000);
						System.exit(0);
					} catch (InterruptedException e) {
						
					}
				}
			}).start();
		context.getBundle(0).stop();
		
		return MSG;
	}

	public String getName() {
		return "shutdown-knapsack";
	}
	
	@Override
	public String getUsage() {			
		return "-f (force)";
	}

	public String getDescription() {
		return "Stop all bundles and shutdown OSGi runtime.";
	}
}
