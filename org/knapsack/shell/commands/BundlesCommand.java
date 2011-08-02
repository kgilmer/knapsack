package org.knapsack.shell.commands;

import org.knapsack.shell.AbstractKnapsackCommand;
import org.osgi.framework.Bundle;

import org.sprinkles.Applier;

/**
 * Print bundle information.
 * 
 * @author kgilmer
 *
 */
public class BundlesCommand extends AbstractKnapsackCommand {

	@Override
	public String execute() throws Exception {
		final StringBuilder sb = new StringBuilder();
		
		Applier.map(context.getBundles(), new PrintBundleFunction(sb, !arguments.contains("-b")));
		
		return sb.toString();
	}

	@Override
	public String getName() {
		return "bundles";
	}
	
	@Override
	public String getUsage() {
		return "[-b (brief)]";
	}
	
	@Override
	public String getDescription() {
		return "Get list of OSGi bundles installed in the framework.";
	}

	public static void appendId(StringBuilder sb, long id) {
		sb.append("[");
		if (id < 10)
			sb.append(" ");
			
		sb.append(id);
		sb.append("]");
	}

	/**
	 * @param b
	 * @return The location on filesystem of bundle
	 */
	public static String getBundleLocation(Bundle b) {
		//Remove "file://" prefix
		return b.getLocation().substring(7);
	}

	/**
	 * @param b
	 * @return The bundle version as defined in the manifest.
	 */
	public static String getBundleVersion(Bundle b) {
		String version = (String) b.getHeaders().get("Bundle-Version");
	
		if (version == null) {
			version = "";
		}
	
		return version;
	}

	public static String getBundleLabel(Bundle b) {
		StringBuilder sb = new StringBuilder();
		
		BundlesCommand.appendId(sb, b.getBundleId());
		sb.append(getBundleName(b));
		sb.append(" (");
		sb.append(BundlesCommand.getBundleVersion(b));
		sb.append(")");
	
		return sb.toString();
	}

	public static String getBundleName(Bundle b) {
		String name = (String) b.getHeaders().get("Bundle-SymbolicName");
	
		if (name == null) {
			name = (String) b.getHeaders().get("Bundle-Name");
		}
	
		if (name == null) {
			name = "Undefined";
		}
	
		if (name.indexOf(";") > -1)
			name = name.split(";")[0];
	
		return name;
	}

	/**
	 * Return state label as defined in OSGi spec.
	 * 
	 * @param state
	 * @return
	 */
	public static String getStateName(int state) {
		switch (state) {
		case 0x00000001:
			return "UNINS";
		case 0x00000002:
			return "INSTL";
		case 0x00000004:
			return "RESOL";
		case 0x00000008:
			return "START";
		case 0x00000010:
			return " STOP";
		case 0x00000020:
			return "ACTIV";
		}
	
		return "[UNKNOWN STATE: " + state + "]";
	}
}
