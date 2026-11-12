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
package org.apache.karaf.util.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.apache.felix.utils.properties.InterpolationHelper;

/**
 * Overrides configuration properties with environment variables and system properties.
 * <p>
 * The property {@code key} of the configuration {@code pid} is overridden by the environment variable
 * {@code PID_KEY} (the pid and key upper-cased, with {@code .}, {@code -} and {@code ~} replaced by {@code _})
 * or, when that variable is not set, by the system property {@code pid.key}.
 * A value {@code [a,b]} gives a {@code String[]}, and {@code ${...}} in a value is substituted with
 * the configuration's own properties, so that an override can extend the value it replaces.
 */
public final class ConfigurationOverrides {

    private ConfigurationOverrides() {
    }

    /**
     * @param pid The configuration pid.
     * @param key The property key.
     * @return The environment variable that overrides the property.
     */
    public static String environmentVariable(String pid, String key) {
        return (pid + "." + key).toUpperCase().replace('.', '_').replace('-', '_').replace('~', '_');
    }

    /**
     * @param pid The configuration pid.
     * @param key The property key.
     * @return The system property that overrides the property.
     */
    public static String systemProperty(String pid, String key) {
        return pid + "." + key;
    }

    /**
     * Overrides the given keys of the properties from the process environment and the system properties.
     *
     * @param pid The configuration pid.
     * @param properties The configuration properties, changed in place.
     * @param keys The keys to look up.
     */
    public static void apply(String pid, Dictionary<String, Object> properties, Collection<String> keys) {
        apply(pid, properties, keys, System::getenv, System::getProperty);
    }

    /**
     * Overrides the given keys of the properties.
     *
     * @param pid The configuration pid.
     * @param properties The configuration properties, changed in place.
     * @param keys The keys to look up.
     * @param environment Looks up an environment variable.
     * @param systemProperties Looks up a system property.
     */
    public static void apply(String pid, Dictionary<String, Object> properties, Collection<String> keys,
                             Function<String, String> environment, Function<String, String> systemProperties) {
        for (String key : keys) {
            Object value = override(pid, key, properties, environment, systemProperties);
            if (value != null) {
                properties.put(key, value);
            }
        }
    }

    /**
     * Looks up the override of one property in the process environment and the system properties,
     * whether or not the properties hold the key.
     *
     * @param pid The configuration pid.
     * @param key The property key.
     * @param properties The configuration properties, which may be null.
     * @return The overriding value, or null when nothing overrides the property.
     */
    public static Object override(String pid, String key, Dictionary<String, ?> properties) {
        return override(pid, key, properties, System::getenv, System::getProperty);
    }

    /**
     * Looks up the override of one property, whether or not the properties hold the key.
     *
     * @param pid The configuration pid.
     * @param key The property key.
     * @param properties The configuration properties, which may be null.
     * @param environment Looks up an environment variable.
     * @param systemProperties Looks up a system property.
     * @return The overriding value, or null when nothing overrides the property.
     */
    public static Object override(String pid, String key, Dictionary<String, ?> properties,
                                  Function<String, String> environment, Function<String, String> systemProperties) {
        String value = environment.apply(environmentVariable(pid, key));
        if (value == null) {
            value = systemProperties.apply(systemProperty(pid, key));
        }
        if (value == null) {
            return null;
        }
        Map<String, String> substitutions = toMap(properties);
        if (value.startsWith("[") && value.endsWith("]")) {
            return Arrays.stream(value.substring(1, value.length() - 1).split(","))
                    .map(element -> InterpolationHelper.substVars(element, null, null, substitutions))
                    .toArray(String[]::new);
        }
        String substituted = InterpolationHelper.substVars(value, null, null, substitutions);
        if (properties != null && properties.get(key) instanceof Number) {
            return Integer.parseInt(substituted);
        }
        return substituted;
    }

    private static Map<String, String> toMap(Dictionary<String, ?> dictionary) {
        Map<String, String> converted = new HashMap<>();
        if (dictionary == null) {
            return converted;
        }
        Enumeration<String> keys = dictionary.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            converted.put(key, dictionary.get(key).toString());
        }
        return converted;
    }

}
