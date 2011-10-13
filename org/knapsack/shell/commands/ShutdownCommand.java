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

import org.knapsack.KnapsackLogger;
import org.knapsack.Launcher;
import org.osgi.framework.launch.Framework;
import org.osgi.service.log.LogService;

/**
 * A command to exit the OSGi framework.
 * 
 * @author kgilmer
 * 
 */
public class ShutdownCommand extends AbstractKnapsackCommand {

	private final KnapsackLogger log;

	public ShutdownCommand(KnapsackLogger log2) {
		this.log = log2;
	}

	private static final String MSG = "OSGi framework is shutting down due to user request via shell.";

	public String execute() throws Exception {
		System.exit(0);		

		return MSG;
	}

	public String getCommandName() {
		return "shutdown";
	}

	@Override
	public String getUsage() {
		return super.getUsage();
	}

	public String getDescription() {
		return "Stop all bundles and shutdown OSGi runtime.";
	}
}
