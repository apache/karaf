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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.felix.utils.properties.Properties;
import org.apache.felix.utils.properties.TypedProperties;

public final class Utils {

    private Utils() { }

    public static void assertNotNull(Object object, String message) {
        if (object == null) {
            throw new IllegalStateException(message);
        }
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    public static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new IllegalStateException(message);
        }
    }

    public static String join(CharSequence sep, Iterable<? extends CharSequence> strings) {
        StringBuilder sb = new StringBuilder();
        for (CharSequence str : strings) {
            if (sb.length() > 0) {
                sb.append(sep);
            }
            sb.append(str);
        }
        return sb.toString();
    }


    public static byte[] toBytes(TypedProperties source) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            source.save(baos);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Cannot store properties", ex);
        }
        return baos.toByteArray();
    }

    public static byte[] toBytes(Properties source) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            source.save(baos);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Cannot store properties", ex);
        }
        return baos.toByteArray();
    }

    public static byte[] toBytes(Map<String, Object> source) {
        return toBytes(toProperties(source));
    }

    public static TypedProperties toProperties(byte[] source)  {
        try {
            TypedProperties rc = new TypedProperties(false);
            if (source != null) {
                rc.load(new ByteArrayInputStream(source));
            }
            return rc;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Cannot load properties", ex);
        }
    }

    public static TypedProperties toProperties(Map<String, Object> source) {
        if (source instanceof TypedProperties) {
            return (TypedProperties) source;
        }
        TypedProperties rc = new TypedProperties(false);
        rc.putAll(source);
        return rc;
    }

    public static String stripSuffix(String value, String suffix) {
        if (value.endsWith(suffix)) {
            return value.substring(0, value.length() - suffix.length());
        } else {
            return value;
        }
    }
}
