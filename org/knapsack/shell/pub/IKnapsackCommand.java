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
package org.knapsack.shell.pub;

import java.io.OutputStream;
import java.util.List;

import org.osgi.framework.BundleContext;

/**
 * A command interface for the Bug Labs OSGi shell.
 * 
 * @author kgilmer
 * 
 */
public interface IKnapsackCommand {
	/**
	 * Command initialization.
	 * 
	 * @param arguments
	 * @param out
	 * @param err
	 * @param context
	 */
	public void initialize(List<String> arguments, BundleContext context);

	/**
	 * @return List of arguments passed to command.
	 */
	public List<String> getArguments();
	/**
	 * Execute the command
	 * 
	 * @throws Exception
	 */
	public String execute() throws Exception;

	/**
	 * @return true if the command and parameters are valid.
	 */
	public boolean isValid();

	/**
	 * @return Name of command.
	 */
	public String getName();

	/**
	 * @return A short textual description of command usage.
	 */
	public String getUsage();

	/**
	 * @return A description of what the command does.
	 */
	public String getDescription();
}
