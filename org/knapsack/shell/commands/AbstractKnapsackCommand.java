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

import org.knapsack.shell.ConsoleSocketListener;
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
	
	public void initialize(List<String> arguments, BundleContext context) {
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
	
	public final String getName() {
		return ConsoleSocketListener.getCommandPrefix() + getCommandName();
	}
	
	/**
	 * @return The root name of the command.  The shell system may append or prepend based on configuration.
	 */
	public abstract String getCommandName();

	public String getDescription() {
		return "No help available for this command.";
	}
}
