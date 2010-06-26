/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.karaf.shell.dev.framework;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Class to represent the underlying OSGi framework
 */
public abstract class Framework {

    /*
     * The Karaf base directory
     */
    private File base;

    /*
     * The contents of the etc/config.properties file
     */
    private List<String> config;

    /**
     * Create a instance of this framework in the given base directory
     *
     * @param base the base directory
     */
    public Framework(File base) {
        super();
        this.base = base;
    }

    /**
     * Get the underlying OSGi framework name
     */
    public abstract String getName();

    /**
     * Enable the OSGi framework's debug logging
     *
     * @param directory the directory containing the Karaf installation
     * @throws IOException when a problem occurs configuring debug settings
     */
    public abstract void enableDebug(File directory) throws IOException;

    /**
     * Disable the OSGI framework's debug logging
     *
     * @param directory the Karaf base installation directory
     * @throws IOException when a problem occurs removing debug configuration settings
     */
    public abstract void disableDebug(File directory) throws IOException;

    /*
     * Save the etc/config.properties file
     */
    protected void saveConfigProperties() throws IOException {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new File(base, "etc/config.properties"));
            for (String line : getConfig()) {
                writer.printf("%s%n", line);
            }
            writer.flush();
            writer.close();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    protected List<String> readPropertyFile(File config) throws IOException {
        List<String> result = new LinkedList<String>();
        BufferedReader reader = new BufferedReader(new FileReader(config));
        String line = reader.readLine();
        while (line != null) {
            result.add(line);
            line = reader.readLine();
        }
        return result;
    }

    /**
     * Set a new key and value in the etc/config.properties - if the given key
     * already exists, the existing value will be overwritten
     *
     * @param key property key
     * @param value property value
     * @throws IOException if the etc/config.properties file can not be read
     */
    protected void setConfigProperty(String key, String value) throws IOException {
        boolean done = false;

        for (int i = 0 ; i < getConfig().size() ; i++) {
            if (getConfig().get(i).startsWith(key)) {
                getConfig().set(i, java.lang.String.format("%s=%s", key, value));
                done = true;
            }
        }

        if (!done) {
            getConfig().add("");
            getConfig().add(java.lang.String.format("%s=%s", key, value, new Date()));
        }
    }

    /**
     * Remove an existing key from the etc/config.properties file
     *
     * @param key the key
     * @throws IOException if the etc/config.properties file can not be read
     */
    protected void removeConfigProperty(String key) throws IOException {
        for (int i = 0 ; i < getConfig().size() ; i++) {
            if (getConfig().get(i).startsWith(key)) {
                getConfig().remove(i);
            }
        }
    }

    /**
     * Access the contents of the etc/config.properties file
     *
     * @return the contents of the file
     * @throws IOException if the etc/config.properties file can not be read
     */
    public List<String> getConfig() throws IOException {
        if (config == null) {
            config = readPropertyFile(new File(base, "etc/config.properties"));
        }
        return config;
    }
}
