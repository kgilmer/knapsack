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
		final StringBuilder sb = new StringBuilder(1024 * 8);
		
		Applier.map(
				context.getBundles(), 
				new PrintBundleFunction(sb, arguments.contains("-v")));
		
		return sb.toString();
	}

	@Override
	public String getCommandName() {
		return "bundles";
	}
	
	@Override
	public String getUsage() {
		return "[-v (verbose)]";
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
		StringBuilder sb = new StringBuilder();
		sb.append(b.getLocation().substring(7));
		
		return sb.toString();
	}

	/**
	 * @param b
	 * @return The bundle version as defined in the manifest.
	 */
	public static String getBundleVersion(Bundle b) {
		if (b == null)
			return "";
		
		String version = (String) b.getHeaders().get("Bundle-Version");	
		
		if (version == null) {
			version = "";
		}
	
		StringBuilder sb = new StringBuilder();
		sb.append(version);
		
		return sb.toString();
	}

	public static String getBundleLabel(Bundle b) {
		if (b == null)
			return "";
		
		StringBuilder sb = new StringBuilder();
		
		BundlesCommand.appendId(sb, b.getBundleId());
		sb.append(getBundleName(b));
		sb.append(" (");
		sb.append(BundlesCommand.getBundleVersion(b));
		sb.append(")");
	
		return sb.toString();
	}

	public static String getBundleName(Bundle b) {
		if (b == null)
			return "[null]";
				
		String name = (String) b.getHeaders().get("Bundle-SymbolicName");
	
		if (name == null) {
			name = (String) b.getHeaders().get("Bundle-Name");
		}
	
		if (name == null) {
			name = "Undefined";
		}
	
		if (name.indexOf(";") > -1)
			name = name.split(";")[0];
	
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		
		return sb.toString();
	}

	/**
	 * Return state label as defined in OSGi spec.
	 * 
	 * @param state
	 * @return
	 */
	public static void getStateName(int state, StringBuilder sb) {
		String l = null;
		switch (state) {
		case 0x00000001:
			l = "UNINS";
			break;
		case 0x00000002:
			l = "INSTL";
			break;
		case 0x00000004:
			l = "RESOL";
			break;
		case 0x00000008:
			l = "START";
			break;
		case 0x00000010:
			l = " STOP";
			break;
		case 0x00000020:
			l = "ACTIV";
			break;
		default:
			l = "[UNKNOWN STATE: " + state + "]";
			break;
		}		
		
		sb.append(l);
	}
}
