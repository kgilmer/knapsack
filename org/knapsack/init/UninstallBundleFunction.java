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
import java.util.Map;

import org.knapsack.Activator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.sprinkles.Applier;

public class UninstallBundleFunction implements Applier.Fn<File, File> {
	private Map<String, Bundle> bundleMap;

	public UninstallBundleFunction() {
		bundleMap = InstallBundleFunction.createLocationList();
	}

	@Override
	public File apply(File element) {
		Bundle bundle = bundleMap.get(InstallBundleFunction.fileToUri(element));

		if (bundle != null) {
			try {
				bundle.uninstall();
				Activator.getBundleSizeMap().remove(element);
			} catch (BundleException e) {
				Activator.logError("Unable to uninstall " + element + ".", e);
				return null;
			}

			return element;
		} else {
			return null;
		}
	}

}
