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
