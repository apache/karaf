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
package org.apache.felix.gogo.commands.converter;

import java.util.Collection;
import java.util.Map;
import java.util.Dictionary;
import java.util.Locale;
import java.util.Properties;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.LinkedHashMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Modifier;
import java.lang.reflect.Constructor;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.lang.reflect.InvocationTargetException;

@Deprecated
public class DefaultConverter {

    private Object loader;

    public DefaultConverter(Object loader) {
        this.loader = loader;
    }

    public Object convert(Object source, Type target) throws Exception {
        return convert(source, new GenericType(target));
    }

    public Object convert(Object fromValue, ReifiedType type) throws Exception {
        // Discard null values
        if (fromValue == null) {
            return null;
        }
        // If the object is an instance of the type, just return it
        if (isAssignable(fromValue, type)) {
            return fromValue;
        }
        Object value = convertWithConverters(fromValue, type);
        if (value == null) {
            if (fromValue instanceof Number && Number.class.isAssignableFrom(unwrap(toClass(type)))) {
                return convertToNumber((Number) fromValue, toClass(type));
            } else if (fromValue instanceof String) {
                return convertFromString((String) fromValue, toClass(type), loader);
            } else if (toClass(type).isArray() && (fromValue instanceof Collection || fromValue.getClass().isArray())) {
                return convertToArray(fromValue, type);
            } else if (Map.class.isAssignableFrom(toClass(type)) && (fromValue instanceof Map || fromValue instanceof Dictionary)) {
                return convertToMap(fromValue, type);
            } else if (Dictionary.class.isAssignableFrom(toClass(type)) && (fromValue instanceof Map || fromValue instanceof Dictionary)) {
                return convertToDictionary(fromValue, type);
            } else if (Collection.class.isAssignableFrom(toClass(type)) && (fromValue instanceof Collection || fromValue.getClass().isArray())) {
                return convertToCollection(fromValue, type);
            } else {
                throw new Exception("Unable to convert value " + fromValue + " to type " + type);
            }
        }
        return value;
    }

    private Object convertWithConverters(Object source, ReifiedType type) throws Exception {
        Object value = null;
//        for (Converter converter : converters) {
//            if (converter.canConvert(source, type)) {
//                value = converter.convert(source, type);
//                if (value != null) {
//                    return value;
//                }
//            }
//        }
        return value;
    }

    public Object convertToNumber(Number value, Class toType) throws Exception {
        toType = unwrap(toType);
        if (AtomicInteger.class == toType) {
            return new AtomicInteger((Integer) convertToNumber(value, Integer.class));
        } else if (AtomicLong.class == toType) {
            return new AtomicLong((Long) convertToNumber(value, Long.class));
        } else if (Integer.class == toType) {
            return value.intValue();
        } else if (Short.class == toType) {
            return value.shortValue();
        } else if (Long.class == toType) {
            return value.longValue();
        } else if (Float.class == toType) {
            return value.floatValue();
        } else if (Double.class == toType) {
            return value.doubleValue();
        } else if (Byte.class == toType) {
            return value.byteValue();
        } else if (BigInteger.class == toType) {
            return new BigInteger(value.toString());
        } else if (BigDecimal.class == toType) {
            return new BigDecimal(value.toString());
        } else {
            throw new Exception("Unable to convert number " + value + " to " + toType);
        }
    }

    public Object convertFromString(String value, Class toType, Object loader) throws Exception {
        toType = unwrap(toType);
        if (ReifiedType.class == toType) {
            try {
                return GenericType.parse(value, loader);
            } catch (ClassNotFoundException e) {
                throw new Exception("Unable to convert", e);
            }
        } else if (Class.class == toType) {
            try {
                return GenericType.parse(value, loader).getRawClass();
            } catch (ClassNotFoundException e) {
                throw new Exception("Unable to convert", e);
            }
        } else if (Locale.class == toType) {
            String[] tokens = value.split("_");
            if (tokens.length == 1) {
                return new Locale(tokens[0]);
            } else if (tokens.length == 2) {
                return new Locale(tokens[0], tokens[1]);
            } else if (tokens.length == 3) {
                return new Locale(tokens[0], tokens[1], tokens[2]);
            } else {
                throw new Exception("Invalid locale string:" + value);
            }
        } else if (Pattern.class == toType) {
            return Pattern.compile(value);
        } else if (Properties.class == toType) {
            Properties props = new Properties();
            ByteArrayInputStream in = new ByteArrayInputStream(value.getBytes("UTF8"));
            props.load(in);
            return props;
        } else if (Boolean.class == toType) {
            if ("yes".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value)) {
                return Boolean.TRUE;
            } else if ("no".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value) || "off".equalsIgnoreCase(value)) {
                return Boolean.FALSE;
            } else {
                throw new RuntimeException("Invalid boolean value: " + value);
            }
        } else if (Integer.class == toType) {
            return Integer.valueOf(value);
        } else if (Short.class == toType) {
            return Short.valueOf(value);
        } else if (Long.class == toType) {
            return Long.valueOf(value);
        } else if (Float.class == toType) {
            return Float.valueOf(value);
        } else if (Double.class == toType) {
            return Double.valueOf(value);
        } else if (Character.class == toType) {
            if (value.length() == 6 && value.startsWith("\\u")) {
                int code = Integer.parseInt(value.substring(2), 16);
                return (char) code;
            } else if (value.length() == 1) {
                return value.charAt(0);
            } else {
                throw new Exception("Invalid value for character type: " + value);
            }
        } else if (Byte.class == toType) {
            return Byte.valueOf(value);
        } else if (Enum.class.isAssignableFrom(toType)) {
            return Enum.valueOf((Class<Enum>) toType, value);
        } else {
            return createObject(value, toType);
        }
    }

    private static Object createObject(String value, Class type) throws Exception {
        if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            throw new Exception("Unable to convert value " + value + " to type " + type + ". Type " + type + " is an interface or an abstract class");
        }
        Constructor constructor = null;
        try {
            constructor = type.getConstructor(String.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to convert to " + type);
        }
        try {
            return constructor.newInstance(value);
        } catch (Exception e) {
            throw new Exception("Unable to convert ", getRealCause(e));
        }
    }

    private static Throwable getRealCause(Throwable t) {
        if (t instanceof InvocationTargetException && t.getCause() != null) {
            return t.getCause();
        }
        return t;
    }

    private Object convertToCollection(Object obj, ReifiedType type) throws Exception {
        ReifiedType valueType = type.getActualTypeArgument(0);
        Collection newCol = (Collection) getCollection(toClass(type)).newInstance();
        if (obj.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(obj); i++) {
                try {
                    newCol.add(convert(Array.get(obj, i), valueType));
                } catch (Exception t) {
                    throw new Exception("Unable to convert from " + obj + " to " + type + "(error converting array element)", t);
                }
            }
        } else {
            for (Object item : (Collection) obj) {
                try {
                    newCol.add(convert(item, valueType));
                } catch (Exception t) {
                    throw new Exception("Unable to convert from " + obj + " to " + type + "(error converting collection entry)", t);
                }
            }
        }
        return newCol;
    }

    private Object convertToDictionary(Object obj, ReifiedType type) throws Exception {
        ReifiedType keyType = type.getActualTypeArgument(0);
        ReifiedType valueType = type.getActualTypeArgument(1);
        Dictionary newDic = new Hashtable();
        if (obj instanceof Dictionary) {
            Dictionary dic = (Dictionary) obj;
            for (Enumeration keyEnum = dic.keys(); keyEnum.hasMoreElements(); ) {
                Object key = keyEnum.nextElement();
                try {
                    newDic.put(convert(key, keyType), convert(dic.get(key), valueType));
                } catch (Exception t) {
                    throw new Exception("Unable to convert from " + obj + " to " + type + "(error converting map entry)", t);
                }
            }
        } else {
            for (Map.Entry e : ((Map<Object, Object>) obj).entrySet()) {
                try {
                    newDic.put(convert(e.getKey(), keyType), convert(e.getValue(), valueType));
                } catch (Exception t) {
                    throw new Exception("Unable to convert from " + obj + " to " + type + "(error converting map entry)", t);
                }
            }
        }
        return newDic;
    }

    private Object convertToMap(Object obj, ReifiedType type) throws Exception {
        ReifiedType keyType = type.getActualTypeArgument(0);
        ReifiedType valueType = type.getActualTypeArgument(1);
        Map newMap = (Map) getMap(toClass(type)).newInstance();
        if (obj instanceof Dictionary) {
            Dictionary dic = (Dictionary) obj;
            for (Enumeration keyEnum = dic.keys(); keyEnum.hasMoreElements(); ) {
                Object key = keyEnum.nextElement();
                try {
                    newMap.put(convert(key, keyType), convert(dic.get(key), valueType));
                } catch (Exception t) {
                    throw new Exception("Unable to convert from " + obj + " to " + type + "(error converting map entry)", t);
                }
            }
        } else {
            for (Map.Entry e : ((Map<Object, Object>) obj).entrySet()) {
                try {
                    newMap.put(convert(e.getKey(), keyType), convert(e.getValue(), valueType));
                } catch (Exception t) {
                    throw new Exception("Unable to convert from " + obj + " to " + type + "(error converting map entry)", t);
                }
            }
        }
        return newMap;
    }

    private Object convertToArray(Object obj, ReifiedType type) throws Exception {
        if (obj instanceof Collection) {
            obj = ((Collection) obj).toArray();
        }
        if (!obj.getClass().isArray()) {
            throw new Exception("Unable to convert from " + obj + " to " + type);
        }
        ReifiedType componentType;
        if (type.size() > 0) {
            componentType = type.getActualTypeArgument(0);
        } else {
            componentType = new GenericType(type.getRawClass().getComponentType());
        }
        Object array = Array.newInstance(toClass(componentType), Array.getLength(obj));
        for (int i = 0; i < Array.getLength(obj); i++) {
            try {
                Array.set(array, i, convert(Array.get(obj, i), componentType));
            } catch (Exception t) {
                throw new Exception("Unable to convert from " + obj + " to " + type + "(error converting array element)", t);
            }
        }
        return array;
    }

    public static boolean isAssignable(Object source, ReifiedType target) {
        return source == null
                || (target.size() == 0
                && unwrap(target.getRawClass()).isAssignableFrom(unwrap(source.getClass())));
    }

    private static Class unwrap(Class c) {
        Class u = primitives.get(c);
        return u != null ? u : c;
    }

    private static Class getMap(Class type) {
        if (hasDefaultConstructor(type)) {
            return type;
        } else if (SortedMap.class.isAssignableFrom(type)) {
            return TreeMap.class;
        } else if (ConcurrentMap.class.isAssignableFrom(type)) {
            return ConcurrentHashMap.class;
        } else {
            return LinkedHashMap.class;
        }
    }

    private static Class getCollection(Class type) {
        if (hasDefaultConstructor(type)) {
            return type;
        } else if (SortedSet.class.isAssignableFrom(type)) {
            return TreeSet.class;
        } else if (Set.class.isAssignableFrom(type)) {
            return LinkedHashSet.class;
        } else if (List.class.isAssignableFrom(type)) {
            return ArrayList.class;
        } else if (Queue.class.isAssignableFrom(type)) {
            return LinkedList.class;
        } else {
            return ArrayList.class;
        }
    }

    private static boolean hasDefaultConstructor(Class type) {
        if (!Modifier.isPublic(type.getModifiers())) {
            return false;
        }
        if (Modifier.isAbstract(type.getModifiers())) {
            return false;
        }
        Constructor[] constructors = type.getConstructors();
        for (Constructor constructor : constructors) {
            if (Modifier.isPublic(constructor.getModifiers()) &&
                    constructor.getParameterTypes().length == 0) {
                return true;
            }
        }
        return false;
    }

    private static final Map<Class, Class> primitives;

    static {
        primitives = new HashMap<Class, Class>();
        primitives.put(byte.class, Byte.class);
        primitives.put(short.class, Short.class);
        primitives.put(char.class, Character.class);
        primitives.put(int.class, Integer.class);
        primitives.put(long.class, Long.class);
        primitives.put(float.class, Float.class);
        primitives.put(double.class, Double.class);
        primitives.put(boolean.class, Boolean.class);
    }

    private Class toClass(ReifiedType type) {
        return type.getRawClass();
    }

}
