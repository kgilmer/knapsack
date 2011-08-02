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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.commons.io.IOUtils;

/**
 * Emulates the portion of netcat that reads from stdin and writes to a socket.  This class
 * is used instead of netcat due to different implementation APIs provided by various hosts.
 * 
 * @author kgilmer
 *
 */
public class Netcat {

	/**
	 * @param args input arguments
	 * @throws UnknownHostException on name resolution error
	 * @throws IOException on IO exception
	 */
	public static void main(String[] args) throws UnknownHostException, IOException {
		//Check input parameters
		if (args.length != 2) {
			System.err.println("Invalid parameters");
			System.out.println("Usage: Netcat [hostname] [port]");
			System.exit(1);
		}
		
		run(args[0], Integer.parseInt(args[1]), System.in, System.out);		
	}
	
	/**
	 * Execute a shell command by connecting to socket, sending command, and reading response.
	 * 
	 * @param host host name
	 * @param port port
	 * @param input input stream to read command from
	 * @param output output stream to send response to
	 * @throws UnknownHostException on name resolution error
	 * @throws IOException on I/O error
	 */
	public static void run(String host, int port, InputStream input, OutputStream output) throws UnknownHostException, IOException {			
		//Create and open socket
		Socket socket = new Socket(host, port);
		OutputStream out = socket.getOutputStream();
		InputStream in = socket.getInputStream();
		
		//Push, pull, close.
		IOUtils.copy(input, out);
		IOUtils.copy(in, output);		
		IOUtils.closeQuietly(socket);
	}
}
