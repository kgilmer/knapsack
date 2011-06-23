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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;

/**
 * Listens for user entry on a socket.
 * 
 * @author kgilmer
 * 
 */
public class ConsoleSocketListener extends Thread {
	private static final String LOOPBACK_IP_ADDRESS = "127.0.0.1";
	/**
	 * Default for the ServerSocket backlog.
	 */
	private static final int SERVER_BACKLOG_DEFAULT = 1;

	private final int port;

	private volatile boolean running = false;

	private final BundleContext context;

	private CommandExecutor executor;

	private final CommandParser parser;

	private final LogService log;

	private ServiceRegistration commandProviderRegistration;

	private ServerSocket socket;

	public ConsoleSocketListener(int port, BundleContext context, LogService log) throws UnknownHostException, IOException, InvalidSyntaxException {
		this.port = port;
		this.parser = new CommandParser(context, log);
		context.addServiceListener(parser, "(" + Constants.OBJECTCLASS + "=" + IKnapsackCommandProvider.class.getName() + ")");
		this.context = context;
		this.log = log;
		this.socket = createServerSocket();
	}
	
	public void run() {
		running = true;
		try {
			if (commandProviderRegistration == null) {
				commandProviderRegistration = context.registerService(IKnapsackCommandProvider.class.getName(), new BuiltinCommands(parser, log), null);
			}

			while (running) {
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
						if (!resp.endsWith("\n"))
							out.write('\n');
					}
				}
				
				connection.close();
			}
		} catch (Exception e) {
			log.log(LogService.LOG_ERROR, "An Error occurred while while processing command.", e);
			if (commandProviderRegistration != null) {
				commandProviderRegistration.unregister();
			}
		} finally {
			socket = null;
			context.removeServiceListener(parser);
		}
	}

	/**
	 * @return A ServerSocket based on runtime configuration.
	 * @throws IOException
	 * @throws UnknownHostException
	 */
	private ServerSocket createServerSocket() throws UnknownHostException, IOException {
		return new ServerSocket(port, SERVER_BACKLOG_DEFAULT, InetAddress.getLocalHost());
	}

	/**
	 * Shutdown the listener. No new client connections will be accepted.
	 */
	public void shutdown() {		
		running = false;
		this.interrupt();
		if (socket != null)
			try {
				socket.close();
			} catch (IOException e) {				
			}		
	}

}
