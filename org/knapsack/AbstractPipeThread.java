/*******************************************************************************
 * Copyright (c) 2010 Bug Labs, Inc.
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
package org.knapsack;

import java.io.File;
import java.io.IOException;

/**
 * Abstract class for filesystem pipes.  Requires that filesystem supports pipes and that 
 * the mkfifi command is available in the /usr/bin directory of the system.
 * 
 * @author kgilmer
 * 
 */
public abstract class AbstractPipeThread extends Thread {

	/**
	 * A default path for the mkfifo command that creates a filesystem pipe.
	 */
	private static final String MKFIFO_COMMAND = "/usr/bin/mkfifo";
	
	/**
	 * File that represents the pipe.
	 */
	protected final File pipe;
	
	/**
	 * The pipe command that is executed.
	 */
	private String mkpipeCommand;

	/**
	 * Initialize pipe thread, use the default pipe command.
	 * 
	 * @param pipe
	 * @param reader
	 * @throws IOException 
	 */
	public AbstractPipeThread(File pipe) throws IOException {
		this.pipe = pipe;
		this.mkpipeCommand = MKFIFO_COMMAND;
		//Test that pipe does not exist
		if (pipe.exists())
			throw new IOException("Pipe already exists.  This means a framework is already running or has crashed in the same directory.  Manually remove " + pipe.getAbsolutePath() + " and run again.");
	}
	
	/**
	 * Initialize pipe thread, specify a pipe command.
	 * 
	 * @param pipe
	 * @param pipeCommand
	 * @param reader
	 * @throws IOException 
	 */
	public AbstractPipeThread(File pipe, String pipeCommand) throws IOException {
		this.pipe = pipe;
		this.mkpipeCommand = pipeCommand;
		//Test that pipe does not exist, and if it does, refuse to continue.
		if (pipe.exists())
			throw new IOException("Pipe already exists.  This means a framework is already running or has crashed in the same directory.  Manually remove " + pipe.getAbsolutePath() + " and run again.");
	}

	/**
	 * Exit writing thread.
	 */
	public abstract void shutdown();

	/**
	 * Create a pipe file
	 * 
	 * @param pipe
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	protected File createPipe() throws IOException, InterruptedException {
		//Test that mkfifo is available
		File mkpipecommand = new File(mkpipeCommand);
		if (!mkpipecommand.isFile() || !mkpipecommand.canExecute())
			throw new IOException("Cannot create pipe, mkfifo command is unavailable in " + mkpipeCommand);
		
		//Test that pipe dir is there or create
		if (!pipe.getParentFile().exists())
			if (!pipe.getParentFile().mkdirs())
				throw new IOException("Cannot create directory: " + pipe.getParentFile().getAbsolutePath());

		Process p = Runtime.getRuntime().exec(new String[] { mkpipeCommand, pipe.getAbsolutePath() });
		p.waitFor();

		if (p.exitValue() < 0) {
			throw new IOException("Unable to create pipe, process returned error exit code: " + p.exitValue());
		}

		return pipe;
	}
}
