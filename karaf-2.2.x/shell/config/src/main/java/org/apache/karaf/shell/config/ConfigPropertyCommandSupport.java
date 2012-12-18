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
package org.apache.karaf.shell.config;

import java.util.Dictionary;

import org.apache.felix.gogo.commands.Option;
import java.util.Properties;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Abstract class which commands that are related to property processing should extend.
 */
public abstract class ConfigPropertyCommandSupport extends ConfigCommandSupport {

    @Option(name = "-p", aliases = "--pid", description = "The configuration pid", required = false, multiValued = false)
    protected String pid;

    @Option(name = "-b", aliases = { "--bypass-storage" }, multiValued = false, required = false, description = "Do not store the configuration in a properties file, but feed it directly to ConfigAdmin")
    protected boolean bypassStorage;


    protected void doExecute(ConfigurationAdmin admin) throws Exception {
        Dictionary props = getEditedProps();
        if (props == null && pid == null) {
            System.err.println("No configuration is being edited--run the edit command first");
        } else {
            if (props == null) {
                props = new Properties();
            }
            propertyAction(props);
            if(requiresUpdate(pid)) {
                update(admin, pid, props, bypassStorage);
            }
        }
    }

    /**
     * Perform an action on the properties.
     * @param props
     */
    protected abstract void propertyAction(Dictionary props);

    /**
     * Checks if the configuration requires to be updated.
     * The default behavior is to update if a valid pid has been passed to the method.
     * @param pid
     * @return
     */
    protected boolean requiresUpdate(String pid) {
        if (pid != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Retrieves confguration from the pid, if used or delegates to session from getting the configuration.
     * @return
     * @throws Exception
     */
    @Override
    protected Dictionary getEditedProps() throws Exception {
        if(pid != null) {
            ConfigurationAdmin configurationAdmin = getConfigurationAdmin();
            if (configurationAdmin != null) {
                Configuration[] configs = configurationAdmin.listConfigurations("(service.pid=" + pid + ")");
                if (configs != null && configs.length > 0) {
                    Configuration configuration = configs[0];
                    if (configuration != null) {
                        return configuration.getProperties();
                    }
                }
            }
        }
        return super.getEditedProps();
    }

}
