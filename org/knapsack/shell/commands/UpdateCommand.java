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

import java.io.File;
import java.util.Arrays;

import org.knapsack.Activator;
import org.knapsack.Config;
import org.knapsack.init.InitThread;
import org.knapsack.shell.AbstractKnapsackCommand;

/**
 * A command to rescan the configured bundle directories looking for changes.
 * 
 * Changes include:
 * - new or removed bundles: results in install or uninstall of bundle
 * - execute bit set or unset: results in start or stop of bundle
 * 
 * @author kgilmer
 *
 */
public class UpdateCommand extends AbstractKnapsackCommand {

	@Override
	public String execute() throws Exception {
		Config config = Activator.getConfig();
		
		InitThread init = new InitThread(new File(config.getString(Config.CONFIG_KEY_ROOT_DIR)), Arrays.asList(config.getString(Config.CONFIG_KEY_BUNDLE_DIRS).split(",")));
		init.start();
		
		return "Rescanning and updating bundles from configured directories.";
	}

	@Override
	public String getName() {
		return "update";
	}
	
	@Override
	public String getDescription() {			
		return "Rescan the bundle directory or directories and update bundlespace accordingly.";
	}
}