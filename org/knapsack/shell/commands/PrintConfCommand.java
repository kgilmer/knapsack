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

import java.util.Map.Entry;

import org.knapsack.shell.AbstractKnapsackCommand;
import org.knapsack.shell.StringConstants;
import org.sprinkles.Applier;

/**
 * Prints the system property configuration.
 * 
 * @author kgilmer
 *
 */
public class PrintConfCommand extends AbstractKnapsackCommand {

	@Override
	public String execute() throws Exception {
		final StringBuilder sb = new StringBuilder();
		
		Applier.map(System.getProperties().entrySet(), new Applier.Fn<Entry<Object, Object>, Object>() {

			@Override
			public Object apply(Entry<Object, Object> e) {
				sb.append(e.getKey());
				sb.append(" = ");
				sb.append(e.getValue());
				sb.append(StringConstants.CRLF);
				return e;
			}
		});
		
		return sb.toString();
	}

	@Override
	public String getName() {
		return "printconfig";
	}
	
	@Override
	public String getUsage() {
		return "";
	}
	
	@Override
	public String getDescription() {
		return "Print the Java system configuration.";
	}
}
