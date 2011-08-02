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
package org.knapsack.shell.pub;

import java.util.List;

import org.osgi.framework.BundleContext;

/**
 * A command interface for the knapsack shell.
 * 
 * @author kgilmer
 * 
 */
public interface IKnapsackCommand {
	/**
	 * Command initialization.
	 * 
	 * @param arguments
	 * @param out
	 * @param err
	 * @param context
	 */
	public void initialize(List<String> arguments, BundleContext context);

	/**
	 * @return List of arguments passed to command.
	 */
	public List<String> getArguments();
	/**
	 * Execute the command
	 * 
	 * @throws Exception
	 */
	public String execute() throws Exception;

	/**
	 * @return true if the command and parameters are valid.
	 */
	public boolean isValid();

	/**
	 * @return Name of command.
	 */
	public String getName();

	/**
	 * @return A short textual description of command usage.
	 */
	public String getUsage();

	/**
	 * @return A description of what the command does.
	 */
	public String getDescription();
}
