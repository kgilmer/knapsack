package org.knapsack.shell.commands;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import org.knapsack.shell.AbstractKnapsackCommand;
import org.knapsack.shell.StringConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;

/**
 * Print the OSGi log.
 * 
 * @author kgilmer
 *
 */
public class LogCommand extends AbstractKnapsackCommand {

	private final SimpleDateFormat dateFormatter;
	
	/**
	 * @param dateFormat
	 */
	public LogCommand(String dateFormat) {
		dateFormatter = new SimpleDateFormat(dateFormat);
	}
	
	/**
	 * 
	 */
	public LogCommand() {
		this("MM.dd HH:mm:ss");
	}
	
	@Override
	public String execute() throws Exception {
		final StringBuilder sb = new StringBuilder();
		final boolean verbose = !arguments.contains("-b");
		
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
		    	addLogEntry(entry, sb, verbose);
		}
		
		return sb.toString();
	}

	@Override
	public String getName() {
		return "log";
	}
	
	@Override
	public String getUsage() {
		return "[-b (brief)]";
	}
	
	@Override
	public String getDescription() {
		return "Print OSGi log.";
	}
	
	private String formatDateTime(long time) {
		return dateFormatter.format(new Date(time));
	}
	
	private void addLogEntry(LogEntry entry, StringBuilder sb, boolean verbose) {

		if (verbose) {
			sb.append(formatDateTime(entry.getTime()));
			sb.append(StringConstants.TAB);
			sb.append(getLevelLabel(entry.getLevel()));
			sb.append(StringConstants.TAB);
			sb.append(entry.getMessage());
			sb.append(StringConstants.TAB);
			sb.append(BundlesCommand.getBundleLabel(entry.getBundle()));	 
		} else {
			sb.append(formatDateTime(entry.getTime()));
			sb.append(entry.getMessage());
		}
				
		sb.append(StringConstants.CRLF);
		
		//Check for an exception, if available display it.
		if (entry.getException() != null) {
			sb.append(entry.getException().getMessage());
			sb.append(StringConstants.CRLF);
			
			StringWriter sWriter = new StringWriter();
			PrintWriter pw = new PrintWriter(sWriter);
			entry.getException().printStackTrace(pw);
			sb.append(sWriter.toString());
			sb.append(StringConstants.CRLF);
		}
	}
	
	/**
	 * @param level
	 * @return A human-readable log level string.
	 */
	public static String getLevelLabel(int level) {
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
	
	private class ComparableLogEntry  implements Comparable<LogEntry>, LogEntry {

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
		public int compareTo(LogEntry o) {
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
