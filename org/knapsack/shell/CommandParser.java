/*******************************************************************************
 * Copyright (c) 2008, 2009 Bug Labs, Inc.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    - Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 *    - Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    - Neither the name of Bug Labs, Inc. nor the names of its contributors may be
 *      used to endorse or promote products derived from this software without
 *      specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/
package org.knapsack.shell;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.knapsack.Activator;
import org.knapsack.FSHelper;
import org.knapsack.shell.pub.IKnapsackCommand;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

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
			Activator.logInfo("A new command is available: " + cmd.getClass().toString());
			
			if (commands.containsKey(cmd.getName())) {
				Activator.logWarning("A shell command named " + cmd.getName() + " has already been registered.  Ignoring second registration.");
			} else {
				addCommand(cmd);				
			}
		} else if (type == ServiceEvent.UNREGISTERING) {
			if (ref.getBundle().getState() != Bundle.UNINSTALLED && context.getBundle() != null) {
				Activator.logDebug("Unregistering " + ref.getBundle().getLocation());
				final IKnapsackCommand cmd = (IKnapsackCommand) context.getService(ref);
		
				Activator.logDebug("Unregistering command " + cmd.getName());
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
			FSHelper.createFilesystemCommand(scriptDir, command.getName());
		} catch (IOException e) {
			//Ignore this error, the symlink was created by a pre-existing instance.
		}
	}
	
	private void removeCommand(IKnapsackCommand command) {
		commands.remove(command.getName());
		try {
			FSHelper.deleteFilesystemCommand(scriptDir, command.getName());
		} catch (IOException e) {
			Activator.logError("Error while unregistering command " + command.getName(), e);
		}
	}
}
