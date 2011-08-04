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

import org.knapsack.Config;
import org.knapsack.shell.ConsoleSocketListener;
import org.knapsack.shell.commands.Ansi.Attribute;
import org.knapsack.shell.commands.Ansi.Color;
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
		sb.append(ansi.fg(Color.BLUE));
		sb.append(id);
		sb.append(ansi.a(Attribute.RESET));
		sb.append("]");		
	}

	/**
	 * @param b
	 * @return The location on filesystem of bundle
	 */
	public static String getBundleLocation(Bundle b) {
		//Remove "file://" prefix
		StringBuilder sb = new StringBuilder();
		sb.append(ansi.a(Attribute.INTENSITY_FAINT));
		sb.append(b.getLocation().substring(7));
		sb.append(ansi.a(Attribute.RESET));
		
		return sb.toString();
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
	
		StringBuilder sb = new StringBuilder();
		sb.append(ansi.fg(Color.CYAN));
		sb.append(version);
		sb.append(ansi.a(Attribute.RESET));
		
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
		sb.append(ansi.a(Attribute.INTENSITY_BOLD));
		sb.append(name);
		sb.append(ansi.a(Attribute.RESET));
		
		return sb.toString();
	}

	/**
	 * Return state label as defined in OSGi spec.
	 * 
	 * @param state
	 * @return
	 */
	public static void getStateName(int state, StringBuilder sb) {
		Color c = Color.BLACK;
		String l = null;
		switch (state) {
		case 0x00000001:
			l = "UNINS";
			c = Color.MAGENTA;
			break;
		case 0x00000002:
			l = "INSTL";
			c = Color.CYAN;
			break;
		case 0x00000004:
			l = "RESOL";
			break;
		case 0x00000008:
			l = "START";
			c = Color.YELLOW;
			break;
		case 0x00000010:
			l = " STOP";
			c = Color.RED;
			break;
		case 0x00000020:
			l = "ACTIV";
			c = Color.GREEN;
			break;
		default:
			l = "[UNKNOWN STATE: " + state + "]";
			break;
		}
		
		sb.append(ansi.a(Attribute.NEGATIVE_ON));
		sb.append(ansi.fg(c));
		sb.append(l);
		sb.append(ansi.a(Attribute.RESET));	
	}
}
