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
			sb.append(BundlesCommand.getStateName(b.getState()));
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
