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
package org.apache.karaf.shell.config;

import java.util.Dictionary;

import org.apache.karaf.shell.console.OsgiCommandSupport;

/**
 * Abstract class from which all commands related to the ConfigurationAdmin
 * service should derive.
 * This command retrieves a reference to the ConfigurationAdmin service before
 * calling another method to actually process the command.
 */
public abstract class ConfigCommandSupport extends OsgiCommandSupport {
    public static final String PROPERTY_CONFIG_PID = "ConfigCommand.PID";
    public static final String PROPERTY_CONFIG_PROPS = "ConfigCommand.Props";
    protected ConfigRepository configRepository;

    @SuppressWarnings("rawtypes")
    protected Dictionary getEditedProps() throws Exception {
        return (Dictionary) this.session.get(PROPERTY_CONFIG_PROPS);
    }
    
    public void setConfigRepository(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

}
