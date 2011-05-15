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