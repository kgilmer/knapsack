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
package org.knapsack.init.pub;

import java.io.File;
import java.util.Collection;

/**
 * Service for accessing Knapsack's bundle management facilities.
 * 
 * Currently this service only allows the 
 * 
 * @author kgilmer
 *
 */
public interface KnapsackInitService {
	/**
	 * triggering of the filesystem scanner to see if filesystem changes have occurred and update bundlespace accordingly.
	 * Iterate through configured directories, look for changes, start, stop, install, uninstall bundles.
	 * 
	 */
	public void updateBundles();
	
	/**
	 * @return List<File> of directories being scanned for OSGi bundles.
	 */
	public Collection<File> getBundleDirectories();
}
