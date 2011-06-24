package org.knapsack.shell;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;

import org.knapsack.Config;
import org.knapsack.init.InitThread;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.sprinkles.Fn;

public class BuiltinCommands implements IKnapsackCommandProvider {
	
	private final CommandParser parser;
	private final LogService log;
	private SimpleDateFormat dateFormatter = new SimpleDateFormat("MM.dd HH:mm:ss");

	public BuiltinCommands(CommandParser parser, LogService log) {
		this.parser = parser;
		this.log = log;
	}

	public List<IKnapsackCommand> getCommands() {
		List<IKnapsackCommand> cmds = new ArrayList<IKnapsackCommand>();

		cmds.add(new ExitCommand());
		cmds.add(new HelpCommand());
		cmds.add(new BundlesCommand());
		cmds.add(new ServicesCommand());
		cmds.add(new LogCommand());
		cmds.add(new UpdateCommand());
		cmds.add(new PrintConfCommand());

		return cmds;
	}
	
	private class UpdateCommand extends AbstractKnapsackCommand {

		@Override
		public String execute() throws Exception {
			Config config = Config.getRef();
			
			InitThread init = new InitThread(new File(config.getString(Config.CONFIG_KEY_ROOT_DIR)), Arrays.asList(config.getString(Config.CONFIG_KEY_BUNDLE_DIRS).split(",")));
			init.start();
			
			return "Rescanning and updating bundles from configured directories.";
		}

		@Override
		public String getName() {
			return "update";
		}
		
		@Override
		public String getDescription() {			
			return "Rescan the bundle directory or directories and update bundlespace accordingly.";
		}
	}
	
	///  Commands
	
	private class LogCommand extends AbstractKnapsackCommand {

		@Override
		public String execute() throws Exception {
			final StringBuffer sb = new StringBuffer();
			final boolean verbose = arguments.contains("-v");
			
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
			return "[-v]";
		}
		
		@Override
		public String getDescription() {
			return "Print OSGi log.";
		}
		
	}
	
	private class ServicesCommand extends AbstractKnapsackCommand {

		@Override
		public String execute() throws Exception {
			final StringBuffer sb = new StringBuffer();
			final boolean verbose = arguments.contains("-v");
			
			Fn.map(new Fn.Function<ServiceReference, ServiceReference>() {

				@Override
				public ServiceReference apply(ServiceReference element) {
					addServiceReference(element, sb, verbose);
					return element;
				}
			}, context.getServiceReferences(null, null));
			
			return sb.toString();
		}

		@Override
		public String getName() {
			return "services";
		}
		
		@Override
		public String getUsage() {
			return "[-v]";
		}
		
		@Override
		public String getDescription() {
			return "Get list of OSGi services active in the framework.";
		}
		
	}
	
	private class PrintConfCommand extends AbstractKnapsackCommand {

		@Override
		public String execute() throws Exception {
			final StringBuffer sb = new StringBuffer();
			
			Fn.map(new Fn.Function<Entry<Object, Object>, Object>() {

				@Override
				public Object apply(Entry<Object, Object> e) {
					sb.append(e.getKey());
					sb.append(" = ");
					sb.append(e.getValue());
					sb.append('\n');
					return e;
				}
			}, System.getProperties().entrySet());
			
			return sb.toString();
		}

		@Override
		public String getName() {
			return "printconfig";
		}
		
		@Override
		public String getUsage() {
			return "";
		}
		
		@Override
		public String getDescription() {
			return "Print the Java system configuration.";
		}
		
	}
	
	private class BundlesCommand extends AbstractKnapsackCommand {

		@Override
		public String execute() throws Exception {
			final StringBuffer sb = new StringBuffer();
			final boolean verbose = arguments.contains("-v");
			
			Fn.map(new Fn.Function<Bundle, Bundle>() {

				@Override
				public Bundle apply(Bundle element) {
					addBundle(element, sb, verbose);
					return element;
				}
			}, context.getBundles());
			
			return sb.toString();
		}

		@Override
		public String getName() {
			return "bundles";
		}
		
		@Override
		public String getUsage() {
			return "[-v]";
		}
		
		@Override
		public String getDescription() {
			return "Get list of OSGi bundles installed in the framework.";
		}
	}

	/**
	 * Exit the framework.
	 * 
	 * @author kgilmer
	 * 
	 */
	private class ExitCommand extends AbstractKnapsackCommand {

		public String execute() throws Exception {
			log.log(LogService.LOG_INFO, "OSGi framework is shutting down due to user request via shell.");
			context.getBundle(0).stop();
			
			return "OSGi framework is shutting down.";
		}

		public String getName() {
			return "exit";
		}

		public String getDescription() {
			return "Stop all bundles and shutdown OSGi runtime.";
		}
	}

	/**
	 * Print help information.
	 * 
	 * @author kgilmer
	 * 
	 */
	private class HelpCommand extends AbstractKnapsackCommand {

		public String execute() throws Exception {
			final StringBuffer sb = new StringBuffer();
			
			Fn.map(new Fn.Function<IKnapsackCommand, IKnapsackCommand>() {
				
				@Override
				public IKnapsackCommand apply(IKnapsackCommand cmd) {
					String sl = cmd.getName() + "\t" + cmd.getUsage() + "\t" + cmd.getDescription() + "\n";
					
					sb.append(sl);
				
					return cmd;
				}
				
			}, parser.getCommands().values());
			
			return sb.toString();
		}

		public String getName() {
			return "help";
		}

		public String getUsage() {

			return super.getUsage();
		}

		public String getDescription() {
			return "Print table of currently available commands.";
		}
	}
	

	private void addLogEntry(LogEntry entry, StringBuffer l, boolean verbose) {
		String line;
		
		if (verbose)
			line = formatDateTime(entry.getTime()) + " " + getLevelLabel(entry.getLevel()) + "\t " + entry.getMessage() + "\t " + getBundleName(entry.getBundle());
		else 
			line = entry.getMessage();
				
		l.append(line);
		l.append('\n');
		
		//Check for an exception, if available display it.
		if (entry.getException() != null) {
			l.append(entry.getException().getMessage());
			l.append('\n');
			
			StringWriter sWriter = new StringWriter();
			PrintWriter pw = new PrintWriter(sWriter);
			entry.getException().printStackTrace(pw);
			l.append(sWriter.toString());
			l.append('\n');
		}
	}

	private String formatDateTime(long time) {
		return dateFormatter.format(new Date(time));
	}

	private void addProperty(Entry<Object, Object> e, List<String> l, boolean verbose) {
		String line = "";

		line = line + e.getKey() + " = " + e.getValue();

		l.add(line);
	}

	private void addServiceReference(ServiceReference sr, StringBuffer l, boolean verbose) {
		if (verbose)
			l.append(getServiceId(sr) + " \t" + getServiceName(sr) + "\n");
		else
			l.append(getServiceName(sr) + "\n");
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

	/**
	 * Add info for Bundle
	 * 
	 * @param b
	 * @param l
	 * @param verbose
	 */
	private void addBundle(Bundle b, StringBuffer l, boolean verbose) {
		if (verbose)
			l.append(getStateName(b.getState()) + " \t" + getBundleName(b) + " \t(" + getBundleVersion(b) + ") \t" + getBundleLocation(b) + "\n");
		else
			l.append(getBundleName(b) + "\n");
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
