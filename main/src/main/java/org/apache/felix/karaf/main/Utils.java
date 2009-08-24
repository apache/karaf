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
package org.apache.felix.karaf.main;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.JarURLConnection;
import java.net.URI;

import org.apache.felix.karaf.main.Main;

public class Utils {

    public static File getKarafHome() throws IOException {
        File rc = null;

        // Use the system property if specified.
        String path = System.getProperty(Main.PROP_KARAF_HOME);
        if (path != null) {
            rc = validateDirectoryExists(path, "Invalid " + Main.PROP_KARAF_HOME + " system property");
        }

        if (rc == null) {
            path = System.getenv(Main.ENV_KARAF_HOME);
            if (path != null) {
                rc = validateDirectoryExists(path, "Invalid " + Main.ENV_KARAF_HOME + " environment variable");
            }
        }

        // Try to figure it out using the jar file this class was loaded from.
        if (rc == null) {
            // guess the home from the location of the jar
            URL url = Main.class.getClassLoader().getResource(Main.class.getName().replace(".", "/") + ".class");
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
            throw new IOException("The Karaf install directory could not be determined.  Please set the " + Main.PROP_KARAF_HOME + " system property or the " + Main.ENV_KARAF_HOME + " environment variable.");
        }

        return rc;
    }

    public static File validateDirectoryExists(String path, String errPrefix) {
        File rc;
        try {
            rc = new File(path).getCanonicalFile();
        } catch (IOException e) {
            throw new IllegalArgumentException(errPrefix + " '" + path + "' : " + e.getMessage());
        }
        if (!rc.exists()) {
            throw new IllegalArgumentException(errPrefix + " '" + path + "' : does not exist");
        }
        if (!rc.isDirectory()) {
            throw new IllegalArgumentException(errPrefix + " '" + path + "' : is not a directory");
        }
        return rc;
    }

    public static File getKarafBase(File defaultValue) {
        File rc = null;

        String path = System.getProperty(Main.PROP_KARAF_BASE);
        if (path != null) {
            rc = validateDirectoryExists(path, "Invalid " + Main.PROP_KARAF_BASE + " system property");
        }

        if (rc == null) {
            path = System.getenv(Main.ENV_KARAF_BASE);
            if (path != null) {
                rc = validateDirectoryExists(path, "Invalid " + Main.ENV_KARAF_BASE + " environment variable");
            }
        }

        if (rc == null) {
            rc = defaultValue;
        }
        return rc;
    }
}
