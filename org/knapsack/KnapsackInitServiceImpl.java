package org.knapsack;

import java.io.File;
import java.util.Collection;

import org.knapsack.init.InitThread;
import org.knapsack.init.pub.KnapsackInitService;
import org.sprinkles.Fn;

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

	@Override
	public Collection<File> getBundleDirectories() {
		if (bundleDirs == null)
			bundleDirs = Fn.map(new Fn.Function<String, File>() {

				@Override
				public File apply(String element) {
					return new File(baseDir, element.trim());
				}
			}, config.getString(Config.CONFIG_KEY_BUNDLE_DIRS).split(","));

		return bundleDirs;
	}
}
