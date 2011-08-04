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

import java.util.Dictionary;
import java.util.Enumeration;

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

