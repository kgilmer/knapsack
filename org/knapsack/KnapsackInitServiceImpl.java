package org.knapsack;

import java.io.File;
import java.util.Collection;

import org.knapsack.init.InitThread;
import org.knapsack.init.pub.KnapsackInitService;
import org.sprinkles.Applier;

public class KnapsackInitServiceImpl implements KnapsackInitService {

	private final Config config;
	private final File baseDir;
	private Collection<File> bundleDirs = null;

	public KnapsackInitServiceImpl(File baseDir, Config config) {
		this.baseDir = baseDir;
		this.config = config;
	}

	@Override
	public void updateBundles() {
		(new InitThread(getBundleDirectories())).start();
	}
	
	
	/**
	 * Called by knapsack Activator synchronously so that all bundles are resolved before framework start event is fired.
	 */
	protected void updateBundlesSync() {
		(new InitThread(getBundleDirectories())).run();
	}

	@Override
	public Collection<File> getBundleDirectories() {
		if (bundleDirs == null)
			bundleDirs = Applier.map(
					config.getString(Config.CONFIG_KEY_BUNDLE_DIRS).split(","), new Applier.Fn<String, File>() {

				@Override
				public File apply(String element) {
					return new File(baseDir, element.trim());
				}
			});

		return bundleDirs;
	}
}
