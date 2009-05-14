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

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scrplugin.tags.*;
import org.apache.maven.plugin.MojoExecutionException;

import com.thoughtworks.qdox.model.Annotation;

/**
 * Helper class for getting values from annotations.
 */
public abstract class Util {

    /**
     * Get a boolean value from an annotation.
     * @param annotation The annotation.
     * @param name The name of the attribute.
     * @param clazz The annotation class.
     * @return The boolean value.
     */
    public static boolean getBooleanValue(Annotation annotation, String name, final Class<?> clazz) {
        final Object obj = annotation.getNamedParameter(name);
        if ( obj != null ) {
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

    public static String[] getStringValues(Annotation annotation, JavaClassDescription desc, String name) {
        final Object obj = annotation.getNamedParameter(name);
        if ( obj != null ) {
            List<String> list;
            if (obj instanceof String) {
                list = new ArrayList<String>();
                list.add((String)obj);
            }
            else {
                list = (List<String>)obj;
            }
            String[] values = new String[list.size()];
            for (int i=0; i<values.length; i++) {
                values[i] = stripQuotes(desc, list.get(i));
            }
            return values;
        }
        return null;
    }

    public static long[] getLongValues(Annotation annotation, JavaClassDescription desc, String name) {
        final Object obj = annotation.getNamedParameter(name);
        if ( obj != null ) {
            List<Long> list;
            if (obj instanceof Long) {
                list = new ArrayList<Long>();
                list.add((Long)obj);
            }
            else {
                list = (List<Long>)obj;
            }
            long[] values = new long[list.size()];
            for (int i=0; i<values.length; i++) {
                values[i] = list.get(i);
            }
            return values;
        }
        return null;
    }

    public static int[] getIntValues(Annotation annotation, JavaClassDescription desc, String name) {
        final Object obj = annotation.getNamedParameter(name);
        if ( obj != null ) {
            List<Integer> list;
            if (obj instanceof Integer) {
                list = new ArrayList<Integer>();
                list.add((Integer)obj);
            }
            else {
                list = (List<Integer>)obj;
            }
            int[] values = new int[list.size()];
            for (int i=0; i<values.length; i++) {
                values[i] = list.get(i);
            }
            return values;
        }
        return null;
    }

    public static float[] getFloatValues(Annotation annotation, JavaClassDescription desc, String name) {
        final Object obj = annotation.getNamedParameter(name);
        if ( obj != null ) {
            List<Float> list;
            if (obj instanceof Float) {
                list = new ArrayList<Float>();
                list.add((Float)obj);
            }
            else {
                list = (List<Float>)obj;
            }
            float[] values = new float[list.size()];
            for (int i=0; i<values.length; i++) {
                values[i] = list.get(i);
            }
            return values;
        }
        return null;
    }

    public static double[] getDoubleValues(Annotation annotation, JavaClassDescription desc, String name) {
        final Object obj = annotation.getNamedParameter(name);
        if ( obj != null ) {
            List<Double> list;
            if (obj instanceof Double) {
                list = new ArrayList<Double>();
                list.add((Double)obj);
            }
            else {
                list = (List<Double>)obj;
            }
            double[] values = new double[list.size()];
            for (int i=0; i<values.length; i++) {
                values[i] = list.get(i);
            }
            return values;
        }
        return null;
    }

    public static char[] getCharValues(Annotation annotation, JavaClassDescription desc, String name) {
        final Object obj = annotation.getNamedParameter(name);
        if ( obj != null ) {
            List<Character> list;
            if (obj instanceof Character) {
                list = new ArrayList<Character>();
                list.add((Character)obj);
            }
            else {
                list = (List<Character>)obj;
            }
            char[] values = new char[list.size()];
            for (int i=0; i<values.length; i++) {
                values[i] = list.get(i);
            }
            return values;
        }
        return null;
    }

    public static short[] getShortValues(Annotation annotation, JavaClassDescription desc, String name) {
        final Object obj = annotation.getNamedParameter(name);
        if ( obj != null ) {
            List<Short> list;
            if (obj instanceof Short) {
                list = new ArrayList<Short>();
                list.add((Short)obj);
            }
            else {
                list = (List<Short>)obj;
            }
            short[] values = new short[list.size()];
            for (int i=0; i<values.length; i++) {
                values[i] = list.get(i);
            }
            return values;
        }
        return null;
    }

    public static byte[] getByteValues(Annotation annotation, JavaClassDescription desc, String name) {
        final Object obj = annotation.getNamedParameter(name);
        if ( obj != null ) {
            List<Byte> list;
            if (obj instanceof Byte) {
                list = new ArrayList<Byte>();
                list.add((Byte)obj);
            }
            else {
                list = (List<Byte>)obj;
            }
            byte[] values = new byte[list.size()];
            for (int i=0; i<values.length; i++) {
                values[i] = list.get(i);
            }
            return values;
        }
        return null;
    }

    public static boolean[] getBooleanValues(Annotation annotation, JavaClassDescription desc, String name) {
        final Object obj = annotation.getNamedParameter(name);
        if ( obj != null ) {
            List<Boolean> list;
            if (obj instanceof Boolean) {
                list = new ArrayList<Boolean>();
                list.add((Boolean)obj);
            }
            else {
                list = (List<Boolean>)obj;
            }
            boolean[] values = new boolean[list.size()];
            for (int i=0; i<values.length; i++) {
                values[i] = list.get(i);
            }
            return values;
        }
        return null;
    }

    public static String getStringValue(Annotation annotation, JavaClassDescription desc, String name, final Class<?> clazz) {
        final Object obj = annotation.getNamedParameter(name);
        if ( obj != null ) {
            if ( obj instanceof String ) {
                return stripQuotes(desc, (String)obj);
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
     * QDox annotations seem to return annotation values always with quotes - remove them.
     * If the string value does not contain a string, it's a reference to a field!
     * @param s String with our without quotes
     * @return String without quotes
     */
    private static String stripQuotes(final JavaClassDescription desc, String s)
    throws IllegalArgumentException {
        try {
            s = s.trim();
            if (s.startsWith("\"") && s.endsWith("\"")) {
                return s.substring(1, s.length() - 1);
            }
            int classSep = s.lastIndexOf('.');
            JavaField field = null;
            if ( classSep == -1 ) {
                // local variable
                field = desc.getFieldByName(s);
            }
            if ( field == null ) {
                field = desc.getExternalFieldByName(s);
            }
            if ( field == null ) {
                throw new IllegalArgumentException("Property references unknown field " + s + " in class " + desc.getName());
            }
            String[] values = field.getInitializationExpression();
            if ( values != null && values.length == 1 ) {
                return values[0];
            }
            throw new IllegalArgumentException("Something is wrong.");
        } catch (MojoExecutionException mee) {
            throw new IllegalArgumentException(mee);
        }
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
                Object o = Enum.valueOf(enumClass, enumName);
                return (T)o;
            }
        }
        return null;
    }

}
