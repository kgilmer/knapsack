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

import java.util.ArrayList;
import java.util.List;

import org.knapsack.shell.pub.IKnapsackCommand;
import org.osgi.framework.BundleContext;

/**
 * A helper base class for commands for the command line. Refer to IKnapsackCommand for
 * details of how to write commands for OSGi shell.
 * 
 * @author kgilmer
 * 
 */
public abstract class AbstractKnapsackCommand implements IKnapsackCommand {

	protected List<String> arguments;

	protected BundleContext context;
	
	public void initialize(List<String> arguments, BundleContext context) {

		if (arguments != null) {
			this.arguments = arguments;
		} else {
			this.arguments = new ArrayList<String>();
		}

		this.context = context;
	}	
	
	@Override
	public List<String> getArguments() {
		return arguments;
	}

	/* (non-Javadoc)
	 * @see com.buglabs.osgi.shell.ICommand#isValid()
	 */
	public boolean isValid() {
		return true;
	}

	public String getUsage() {
		return "";
	}

	public String getDescription() {
		return "No help available for this command.";
	}
}
