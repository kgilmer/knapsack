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
package org.knapsack.out;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.knapsack.Activator;
import org.knapsack.Config;
import org.knapsack.out.PipeWriterThread.WriterInput;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;

/**
 * @author kgilmer
 * 
 */
public class KnapsackWriterInput implements WriterInput {
	private static final String HEADER_CONFIG = 	"CONFIG : ";
	private static final String HEADER_BUNDLE = 	"BUNDLE : ";
	private static final String HEADER_PROPERTY = 	"PROP   : ";
	private static final String HEADER_SERVICE = 	"SERVICE: ";
	private static final String HEADER_LOG = 		"LOG    : ";
	
	private Config config;
	private BundleContext context;
	private SimpleDateFormat dateFormatter;

	public KnapsackWriterInput() {
		config = Activator.getConfig();
		context = Activator.getContext();
		dateFormatter = new SimpleDateFormat("MM.dd HH:mm:ss");
	}

	@Override
	public Iterator<String> getIterator() {
		//This sleep is added for the shell commands.  
		//Because the pipe is async, w/out this sleep the output is generated before the configuration is set.
		try {
			Thread.sleep(50);
		} catch (InterruptedException e1) {
			return null;
		}
		
		List<String> l = new ArrayList<String>();
		boolean verbose = config.getBoolean(Config.CONFIG_KEY_VERBOSE);

		if (config.getBoolean(Config.CONFIG_KEY_OUT_BUNDLE)) {
			for (Bundle b : Arrays.asList(context.getBundles()))
				addBundle(b, l, verbose);
		}

		if (config.getBoolean(Config.CONFIG_KEY_OUT_SERVICE)) {
			try {
				ServiceReference[] srs = context.getServiceReferences(null, null);
				if (srs != null)
					for (ServiceReference sr : Arrays.asList(srs))
						addServiceReference(sr, l, verbose);
			} catch (InvalidSyntaxException e) {
				Activator.log(LogService.LOG_ERROR, e.getMessage(), e);
			}
		}

		if (config.getBoolean(Config.CONFIG_KEY_OUT_PROPERTY)) {
			for (Map.Entry<Object, Object> e : System.getProperties().entrySet())
				addProperty(e, l, verbose);
		}

		if (config.getBoolean(Config.CONFIG_KEY_OUT_CONFIG)) {
			for (Map.Entry<Object, Object> e : config.entrySet())
				addConfigEntry(e, l, verbose);
		}
		
		if (config.getBoolean(Config.CONFIG_KEY_OUT_LOG)) {
			ServiceReference ref = context.getServiceReference(LogReaderService.class.getName());
			if (ref != null)
			{
			    LogReaderService reader = (LogReaderService) context.getService(ref);	
			    Enumeration<LogEntry> latestLogs = reader.getLog();
			    List<ComparableLogEntry> entries = new ArrayList<ComparableLogEntry>();
			    
			    while (latestLogs.hasMoreElements())
			    	entries.add(new ComparableLogEntry(latestLogs.nextElement()));
			    
			    Collections.sort(entries);
			    	
			    for (ComparableLogEntry entry : entries)
			    	addLogEntry(entry, l, verbose);
			}
		}

		return l.iterator();
	}

	private void addLogEntry(LogEntry entry, List<String> l, boolean verbose) {
		String line = formatDateTime(entry.getTime()) + " " + getLevelLabel(entry.getLevel()) + "\t " + entry.getMessage() + "\t " + getBundleName(entry.getBundle());
		
		if (verbose)
			line = HEADER_LOG + line;
		
		l.add(line);
		
		//Check for an exception, if available display it.
		if (entry.getException() != null) {
			l.add(entry.getException().getMessage());
			
			StringWriter sWriter = new StringWriter();
			PrintWriter pw = new PrintWriter(sWriter);
			entry.getException().printStackTrace(pw);
			l.add(sWriter.toString());
		}
	}

	private String formatDateTime(long time) {
		return dateFormatter.format(new Date(time));
	}

	private void addProperty(Entry<Object, Object> e, List<String> l, boolean verbose) {
		String line = "";

		if (verbose)
			line = line + HEADER_PROPERTY;

		line = line + e.getKey() + " = " + e.getValue();

		l.add(line);
	}

	private void addServiceReference(ServiceReference sr, List<String> l, boolean verbose) {
		if (verbose)
			l.add(HEADER_SERVICE + getServiceId(sr) + " \t" + getServiceName(sr));
		else
			l.add(getServiceName(sr));
	}

	/**
	 * Return name or objectClass of service.
	 * 
	 * @param sr
	 * @return
	 */
	public static String getServiceName(ServiceReference sr) {	
		return ((String[]) sr.getProperty("objectClass"))[0];
	}

	/**
	 * Return id of service
	 * @param sr
	 * @return
	 */
	public static String getServiceId(ServiceReference sr) {		
		return sr.getProperty("service.id").toString();
	}

	private void addConfigEntry(Entry<Object, Object> e, List<String> l, boolean verbose) {
		String line = "";

		if (verbose)
			line = line + HEADER_CONFIG;

		line = line + e.getKey() + " = " + e.getValue();

		l.add(line);
	}

	/**
	 * Add info for Bundle
	 * 
	 * @param b
	 * @param l
	 * @param verbose
	 */
	private void addBundle(Bundle b, List<String> l, boolean verbose) {
		if (verbose)
			l.add(HEADER_BUNDLE + getStateName(b.getState()) + " \t" + getBundleName(b) + " \t(" + getBundleVersion(b) + ") \t" + getBundleLocation(b));
		else
			l.add(getBundleName(b));
	}

	/**
	 * @param b
	 * @return The location on filesystem of bundle
	 */
	public static String getBundleLocation(Bundle b) {
		//Remove "file://" prefix
		return b.getLocation().substring(7);
	}

	/**
	 * @param b
	 * @return The bundle version as defined in the manifest.
	 */
	public static String getBundleVersion(Bundle b) {
		String version = (String) b.getHeaders().get("Bundle-Version");

		if (version == null) {
			version = "";
		}

		return version;
	}

	public static String getBundleName(Bundle b) {
		String name = (String) b.getHeaders().get("Bundle-SymbolicName");

		if (name == null) {
			name = (String) b.getHeaders().get("Bundle-Name");
		}

		if (name == null) {
			name = "Undefined";
		}

		if (name.indexOf(";") > -1)
			name = name.split(";")[0];

		return name;
	}

	/**
	 * Return state label as defined in OSGi spec.
	 * 
	 * @param state
	 * @return
	 */
	public static String getStateName(int state) {
		switch (state) {
		case 0x00000001:
			return "UNINS";
		case 0x00000002:
			return "INSTL";
		case 0x00000004:
			return "RESOL";
		case 0x00000008:
			return "START";
		case 0x00000010:
			return " STOP";
		case 0x00000020:
			return "ACTIV";

		}

		return "[UNKNOWN STATE: " + state + "]";
	}
	
	/**
	 * @param level
	 * @return A human-readable log level string.
	 */
	private static String getLevelLabel(int level) {
		switch (level) {
		case 1:
			return "ERROR  ";
		case 2:
			return "WARNING";
		case 3:
			return "INFO   ";
		case 4:
			return "DEBUG  ";
		}

		return "UNKNOWN";
	}
	private class ComparableLogEntry  implements Comparable, LogEntry {

		private final LogEntry entry;

		public Bundle getBundle() {
			return entry.getBundle();
		}

		public ServiceReference getServiceReference() {
			return entry.getServiceReference();
		}

		public int getLevel() {
			return entry.getLevel();
		}

		public String getMessage() {
			return entry.getMessage();
		}

		public Throwable getException() {
			return entry.getException();
		}

		public long getTime() {
			return entry.getTime();
		}

		public ComparableLogEntry(LogEntry entry) {
			this.entry = entry;
			
		}

		@Override
		public int compareTo(Object o) {
			if (o instanceof ComparableLogEntry) {
				long result = this.getTime() - (((ComparableLogEntry) o).getTime());
				
				if (result > 0)
					return 1;
				if (result < 0)
					return -1;
			}
			
			return 0;
		}
		
	}
}
