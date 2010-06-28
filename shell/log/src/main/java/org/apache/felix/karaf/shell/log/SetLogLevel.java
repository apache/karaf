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

import java.io.IOException;
import java.util.Dictionary;

import org.apache.felix.karaf.shell.console.OsgiCommandSupport;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Set the log level for a given logger
 */
@Command(scope = "log", name = "set", description = "Sets the log level.")
public class SetLogLevel extends OsgiCommandSupport {
    
    @Argument(index = 0, name = "level", description = "The log level to set (TRACE, DEBUG, INFO, WARN, ERROR) or DEFAULT to unset", required = true, multiValued = false)
    String level;

    @Argument(index = 1, name = "logger", description = "Logger name or ROOT (default)", required = false, multiValued = false)
    String logger;

    static final String CONFIGURATION_PID  = "org.ops4j.pax.logging";
    static final String ROOT_LOGGER_PREFIX = "log4j.rootLogger";
    static final String LOGGER_PREFIX      = "log4j.logger.";
    static final String ROOT_LOGGER        = "ROOT";

    protected Object doExecute() throws Exception {
        if (ROOT_LOGGER.equalsIgnoreCase(this.logger)) {
            this.logger = null;
        }
        
        // make sure both uppercase and lowercase levels are supported
        level = level.toUpperCase();
        
        try {
            Level.valueOf(level);
        } catch (IllegalArgumentException e) {
            System.err.println("level must be set to TRACE, DEBUG, INFO, WARN or ERROR (or DEFAULT to unset it)");
            return null;
        }
        
        if (Level.isDefault(level) && logger == null) {
            System.err.println("Can not unset the ROOT logger");
            return null;
        }

        Configuration cfg = getConfiguration();
        Dictionary props = cfg.getProperties();

        String logger = this.logger;
        String val;
        String prop;
        if (logger == null) {
            prop = ROOT_LOGGER_PREFIX;
        } else {
            prop = LOGGER_PREFIX + logger;
        }
        val = (String) props.get(prop);
        if (Level.isDefault(level)) {
            if (val != null) {
                val = val.trim();
                int idx = val.indexOf(",");
                if (idx < 0) {
                    val = null;
                } else {
                    val = val.substring(idx);
                }
            }
        } else {
            if (val == null) {
                val = level;
            } else {
                val = val.trim();
                int idx = val.indexOf(",");
                if (idx < 0) {
                    val = level;
                } else {
                    val = level + val.substring(idx);
                }
            }
        }
        if (val == null) {
            props.remove(prop);
        } else {
            props.put(prop, val);
        }
        cfg.update(props);

        return null;
    }
    
    

    protected Configuration getConfiguration() throws IOException {
        Configuration cfg = getConfigAdmin().getConfiguration(CONFIGURATION_PID, null);
        return cfg;
    }

    protected ConfigurationAdmin getConfigAdmin() {
        ServiceReference ref = getBundleContext().getServiceReference(ConfigurationAdmin.class.getName());
        return getService(ConfigurationAdmin.class, ref);
    }

}
