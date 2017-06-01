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
package org.apache.karaf.config.command;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.osgi.service.cm.Configuration;

@Command(scope = "config", name = "list", description = "Lists existing configurations.")
@Service
public class ListCommand extends ConfigCommandSupport {

    @Argument(index = 0, name = "query", description = "Query in LDAP syntax. Example: \"(service.pid=org.apache.karaf.log)\"", required = false, multiValued = false)
    String query;

    @SuppressWarnings("rawtypes")
    protected Object doExecute() throws Exception {
        Configuration[] configs = configRepository.getConfigAdmin().listConfigurations(query);
        if (configs != null) {
            Map<String, Configuration> sortedConfigs = new TreeMap<>();
            for (Configuration config : configs) {
                sortedConfigs.put(config.getPid(), config);
            }
            for (String pid : sortedConfigs.keySet()) {
                Configuration config = sortedConfigs.get(pid);
                System.out.println("----------------------------------------------------------------");
                System.out.println("Pid:            " + config.getPid());
                if (config.getFactoryPid() != null) {
                    System.out.println("FactoryPid:     " + config.getFactoryPid());
                }
                System.out.println("BundleLocation: " + config.getBundleLocation());
                if (config.getProperties() != null) {
                    System.out.println("Properties:");
                    Dictionary props = config.getProperties();
                    Map<String, Object> sortedProps = new TreeMap<>();
                    for (Enumeration e = props.keys(); e.hasMoreElements();) {
                        Object key = e.nextElement();
                        sortedProps.put(key.toString(), props.get(key));
                    }
                    for (String key : sortedProps.keySet()) {
                        System.out.println("   " + key + " = " + displayValue(sortedProps.get(key)));
                    }
                }
            }
        }
        return null;
    }
}
