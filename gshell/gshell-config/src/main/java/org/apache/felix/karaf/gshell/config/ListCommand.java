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
package org.apache.felix.karaf.gshell.config;

import java.util.Dictionary;
import java.util.Enumeration;

import org.apache.geronimo.gshell.clp.Argument;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class ListCommand extends ConfigCommandSupport {

    @Argument(required = false, description = "LDAP query")
    String query;

    protected void doExecute(ConfigurationAdmin admin) throws Exception {
        Configuration[] configs = admin.listConfigurations(query);
        for (Configuration config : configs) {
            io.out.println("----------------------------------------------------------------");
            io.out.println("Pid:            " + config.getPid());
            if (config.getFactoryPid() != null) {
                io.out.println("FactoryPid:     " + config.getFactoryPid());
            }
            io.out.println("BundleLocation: " + config.getBundleLocation());
            if (config.getProperties() != null) {
                io.out.println("Properties:");
                Dictionary props = config.getProperties();
                for (Enumeration e = props.keys(); e.hasMoreElements();) {
                    Object key = e.nextElement();
                    io.out.println("   " + key + " = " + props.get(key));
                }
            }
        }
    }
}
