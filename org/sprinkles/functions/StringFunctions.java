/*
 * StringFunctions.java - This class holds common String functions.
 * Created by Ken Gilmer, July, 2011.  See https://github.com/kgilmer/Sprinkles
 * Released into the public domain.
 */
package org.sprinkles.functions;

import java.util.Collection;

import org.sprinkles.Applier;
import org.sprinkles.Applier.FoldFn;

/**
 * @author kgilmer
 *
 */
public final class StringFunctions {

	/**
	 * Stateless utility class.
	 */
	private StringFunctions() {		
	}
	
	/**
	 * Convenience method to call fold on the JoinFn.
	 * @param in set of input elements
	 * @param delimiter string to place between each element
	 * @return A new string with all the elements joined with the delimiter.
	 */
	public static String join(Collection<String> in, String delimiter) {
		return Applier.fold(in, new JoinFn(delimiter)).toString();
	}
	
	/**
	 * Create a string of the elements of a String collection with a delimiter between each entry.
	 */
	public static final class JoinFn implements FoldFn<String, StringBuilder> {

		private final String delimiter;

		/**
		 * @param delimiter delimiter to use in join
		 */
		public JoinFn(String delimiter) {
			this.delimiter = delimiter;
		}
		
		@Override
		public StringBuilder apply(String element, StringBuilder result) {
			if (result == null)
				result = new StringBuilder();
			
			result.append(element);
			result.append(delimiter);
			
			return result;
		}
		
	}
}
