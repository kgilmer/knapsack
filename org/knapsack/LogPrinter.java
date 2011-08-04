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
package org.knapsack;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Tracks a log service and prints output to STDOUT in a way that's consistent
 * with Felix internal logging.
 * 
 * @author kgilmer
 * 
 */
public class LogPrinter implements ServiceTrackerCustomizer, LogListener {
	private final BundleContext bundleContext;
	private final List<LogReaderService> logListeners;
	private final int logLevel;

	public LogPrinter(BundleContext bundleContext, int logLevel) {
		this.bundleContext = bundleContext;
		this.logLevel = logLevel;
		logListeners = new ArrayList<LogReaderService>();
		ServiceTracker st = new ServiceTracker(bundleContext, LogReaderService.class.getName(), this);
		st.open();
	}

	@Override
	public Object addingService(ServiceReference reference) {
		LogReaderService svc = (LogReaderService) bundleContext.getService(reference);

		// Add ourselves to every LogReader service available to get the
		// superset of all log data.
		if (!logListeners.contains(svc))
			svc.addLogListener(this);

		return svc;
	}

	@Override
	public void modifiedService(ServiceReference reference, Object service) {

	}

	@Override
	public void removedService(ServiceReference reference, Object service) {
		// Since the service is going away, assume de-registration is
		// unnecessary.
	}

	@Override
	public void logged(LogEntry logentry) {
		if (logentry.getLevel() <= logLevel)
			Logger.doKnapsackLog(logentry.getBundle(), logentry.getServiceReference(), logentry.getLevel(), logentry.getMessage(), logentry.getException());
	}
}
