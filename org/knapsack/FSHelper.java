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

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.knapsack.shell.StringConstants;
import org.osgi.service.log.LogService;

/**
 * Static methods that interface with the filesystem for specific Knapsack tasks.
 * 
 * Some of these methods were adapted from Apache commons-io version 2.0.1.  
 * Specifically copy(), copyLarge(), closeQuietly(), and write().
 * See http://commons.apache.org/io/ for details of full commons-io library.
 * 
 * @author kgilmer
 *
 */
public class FSHelper {
	  private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
	  
	/**
     * Unconditionally close a <code>Closeable</code>.
     * <p>
     * Equivalent to {@link Closeable#close()}, except any exceptions will be ignored.
     * This is typically used in finally blocks.
     * <p>
     * Example code:
     * <pre>
     *   Closeable closeable = null;
     *   try {
     *       closeable = new FileReader("foo.txt");
     *       // process closeable
     *       closeable.close();
     *   } catch (Exception e) {
     *       // error handling
     *   } finally {
     *       IOUtils.closeQuietly(closeable);
     *   }
     * </pre>
     *
     * @param closeable the object to close, may be null or already closed
     */
    public static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ioe) {
            // ignore
        }
    };

	/**
     * Unconditionally close an <code>OutputStream</code>.
     * <p>
     * Equivalent to {@link OutputStream#close()}, except any exceptions will be ignored.
     * This is typically used in finally blocks.
     * <p>
     * Example code:
     * <pre>
     * byte[] data = "Hello, World".getBytes();
     *
     * OutputStream out = null;
     * try {
     *     out = new FileOutputStream("foo.txt");
     *     out.write(data);
     *     out.close(); //close errors are handled
     * } catch (IOException e) {
     *     // error handling
     * } finally {
     *     IOUtils.closeQuietly(out);
     * }
     * </pre>
     *
     * @param output  the OutputStream to close, may be null or already closed
     */
    public static void closeQuietly(OutputStream output) {
        closeQuietly((Closeable)output);
    }
	
	  /**
     * Unconditionally close a <code>Socket</code>.
     * <p>
     * Equivalent to {@link Socket#close()}, except any exceptions will be ignored.
     * This is typically used in finally blocks.
     * <p>
     * Example code:
     * <pre>
     *   Socket socket = null;
     *   try {
     *       socket = new Socket("http://www.foo.com/", 80);
     *       // process socket
     *       socket.close();
     *   } catch (Exception e) {
     *       // error handling
     *   } finally {
     *       IOUtils.closeQuietly(socket);
     *   }
     * </pre>
     *
     * @param sock the Socket to close, may be null or already closed
     */
    public static void closeQuietly(Socket sock){
        if (sock != null){
            try {
                sock.close();
            } catch (IOException ioe) {
                // ignored
            }
        }
    }

	/**
     * Copy bytes from an <code>InputStream</code> to an
     * <code>OutputStream</code>.
     * <p>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     * <p>
     * Large streams (over 2GB) will return a bytes copied value of
     * <code>-1</code> after the copy has completed since the correct
     * number of bytes cannot be returned as an int. For large streams
     * use the <code>copyLarge(InputStream, OutputStream)</code> method.
     * 
     * @param input  the <code>InputStream</code> to read from
     * @param output  the <code>OutputStream</code> to write to
     * @return the number of bytes copied, or -1 if &gt; Integer.MAX_VALUE
     * @throws NullPointerException if the input or output is null
     * @throws IOException if an I/O error occurs
     */
    public static int copy(InputStream input, OutputStream output) throws IOException {
        long count = copyLarge(input, output);
        if (count > Integer.MAX_VALUE) {
            return -1;
        }
        return (int) count;
    }

	/**
     * Copy bytes from a large (over 2GB) <code>InputStream</code> to an
     * <code>OutputStream</code>.
     * <p>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     * 
     * @param input  the <code>InputStream</code> to read from
     * @param output  the <code>OutputStream</code> to write to
     * @return the number of bytes copied
     * @throws NullPointerException if the input or output is null
     * @throws IOException if an I/O error occurs
     */
    public static long copyLarge(InputStream input, OutputStream output)
            throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        long count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
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
		
		copy(istream, fos);
		closeQuietly(fos);
	}

	/**
	 * Copy shell scripts from the Jar into the deployment directory.
	 * @param scriptDir
	 * @param shellPort
	 * @param command
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static void copyScripts(File scriptDir, int shellPort, String command) throws IOException, URISyntaxException {
		if (!scriptDir.exists())
			if (!scriptDir.mkdirs())
				throw new IOException("Unable to create directories: " + scriptDir);

		File baseScriptFile = new File(scriptDir, ConfigurationConstants.BASE_SCRIPT_FILENAME);

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
		
			InputStream istream = ConfigurationConstants.class.getResourceAsStream("/scripts/" + ConfigurationConstants.BASE_SCRIPT_FILENAME);
			if (istream == null)
				throw new IOException("Jar resource does not exist: " + baseScriptFile);

			FileOutputStream fos = new FileOutputStream(baseScriptFile);
			
			write(sb.toString(), fos);
			copy(istream, fos);

			fos.close();
			
			baseScriptFile.setExecutable(true, true);
		}
	}

	/**
	 * Generate the filesystem symlink necessary to allow a command to be called
	 * from the shell environment.
	 * @param scriptDir
	 * @param commandName
	 * @param logger
	 * @throws IOException
	 */
	public static void createFilesystemCommand(File scriptDir, String commandName, KnapsackLogger logger) throws IOException {
		File sf = null;
		if (System.getProperty(ConfigurationConstants.CONFIG_KEY_COMMAND_PREFIX) != null)
			sf = new File(System.getProperty(ConfigurationConstants.CONFIG_KEY_COMMAND_PREFIX) + scriptDir, commandName);
		else 
			sf = new File(scriptDir, commandName);
		
		File baseScriptFile = new File(scriptDir, ConfigurationConstants.BASE_SCRIPT_FILENAME);

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
	 * Delete the filesystem symlink for a command.
	 * @param scriptDir
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
	 * @param dir
	 * @return true if directory has files.
	 * @throws IOException
	 */
	public static boolean directoryHasFiles(File dir) throws IOException {
		validateFile(dir, false, true, false, true);
		File[] children = dir.listFiles();
		return children != null && children.length > 0;
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
     * Writes chars from a <code>String</code> to bytes on an
     * <code>OutputStream</code> using the default character encoding of the
     * platform.
     * <p>
     * This method uses {@link String#getBytes()}.
     * 
     * @param data  the <code>String</code> to write, null ignored
     * @param output  the <code>OutputStream</code> to write to
     * @throws NullPointerException if output is null
     * @throws IOException if an I/O error occurs
     */
    public static void write(String data, OutputStream output)
            throws IOException {
        if (data != null) {
            output.write(data.getBytes());
        }
    }
    
    /**
	 * Static class
	 */
	private FSHelper() {
	}

}
