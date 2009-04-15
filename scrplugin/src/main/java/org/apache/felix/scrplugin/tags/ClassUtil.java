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
package org.apache.felix.scrplugin.tags;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

/**
 * Utility class.
 */
public class ClassUtil {

    public static ClassLoader classLoader;

    /**
     * Try to get the initial value of a static field
     * @param clazz     The class.
     * @param fieldName The name of the field.
     * @return The initial value or null.
     */
    public static String[] getInitializationExpression(Class<?> clazz, String fieldName) {
        try {
            final Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            final Object value = field.get(null);
            if ( value != null ) {
                if ( value.getClass().isArray() ) {
                    final String[] values = new String[Array.getLength(value)];
                    for(int i=0; i<values.length; i++) {
                        values[i] = Array.get(value, i).toString();
                    }
                    return values;
                }
                return new String[] {value.toString()};
            }
            return null;
        } catch (NoClassDefFoundError e) {
            // ignore and just return null
        } catch (Exception e) {
            // ignore and just return null
        }
        return null;
    }

    /**
     * Get the compiled class.
     */
    public static Class<?> getClass(String name) {
        if ( classLoader == null ) {
            return null;
        }
        try {
            if ( name.endsWith(".class") ) {
                name = name.substring(0, name.length() - 6);
            }
            return classLoader.loadClass(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
