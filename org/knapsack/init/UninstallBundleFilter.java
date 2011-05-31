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
import java.util.ArrayList;
import java.util.Collection;

import org.sprinkles.Fn;

/**
 * Return only BundleJars that are designated to be set to the START state by the filesystem.
 * @author kgilmer
 *
 */
class UninstallBundleFilter implements Fn.Function<File, File> {

	private ArrayList<String> bundleLocations;

	public UninstallBundleFilter(Collection<BundleJarWrapper> bundles) {
		bundleLocations = new ArrayList<String>();
		for (BundleJarWrapper wrapper : bundles)
			bundleLocations.add(wrapper.getBundle().getLocation());		
	}

	@Override
	public File apply(File element) {
		//If we have a bundle in cache that is not in the filesystem, it has been deleted and needs to be uninstalled
		if (!bundleLocations.contains(InstallBundleFunction.fileToUri(element)))
			return element;
		
		return null;
	}
}