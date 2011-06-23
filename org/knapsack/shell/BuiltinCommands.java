package org.knapsack.shell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.log.LogService;
import org.sprinkles.Fn;

public class BuiltinCommands implements IKnapsackCommandProvider {
	
	private final CommandParser parser;
	private final LogService log;

	public BuiltinCommands(CommandParser parser, LogService log) {
		this.parser = parser;
		this.log = log;
	}

	public List<IKnapsackCommand> getCommands() {
		List<IKnapsackCommand> cmds = new ArrayList<IKnapsackCommand>();

		cmds.add(new ExitCommand());
		cmds.add(new HelpCommand());

		return cmds;
	}
	

	/**
	 * Exit the framework.
	 * 
	 * @author kgilmer
	 * 
	 */
	private class ExitCommand extends AbstractKnapsackCommand {

		public String execute() throws Exception {
			log.log(LogService.LOG_INFO, "OSGi framework is shutting down due to user request via shell.");
			context.getBundle(0).stop();
			
			return "OSGi framework is shutting down.";
		}

		public String getName() {
			return "exit";
		}

		public String getDescription() {
			return "Stop all bundles and shutdown OSGi runtime.";
		}
	}

	/**
	 * Print help information.
	 * 
	 * @author kgilmer
	 * 
	 */
	private class HelpCommand extends AbstractKnapsackCommand {

		public String execute() throws Exception {
			final StringBuffer sb = new StringBuffer();
			
			Fn.map(new Fn.Function<IKnapsackCommand, IKnapsackCommand>() {
				
				@Override
				public IKnapsackCommand apply(IKnapsackCommand cmd) {
					String sl = cmd.getName() + "\t" + cmd.getUsage() + "\t" + cmd.getDescription() + "\n";
					
					sb.append(sl);
				
					return cmd;
				}
				
			}, parser.getCommands().values());
			
			return sb.toString();
		}

		public String getName() {
			return "help";
		}

		public String getUsage() {

			return super.getUsage();
		}

		public String getDescription() {
			return "Print table of currently available commands.";
		}
	}
}
