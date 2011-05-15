package org.knapsack.init;

import org.knapsack.Activator;
import org.osgi.service.log.LogService;
import org.sprinkles.Fn;

/**
 * Start a bundle.
 * @author kgilmer
 *
 */
class StartBundleFunction implements Fn.Function<BundleJarWrapper, BundleJarWrapper> {

	@Override
	public BundleJarWrapper apply(BundleJarWrapper element) {

		try {
			element.getBundle().start();
			return element;
		} catch (Exception e) {
			Activator.log(LogService.LOG_INFO, "Unable to start " + element.getJar() + ".", e);
			return null;
		}			
	}		
}