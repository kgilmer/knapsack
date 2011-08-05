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
import java.util.Collection;

import org.knapsack.ConfigurationConstants;
import org.knapsack.init.pub.KnapsackInitService;
import org.sprinkles.Applier;

/**
 * The native service implementation for KnapsackInitService.
 * 
 * @author kgilmer
 *
 */
public class KnapsackInitServiceImpl implements KnapsackInitService {
	private final File baseDir;
	private Collection<File> bundleDirs = null;
	private String dirList;

	/**
	 * @param baseDir
	 * @param config
	 */
	public KnapsackInitServiceImpl(File baseDir) {
		this.baseDir = baseDir;
		this.dirList = System.getProperty(ConfigurationConstants.CONFIG_KEY_BUNDLE_DIRS);
		
		if (dirList == null)
			dirList = ConfigurationConstants.DEFAULT_BUNDLE_DIRECTORY;
	}

	@Override
	public void updateBundles() {
		(new BundleInitThread(getBundleDirectories())).start();
	}
	
	
	/**
	 * Called by knapsack Activator synchronously so that all bundles are resolved before framework start event is fired.
	 */
	public void updateBundlesSync() {
		(new BundleInitThread(getBundleDirectories())).run();
	}

	@Override
	public Collection<File> getBundleDirectories() {
		if (bundleDirs == null)
			bundleDirs = Applier.map(
					dirList.split(","), new Applier.Fn<String, File>() {

				@Override
				public File apply(String element) {
					return new File(baseDir, element.trim());
				}
			});

		return bundleDirs;
	}
}
