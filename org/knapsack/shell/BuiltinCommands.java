package org.knapsack.shell;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;

import org.knapsack.Activator;
import org.knapsack.Config;
import org.knapsack.init.InitThread;
import org.knapsack.shell.pub.IKnapsackCommand;
import org.knapsack.shell.pub.IKnapsackCommandSet;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.sprinkles.Fn;

/**
 * A set of basic built-in commands for inspecting and changing OSGi instance.
 * 
 * @author kgilmer
 *
 */
public class BuiltinCommands implements IKnapsackCommandSet {
	
	private final CommandParser parser;
	private final LogService log;
	private SimpleDateFormat dateFormatter = new SimpleDateFormat("MM.dd HH:mm:ss");
	private static final String TAB = " \t";

	public BuiltinCommands(CommandParser parser, LogService log) {
		this.parser = parser;
		this.log = log;
	}

	public List<IKnapsackCommand> getCommands() {
		List<IKnapsackCommand> cmds = new ArrayList<IKnapsackCommand>();

		cmds.add(new ShutdownCommand());
		cmds.add(new HelpCommand());
		cmds.add(new BundlesCommand());
		cmds.add(new ServicesCommand());
		cmds.add(new LogCommand());
		cmds.add(new UpdateCommand());
		cmds.add(new PrintConfCommand());
		cmds.add(new HeadersCommand());

		return cmds;
	}
	
	private class UpdateCommand extends AbstractKnapsackCommand {

		@Override
		public String execute() throws Exception {
			Config config = Activator.getConfig();
			
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
		
	}
	
	private class ServicesCommand extends AbstractKnapsackCommand {

		@Override
		public String execute() throws Exception {
			final StringBuilder sb = new StringBuilder();
			final boolean verbose = !arguments.contains("-b");
			final boolean dependencies = arguments.contains("-d");
			final boolean properties = arguments.contains("-p");
			
			Fn.map(new Fn.Function<ServiceReference, ServiceReference>() {

				@Override
				public ServiceReference apply(ServiceReference sr) {
					if (verbose) {
						appendId(sb, getServiceId(sr));
						sb.append(TAB);
						sb.append(getServiceName(sr));
						sb.append(TAB);
						sb.append(getBundleLabel(sr.getBundle()));						
						sb.append(AbstractKnapsackCommand.CRLF);
					} else {
						appendId(sb, getServiceId(sr));						
						sb.append(TAB);
						sb.append(getServiceName(sr));
						sb.append(AbstractKnapsackCommand.CRLF);
					}
					
					if (properties) {
						sb.append(getServiceProperties(sr));
					}
					
					if (dependencies) {
						Bundle[] db = sr.getUsingBundles();
						
						if (db != null)
							for (Bundle b : Arrays.asList(db)) {
								sb.append("\tUsed by ");
								sb.append(getBundleLabel(b));
								sb.append(AbstractKnapsackCommand.CRLF);
							}
					}
					
					return sr;
				}
			}, context.getServiceReferences(null, null));
			
			return sb.toString();
		}
		
		private String getServiceProperties(ServiceReference sr) {
			StringBuffer sb = new StringBuffer();
			
			for (String key : Arrays.asList(sr.getPropertyKeys())) {
				if (key.equals("service.id") || key.equals("objectClass"))
					continue;
				
				sb.append(TAB);
				sb.append(key);
				sb.append(" = ");

				Object o = sr.getProperty(key);

				if (o instanceof String) {
					sb.append((String) o);
				} else if (o instanceof Object[]) {
					Object[] oa = (Object[]) o;

					sb.append("[");
					for (int j = 0; j < oa.length; ++j) {
						sb.append(oa[j].toString());

						if (j != oa.length - 2) {
							sb.append(", ");
						}
					}
					sb.append("]");
				}	
				sb.append(AbstractKnapsackCommand.CRLF);
			}		

			return sb.toString();
		}

		@Override
		public String getName() {
			return "services";
		}
		
		@Override
		public String getUsage() {
			return "[-b (brief)] [-d (show dependencies)] [-p (show properties)]";
		}
		
		@Override
		public String getDescription() {
			return "Display OSGi services active in the framework.";
		}
		
	}
	
	private class PrintConfCommand extends AbstractKnapsackCommand {

		@Override
		public String execute() throws Exception {
			final StringBuilder sb = new StringBuilder();
			
			Fn.map(new Fn.Function<Entry<Object, Object>, Object>() {

				@Override
				public Object apply(Entry<Object, Object> e) {
					sb.append(e.getKey());
					sb.append(" = ");
					sb.append(e.getValue());
					sb.append(CRLF);
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
			final StringBuilder sb = new StringBuilder();
			
			Fn.map(new PrintBundleFunction(sb, !arguments.contains("-b")), context.getBundles());
			
			return sb.toString();
		}

		@Override
		public String getName() {
			return "bundles";
		}
		
		@Override
		public String getUsage() {
			return "[-b (brief)]";
		}
		
		@Override
		public String getDescription() {
			return "Get list of OSGi bundles installed in the framework.";
		}
	}
	

	private class HeadersCommand extends AbstractKnapsackCommand {

		@Override
		public String execute() throws Exception {
			final StringBuilder sb = new StringBuilder();
			PrintHeadersFunction function = new PrintHeadersFunction(sb);
			
			if (arguments.size() == 1) {
				int bundleId = Integer.parseInt(arguments.get(0));
				function.setPrintBundle(false);
				Bundle b = context.getBundle(bundleId);
				
				if (b != null)
					function.apply(b);
				
			} else {
				function.setPrintBundle(true);
				Fn.map(function , context.getBundles());
			}
			
			return sb.toString();
		}

		@Override
		public String getName() {
			return "headers";
		}
		
		@Override
		public String getUsage() {
			return "[bundle id]";
		}
		
		@Override
		public String getDescription() {
			return "Print bundle headers.";
		}
	}
	

	/**
	 * Exit the framework.
	 * 
	 * @author kgilmer
	 * 
	 */
	private class ShutdownCommand extends AbstractKnapsackCommand {

		private static final String MSG = "OSGi framework is shutting down due to user request via shell.";
		public String execute() throws Exception {
			log.log(LogService.LOG_INFO, MSG);
			context.getBundle(0).stop();
			
			return MSG;
		}

		public String getName() {
			return "shutdown-knapsack";
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
			final StringBuilder sb = new StringBuilder();
			
			Fn.map(new PrintHelpFunction(sb), parser.getCommands().values());
			
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
	

	private void addLogEntry(LogEntry entry, StringBuilder sb, boolean verbose) {

		if (verbose) {
			sb.append(formatDateTime(entry.getTime()));
			sb.append(TAB);
			sb.append(getLevelLabel(entry.getLevel()));
			sb.append(TAB);
			sb.append(entry.getMessage());
			sb.append(TAB);
			sb.append(getBundleLabel(entry.getBundle()));	 
		} else {
			sb.append(formatDateTime(entry.getTime()));
			sb.append(entry.getMessage());
		}
				
		sb.append(AbstractKnapsackCommand.CRLF);
		
		//Check for an exception, if available display it.
		if (entry.getException() != null) {
			sb.append(entry.getException().getMessage());
			sb.append(AbstractKnapsackCommand.CRLF);
			
			StringWriter sWriter = new StringWriter();
			PrintWriter pw = new PrintWriter(sWriter);
			entry.getException().printStackTrace(pw);
			sb.append(sWriter.toString());
			sb.append(AbstractKnapsackCommand.CRLF);
		}
	}

	private String formatDateTime(long time) {
		return dateFormatter.format(new Date(time));
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
	public static long getServiceId(ServiceReference sr) {		
		return Long.parseLong(sr.getProperty("service.id").toString());
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

	public static String getBundleLabel(Bundle b) {
		StringBuilder sb = new StringBuilder();
		
		appendId(sb, b.getBundleId());
		sb.append(getBundleName(b));
		sb.append(" (");
		sb.append(getBundleVersion(b));
		sb.append(")");

		return sb.toString();
	}
	
	public static void appendId(StringBuilder sb, long id) {
		sb.append("[");
		if (id < 10)
			sb.append(" ");
			
		sb.append(id);
		sb.append("]");
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
	
	private class PrintBundleFunction implements Fn.Function<Bundle, Bundle> {
		private final boolean verbose;
		private final StringBuilder sb;

		public PrintBundleFunction(StringBuilder sb, boolean verbose) {
			this.sb = sb;
			this.verbose = verbose;
			
		}

		@Override
		public Bundle apply(Bundle b) {
			if (verbose) {
				sb.append(getStateName(b.getState()));
				sb.append(TAB);
				sb.append(getBundleLabel(b));
				sb.append(TAB);
				sb.append(getBundleLocation(b));				
			} else {
				sb.append(getBundleLabel(b));
			}
			sb.append(AbstractKnapsackCommand.CRLF);
			
			return b;
		}
	}
	
	private class PrintHeadersFunction implements Fn.Function<Bundle, Bundle> {
		
		private final StringBuilder sb;
		private boolean printBundle = false;
		private PrintBundleFunction printBundleFunction;

		public PrintHeadersFunction(StringBuilder sb) {
			this.sb = sb;			
			this.printBundleFunction = new PrintBundleFunction(sb, true);
		}
		
		public void setPrintBundle(boolean b) {
			this.printBundle  = b;
		}

		@Override
		public Bundle apply(Bundle b) {
			Dictionary headers = b.getHeaders();
			Enumeration keys = headers.keys();
			
			if (printBundle) 
				printBundleFunction.apply(b);	
			
			while (keys.hasMoreElements()) {
				Object key = keys.nextElement();
				sb.append(key.toString());
				sb.append(": ");
				sb.append(headers.get(key).toString());
				sb.append(AbstractKnapsackCommand.CRLF);
			}
			
			if (printBundle) 
				sb.append(AbstractKnapsackCommand.CRLF);	
			
			return b;
		}
	}
	
	private class PrintHelpFunction implements Fn.Function<IKnapsackCommand, IKnapsackCommand> {
		
		private final StringBuilder sb;

		public PrintHelpFunction(StringBuilder sb) {
			this.sb = sb;
		}

		@Override
		public IKnapsackCommand apply(IKnapsackCommand cmd) {
			sb.append(pad(cmd.getName() + " " + cmd.getUsage(), 20));
	
			sb.append(TAB);
			sb.append(cmd.getDescription());
			sb.append(AbstractKnapsackCommand.CRLF);
					
			return cmd;
		}
		
		private String pad(String in, int len) {
			if (in.length() >= len)
				return in;
			
			int diff = len - in.length();
			StringBuilder sb = new StringBuilder(in);
			for (int i = 0; i < diff; i++)
				sb.append(' ');
			
			return sb.toString();
		}
	}
}
