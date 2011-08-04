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
package org.knapsack.shell.commands;

import org.knapsack.shell.CommandParser;
import org.knapsack.shell.StringConstants;
import org.knapsack.shell.commands.Ansi.Attribute;
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
			sb.append(ansi.a(Attribute.INTENSITY_BOLD));
			sb.append(pad(cmd.getName(), 10));
			sb.append(ansi.a(Attribute.RESET));
			sb.append(" ");
			sb.append(ansi.a(Attribute.ITALIC));
			sb.append(pad(cmd.getUsage(), 10));
			sb.append(ansi.a(Attribute.RESET));
			sb.append(StringConstants.TAB);
			sb.append(ansi.a(Attribute.INTENSITY_FAINT));
			sb.append(cmd.getDescription());
			sb.append(ansi.a(Attribute.RESET));
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
