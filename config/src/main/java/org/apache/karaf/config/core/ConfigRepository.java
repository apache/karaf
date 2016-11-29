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
package org.apache.karaf.config.core;

import java.io.IOException;
import java.util.Dictionary;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationAdmin;

public interface ConfigRepository {

    /**
     * Save config to storage or ConfigurationAdmin.
     *
     * @param pid the configuration PID.
     * @param props the dictionary used to update the configuration.
     * @throws IOException in case of update failure.
     */
    void update(String pid, Dictionary<String, Object> props) throws IOException;

    void delete(String pid) throws Exception;

    Dictionary<String, Object> getConfigProperties(String pid) throws IOException, InvalidSyntaxException;

    ConfigurationAdmin getConfigAdmin();

    /**
     * Create a factory based configuration.
     *
     * @param factoryPid the configuration factory PID.
     * @param properties the new properties to set in the configuration.
     * @return the created configuration PID.
     */
    String createFactoryConfiguration(String factoryPid, Dictionary<String, Object> properties);

    String createFactoryConfiguration(String factoryPid, String alias, Dictionary<String, Object> properties);
}
