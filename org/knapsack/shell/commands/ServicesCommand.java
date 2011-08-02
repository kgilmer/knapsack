package org.knapsack.shell.commands;

import java.util.Arrays;

import org.knapsack.shell.AbstractKnapsackCommand;
import org.knapsack.shell.StringConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.sprinkles.Applier;

/**
 * Prints OSGi Service Registry information.
 * 
 * @author kgilmer
 *
 */
public class ServicesCommand extends AbstractKnapsackCommand {

	@Override
	public String execute() throws Exception {
		final StringBuilder sb = new StringBuilder();
		final boolean verbose = !arguments.contains("-b");
		final boolean dependencies = arguments.contains("-d");
		final boolean properties = arguments.contains("-p");
		
		Applier.map(context.getServiceReferences(null, null), 
				new Applier.Fn<ServiceReference, ServiceReference>() {

			@Override
			public ServiceReference apply(ServiceReference sr) {
				if (verbose) {
					BundlesCommand.appendId(sb, ServicesCommand.getServiceId(sr));
					sb.append(StringConstants.TAB);
					sb.append(ServicesCommand.getServiceName(sr));
					sb.append(StringConstants.TAB);
					sb.append(BundlesCommand.getBundleLabel(sr.getBundle()));						
					sb.append(StringConstants.CRLF);
				} else {
					BundlesCommand.appendId(sb, ServicesCommand.getServiceId(sr));						
					sb.append(StringConstants.TAB);
					sb.append(ServicesCommand.getServiceName(sr));
					sb.append(StringConstants.CRLF);
				}
				
				if (properties) {
					sb.append(getServiceProperties(sr));
				}
				
				if (dependencies) {
					Bundle[] db = sr.getUsingBundles();
					
					if (db != null)
						for (Bundle b : Arrays.asList(db)) {
							sb.append("\tUsed by ");
							sb.append(BundlesCommand.getBundleLabel(b));
							sb.append(StringConstants.CRLF);
						}
				}
				
				return sr;
			}
		});
		
		return sb.toString();
	}
	
	private String getServiceProperties(ServiceReference sr) {
		StringBuffer sb = new StringBuffer();
		
		for (String key : Arrays.asList(sr.getPropertyKeys())) {
			if (key.equals("service.id") || key.equals("objectClass"))
				continue;
			
			sb.append(StringConstants.TAB);
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
			sb.append(StringConstants.CRLF);
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
	
}
