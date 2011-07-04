package org.knapsack.init.pub;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * Service for accessing Knapsack's bundle management facilities.
 * 
 * Currently this service only allows the 
 * 
 * @author kgilmer
 *
 */
public interface KnapsackInitService {
	/**
	 * triggering of the filesystem scanner to see if filesystem changes have occurred and update bundlespace accordingly.
	 * Iterate through configured directories, look for changes, start, stop, install, uninstall bundles.
	 * 
	 */
	public void updateBundles();
	
	/**
	 * @return List<File> of directories being scanned for OSGi bundles.
	 */
	public Collection<File> getBundleDirectories();
}
