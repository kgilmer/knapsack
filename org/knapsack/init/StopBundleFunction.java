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

import org.knapsack.Activator;
import org.osgi.service.log.LogService;
import org.sprinkles.Fn;

/**
 * Start a bundle.
 * @author kgilmer
 *
 */
class StopBundleFunction implements Fn.Function<BundleJarWrapper, BundleJarWrapper> {

	@Override
	public BundleJarWrapper apply(BundleJarWrapper element) {

		try {
			element.getBundle().stop();
			return element;
		} catch (Exception e) {
			Activator.log(LogService.LOG_INFO, "Unable to stop " + element.getJar() + ".", e);
			return null;
		}			
	}		
}