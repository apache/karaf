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
package org.apache.felix.karaf.shell.config;

import java.util.Dictionary;

import org.apache.felix.karaf.shell.console.OsgiCommandSupport;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Abstract class from which all commands related to the ConfigurationAdmin
 * service should derive.
 * This command retrieves a reference to the ConfigurationAdmin service before
 * calling another method to actually process the command.
 */
public abstract class ConfigCommandSupport extends OsgiCommandSupport {

    public static final String PROPERTY_CONFIG_PID = "ConfigCommand.PID";
    public static final String PROPERTY_CONFIG_PROPS = "ConfigCommand.Props";

    protected Object doExecute() throws Exception {
        // Get config admin service.
        ServiceReference ref = getBundleContext().getServiceReference(ConfigurationAdmin.class.getName());
        if (ref == null) {
            System.out.println("ConfigurationAdmin service is unavailable.");
            return null;
        }
        try {
            ConfigurationAdmin admin = (ConfigurationAdmin) getBundleContext().getService(ref);
            if (admin == null) {
                System.out.println("ConfigAdmin service is unavailable.");
                return null;
            }

            doExecute(admin);
        }
        finally {
            getBundleContext().ungetService(ref);
        }
        return null;
    }

    protected Dictionary getEditedProps() throws Exception {
        return (Dictionary) this.session.get(PROPERTY_CONFIG_PROPS);
    }

    protected abstract void doExecute(ConfigurationAdmin admin) throws Exception;

}
