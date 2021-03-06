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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.knapsack.KnapsackLogger;
import org.knapsack.PropertyHelper;
import org.knapsack.ConfigurationConstants;
import org.knapsack.shell.commands.BounceCommand;
import org.knapsack.shell.commands.BundlesCommand;
import org.knapsack.shell.commands.HeadersCommand;
import org.knapsack.shell.commands.HelpCommand;
import org.knapsack.shell.commands.LogCommand;
import org.knapsack.shell.commands.PackagesCommand;
import org.knapsack.shell.commands.PrintConfCommand;
import org.knapsack.shell.commands.ServicesCommand;
import org.knapsack.shell.commands.ShutdownCommand;
import org.knapsack.shell.commands.UpdateCommand;
import org.knapsack.shell.pub.IKnapsackCommand;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;

/**
 * Listens for commands on a configured socket.
 * 
 * @author kgilmer
 * 
 */
public class ConsoleSocketListener extends Thread {
	/**
	 * Default for the ServerSocket backlog.
	 */
	private static final int SERVER_BACKLOG_DEFAULT = 1;

	private final int port;

	private volatile boolean running = false;

	private final BundleContext context;

	private CommandExecutor executor;

	private static CommandParser parser;

	private final KnapsackLogger log;

	private List<ServiceRegistration> commandRegistrations;

	private ServerSocket socket;

	private static String commandPrefix;

	/**
	 * @param config
	 * @param port
	 * @param context
	 * @param log
	 * @param parser
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws InvalidSyntaxException
	 */
	public ConsoleSocketListener(int port, BundleContext context, KnapsackLogger log, CommandParser parser, String commandPrefix)
			throws UnknownHostException, IOException, InvalidSyntaxException {

		this.parser = parser;
		this.commandPrefix = commandPrefix;
		context.addServiceListener(parser, "(" + Constants.OBJECTCLASS + "=" + IKnapsackCommand.class.getName() + ")");
		this.context = context;
		this.log = log;
		this.port = port;
	}	

	public void run() {
		running = true;
		try {
			if (commandRegistrations == null) {
				commandRegistrations = registerCommands();
			}
			
			this.socket = createServerSocket();

			while (running) {
				try {
					Socket connection = socket.accept();

					if (!running)
						return;

					if (executor == null)
						executor = new CommandExecutor(parser);

					BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
					OutputStream out = connection.getOutputStream();

					String sl = in.readLine();

					if (sl != null) {
						String resp = executor.executeCommand(sl.trim());

						if (resp != null && resp.length() > 0) {
							out.write(resp.getBytes());
							if (!resp.endsWith(StringConstants.CRLF))
								out.write(StringConstants.CRLF.getBytes());
						}
					}

					connection.close();
				} catch (Exception e) {
					log.log(LogService.LOG_ERROR, "An Error occurred while while processing command.", e);					
				}
			}
		} catch (Exception e) {
			if (e instanceof SocketException && ((SocketException)e).getMessage().startsWith("Socket closed")) {
				//Ignore exception
			} else {
				log.log(LogService.LOG_ERROR, "An Error occurred while while processing command.", e);
				if (commandRegistrations != null) {
					for (ServiceRegistration sr : commandRegistrations)
						sr.unregister();
				}
			}
		} finally {
			socket = null;
			try {
				context.removeServiceListener(parser);
			} catch (Exception e) {
				// Ignore unregistration errors.
			}
		}
	}

	private List<ServiceRegistration> registerCommands() {
		List<ServiceRegistration> cr = new ArrayList<ServiceRegistration>();
		
		cr.add(registerCommand(new ShutdownCommand()));
		cr.add(registerCommand(new HelpCommand()));
		cr.add(registerCommand(new BundlesCommand()));
		cr.add(registerCommand(new ServicesCommand()));
		cr.add(registerCommand(new LogCommand()));
		cr.add(registerCommand(new UpdateCommand()));
		cr.add(registerCommand(new PrintConfCommand()));
		cr.add(registerCommand(new HeadersCommand()));
		cr.add(registerCommand(new BounceCommand()));
		cr.add(registerCommand(new PackagesCommand()));
		
		return commandRegistrations;
	}
	
	private ServiceRegistration registerCommand(IKnapsackCommand cmd) {
		return context.registerService(IKnapsackCommand.class.getName(), cmd, createDictionary("command.name=" + cmd.getName()));
	}
	
	public static CommandParser getParser() {
		return parser;
	}

	/**
	 * Create a property dictionary by specifing name value pairs as one string seperated by "=" character.
	 * @param nvp String or set of Strings in the style "name1=value1".
	 * @return Dictionary loaded with name/value pairs.
	 */
	private Dictionary createDictionary(String ... nvp) {
		Dictionary d = new Hashtable();
		
		for (String p : Arrays.asList(nvp)) {
			String [] elems = p.split("=");
			
			if (elems.length != 2)
				throw new RuntimeException("Invalid name/value pair syntax: " + p);
			
			d.put(elems[0].trim(), elems[1].trim());
		}
		
		return d;
	}
	
	public static String getCommandPrefix() {
		return commandPrefix;
	}

	/**
	 * @return A ServerSocket based on runtime configuration.
	 * @throws IOException
	 * @throws UnknownHostException
	 */
	private ServerSocket createServerSocket() throws UnknownHostException, IOException {
		ServerSocket s;

		if (PropertyHelper.getBoolean(ConfigurationConstants.CONFIG_KEY_ACCEPT_ANY_HOST)) {
			s = new ServerSocket(port, SERVER_BACKLOG_DEFAULT, null);
			log.log(LogService.LOG_INFO, "Accepting socket connections from any host on port " + port);
		} else {
			InetAddress localhost = InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 });
			s = new ServerSocket(port, SERVER_BACKLOG_DEFAULT, localhost);
			log.log(LogService.LOG_INFO, "Accepting socket connections from " + localhost + " on port " + port);
		}

		return s;
	}

	/**
	 * @return the port that the socket listens on.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Shutdown the listener. No new client connections will be accepted.
	 */
	public void shutdown() {
		running = false;

		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}
}
