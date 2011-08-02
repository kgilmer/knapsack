package org.knapsack.shell.commands;

import java.util.Collection;

import org.knapsack.shell.AbstractKnapsackCommand;
import org.knapsack.shell.StringConstants;
import org.osgi.framework.Bundle;
import org.sprinkles.Applier;
import org.sprinkles.Applier.Fn;

/**
 * Print bundle package information.
 * 
 * @author kgilmer
 * 
 */
public class PackagesCommand extends AbstractKnapsackCommand {

	@Override
	public String execute() throws Exception {
		final StringBuilder sb = new StringBuilder();
		boolean brief = arguments.contains("-b");
		
		PrintPackagesForBundleFunction function = new PrintPackagesForBundleFunction(sb, brief);
		
		if ((!brief && arguments.size() == 1) || (brief && arguments.size() == 2)) {
			String la = arguments.get(arguments.size() - 1);
			if (isNumber(la)) {
				//Print one bundle's package data.
				int bundleId = Integer.parseInt(la);
				function.setPrintBundle(false);
				Bundle b = context.getBundle(bundleId);

				if (b != null)
					function.apply(b);
			} else {
				//Print package info
				printPackageInfo(sb, la.trim(), brief);
			}
		} else {
			//Print all bundle package data.
			function.setPrintBundle(true);
			Applier.map(context.getBundles(), function);
		}

		return sb.toString();
	}

	/**
	 * Print the provider and consumers of the given package.
	 * 
	 * @param sb
	 * @param pkg
	 */
	private void printPackageInfo(StringBuilder sb, String pkg, boolean brief) {
		sb.append("Provided by");
		sb.append(StringConstants.CRLF);
		sb.append(StringConstants.TAB);
		
		Bundle providerBundle = Applier.find(context.getBundles(), new FindPackageProviderFunction(pkg, "Export-Package"));
		
		if (providerBundle == null) 
			sb.append("[NONE]");
		else
			if (!brief)
				sb.append(BundlesCommand.getBundleLabel(providerBundle));
			else
				sb.append(BundlesCommand.getBundleName(providerBundle));

		sb.append(StringConstants.CRLF);
		
		sb.append("Used by");
		sb.append(StringConstants.CRLF);
		Collection<Bundle> consumerBundles = Applier.map(context.getBundles(), new FindPackageProviderFunction(pkg, "Import-Package"));
		
		for (Bundle b : consumerBundles) {
			sb.append(StringConstants.TAB);
			if (!brief)
				sb.append(BundlesCommand.getBundleLabel(b));
			else
				sb.append(BundlesCommand.getBundleName(b));
			sb.append(StringConstants.CRLF);
		}
	}

	/**
	 * @param s
	 * @return
	 */
	private boolean isNumber(String s) {
		try {
			Integer.parseInt(s);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	@Override
	public String getName() {
		return "packages";
	}

	@Override
	public String getUsage() {
		return "[-b (brief)] [bundle id | package name]";
	}

	@Override
	public String getDescription() {
		return "Print package information.";
	}

	private class PrintPackagesForBundleFunction implements Applier.Fn<Bundle, Bundle> {

		private final StringBuilder sb;
		private boolean printBundle = false;
		private PrintBundleFunction printBundleFunction;
		private final boolean brief;

		public PrintPackagesForBundleFunction(StringBuilder sb, boolean brief) {
			this.sb = sb;
			this.brief = brief;
			this.printBundleFunction = new PrintBundleFunction(sb, true);
		}

		public void setPrintBundle(boolean b) {
			this.printBundle = b;
		}

		@Override
		public Bundle apply(Bundle b) {		
			if (printBundle) 
				printBundleFunction.apply(b);	
			
			Object imports = b.getHeaders().get("Import-Package");
			Object exports = b.getHeaders().get("Export-Package");
			
			FormatPackageElementFunction pf = new FormatPackageElementFunction(sb, brief);
			
			if (imports != null) {
				sb.append("Imports");
				sb.append(StringConstants.CRLF);
				
				Applier.map(imports.toString().split(","), pf);
			}
			
			if (exports != null) {
				sb.append("Exports");
				sb.append(StringConstants.CRLF);
				
				Applier.map(exports.toString().split(","), pf);		
			}
			
			if (printBundle) 
				sb.append(StringConstants.CRLF);	
			
			return b;
		}
	}
	
	private class FindPackageProviderFunction implements Fn<Bundle, Bundle> {

		private final String pkg;
		private final String header;

		public FindPackageProviderFunction(String pkg, String header) {
			this.pkg = pkg;
			this.header = header;
		}

		@Override
		public Bundle apply(final Bundle input) {
			Object exportsLine = input.getHeaders().get(header);
			
			if (exportsLine == null)
				return null;
			
			return Applier.find(exportsLine.toString().split(","), new Applier.Fn<String, Bundle>() {

				@Override
				public Bundle apply(String pkgName) {
					String [] elems = pkgName.split(";");
					
					if (elems.length > 0 && elems[0].trim().equals(pkg))
						return input;
					
					return null;
				}
			});			
		}
	}

	private class FormatPackageElementFunction implements Fn<String, String> {

		private final StringBuilder sb;
		private final boolean brief;

		public FormatPackageElementFunction(StringBuilder sb, boolean brief) {
			this.sb = sb;
			this.brief = brief;
		}

		@Override
		public String apply(String input) {
			if (brief)
				input = input.split(";")[0];
			
			sb.append(StringConstants.TAB);
			sb.append(input.trim());
			sb.append(StringConstants.CRLF);
			return input;
		}
	}
}
