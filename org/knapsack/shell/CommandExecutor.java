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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.knapsack.shell.pub.IKnapsackCommand;
import org.osgi.framework.BundleException;

/**
 * Given an input String, find command to execute, execute, and return results.
 * 
 * @author kgilmer
 * 
 */
public class CommandExecutor {
	private final CommandParser parser;
	protected CommandExecutor(CommandParser parser) {	
		this.parser = parser;
	}
	

	/**
	 * Executes the command entered by user.
	 * @param line
	 * @param justCrlf
	 * @param pwErr
	 * @throws IOException
	 */
	public String executeCommand(String line) throws IOException {
		if (line == null) {
			// JVM is being shutdown
			return "";
		} else if (line.length() > 0) {		
			IKnapsackCommand cmd = parser.parse(line);

			if (cmd == null) 
				return "Unknown command: " + line;
				
			if (hasHelpParam(cmd)) {
				String rs = "";
				
				if (cmd.getDescription() != null)
					rs = cmd.getDescription();
				
				return rs + StringConstants.CRLF + "Usage: " + cmd.getName() + " " + cmd.getUsage();
			} else if (cmd != null && cmd.isValid()) {
				try {
					return cmd.execute();
				} catch (Exception e) {
					String es = "An error occurred while executing: " + cmd.getName();
					
					if (e.getCause() != null && e.getCause().getMessage() != null) {
						es = es + StringConstants.CRLF + "Message: " + e.getMessage() + StringConstants.CRLF + e.getCause().getMessage() + StringConstants.CRLF;
					} else if (e.getMessage() != null) {
						es = es + StringConstants.CRLF + "Message: " + e.getMessage() + StringConstants.CRLF;
					} 
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					PrintWriter pw = new PrintWriter(new OutputStreamWriter(baos));
					
					e.printStackTrace(new PrintWriter(pw));
					pw.flush();

					if (e instanceof BundleException)
						if (((BundleException)e).getNestedException() != null) {
							((BundleException)e).getNestedException().printStackTrace(pw);
							pw.flush();
						}
					
					return es + baos.toString();
				} 
			} else {
				if (cmd == null) {
					return "Unknown command: " + line.split(" ")[0];
				} else {
					String es = "Invalid usage of command " + cmd.getName() + StringConstants.CRLF;
					return es + "Usage: " + cmd.getName() + " " + cmd.getUsage();
				}
			}
		} 
		
		//The input was empty
		return "";
	}


	/**
	 * @param cmd input command
	 * @return true if input command has the "-h" or "--help" option, false otherwise
	 */
	private boolean hasHelpParam(IKnapsackCommand cmd) {
		if (cmd == null || cmd.getArguments() == null)
			return false;		
		
		return cmd.getArguments().contains("-h") || cmd.getArguments().contains("--help");
	}
}
