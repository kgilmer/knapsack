package org.knapsack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;

/**
 * Static methods that interface with the filesystem for specific Knapsack tasks.
 * @author kgilmer
 *
 */
public class FSHelper {

	private static final String LS = System.getProperty("line.separator");

	/**
	 * Static class
	 */
	private FSHelper() {
	};

	/**
	 * Copy shell scripts from the Jar into the deployment directory.
	 * 
	 * @param baseDirectory
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void copyScripts(File baseDirectory, int shellPort) throws IOException {
		File scriptDir = new File(baseDirectory, "bin");

		if (!scriptDir.exists())
			if (!scriptDir.mkdirs())
				throw new IOException("Unable to create directories: " + scriptDir);

		File baseScriptFile = new File(scriptDir, Config.BASE_SCRIPT_FILENAME);

		if (!baseScriptFile.exists()) {
			String scriptPrefix = "#!/bin/bash" + LS + "KNAPSACK_PORT=" + shellPort + LS;
			InputStream istream = Config.class.getResourceAsStream("/scripts/" + Config.BASE_SCRIPT_FILENAME);
			if (istream == null)
				throw new IOException("Jar resource does not exist: " + baseScriptFile);

			FileOutputStream fos = new FileOutputStream(baseScriptFile);
			
			IOUtils.write(scriptPrefix, fos);
			IOUtils.copy(istream, fos);

			fos.close();
			
			baseScriptFile.setExecutable(true, true);
		}
	}

	/**
	 * Generate the filesystem symlink necessary to allow a command to be called
	 * from the shell environment.
	 * 
	 * @param commandName
	 * @throws IOException
	 */
	public static void createFilesystemCommand(File scriptDir, String commandName) throws IOException {
		File sf = new File(scriptDir, commandName);
		File baseScriptFile = new File(scriptDir, Config.BASE_SCRIPT_FILENAME);

		if (sf.exists())
			throw new IOException(commandName + " already exists in " + scriptDir);

		try {
			createSymlink(baseScriptFile.getAbsolutePath(), scriptDir + File.separator + commandName);
			Activator.logDebug("Created symlink " + commandName);
		} catch (InterruptedException e) {
			throw new IOException("Process was interrupted.", e);
		}
	}

	/**
	 * Create symlink via native system command.
	 * @param baseFile
	 * @param link
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private static void createSymlink(String baseFile, String link) throws InterruptedException, IOException {
		String[] cmd = { "ln", "-s", baseFile, link };
		Runtime.getRuntime().exec(cmd).waitFor();
	}

	/**
	 * Delete the filesystem symlink for a command.
	 * 
	 * @param commandName
	 * @throws IOException
	 */
	public static void deleteFilesystemCommand(File scriptDir, String commandName) throws IOException {
		File cmd = new File(scriptDir, commandName);

		if (!cmd.exists() || !cmd.isFile())
			throw new IOException("Invalid file: " + cmd);

		if (!cmd.delete())
			throw new IOException("Failed to delete " + cmd);
	}

	/**
	 * Generate the default configuration.
	 * 
	 * @param targetConfFile
	 * @return
	 * @throws IOException
	 */
	public static void copyDefaultConfiguration(String sourceResourceFilename, File targetConfFile, File baseDirectory) throws IOException {
		InputStream istream = Config.class.getResourceAsStream(sourceResourceFilename);

		if (istream == null)
			throw new IOException("Configuration resource is not present: " + sourceResourceFilename);

		OutputStream fos = new FileOutputStream(targetConfFile);
		
		StringBuilder fullLine = new StringBuilder();
		for (String line : IOUtils.readLines(istream)) {
			if (line.length() == 0 || line.startsWith("#")) {
				IOUtils.write(line, fos);
				IOUtils.write(LS, fos);
				continue;
			}
			
			fullLine.append(line);
			fullLine.append(LS);
			
			if (!line.endsWith("\\")) {
				line = fullLine.toString();
				String [] elems = line.split("=");
				
				if (elems.length < 2)
					throw new IOException("Invalid line in config admin property file: " + line);
				
				String key = elems[0];
				
				String outLine = null;
				if (System.getProperty(key.trim()) != null) {
					outLine = key + "=" + System.getProperty(key.trim());
				} else {
					outLine = key + "=" + line.substring(key.length() + 1);
				}
				IOUtils.write(outLine, fos);
				IOUtils.write(LS, fos);
				fullLine = new StringBuilder();
			} 		
		}

		if (baseDirectory != null) {
			File configAdminDir = new File(baseDirectory, Config.CONFIGADMIN_DIRECTORY_NAME);
			validateFile(configAdminDir, true, true, false, true);
			// Since this property is not static, create dynamically. If
			// multiple properties need to be set dynamically in the future,
			// consider using a template format.
			IOUtils.write(LS + "felix.cm.dir = " + configAdminDir + LS, fos);
		}

		istream.close();
		fos.close();
	}
	
	private static String appendElementsAsString(String[] elems, int start) {
		StringBuilder sb = new StringBuilder();
		
		for (int i = start; i < elems.length; ++i)
			sb.append(elems[i]);
		
		return sb.toString();
	}

	/**
	 * Determine that a File is as specified by input parameters, throw exception if something does not match.
	 * 
	 * @param f input file
	 * @param createIfNecessary In case of directory, create the directory(s) if don't exist.
	 * @param shouldExist fail if does not exist.
	 * @param isFile fail if not a file.
	 * @param isDirectory fail if not a directory.
	 * @throws IOException thrown when input parameters do not match input file.
	 */
	public static void validateFile(File f, boolean createIfNecessary, boolean shouldExist, boolean isFile, boolean isDirectory) throws IOException {
		if (createIfNecessary && !shouldExist)
			throw new IOException("Invalid parameters: create and !shouldExist");

		if (createIfNecessary && isFile)
			throw new IOException("Invalid parameters: create and isFile.");

		if (createIfNecessary && !f.exists())
			if (!f.mkdirs())
				throw new IOException("Unable to create " + f);

		if (shouldExist && !f.exists())
			throw new IOException(f.getAbsolutePath() + " does not exist.");

		if (!shouldExist && f.exists())
			throw new IOException(f.getAbsolutePath() + " already exists.");

		if (isFile && !f.isFile())
			throw new IOException(f.getAbsolutePath() + " is not a file.");

		if (isDirectory && !f.isDirectory())
			throw new IOException(f.getAbsolutePath() + " is not a directory.");
	}

	public static boolean directoryHasFiles(File dir) throws IOException {
		validateFile(dir, false, true, false, true);
		File[] children = dir.listFiles();
		return children != null && children.length > 0;
	}

	public static void deleteFilesInDir(File dir) throws IOException {
		validateFile(dir, false, true, false, true);
		for (File f : Arrays.asList(dir.listFiles()))
			if (!f.delete())
				throw new IOException("Unable to delete: " + f);
	}
}
