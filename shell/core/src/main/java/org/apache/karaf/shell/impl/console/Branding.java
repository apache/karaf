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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.karaf.shell.api.console.Terminal;


public final class Branding {
   
    static final Logger LOGGER = LoggerFactory.getLogger(Branding.class);
     
    private Branding() { }

    public static Properties loadBrandingProperties() {
        Properties props = new Properties();
        loadProps(props, "org/apache/karaf/shell/console/branding.properties");
        loadProps(props, "org/apache/karaf/branding/branding.properties");
        return props;
    }

    public static Properties loadBrandingProperties(Terminal terminal) {
        Properties props = new Properties();
        if (terminal != null && terminal.getClass().getName().endsWith("SshTerminal")) {
            //it's a ssh client, so load branding seperately
            loadProps(props, "org/apache/karaf/shell/console/branding-ssh.properties");
        } else {
            loadProps(props, "org/apache/karaf/shell/console/branding.properties");
        }

        loadProps(props, "org/apache/karaf/branding/branding.properties");
        // load branding from etc/branding.properties
        File etcBranding = new File(System.getProperty("karaf.etc"), "branding.properties");
        if (etcBranding.exists()) {
            FileInputStream etcBrandingIs = null;
            try {
                etcBrandingIs = new FileInputStream(etcBranding);
            } catch (FileNotFoundException e) {
                LOGGER.trace("Could not load branding.", e);
            }
            loadProps(props, etcBrandingIs);
        }

        return props;
    }
    
    protected static void loadProps(Properties props, String resource) {
        InputStream is = null;
        try {
            is = Branding.class.getClassLoader().getResourceAsStream(resource);
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            // ignore
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    protected static void loadProps(Properties props, InputStream is) {
        try {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            // ignore
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

}
