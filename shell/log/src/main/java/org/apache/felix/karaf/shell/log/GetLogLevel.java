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
package org.apache.felix.karaf.shell.log;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.apache.felix.karaf.shell.console.OsgiCommandSupport;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;

/**
 * Get the log level for a given logger
 */
@Command(scope = "log", name = "get", description = "Show log level.")
public class GetLogLevel extends OsgiCommandSupport {

    @Argument(required = false, name = "LOGGER", description = "Logger name, ALL or ROOT (default)")
    String logger;

    static final String CONFIGURATION_PID  = "org.ops4j.pax.logging";
    static final String ROOT_LOGGER_PREFIX = "log4j.rootLogger";
    static final String LOGGER_PREFIX      = "log4j.logger.";
    static final String ALL_LOGGER         = "ALL";
    static final String ROOT_LOGGER        = "ROOT";

    protected Object doExecute() throws Exception {
        ConfigurationAdmin cfgAdmin = getConfigAdmin();
        Configuration cfg = cfgAdmin.getConfiguration(CONFIGURATION_PID, null);
        Dictionary props = cfg.getProperties();

        if (ROOT_LOGGER.equalsIgnoreCase(this.logger)) {
            this.logger = null;
        }
        if (ALL_LOGGER.equalsIgnoreCase(logger)) {
            String root = getLevel((String) props.get(ROOT_LOGGER_PREFIX));
            Map<String, String> loggers = new TreeMap<String, String>();
            for (Enumeration e = props.keys(); e.hasMoreElements();) {
                String prop = (String) e.nextElement();
                if (prop.startsWith(LOGGER_PREFIX)) {
                    String val = getLevel((String) props.get(prop));
                    loggers.put(prop.substring(LOGGER_PREFIX.length()), val);
                }
            }
            System.out.println("ROOT: " + root);
            for (String logger : loggers.keySet()) {
                System.out.println(logger + ": " + loggers.get(logger));
            }
        } else {
            String logger = this.logger;
            String val;
            for (;;) {
                String prop;
                if (logger == null) {
                    prop = ROOT_LOGGER_PREFIX;
                } else {
                    prop = LOGGER_PREFIX + logger;
                }
                val = (String) props.get(prop);
                val = getLevel(val);
                if (val != null || logger == null) {
                    break;
                }
                int idx = logger.lastIndexOf('.');
                if (idx < 0) {
                    logger = null;
                } else {
                    logger = logger.substring(0, idx);
                }
            }
            String st = "Level: " + val;
            if (logger != this.logger) {
                st += " (inherited from " + (logger != null ? logger : "ROOT") + ")";
            }
            System.out.println(st);
        }
        return null;
    }

    protected String getLevel(String prop) {
        if (prop == null) {
            return null;
        } else {
            String val = prop.trim();
            int idx = val.indexOf(",");
            if (idx == 0) {
                val = null;
            } else if (idx > 0) {
                val = val.substring(0, idx);
            }
            return val;
        }
    }

    protected ConfigurationAdmin getConfigAdmin() {
        ServiceReference ref = getBundleContext().getServiceReference(ConfigurationAdmin.class.getName());
        return getService(ConfigurationAdmin.class, ref);
    }

}
