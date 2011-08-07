/*
 * Applier.java - Class for applying functions to sets.
 * Created by Ken Gilmer, July, 2011.  See https://github.com/kgilmer/Sprinkles
 * Released into the public domain.
 */
package org.sprinkles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Map a function from one list to another. A way of applying a function to list
 * and tree data structures. See
 * http://en.wikipedia.org/wiki/Map_(higher-order_function)
 * 
 * @author kgilmer
 * 
 */
public final class Applier {
	
	/**
	 * Stateless utility class.
	 */
	private Applier() {
	}

	/**
	 * Interface that represents a Function in Sprinkles.
	 *
	 * @param <I> Input type
	 * @param <O> Output type
	 */
	public interface Fn<I, O> {
		/**
		 * @param input Input to function
		 * @return result of function. A null result will not be tracked in any further recursing on input set for map() or find().
		 */		
		O apply(I input);
	}

	/**
	 * A function in a fold operation.
	 * 
	 * @author kgilmer
	 * 
	 */
	public interface FoldFn<I, O> {
		/**
		 * @param input Input to function
		 * @param previousOutput result from previous application of function.
		 * @return result of application, which will be passed as <code>previousOutput</code> to next set element if exists.
		 */
		O apply(I input, O previousOutput);
	}

	/**
	 * Map a function over an input. Will recurse if any element is also Iterable.
	 * @param input
	 *            element or Collection to apply the function.
	 *            If input is null, function is not executed and empty list is returned.
	 * @param function
	 *            to apply to input.
	 * 
	 * @return collection of results of execution of function. If null is
	 *         returned from function, nothing is added.
	 */
	/**
	 * @param <I> The type that will be passed into the function as input.
	 * @param <O> The type that will be returned from the function as output.
	 * @param input input to the function, can be a single value, array, or Collection, all of which will be treated as a set.
	 * @param function the function that will be applied to elements of the input
	 * @return A collection of the results of the application of the function against the input element(s).
	 */
	public static <I, O> Collection<O> map(Object input, Fn<I, O> function) {
		if (input == null)
			return Collections.emptyList();
		
		Collection<O> out = new ArrayList<O>();
		Iterable<?> in;

		// If the input is iterable, treat as such, otherwise apply function to
		// single element.
		if (input instanceof Iterable)
			in = (Iterable<?>) input;
		else if (input instanceof Object[])
			in = Arrays.asList((Object []) input);
		else 
			in = Arrays.asList(input);

		applyMap(function, in, out, false, true, true);

		return Collections.unmodifiableCollection(out);
	}
		
	/**
	 * Map a function over an input, adding results to a pre-existing output collection.
	 * @param <I> The type that will be passed into the function as input.
	 * @param <O> The type that will be returned from the function as output.
	 * @param input input to the function, can be a single value, array, or Collection, all of which will be treated as a set.
	 * @param out Pre-existing collection to which results will be added.
	 * @param function the function that will be applied to elements of the input
	 */
	public static <I, O> void map(Object input, Collection<O> out, Fn<I, O> function) {		
		Iterable<?> in;

		// If the input is iterable, treat as such, otherwise apply function to
		// single element.
		if (input instanceof Iterable)
			in = (Iterable<?>) input;
		else
			in = Arrays.asList(input);

		applyMap(function, in, out, false, true, true);
	}

	/**
	 * The map function.
	 * 
	 * @param <I> The type that will be passed into the function as input.
	 * @param <O> The type that will be returned from the function as output.
	 * @param function
	 *            Function to be applied
	 * @param input
	 *            Collection to apply the function to.
	 * @param collection
	 *            Stores the result of function application.
	 * @param stopFirstMatch
	 *            Return (stop recursing) after first time function returns
	 *            non-null value.
	 * @param adaptMap
	 *            Inspect input types, if is a Map, iterate over values of map.
	 * @param recurse
	 *            Call apply on any elements of collection that are iterable.
	 */
	private static <I, O> void applyMap(Fn<I, O> function, Iterable<?> input
			, Collection<O> collection, boolean stopFirstMatch, boolean adaptMap, boolean recurse) {
		
		for (Object child : input) {
			boolean isIterable = child instanceof Collection;
			
			if (!isIterable && adaptMap) {
				if (child instanceof Map) {
					child = (I) ((Map) child).values();
					isIterable = true;
				}
			}

			if (isIterable && recurse) {
				
				applyMap(function, (Iterable<?>) child, collection, stopFirstMatch, adaptMap, recurse);
			} else {
				O result = function.apply((I) child);

				if (result != null) {
					collection.add(result);

					if (stopFirstMatch) {
						return;
					}
				}
			}
		}
	}

	/**
	 * Apply a function to a list of elements, passing the result of the
	 * previous call on to the next.
	 * 
	 * @param <I> The type that will be passed into the function as input.
	 * @param <O> The type that will be returned from the function as output.
	 * @param input input to the function, can be a single value, array, or Collection, all of which will be treated as a set.
	 * @param function the function that will be applied to elements of the input
	 * @return A collection of the results of the application of the function against the input element(s).
	 */
	public static <I, O> O fold(Object input, FoldFn<I, O> function) {
		Collection<?> in;

		// If the input is iterable, treat as such, otherwise apply function to
		// single element.
		if (input instanceof Collection)
			in = (Collection<?>) input;
		else if (input instanceof Object[])
			in = Arrays.asList((Object []) input);
		else
			in = Arrays.asList(input);

		return applyFold(function, in, null, true, true);
	}

	/**
	 * @param <I> The type that will be passed into the function as input.
	 * @param <O> The type that will be returned from the function as output.
	 * @param function Fold function to apply
	 * @param input Object, array, or Collection to apply
	 * @param result Result of final call to apply() for last element of set.
	 * @param adaptMap Adapt the Map type such that values that are Collections are re-evaluated recursively.
	 * @param recurse A function that returns a Collection will be recursed into.
	 * @return A collection of the results of the application of the fold function against the input element(s).
	 */
	private static <I, O> O applyFold(FoldFn<I, O> function, Iterable<?> input
			, O result, boolean adaptMap, boolean recurse) {
		
		for (Object child : input) {
			boolean isIterable = child instanceof Collection;

			if (!isIterable && adaptMap) {
				if (child instanceof Map) {
					child = ((Map<?, ?>) child).values();
					isIterable = true;
				}
			}

			if (isIterable && recurse) {
				result = applyFold(function, (Iterable<?>) child, result, adaptMap, recurse);
			} else {
				result = function.apply((I) child, result);
			}
		}

		return result;
	}

	/**
	 * 
	 * Apply function to element until one function returns non-null value. If
	 * no function matches, returns null, otherwise returns results of
	 * Function.apply().
	 * @param <I> The type that will be passed into the function as input.
	 * @param <O> The type that will be returned from the function as output.
	 * @param input input passed to function, if null is passed, null is returned without evaluation.
	 * @param function to be executed
	 * 
	 * @return The first non-null result from application of function.
	 */
	public static <I, O> O find(Object input, Fn<I, O> function) {
		if (input == null)
			return null;
		
		Collection<O> out = new ArrayList<O>();
		Collection<?> in;
		if (input instanceof Collection)
			in = (Collection<?>) input;
		else if (input instanceof Object[])
			in = Arrays.asList((Object []) input);
		else
			in = Arrays.asList(input);

		Applier.applyMap(function, in, out, true, true, true);

		if (out.size() > 1) {
			throw new RuntimeException(
					"Sanity check failed; call to apply with stopFirstMatch should never add more than one element to collection.");
		} else if (out.size() == 1) {
			return out.iterator().next();
		}

		return null;
	}

	/**
	 * Apply collection of functions in order for each element of input (depth
	 * first). A result that is also Iterable will cause the function to
	 * recurse.
	 * @param input input to evaluate, if null passed in empty set is returned and no evaluation occurs.
	 * @param fns
	 * 
	 * @return
	 */
	/**
	 * @param <I> The type that will be passed into the function as input.
	 * @param <O> The type that will be returned from the function as output.
	 * @param input input to evaluate, if null passed in empty set is returned and no evaluation occurs.
	 * @param functions Set of functions to evaluate.
	 * @return Collection of the results
	 */
	public static <I, O> Collection<O> map(Object input, Collection<Fn<I, O>> functions) {
		if (input == null)
			return Collections.emptyList();
		
		Collection<O> results = new ArrayList<O>();
		Collection ic = null;
		if (input instanceof Collection) {
			ic = (Collection) input;
		} else if (input instanceof Object[]) {
			ic = Arrays.asList((Object []) input);
		} else {
			ic = new ArrayList();

			ic.add(input);
		}

		mapDepthFirst(new ArrayList(functions), ic, results, 0);

		return results;
	}

	/**
	 * Internal function that does the depth-first match operation.
	 * 
	 * @param functions List of Fn to evaluate
	 * @param input input to evaluate against
	 * @param results Collection to store results of all calls.
	 * @param functionIndex index of current function in list that is being evaluated.
	 */
	private static void mapDepthFirst(List<Fn> functions, Object input, Collection results, int functionIndex) {
		Fn f = functions.get(functionIndex);

		if (input instanceof Collection) {
			for (Object elem : (Collection) input) {
				Object result = f.apply(elem);

				if (result == null) {
					continue;
				}

				if (functionIndex < functions.size() - 1) {
					mapDepthFirst(functions, result, results, functionIndex + 1);
				} else {
					results.add(result);
				}
			}
		} else {
			Object result = f.apply(input);

			if (result == null) {
				return;
			}

			if (functionIndex < functions.size() - 1) {
				mapDepthFirst(functions, result, results, functionIndex + 1);
			} else {
				results.add(result);
			}
		}
	}
}
