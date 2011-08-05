package org.knapsack;

public class PropertyHelper {

	public static boolean getBoolean(String key) {
		if (System.getProperty(key) == null)
			return false;
	
		return Boolean.parseBoolean(System.getProperty(key));
	}
}
