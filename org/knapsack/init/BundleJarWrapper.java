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
package org.knapsack.init;

import java.io.File;

import org.knapsack.shell.BuiltinCommands;
import org.osgi.framework.Bundle;

/**
 * Wrap a Bundle and the file Jar that it started from.
 * 
 * @author kgilmer
 *
 */
class BundleJarWrapper {
	private final File jar;
	private final Bundle bundle;

	public BundleJarWrapper(File jar, Bundle bundle) {
		this.jar = jar;
		this.bundle = bundle;			
	}
	
	public File getJar() {
		return jar;
	}
	
	public Bundle getBundle() {
		return bundle;
	}
	
	@Override
	public String toString() {
		return BuiltinCommands.getBundleName(bundle);
	}
}