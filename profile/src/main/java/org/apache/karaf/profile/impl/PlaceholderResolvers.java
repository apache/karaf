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
package org.apache.karaf.profile.impl;

import java.util.Map;

import org.apache.karaf.profile.PlaceholderResolver;
import org.osgi.framework.BundleContext;

public final class PlaceholderResolvers {

    private PlaceholderResolvers() { }

    public static class ProfilePlaceholderResolver implements PlaceholderResolver {

        public final String SCHEME = "profile";

        @Override
        public String getScheme() {
            return SCHEME;
        }

        @Override
        public String resolve(Map<String, Map<String, String>> profile, String pid, String key, String value) {
            int index = value.indexOf("/");
            if (index >= 0) {
                String propertyPid = value.substring(0, index);
                String propertyKey = value.substring(index + 1);
                Map<String, String> props = profile.get(propertyPid);
                if (props != null && props.containsKey(propertyKey)) {
                    Object v = props.get(propertyKey);
                    return v.toString();
                }
            }
            return null;
        }
    }

    /**
     * Substitute a placeholder with profile:[property file]/[key], with the target value.
     *
     * @param key The key in the configuration.
     * @param configs A {@link Map} of configurations where to perform the substitution.
     * @return The target value or the key as is.
     */
    public static String substituteProfileProperty(String key, Map<String, Map<String, String>> configs) {
        String pid = key.substring("profile:".length(), key.indexOf("/"));
        String propertyKey = key.substring(key.indexOf("/") + 1);
        Map<String, String> targetProps = configs.get(pid);
        if (targetProps != null && targetProps.containsKey(propertyKey)) {
            return targetProps.get(propertyKey);
        } else {
            return key;
        }
    }

    /**
     * Substitutes bundle property.
     *
     * @param key The key in the configuration.
     * @param bundleContext The bundle context to use.
     * @return The target value or an empty String.
     */
    public static String substituteBundleProperty(String key, BundleContext bundleContext) {
        String value = null;
        if (bundleContext != null) {
            value = bundleContext.getProperty(key);
        }
        if (value == null) {
            value = System.getProperty(key);
        }
        return value != null ? value : "";
    }

}
