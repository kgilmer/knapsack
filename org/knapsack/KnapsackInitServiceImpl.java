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
package org.knapsack;

import java.io.File;
import java.util.Collection;

import org.knapsack.init.InitThread;
import org.knapsack.init.pub.KnapsackInitService;
import org.sprinkles.Applier;

/**
 * The native service implementation for KnapsackInitService.
 * 
 * @author kgilmer
 *
 */
public class KnapsackInitServiceImpl implements KnapsackInitService {

	private final Config config;
	private final File baseDir;
	private Collection<File> bundleDirs = null;

	/**
	 * @param baseDir
	 * @param config
	 */
	public KnapsackInitServiceImpl(File baseDir, Config config) {
		this.baseDir = baseDir;
		this.config = config;
	}

	@Override
	public void updateBundles() {
		(new InitThread(getBundleDirectories())).start();
	}
	
	
	/**
	 * Called by knapsack Activator synchronously so that all bundles are resolved before framework start event is fired.
	 */
	protected void updateBundlesSync() {
		(new InitThread(getBundleDirectories())).run();
	}

	@Override
	public Collection<File> getBundleDirectories() {
		if (bundleDirs == null)
			bundleDirs = Applier.map(
					config.getString(Config.CONFIG_KEY_BUNDLE_DIRS).split(","), new Applier.Fn<String, File>() {

				@Override
				public File apply(String element) {
					return new File(baseDir, element.trim());
				}
			});

		return bundleDirs;
	}
}
