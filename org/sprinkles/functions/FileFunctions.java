/*
 * Applier.java - Class for applying functions to sets.
 * Created by Ken Gilmer, July, 2011.  See https://github.com/kgilmer/Sprinkles
 * Released into the public domain.
 */
package org.sprinkles.functions;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.sprinkles.Applier;
import org.sprinkles.Applier.Fn;

/**
 * A function to return files and directories.
 * @author kgilmer
 *
 */
public final class FileFunctions  {
	
	/**
	 * Stateless utility class.
	 */
	private FileFunctions() {
	}
	
	/**
	 * Get all files (not directories).  Assumes input is a File or Collection of Files.
	 */
	public static final Fn<File, Collection<File>> GET_FILES_FN = new GetFiles(true, false, null, null);
	/**
	 * Get all directories.  Assumes input is a File or Collection of Files.
	 */
	public static final Fn<File, Collection<File>> GET_DIRS_FN = new GetFiles(false, true, null, null);
	/**
	 * Get all files and directories.  Assumes input is a File or Collection of Files.
	 */
	public static final Fn<File, Collection<File>> GET_FILES_AND_DIRS_FN = new GetFiles(true, true, null, null);	
	
	/**
	 * A function that walks a directory tree and adds files to the result collection.
	 * 
	 * @author kgilmer
	 *
	 */
	public static class GetFiles implements Applier.Fn<File, Collection<File>> {

		private boolean incFile;
		private boolean incDir;
		private FileFilter ffilter;
		private FilenameFilter fnfilter;

		/**
		 * @param file get files
		 * @param dir get directories
		 * @param ffilter file filter
		 * @param fnfilter filename filter
		 */
		public GetFiles(boolean file, boolean dir, FileFilter ffilter, FilenameFilter fnfilter) {
			this.incFile = file;
			this.incDir = dir;
			this.ffilter = ffilter;
			this.fnfilter = fnfilter;
		}
		
		@Override
		public Collection<File> apply(File f) {		
			Collection<File> c = new ArrayList<File>();
			
			this.fileToCollection(f, c);
			
			return c;
		}
		
		/**
		 * @param f base file
		 * @param container container to add to
		 */
		private void fileToCollection(File f, Collection<File> container) {
			if ((f.isFile() && incFile) || (f.isDirectory() && incDir)) {
				container.add(f);
			} 

			File[] oa = null;
			
			if (ffilter != null) {
				oa = f.listFiles(ffilter);
			} else if (fnfilter != null) {
				oa = f.listFiles(fnfilter);
			} else {
				oa = f.listFiles();
			}
			
			if (oa != null && oa.length > 0) {
				for (File cf : Arrays.asList(oa)) {
					fileToCollection(cf, container);
				}
			}
		}
	}
}
