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
package org.apache.karaf.main.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Utils {
	public static final Pattern mvnPattern = Pattern
			.compile("mvn:([^/ ]+)/([^/ ]+)/([^/ ]*)(/([^/ ]+)(/([^/ ]+))?)?");

    public static File getKarafHome(Class<?> mainClass, String karafHomeProperty, String karafHomeEnv) throws IOException {
        File rc = null;

        // Use the system property if specified.
        String path = System.getProperty(karafHomeProperty);
        if (path != null) {
            rc = validateDirectoryExists(path, "Invalid " + karafHomeProperty + " system property", false, true);
        }

        if (rc == null) {
            path = System.getenv(karafHomeEnv);
            if (path != null) {
                rc = validateDirectoryExists(path, "Invalid " + karafHomeEnv + " environment variable", false, true);
            }
        }

        // Try to figure it out using the jar file this class was loaded from.
        if (rc == null) {
            // guess the home from the location of the jar
            URL url = mainClass.getClassLoader().getResource(mainClass.getName().replace(".", "/") + ".class");
            if (url != null) {
                try {
                    JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
                    url = jarConnection.getJarFileURL();
                    rc = new File(new URI(url.toString())).getCanonicalFile().getParentFile().getParentFile();
                } catch (Exception ignored) {
                }
            }
        }

        if (rc == null) {
            // Dig into the classpath to guess the location of the jar
            String classpath = System.getProperty("java.class.path");
            int index = classpath.toLowerCase().indexOf("karaf.jar");
            int start = classpath.lastIndexOf(File.pathSeparator, index) + 1;
            if (index >= start) {
                String jarLocation = classpath.substring(start, index);
                rc = new File(jarLocation).getCanonicalFile().getParentFile();
            }
        }
        if (rc == null) {
            throw new IOException("The Karaf install directory could not be determined.  Please set the " + karafHomeProperty + " system property or the " + karafHomeEnv + " environment variable.");
        }

        return rc;
    }

    public static File validateDirectoryExists(String path, String errPrefix, boolean createDirectory, boolean validate) {
        File rc;
        try {
            rc = new File(path).getCanonicalFile();
        } catch (IOException e) {
            throw new IllegalArgumentException(errPrefix + " '" + path + "' : " + e.getMessage());
        }
        if (!rc.exists() && !createDirectory && validate) {
            throw new IllegalArgumentException(errPrefix + " '" + path + "' : does not exist");
        }
        if (!rc.exists() && createDirectory) {
            try {
                rc.mkdirs();
            } catch (SecurityException se) {
                throw new IllegalArgumentException(errPrefix + " '" + path + "' : " + se.getMessage());
            }
        }
        if (rc.exists() && !rc.isDirectory()) {
            throw new IllegalArgumentException(errPrefix + " '" + path + "' : is not a directory");
        }
        return rc;
    }
    
    public static File getKarafDirectory(String directoryProperty, String directoryEnvironmentVariable, File defaultValue, boolean create, boolean validate) {
        File rc = null;
        
        String path = System.getProperty(directoryProperty);
        if (path != null) {
            rc = validateDirectoryExists(path, "Invalid " + directoryProperty + " system property", create, validate);
        }
        
        if (rc == null) {
            path = System.getenv(directoryEnvironmentVariable);
            if (path != null && validate) {
                rc = validateDirectoryExists(path, "Invalid " + directoryEnvironmentVariable  + " environment variable", create, validate);
            }
        }
        
        if (rc == null) {
            rc = defaultValue;
        }
        
        return rc;
    }

    //-----------------------------------------------------------------------
    /**
     * Recursively delete a directory.
     * @param directory directory to delete
     * @throws IOException in case deletion is unsuccessful
     */
    public static void deleteDirectory(File directory)
        throws IOException {
        if (!directory.exists()) {
            return;
        }

        cleanDirectory(directory);
        if (!directory.delete()) {
            String message =
                "Unable to delete directory " + directory + ".";
            throw new IOException(message);
        }
    }

    /**
     * Clean a directory without deleting it.
     * @param directory directory to clean
     * @throws IOException in case cleaning is unsuccessful
     */
    public static void cleanDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            String message = directory + " does not exist";
            throw new IllegalArgumentException(message);
        }

        if (!directory.isDirectory()) {
            String message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        }

        File[] files = directory.listFiles();
        if (files == null) {  // null if security restricted
            throw new IOException("Failed to list contents of " + directory);
        }

        IOException exception = null;
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            try {
                forceDelete(file);
            } catch (IOException ioe) {
                exception = ioe;
            }
        }

        if (null != exception) {
            throw exception;
        }
    }

    /**
     * <p>
     * Delete a file. If file is a directory, delete it and all sub-directories.
     * </p>
     * <p>
     * The difference between File.delete() and this method are:
     * </p>
     * <ul>
     * <li>A directory to be deleted does not have to be empty.</li>
     * <li>You get exceptions when a file or directory cannot be deleted.
     *      (java.io.File methods returns a boolean)</li>
     * </ul>
     * @param file file or directory to delete.
     * @throws IOException in case deletion is unsuccessful
     */
    public static void forceDelete(File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            if (!file.exists()) {
                throw new FileNotFoundException("File does not exist: " + file);
            }
            if (!file.delete()) {
                String message =
                    "Unable to delete file: " + file;
                throw new IOException(message);
            }
        }
    }

	/**
	 * Returns a path for an srtifact. Input: path (no ':') returns path Input:
	 * mvn:<groupId>/<artifactId>/<version>/<type>/<classifier> converts to
	 * default repo location path // * Input:
	 * <groupId>:<artifactId>:<version>:<type>:<classifier> converts to default
	 * repo location path type and classifier are optional.
	 * 
	 * 
	 * @param name
	 *            input artifact info
	 * @return path as supplied or a default maven repo path
	 */
	private static String fromMaven(String name) {
		Matcher m = mvnPattern.matcher(name);
		if (!m.matches()) {
			return name;
		}
		StringBuilder b = new StringBuilder();
		b.append(m.group(1));
		for (int i = 0; i < b.length(); i++) {
			if (b.charAt(i) == '.') {
				b.setCharAt(i, '/');
			}
		}
		b.append("/");// groupId
		String artifactId = m.group(2);
		String version = m.group(3);
		String extension = m.group(5);
		String classifier = m.group(7);
		b.append(artifactId).append("/");// artifactId
		b.append(version).append("/");// version
		b.append(artifactId).append("-").append(version);
		if (present(classifier)) {
			b.append("-").append(classifier);
		}
        if (present(extension)) {
            b.append(".").append(extension);
        } else {
            b.append(".jar");
        }
        return b.toString();
	}
	
	public static boolean present(String part) {
		return part != null && !part.isEmpty();
	}

	public static File findFile(List<File> bundleDirs, String name) {
		for (File bundleDir : bundleDirs) {
			File file = Utils.findFile(bundleDir, name);
			if (file != null) {
				return file;
			}
		}
		return null;
	}

	public static File findFile(File dir, String name) {
		name = fromMaven(name);
		File theFile = new File(dir, name);
	
		if (theFile.exists() && !theFile.isDirectory()) {
			return theFile;
		}
		return null;
	}

	public static void findJars(File dir, ArrayList<File> jars) {
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				findJars(file, jars);
			} else {
				if (file.toString().endsWith(".jar")) {
					jars.add(file);
				}
			}
		}
	}

	public static String[] convertToMavenUrlsIfNeeded(String location,
			boolean convertToMavenUrls) {
		String[] parts = location.split("\\|");
		if (convertToMavenUrls) {
			String[] p = parts[1].split("/");
			if (p.length >= 4
					&& p[p.length - 1].startsWith(p[p.length - 3] + "-"
							+ p[p.length - 2])) {
				String artifactId = p[p.length - 3];
				String version = p[p.length - 2];
				String classifier;
				String type;
				String artifactIdVersion = artifactId + "-" + version;
				StringBuffer sb = new StringBuffer();
				if (p[p.length - 1].charAt(artifactIdVersion.length()) == '-') {
					classifier = p[p.length - 1].substring(
							artifactIdVersion.length() + 1,
							p[p.length - 1].lastIndexOf('.'));
				} else {
					classifier = null;
				}
				type = p[p.length - 1].substring(p[p.length - 1]
						.lastIndexOf('.') + 1);
				sb.append("mvn:");
				for (int j = 0; j < p.length - 3; j++) {
					if (j > 0) {
						sb.append('.');
					}
					sb.append(p[j]);
				}
				sb.append('/').append(artifactId).append('/').append(version);
				if (!"jar".equals(type) || classifier != null) {
					sb.append('/');
					if (!"jar".equals(type)) {
						sb.append(type);
					}
					if (classifier != null) {
						sb.append('/').append(classifier);
					}
				}
				parts[1] = parts[0];
				parts[0] = sb.toString();
			} else {
				parts[1] = parts[0];
			}
		} else {
			parts[1] = parts[0];
		}
		return parts;
	}



}
