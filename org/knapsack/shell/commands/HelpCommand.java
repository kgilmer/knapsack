package org.knapsack.shell.commands;

import org.knapsack.shell.AbstractKnapsackCommand;
import org.knapsack.shell.CommandParser;
import org.knapsack.shell.StringConstants;
import org.knapsack.shell.pub.IKnapsackCommand;

import org.sprinkles.Applier;

/**
 * Print help information.
 * 
 * @author kgilmer
 * 
 */
public class HelpCommand extends AbstractKnapsackCommand {
	
	private final CommandParser parser;

	public HelpCommand(CommandParser parser) {
		this.parser = parser;		
	}

	public String execute() throws Exception {
		final StringBuilder sb = new StringBuilder();
		
		Applier.map(parser.getCommands().values(), new PrintHelpFunction(sb));
		
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
	
	private class PrintHelpFunction implements Applier.Fn<IKnapsackCommand, IKnapsackCommand> {
		
		private final StringBuilder sb;

		public PrintHelpFunction(StringBuilder sb) {
			this.sb = sb;
		}

		@Override
		public IKnapsackCommand apply(IKnapsackCommand cmd) {
			sb.append(pad(cmd.getName() + " " + cmd.getUsage(), 20));
	
			sb.append(StringConstants.TAB);
			sb.append(cmd.getDescription());
			sb.append(StringConstants.CRLF);
					
			return cmd;
		}
		
		private String pad(String in, int len) {
			if (in.length() >= len)
				return in;
			
			int diff = len - in.length();
			StringBuilder sb = new StringBuilder(in);
			for (int i = 0; i < diff; i++)
				sb.append(' ');
			
			return sb.toString();
		}
	}
}
