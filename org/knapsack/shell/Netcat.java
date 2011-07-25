package org.knapsack.shell;

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
		
		//Load parameters
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		
		//Create and open socket
		Socket socket = new Socket(host, port);
		OutputStream out = socket.getOutputStream();
		InputStream in = socket.getInputStream();
		
		//Push, pull, close.
		IOUtils.copy(System.in, out);
		IOUtils.copy(in, System.out);		
		IOUtils.closeQuietly(socket);
	}
}
