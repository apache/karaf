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
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "config", name = "update", description = "Saves and propagates changes from the configuration being edited.")
@Service
public class UpdateCommand extends ConfigCommandSupport {

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Object doExecute() throws Exception {
        TypedProperties props = getEditedProps();
        if (props == null) {
            System.err.println("No configuration is being edited. Run the edit command first.");
            return null;
        }

        String pid = (String) this.session.get(PROPERTY_CONFIG_PID);
        String suffix = (String) this.session.get(PROPERTY_TYPE);
        boolean isFactory = this.session.get(PROPERTY_FACTORY) != null && (Boolean) this.session.get(PROPERTY_FACTORY);
        if (isFactory) {
            String alias = (String) this.session.get(PROPERTY_ALIAS);
            this.configRepository.createFactoryConfiguration(pid, alias, props, suffix);
        } else {
        	this.configRepository.update(pid, props, suffix);
        }
        this.session.put(PROPERTY_CONFIG_PID, null);
        this.session.put(PROPERTY_FACTORY, null);
        this.session.put(PROPERTY_CONFIG_PROPS, null);
        this.session.put(PROPERTY_TYPE, null);
        if (this.session.get(PROPERTY_ALIAS) != null) {
            this.session.put(PROPERTY_ALIAS, null);
        }
        return null;
    }
}
