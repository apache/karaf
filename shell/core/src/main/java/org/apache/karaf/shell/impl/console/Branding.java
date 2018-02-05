/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.impl.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;


public final class Branding {
    static final Logger LOGGER = LoggerFactory.getLogger(Branding.class);

    private Branding() { }

    public static Properties loadBrandingProperties(boolean ssh) {
        Properties props = new Properties();
        loadPropsFromResource(props, "org/apache/karaf/shell/console/", ssh);
        loadPropsFromResource(props, "org/apache/karaf/branding/", ssh);
        loadPropsFromFile(props, System.getProperty("karaf.etc") + "/", ssh);
        return props;
    }

    private static void loadPropsFromFile(Properties props, String fileName, boolean ssh) {
        loadPropsFromFile(props, fileName + "branding.properties");
        if (ssh) {
            loadPropsFromFile(props, fileName + "branding-ssh.properties");
        }
    }

    private static void loadPropsFromFile(Properties props, String fileName) {
        try (FileInputStream is = new FileInputStream(fileName)) {
            loadProps(props, is);
        } catch (IOException e) {
            LOGGER.trace("Could not load branding.", e);
        }
    }

    private static void loadPropsFromResource(Properties props, String resource, boolean ssh) {
        loadPropsFromResource(props, resource + "branding.properties");
        if (ssh) {
            loadPropsFromResource(props, resource + "branding-ssh.properties");
        }
    }

    private static void loadPropsFromResource(Properties props, String resource) {
        try (InputStream is = Branding.class.getClassLoader().getResourceAsStream(resource)) {
            loadProps(props, is);
        } catch (IOException e) {
            LOGGER.trace("Could not load branding.", e);
        }
    }

    private static void loadProps(Properties props, InputStream is) throws IOException {
        if (is != null) {
            props.load(is);
        }
    }

}
