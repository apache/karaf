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
package org.apache.karaf.features.internal.util;

import java.util.Dictionary;

import org.osgi.framework.BundleContext;

/**
 * Resolves a property by name, optionally delegating to a fallback resolver.
 *
 * <p>A blank value is treated as no value, so resolution falls through to the fallback.</p>
 */
@FunctionalInterface
public interface PropertyResolver {

    /**
     * Resolve a property.
     *
     * @param key the property name.
     * @return the property value, or <code>null</code> if unknown.
     */
    String get(String key);

    /**
     * A resolver backed by a dictionary.
     *
     * @param properties the properties to read, may be <code>null</code>.
     * @return the resolver.
     */
    static PropertyResolver forDictionary(Dictionary<?, ?> properties) {
        return forDictionary(properties, null);
    }

    /**
     * A resolver backed by a dictionary, falling back to another resolver.
     *
     * @param properties the properties to read, may be <code>null</code>.
     * @param fallback the resolver to consult when the dictionary has no value, may be <code>null</code>.
     * @return the resolver.
     */
    static PropertyResolver forDictionary(Dictionary<?, ?> properties, PropertyResolver fallback) {
        return key -> {
            String value = null;
            if (properties != null) {
                Object raw = properties.get(key);
                if (raw instanceof String) {
                    value = (String) raw;
                }
            }
            if (value != null && value.trim().isEmpty()) {
                value = null;
            }
            if (value == null && fallback != null) {
                value = fallback.get(key);
            }
            return value;
        };
    }

    /**
     * A resolver backed by the framework properties, which themselves fall back to system properties.
     * It gives access to e.g. <code>${karaf.base}</code>.
     *
     * @param bundleContext the bundle context to read, may be <code>null</code>.
     * @return the resolver.
     */
    static PropertyResolver forBundleContext(BundleContext bundleContext) {
        return key -> {
            String value = bundleContext == null ? null : bundleContext.getProperty(key);
            return value != null && value.trim().isEmpty() ? null : value;
        };
    }

}
