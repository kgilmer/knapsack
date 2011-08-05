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

import java.util.ArrayList;
import java.util.List;

import org.knapsack.PropertyHelper;
import org.knapsack.PropertyKeys;
import org.knapsack.shell.pub.IKnapsackCommand;
import org.osgi.framework.BundleContext;

/**
 * A helper base class for commands for the command line. Refer to IKnapsackCommand for
 * details of how to write commands for OSGi shell.
 * 
 * @author kgilmer
 * 
 */
public abstract class AbstractKnapsackCommand implements IKnapsackCommand {

	protected List<String> arguments;

	protected BundleContext context;
	
	protected static Ansi ansi;
	
	public void initialize(List<String> arguments, BundleContext context) {
		Ansi.setEnabled(PropertyHelper.getBoolean(PropertyKeys.CONFIG_KEY_COLOR_OUTPUT));
		this.ansi = Ansi.ansi();
		
		if (arguments != null) {
			this.arguments = arguments;
		} else {
			this.arguments = new ArrayList<String>();
		}

		this.context = context;
	}	
	
	@Override
	public List<String> getArguments() {
		return arguments;
	}

	/* (non-Javadoc)
	 * @see com.buglabs.osgi.shell.ICommand#isValid()
	 */
	public boolean isValid() {
		return true;
	}

	public String getUsage() {
		return "";
	}

	public String getDescription() {
		return "No help available for this command.";
	}
}