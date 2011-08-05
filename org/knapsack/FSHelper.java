/*
 *    Copyright 2011 Ken Gilmer
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.knapsack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.knapsack.shell.StringConstants;
import org.osgi.service.log.LogService;

/**
 * Static methods that interface with the filesystem for specific Knapsack tasks.
 * @author kgilmer
 *
 */
public class FSHelper {
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
	 * @throws URISyntaxException 
	 * @throws InterruptedException
	 */
	public static void copyScripts(File scriptDir, int shellPort, String command) throws IOException, URISyntaxException {
		if (!scriptDir.exists())
			if (!scriptDir.mkdirs())
				throw new IOException("Unable to create directories: " + scriptDir);

		File baseScriptFile = new File(scriptDir, PropertyKeys.BASE_SCRIPT_FILENAME);

		if (!baseScriptFile.exists()) {
			StringBuilder sb = new StringBuilder();
			sb.append("#!/bin/sh");
			sb.append(StringConstants.CRLF);
			sb.append("KNAPSACK_PORT=");
			sb.append(shellPort);
			sb.append(StringConstants.CRLF);
			sb.append("KNAPSACK_JAR=");
			sb.append(FSHelper.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
			sb.append(StringConstants.CRLF);
			sb.append("COMMAND=\"");
			sb.append(command);
			sb.append("\"");
			sb.append(StringConstants.CRLF);
		
			InputStream istream = PropertyKeys.class.getResourceAsStream("/scripts/" + PropertyKeys.BASE_SCRIPT_FILENAME);
			if (istream == null)
				throw new IOException("Jar resource does not exist: " + baseScriptFile);

			FileOutputStream fos = new FileOutputStream(baseScriptFile);
			
			IOUtils.write(sb.toString(), fos);
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
	public static void createFilesystemCommand(File scriptDir, String commandName, KnapsackLogger logger) throws IOException {
		File sf = new File(scriptDir, commandName);
		File baseScriptFile = new File(scriptDir, PropertyKeys.BASE_SCRIPT_FILENAME);

		if (sf.exists())
			throw new IOException(commandName + " already exists in " + scriptDir);

		try {
			createSymlink(baseScriptFile.getAbsolutePath(), scriptDir + File.separator + commandName);
			logger.log(LogService.LOG_DEBUG, "Created symlink " + commandName);
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

	/**
	 * @param dir
	 * @return
	 * @throws IOException
	 */
	public static boolean directoryHasFiles(File dir) throws IOException {
		validateFile(dir, false, true, false, true);
		File[] children = dir.listFiles();
		return children != null && children.length > 0;
	}

	/**
	 * @param dir
	 * @throws IOException
	 */
	public static void deleteFilesInDir(File dir) throws IOException {
		validateFile(dir, false, true, false, true);
		for (File f : Arrays.asList(dir.listFiles()))
			if (!f.delete())
				throw new IOException("Unable to delete: " + f);
	}	

	/**
	 * @param resourceFilename
	 * @param outFile
	 * @throws IOException
	 */
	public static void copyResourceToFile(String resourceFilename, File outFile) throws IOException {
		if (outFile.exists())
			throw new IOException(outFile + " already exists, not overwriting.");
		
		InputStream istream = FSHelper.class.getResourceAsStream(resourceFilename);

		if (istream == null)
			throw new IOException("Jar resource is not present: " + resourceFilename);
		
		FileOutputStream fos = new FileOutputStream(outFile);
		
		IOUtils.copy(istream, fos);
		IOUtils.closeQuietly(fos);
	}
}
