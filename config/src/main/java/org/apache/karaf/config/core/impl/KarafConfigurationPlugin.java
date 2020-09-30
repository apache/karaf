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
package org.apache.karaf.config.core.impl;

import org.apache.felix.utils.properties.InterpolationHelper;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationPlugin;

import java.util.*;

public class KarafConfigurationPlugin implements ConfigurationPlugin {

    public static final String PLUGIN_ID = "org.apache.karaf.config.plugin";
    public static final int PLUGIN_RANKING = 500;

    @Override
    public void modifyConfiguration(ServiceReference<?> reference, Dictionary<String, Object> properties) {
        final Object pid = properties.get(Constants.SERVICE_PID);
        for (Enumeration<String> keys = properties.keys(); keys.hasMoreElements(); ) {
            String key = keys.nextElement();
            // looking for env variable and system property matching key (pid.key).toUpperCase().replace('.', '_')
            String env = (pid + "." + key).toUpperCase().replaceAll("\\.", "_");
            String sys = pid + "." + key;
            if (System.getenv(env) != null) {
                String value = InterpolationHelper.substVars(System.getenv(env), null,null, convertDictionaryToMap(properties));
                if (properties.get(key) != null && (properties.get(key) instanceof Number)) {
                    properties.put(key, Integer.parseInt(value));
                } else {
                    properties.put(key, value);
                }
            } else if (System.getProperty(sys) != null) {
                String value = InterpolationHelper.substVars(System.getProperty(sys), null, null, convertDictionaryToMap(properties));
                if (properties.get(key) != null && (properties.get(key) instanceof Number)) {
                    properties.put(key, Integer.parseInt(value));
                } else {
                    properties.put(key, value);
                }
            }
        }
    }

    private static Map<String, String> convertDictionaryToMap(Dictionary<String, Object> dictionary) {
        Map<String, String> converted = new HashMap<>();
        Enumeration<String> keys = dictionary.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            converted.put(key, dictionary.get(key).toString());
        }
        return converted;
    }

}
