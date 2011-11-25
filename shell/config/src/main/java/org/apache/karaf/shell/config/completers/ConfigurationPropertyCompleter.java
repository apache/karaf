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

package org.apache.karaf.shell.config.completers;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.config.ConfigCommandSupport;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.apache.karaf.shell.console.jline.CommandSessionHolder;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * {@link jline.Completor} for Configuration Admin properties.
 *
 * Displays a list of existing properties based on the current configuration being edited.
 *
 */
public class ConfigurationPropertyCompleter implements Completer {

    private final StringsCompleter delegate = new StringsCompleter();

    private ConfigurationAdmin configAdmin;



    public int complete(final String buffer, final int cursor, final List candidates) {
        CommandSession session = CommandSessionHolder.getSession();
        if (session != null) {
            String pid = (String) session.get(ConfigCommandSupport.PROPERTY_CONFIG_PID);
            Set<String> propertyNames = getPropertyNames(pid);
            if (propertyNames != null && !propertyNames.isEmpty()) {
                delegate.getStrings().addAll(propertyNames);
            }
        }
        return delegate.complete(buffer,cursor,candidates);
    }

    private Set<String> getPropertyNames(String pid) {
        Set<String> propertyNames = new HashSet<String>();
        if (pid != null) {
            Configuration configuration = null;
            try {
                configuration = configAdmin.getConfiguration(pid);
                if (configuration != null) {
                    Dictionary properties = configuration.getProperties();
                    if (properties != null) {
                        Enumeration keys = properties.keys();
                        while (keys.hasMoreElements()) {
                            propertyNames.add(String.valueOf(keys.nextElement()));
                        }
                    }
                }
            } catch (IOException e) {
              //Ignore
            }
        }
        return propertyNames;
    }

    public ConfigurationAdmin getConfigAdmin() {
        return configAdmin;
    }

    public void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }
}
