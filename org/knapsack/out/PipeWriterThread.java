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
package org.knapsack.out;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import org.knapsack.AbstractPipeThread;
import org.knapsack.Activator;
import org.osgi.service.log.LogService;

/**
 * Push output of reader to clients on other side of pipe.
 * 
 * @author kgilmer
 * 
 */
public class PipeWriterThread extends AbstractPipeThread {

	/**
	 * Defines the source of content from which will be read and sent to clients
	 * on the pipe.
	 * 
	 * @author kgilmer
	 * 
	 */
	public interface WriterInput {
		public Iterator<String> getIterator();
	}

	private final WriterInput source;

	/**
	 * Create writer
	 * 
	 * @param pipe
	 * @param reader
	 * @throws IOException 
	 */
	public PipeWriterThread(File pipe, WriterInput output) throws IOException {
		super(pipe);
		this.source = output;
	}

	public void run() {
		File pfile = null;
		try {
			Activator.logError("Creating fifo pipe for writing: " + pipe.getAbsolutePath());
			pfile = createPipe();
			BufferedWriter bw = null;
			Iterator<String> contentIterator = null;

			while (!Thread.interrupted()) {
				if (bw == null) {
					Thread.sleep(500);
					bw = new BufferedWriter(new FileWriter(pfile));

					if (Thread.interrupted()) {
						return;
					}

					if (source != null) {
						contentIterator = source.getIterator();
					}
				}

				if (source != null && contentIterator != null) {
					while (contentIterator.hasNext()) {
						bw.write(contentIterator.next().toString().trim());
						bw.write('\n');
					}
				}

				bw.flush();
				bw.close();
				bw = null;
			}
		} catch (IOException e) {
			Activator.logError("Error occured while writing to client on pipe: " + pipe.getAbsolutePath(), e);
		} catch (InterruptedException e) {
		} finally {
			if (pfile != null) {
				Activator.logError("Deleting pipe file: " + pipe.getAbsolutePath());
				pfile.delete();
			}
		}
	}

	/**
	 * Exit writing thread.
	 */
	public void shutdown() {
		this.interrupt();

		try {
			FileReader fr = new FileReader(pipe);
			fr.close();
		} catch (Exception e) {
			// Ignore any problems.
		}
		
		if (pipe != null) {
			pipe.delete();
		}
	}
}
