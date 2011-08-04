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

import org.knapsack.shell.StringConstants;
import org.osgi.framework.Bundle;
import org.sprinkles.Applier;

/**
 * A function that prints bundle information to the input StringBuilder.
 * 
 * @author kgilmer
 *
 */
class PrintBundleFunction implements Applier.Fn<Bundle, Bundle> {
	private final boolean verbose;
	private final StringBuilder sb;

	/**
	 * @param sb StringBuilder
	 * @param verbose verbose output
	 */
	public PrintBundleFunction(StringBuilder sb, boolean verbose) {
		this.sb = sb;
		this.verbose = verbose;
	}

	@Override
	public Bundle apply(Bundle b) {
		if (verbose) {	
			BundlesCommand.getStateName(b.getState(), sb);
			sb.append(StringConstants.TAB);			
			sb.append(BundlesCommand.getBundleLabel(b));
			sb.append(StringConstants.TAB);			
			sb.append(BundlesCommand.getBundleLocation(b));				
		} else {
			sb.append(BundlesCommand.getBundleLabel(b));
		}
		sb.append(StringConstants.CRLF);
		
		return b;
	}
}
