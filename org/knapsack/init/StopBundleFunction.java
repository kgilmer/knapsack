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