/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.jaas.modules;

import org.apache.karaf.jaas.boot.ProxyLoginModule;

import javax.security.auth.login.AppConfigurationEntry;
import java.util.List;

public class BackingEngineService {

    private List<BackingEngineFactory> engineFactories;

    public BackingEngine get(AppConfigurationEntry entry) {

        if (engineFactories != null) {
            for (BackingEngineFactory factory : engineFactories) {
                String loginModuleClass = (String) entry.getOptions().get(ProxyLoginModule.PROPERTY_MODULE);
                if (factory.getModuleClass().equals(loginModuleClass)) {
                    return factory.build(entry.getOptions());
                }
            }
        }
        return null;
    }

    public List<BackingEngineFactory> getEngineFactories() {
        return engineFactories;
    }

    public void setEngineFactories(List<BackingEngineFactory> engineFactories) {
        this.engineFactories = engineFactories;
    }
}
