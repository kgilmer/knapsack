package org.knapsack.init;

import java.io.File;

import org.knapsack.out.KnapsackWriterInput;
import org.osgi.framework.Bundle;

/**
 * Wrap a Bundle and the file Jar that it started from.
 * 
 * @author kgilmer
 *
 */
class BundleJarWrapper {
	private final File jar;
	private final Bundle bundle;

	public BundleJarWrapper(File jar, Bundle bundle) {
		this.jar = jar;
		this.bundle = bundle;			
	}
	
	public File getJar() {
		return jar;
	}
	
	public Bundle getBundle() {
		return bundle;
	}
	
	@Override
	public String toString() {
		return KnapsackWriterInput.getBundleName(bundle);
	}
}