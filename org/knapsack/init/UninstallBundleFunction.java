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

import org.knapsack.KnapsackLogger;
import org.knapsack.Launcher;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.log.LogService;
import org.sprinkles.Applier;

public class UninstallBundleFunction implements Applier.Fn<File, File> {
	private Map<String, Bundle> bundleMap;
	private KnapsackLogger logger;

	public UninstallBundleFunction() {
		logger = Launcher.getLogger();
		bundleMap = InstallBundleFunction.createLocationList(Launcher.getBundleContext());
	}

	@Override
	public File apply(File element) {
		Bundle bundle = bundleMap.get(InstallBundleFunction.fileToUri(element));

		if (bundle != null) {
			try {
				bundle.uninstall();
				InitThread.getBundleSizeMap().remove(element);
			} catch (BundleException e) {
				logger.log(LogService.LOG_ERROR, "Unable to uninstall " + element + ".", e);
				return null;
			}

			return element;
		} else {
			return null;
		}
	}

}
