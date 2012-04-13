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
import java.util.Properties;

import org.apache.karaf.shell.commands.Option;

/**
 * Abstract class which commands that are related to property processing should extend.
 */
public abstract class ConfigPropertyCommandSupport extends ConfigCommandSupport {

    @Option(name = "-p", aliases = "--pid", description = "The configuration pid", required = false, multiValued = false)
    protected String pid;

    @SuppressWarnings("rawtypes")
    protected Object doExecute() throws Exception {
        Dictionary props = getEditedProps();
        if (props == null && pid == null) {
            System.err.println("No configuration is being edited--run the edit command first");
        } else {
            if (props == null) {
                props = new Properties();
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
     * @param props
     */
    @SuppressWarnings("rawtypes")
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
    @SuppressWarnings("rawtypes")
    @Override
    protected Dictionary getEditedProps() throws Exception {
        Dictionary props = this.configRepository.getConfigProperties(pid);
        return (props != null) ? props : super.getEditedProps();
    }
}
