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
package org.apache.felix.scrplugin.tags.annotation.defaulttag;

import java.util.EnumSet;
import java.util.List;

import org.apache.felix.scrplugin.tags.ClassUtil;

import com.thoughtworks.qdox.model.Annotation;

public abstract class Util {

    public static boolean getBooleanValue(Annotation annotation, String name, final Class<?> clazz) {
        final Object obj = annotation.getNamedParameter(name);
        if ( obj != null ) {
            if ( obj instanceof String ) {
                return Boolean.valueOf((String)obj);
            } else if ( obj instanceof Boolean ) {
                return (Boolean)obj;
            }
            return Boolean.valueOf(obj.toString());
        }
        try {
            return (Boolean) clazz.getMethod(name).getDefaultValue();
        } catch( NoSuchMethodException mnfe) {
            // we ignore this
            return true;
        }
    }

    public static int getIntValue(Annotation annotation, String name, final Class<?> clazz) {
        final Object obj = annotation.getNamedParameter(name);
        if ( obj != null ) {
            if ( obj instanceof String ) {
                return Integer.valueOf((String)obj);
            } else if ( obj instanceof Number ) {
                return ((Number)obj).intValue();
            }
            return Integer.valueOf(obj.toString());
        }
        try {
            return (Integer) clazz.getMethod(name).getDefaultValue();
        } catch( NoSuchMethodException mnfe) {
            // we ignore this
            return 0;
        }
    }

    public static String[] getStringValues(Annotation annotation, String name, final Class<?> clazz) {
        final Object obj = annotation.getNamedParameter(name);
        if ( obj != null ) {
            List<String> list = (List<String>)obj;
            String[] values = new String[list.size()];
            for (int i=0; i<values.length; i++) {
                values[i] = stripQuotes(list.get(i));
            }
            return values;
        }
        try {
            return (String[]) clazz.getMethod(name).getDefaultValue();
        } catch( NoSuchMethodException mnfe) {
            // we ignore this
            return null;
        }
    }

    public static String getStringValue(Annotation annotation, String name, final Class<?> clazz) {
        final Object obj = annotation.getNamedParameter(name);
        if ( obj != null ) {
            if ( obj instanceof String ) {
                return stripQuotes((String)obj);
            }
            return obj.toString();
        }
        try {
            return (String) clazz.getMethod(name).getDefaultValue();
        } catch( NoSuchMethodException mnfe) {
            // we ignore this
            return "";
        }
    }
    
    /**
     * QDox annotations seemt to return annotation values always with quotes - remove them
     * @param s String with our without quotes
     * @return String without quotes
     */
    private static String stripQuotes(String s) {
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    public static Class<?> getClassValue(Annotation annotation, String name, final Class<?> clazz) {
        final Object obj = annotation.getNamedParameter(name);
        if ( obj != null ) {
            if ( obj instanceof Class ) {
                return (Class<?>)obj;
            }
            return ClassUtil.getClass(obj.toString());
        }
        try {
            return (Class<?>) clazz.getMethod(name).getDefaultValue();
        } catch( NoSuchMethodException mnfe) {
            // we ignore this
            return null;
        }
    }

    public static <T extends Enum> T getEnumValue(Annotation annotation, String name, final Class<T> enumClass, final Class<?> clazz) {
        Object obj = annotation.getNamedParameter(name);
        if (obj == null) {
            try {
                obj = clazz.getMethod(name).getDefaultValue();
            } catch( NoSuchMethodException mnfe) {
                // we ignore this
            }
        }
        if ( obj != null ) {
            if (enumClass.isAssignableFrom(obj.getClass())) {
                return (T)obj;
            }
            else if ( obj instanceof String ) {
                String enumName = (String)obj;
                int dotPos = enumName.lastIndexOf('.');
                if (dotPos >= 0) {
                    enumName = enumName.substring(dotPos+1);
                }
                return Enum.valueOf(enumClass, enumName);
            }
        }
        return null;
    }

}
