package org.knapsack.shell.commands;

import java.util.Map.Entry;

import org.knapsack.shell.AbstractKnapsackCommand;
import org.knapsack.shell.StringConstants;
import org.sprinkles.Applier;

/**
 * Prints the system property configuration.
 * 
 * @author kgilmer
 *
 */
public class PrintConfCommand extends AbstractKnapsackCommand {

	@Override
	public String execute() throws Exception {
		final StringBuilder sb = new StringBuilder();
		
		Applier.map(System.getProperties().entrySet(), new Applier.Fn<Entry<Object, Object>, Object>() {

			@Override
			public Object apply(Entry<Object, Object> e) {
				sb.append(e.getKey());
				sb.append(" = ");
				sb.append(e.getValue());
				sb.append(StringConstants.CRLF);
				return e;
			}
		});
		
		return sb.toString();
	}

	@Override
	public String getName() {
		return "printconfig";
	}
	
	@Override
	public String getUsage() {
		return "";
	}
	
	@Override
	public String getDescription() {
		return "Print the Java system configuration.";
	}
}
