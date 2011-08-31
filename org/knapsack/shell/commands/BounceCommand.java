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

/**
 * Stop and restart a bundle.  Waits 1 second after stop for bundle to perform any cleanup.
 * 
 * @author kgilmer
 *
 */
public class BounceCommand extends AbstractKnapsackCommand {

	@Override
	public String execute() throws Exception {
	
		int bundleId = Integer.parseInt(arguments.get(0));
		
		Bundle b = context.getBundle(bundleId);
		
		if (b != null) {
			b.stop();
			Thread.sleep(1000);
			b.start();
		}
			
		return "Bounced " + BundlesCommand.getBundleName(b) + ".";
	}

	@Override
	public String getName() {
		return "bounde";
	}
	
	@Override
	public boolean isValid() {
		
		return arguments.size() == 1;
	}
	
	@Override
	public String getUsage() {
		return "(bundle id)";
	}
	
	@Override
	public String getDescription() {
		return "Stop and then start a bundle.";
	}
}

