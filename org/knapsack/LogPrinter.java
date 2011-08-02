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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
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

	private static final String STR_DEBUG = "DEBUG:";
	private static final String STR_ERROR = "ERROR:";
	private static final String STR_INFO = "INFO: ";
	private static final String STR_WARNING = "WARN: ";

	private final BundleContext bundleContext;
	private final List<LogReaderService> logListeners;

	public LogPrinter(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
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
		doLog(logentry.getBundle(), logentry.getServiceReference(), logentry.getLevel(), logentry.getMessage(), logentry.getException());
	}

	/**
	 * Method copied from Felix Logger class to display consistent logging in
	 * case of OSGi service unavailability.
	 * 
	 * @param bundle
	 * @param sr
	 * @param level
	 * @param msg
	 * @param throwable
	 */
	protected static void doLog(Bundle bundle, ServiceReference sr, int level, String msg, Throwable throwable) {
		String s = "";
		
		if (sr != null) {
			s = s + "SvcRef " + sr + " ";
		} else if (bundle != null) {
			s = s + "Bundle " + bundle.toString() + " ";
		}
		s = s + msg;
		if (throwable != null) {
			s = s + " (" + throwable + ")";
		}
		switch (level) {
		case LogService.LOG_DEBUG:
			System.out.println(STR_DEBUG + s);
			break;
		case LogService.LOG_ERROR:
			System.out.println(STR_ERROR + s);
			if (throwable != null) {
				if ((throwable instanceof BundleException) && (((BundleException) throwable).getNestedException() != null)) {
					throwable = ((BundleException) throwable).getNestedException();
				}
				throwable.printStackTrace();
			}
			break;
		case LogService.LOG_INFO:
			System.out.println(STR_INFO + s);
			break;
		case LogService.LOG_WARNING:
			System.out.println(STR_WARNING + s);
			break;
		default:
			System.out.println("UNKNOWN[" + level + "]: " + s);
		}
	}
}
