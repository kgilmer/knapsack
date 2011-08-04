/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.knapsack;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.knapsack.shell.StringConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * A subclass of the Felix logger that prints log output in a different style.
 **/
public class Logger extends org.apache.felix.framework.Logger {
	private static final String DEFAULT_DATE_FORMAT = "MM.dd HH:mm:ss";
	private static SimpleDateFormat dateFormatter;
	
	/**
	 * @param dateFormat
	 */
	public Logger(String dateFormat) {
		dateFormatter = new SimpleDateFormat(dateFormat);
	}
	
	/**
	 * 
	 */
	public Logger() {
		this(DEFAULT_DATE_FORMAT);
	}
	
	protected void doLog(Bundle bundle, ServiceReference sr, int level, String msg, Throwable throwable) {
		doKnapsackLog(bundle, sr, level, msg, throwable);
	}
	
	/**
	 * Print a log entry in the knapsack style.
	 * @param bundle
	 * @param sr
	 * @param level
	 * @param msg
	 * @param throwable
	 */
	public static void doKnapsackLog(Bundle bundle, ServiceReference sr, int level, String msg, Throwable throwable) {
		if (dateFormatter == null)
			dateFormatter = new SimpleDateFormat(DEFAULT_DATE_FORMAT);
		
		StringBuilder sb = new StringBuilder();
		
		sb.append(dateFormatter.format(new Date(System.currentTimeMillis())));
		sb.append(' ');
		getLevelLabel(level, sb);
		sb.append(' ');
		sb.append(msg);
		sb.append(StringConstants.TAB);
		getBundleLabel(bundle, sb);	 
		
		//Check for an exception, if available display it.
		if (throwable != null) {
			sb.append(throwable.getMessage());
			sb.append(StringConstants.CRLF);
			
			StringWriter sWriter = new StringWriter();
			PrintWriter pw = new PrintWriter(sWriter);
			throwable.printStackTrace(pw);
			sb.append(sWriter.toString());
		}
		
		System.out.println(sb.toString());
	}
	
	/**
	 * @param b
	 * @param sb
	 */
	private static void getBundleLabel(Bundle b, StringBuilder sb) {
		if (b == null)
			return;		
		
		appendId(sb, b.getBundleId());
		getBundleName(b, sb);
		sb.append(" (");
		getBundleVersion(b, sb);
		sb.append(")");	
	}
	
	/**
	 * @param b
	 * @return The bundle version as defined in the manifest.
	 */
	public static void getBundleVersion(Bundle b, StringBuilder sb) {
		String version = (String) b.getHeaders().get("Bundle-Version");
	
		if (version == null) {
			version = "";
		}

		sb.append(version);		
	}
	
	/**
	 * @param b
	 * @param sb
	 */
	public static void getBundleName(Bundle b, StringBuilder sb) {
		String name = (String) b.getHeaders().get("Bundle-SymbolicName");
	
		if (name == null) {
			name = (String) b.getHeaders().get("Bundle-Name");
		}
	
		if (name == null) {
			name = "Undefined";
		}
	
		if (name.indexOf(";") > -1)
			name = name.split(";")[0];
	

		sb.append(name);		
	}
	
	/**
	 * @param sb
	 * @param id
	 */
	private static void appendId(StringBuilder sb, long id) {		
		sb.append("[");
		if (id < 10)
			sb.append(" ");
		sb.append(id);
		sb.append("]");		
	}
	
	/**
	 * @param level
	 * @return A human-readable log level string.
	 */
	private static void getLevelLabel(int level, StringBuilder sb) {
						
		switch (level) {
		case 1:		
			sb.append("ERROR  ");
		case 2:		
			sb.append("WARNING");
		case 3:		
			sb.append("INFO   ");	
		case 4:			
			sb.append("DEBUG  ");			
		default:
			sb.append("UNKNOWN");				
		}	
	}
}
