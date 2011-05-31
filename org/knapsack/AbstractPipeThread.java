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
 * Push output of reader to clients on other side of pipe.
 * 
 * @author kgilmer
 * 
 */
public abstract class AbstractPipeThread extends Thread {

	private static final String MKFIFO_COMMAND = "/usr/bin/mkfifo";
	protected final File pipe;

	/**
	 * Create writer
	 * 
	 * @param pipe
	 * @param reader
	 */
	public AbstractPipeThread(File pipe) {
		this.pipe = pipe;
	}

	/**
	 * Exit writing thread.
	 */
	public abstract void shutdown();

	/**
	 * Create a pipe file
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	protected File createPipe(File file) throws IOException, InterruptedException {
		if (!file.getParentFile().exists())
			if (!file.getParentFile().mkdirs())
				throw new IOException("Cannot create directory: " + file.getParentFile().getAbsolutePath());

		if (file.exists() && file.isFile())
			if (!file.delete())
				throw new IOException("Unable to delete existing file before pipe creation: " + file.getAbsolutePath());

		Process p = Runtime.getRuntime().exec(new String[] { MKFIFO_COMMAND, file.getAbsolutePath() });
		p.waitFor();

		if (p.exitValue() < 0) {
			throw new IOException("Unable to create pipe, process returned error exit code: " + p.exitValue());
		}

		return file;
	}
}
