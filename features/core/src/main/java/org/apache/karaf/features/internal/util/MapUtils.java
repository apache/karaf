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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MapUtils {

    public static <S, T> Map<S, Set<T>> invert(Map<T, S> map) {
        Map<S, Set<T>> inverted = new HashMap<S, Set<T>>();
        for (Map.Entry<T, S> entry : map.entrySet()) {
            addToMapSet(inverted, entry.getValue(), entry.getKey());
        }
        return inverted;
    }

    public static <S, T> Map<S, Set<T>> copyMapSet(Map<S, Set<T>> from) {
        Map<S, Set<T>> to = new HashMap<S, Set<T>>();
        copyMapSet(from, to);
        return to;
    }

    public static <S, T> void copyMapSet(Map<S, Set<T>> from, Map<S, Set<T>> to) {
        for (Map.Entry<S, Set<T>> entry : from.entrySet()) {
            to.put(entry.getKey(), new HashSet<T>(entry.getValue()));
        }
    }

    public static <S, T> void addToMapSet(Map<S, Set<T>> map, S key, T value) {
        Set<T> values = map.get(key);
        if (values == null) {
            values = new HashSet<T>();
            map.put(key, values);
        }
        values.add(value);
    }

    public static <S, T> void removeFromMapSet(Map<S, Set<T>> map, S key, T value) {
        Set<T> values = map.get(key);
        if (values != null) {
            values.remove(value);
            if (values.isEmpty()) {
                map.remove(key);
            }
        }
    }

}
