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
package org.knapsack.in;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.knapsack.AbstractPipeThread;
import org.knapsack.Activator;
import org.osgi.service.log.LogService;

/**
 * Given a filename and an InputAction, will create and read from a pipe, passing input to the InputAction.
 * @author kgilmer
 *
 */
public class PipeReaderThread extends AbstractPipeThread {

	private final File pipe;
	private final ReaderOutput action;

	public interface ReaderOutput {
		public void inputReceived(String input);
	}
	
	public PipeReaderThread(File pipe, ReaderOutput action) {
		super(pipe);
		this.pipe = pipe;
		this.action = action;
	}
	
	@Override
	public void run() {
		try {
			Activator.log(LogService.LOG_DEBUG, "Creating fifo pipe for reading: " + pipe.getAbsolutePath());
			createPipe(pipe);

			while (!Thread.interrupted()) {
				if (pipe == null)
					return;
				
				BufferedReader br = new BufferedReader(new FileReader(pipe));
				
				if (Thread.interrupted()) {
					return;
				}
				String line = null;
				
				while ((line = br.readLine()) != null) {
					action.inputReceived(line);
				}
				
				br.close();
			}
		} catch (IOException e) {
			Activator.log(LogService.LOG_ERROR, "Error occured while reading from client on pipe: " + pipe.getAbsolutePath(), e);
		} catch (InterruptedException e) {			
		} finally {
			Activator.log(LogService.LOG_DEBUG, "Deleting pipe file: " + pipe.getAbsolutePath());
			pipe.delete();
		}
	}
	
	public void shutdown() {
		this.interrupt();
		
		try {
			FileWriter fw = new FileWriter(pipe);
			fw.close();
		} catch (Exception e) {		
		}
		
		if (pipe != null) {
			pipe.delete();
		}
	}
}
