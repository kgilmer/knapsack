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
package org.knapsack.shell;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.knapsack.FSHelper;
import org.knapsack.Launcher;
import org.knapsack.shell.pub.IKnapsackCommand;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 * Parses Strings into IKnapSackCommand instances.
 * 
 * @author kgilmer
 * 
 */
public class CommandParser implements ServiceListener {
	private Map<String, IKnapsackCommand> commands;

	private final BundleContext context;

	private final File scriptDir;

	public CommandParser(final BundleContext context, final File scriptDir) throws IOException {
		this.context = context;
		this.scriptDir = scriptDir;
		commands = new Hashtable<String, IKnapsackCommand>();
	}

	/**
	 * @param commandLine
	 * @return
	 * @throws IOException
	 */
	protected IKnapsackCommand parse(String commandLine) throws IOException {	
		String[] tokens = commandLine.split(" ");
		boolean quoteMode = false;
		String command = tokens[0];
		List<String> args = null;

		if (tokens.length > 1) {
			StringBuffer quotedParam = new StringBuffer();
			args = new ArrayList<String>();

			for (int i = 1; i < tokens.length; ++i) {
				if (tokens[i].startsWith("\"") && tokens[i].endsWith("\"")) {
					tokens[i] = tokens[i].substring(1, tokens[i].length() - 1);
				} else if (tokens[i].startsWith("\"")) {
					if (quoteMode) {
						throw new IOException("Parse Error: Cannot nest quotes.");
					}
					quoteMode = true;
					quotedParam = new StringBuffer();
					tokens[i] = tokens[i].substring(1);
				} else if (tokens[i].endsWith("\"")) {
					if (!quoteMode) {
						throw new IOException("Parse Error: End quote with no starting quote." + StringConstants.CRLF);
					}
					quoteMode = false;
					quotedParam.append(tokens[i].substring(0, tokens[i].length() - 1));
					tokens[i] = quotedParam.toString();
				}

				if (!quoteMode) {
					if (tokens[i].trim().length() > 0) {
						args.add(tokens[i].trim());
					}
				} else {
					quotedParam.append(tokens[i]);
					quotedParam.append(" ");
				}
			}
		}

		if (quoteMode)
			throw new IOException("Parse Error: unclosed quotes." + StringConstants.CRLF);

		if (commands.containsKey(command)) {
			IKnapsackCommand cmd = (IKnapsackCommand) commands.get(command);
			cmd.initialize(args, context);
			return cmd;
		}

		return null;
	}

	/* (non-Javadoc)
	 * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
	 */
	public void serviceChanged(ServiceEvent event) {
		final ServiceReference ref = event.getServiceReference();
		final int type = event.getType();

		if (type == ServiceEvent.REGISTERED) {
			final IKnapsackCommand cmd = (IKnapsackCommand) context.getService(ref);		
			
			if (commands.containsKey(cmd.getName())) {
				Launcher.getLogger().log(LogService.LOG_WARNING, "A shell command named " + cmd.getName() + " has already been registered.  Ignoring second registration.");
			} else {
				addCommand(cmd);				
			}
		} else if (type == ServiceEvent.UNREGISTERING) {
			if (ref.getBundle().getState() != Bundle.UNINSTALLED && context.getBundle() != null) {
				final IKnapsackCommand cmd = (IKnapsackCommand) context.getService(ref);				
				removeCommand(cmd);
			}
		}
	}
	
	public Map<String, IKnapsackCommand> getCommands() {
		return Collections.unmodifiableMap(commands);
	}
	
	private void addCommand(IKnapsackCommand command) {
		commands.put(command.getName(), command);
		try {
			FSHelper.createFilesystemCommand(scriptDir, command.getName(), Launcher.getLogger());
		} catch (IOException e) {
			//Ignore this error, the symlink was created by a pre-existing instance.
		}
	}
	
	private void removeCommand(IKnapsackCommand command) {
		commands.remove(command.getName());
		try {
			FSHelper.deleteFilesystemCommand(scriptDir, command.getName());
		} catch (IOException e) {
			Launcher.getLogger().log(LogService.LOG_ERROR, "Error while unregistering command " + command.getName(), e);
		}
	}
}
