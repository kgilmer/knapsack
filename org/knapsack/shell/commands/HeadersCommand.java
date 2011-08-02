package org.knapsack.shell.commands;

import java.util.Dictionary;
import java.util.Enumeration;

import org.knapsack.shell.AbstractKnapsackCommand;
import org.knapsack.shell.StringConstants;
import org.osgi.framework.Bundle;
import org.sprinkles.Applier;

/**
 * Print bundle header information.
 * 
 * @author kgilmer
 *
 */
public class HeadersCommand extends AbstractKnapsackCommand {

	@Override
	public String execute() throws Exception {
		final StringBuilder sb = new StringBuilder();
		PrintHeadersFunction function = new PrintHeadersFunction(sb);
		
		if (arguments.size() == 1) {
			int bundleId = Integer.parseInt(arguments.get(0));
			function.setPrintBundle(false);
			Bundle b = context.getBundle(bundleId);
			
			if (b != null)
				function.apply(b);
			
		} else {
			function.setPrintBundle(true);
			Applier.map(context.getBundles(), function);
		}
		
		return sb.toString();
	}

	@Override
	public String getName() {
		return "headers";
	}
	
	@Override
	public String getUsage() {
		return "[bundle id]";
	}
	
	@Override
	public String getDescription() {
		return "Print bundle headers.";
	}
	
	private class PrintHeadersFunction implements Applier.Fn<Bundle, Bundle> {
		
		private final StringBuilder sb;
		private boolean printBundle = false;
		private PrintBundleFunction printBundleFunction;

		public PrintHeadersFunction(StringBuilder sb) {
			this.sb = sb;			
			this.printBundleFunction = new PrintBundleFunction(sb, true);
		}
		
		public void setPrintBundle(boolean b) {
			this.printBundle  = b;
		}

		@Override
		public Bundle apply(Bundle b) {
			Dictionary headers = b.getHeaders();
			Enumeration keys = headers.keys();
			
			if (printBundle) 
				printBundleFunction.apply(b);	
			
			while (keys.hasMoreElements()) {
				Object key = keys.nextElement();
				sb.append(key.toString());
				sb.append(": ");
				sb.append(headers.get(key).toString());
				sb.append(StringConstants.CRLF);
			}
			
			if (printBundle) 
				sb.append(StringConstants.CRLF);	
			
			return b;
		}
	}
}

