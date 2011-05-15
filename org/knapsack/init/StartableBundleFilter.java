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

import org.osgi.framework.Bundle;
import org.sprinkles.Fn;

/**
 * Return only BundleJars that are designated to be set to the START state by the filesystem.
 * @author kgilmer
 *
 */
class StartableBundleFilter implements Fn.Function<BundleJarWrapper, BundleJarWrapper> {

	@Override
	public BundleJarWrapper apply(BundleJarWrapper element) {
		//We determine a bundle should be started if its execute permission is on.
		if (element.getJar().canExecute() && notStarted(element.getBundle()))
			return element;
		
		return null;
	}

	private boolean notStarted(Bundle bundle) {
		return bundle.getState() != Bundle.ACTIVE && bundle.getState() != Bundle.STARTING;
	}
}