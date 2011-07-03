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
	private static final String CRLF = System.getProperty("line.separator");

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

			if (hasHelpParam(cmd)) {
				String rs = "";
				
				if (cmd.getDescription() != null)
					rs = cmd.getDescription();
				
				return rs + CRLF + "Usage: " + cmd.getName() + " " + cmd.getUsage();
			} else if (cmd != null && cmd.isValid()) {
				try {
					return cmd.execute();
				} catch (Exception e) {
					String es = "An error occurred while executing: " + cmd.getName();
					
					if (e.getCause() != null && e.getCause().getMessage() != null) {
						es = es + CRLF + "Message: " + e.getMessage() + CRLF + e.getCause().getMessage() + CRLF;
					} else if (e.getMessage() != null) {
						es = es + CRLF + "Message: " + e.getMessage() + CRLF;
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
					String es = "Invalid usage of command " + cmd.getName() + CRLF;
					return es + "Usage: " + cmd.getName() + " " + cmd.getUsage();
				}
			}
		} 
		
		//The input was empty
		return "";
	}


	private boolean hasHelpParam(IKnapsackCommand cmd) {
		return cmd.getArguments().contains("-h") || cmd.getArguments().contains("--help");
	}
}
