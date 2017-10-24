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

import org.apache.felix.utils.properties.TypedProperties;
import org.apache.karaf.config.command.completers.ConfigurationCompleter;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;

/**
 * Abstract class which commands that are related to property processing should extend.
 */
public abstract class ConfigPropertyCommandSupport extends ConfigCommandSupport {

    @Option(name = "-p", aliases = "--pid", description = "The configuration pid", required = false, multiValued = false)
    @Completion(ConfigurationCompleter.class)
    protected String pid;

    protected Object doExecute() throws Exception {
        TypedProperties props = getEditedProps();
        if (props == null && pid == null) {
            System.err.println("No configuration is being edited--run the edit command first");
        } else {
            if (props == null) {
                props = new TypedProperties();
            }
            propertyAction(props);
            if(requiresUpdate(pid)) {
                this.configRepository.update(pid, props);
            }
        }
        return null;
    }

    /**
     * Perform an action on the properties.
     *
     * @param props the dictionary where to apply the action.
     */
    protected abstract void propertyAction(TypedProperties props);

    /**
     * Check if the configuration requires to be updated.
     * The default behavior is to update if a valid pid has been passed to the method.
     *
     * @param pid the PID to check.
     * @return <code>true</code> if the configuration requires an update, <code>false</code> else.
     */
    protected boolean requiresUpdate(String pid) {
        return pid != null;
    }

    /**
     * Retrieve configuration from the pid, if used or delegates to session from getting the configuration.
     *
     * @return the edited dictionary.
     * @throws Exception in case of configuration failure.
     */
    @Override
    protected TypedProperties getEditedProps() throws Exception {
        if (pid != null) {
            return this.configRepository.getConfig(pid);
        }
        else {
            return super.getEditedProps();
        }
    }

}
