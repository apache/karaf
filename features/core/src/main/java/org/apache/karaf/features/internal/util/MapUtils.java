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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toSet;

public final class MapUtils {

    private MapUtils() {
    }

    public static <S, T> Map<S, Set<T>> invert(Map<T, S> map) {
        Map<S, Set<T>> inverted = new HashMap<>(map.size());
        for (Map.Entry<T, S> entry : map.entrySet()) {
            addToMapSet(inverted, entry.getValue(), entry.getKey());
        }
        return inverted;
    }

    /**
     * Changes mapping from <code>S</code> -&gt; <code>Set&lt;T&gt;</code> to mapping
     * <code>S</code> -&gt; <code>Set&lt;U&gt;</code> using {@link Function} that can change <code>T</code> to
     * <code>U</code>.
     *
     * @param mapset
     * @param function
     * @param <S> A key that maps to set of values in input and result map
     * @param <T> A type of input set of values
     * @param <U> A type of result set of values
     * @return
     */
    public static <S, T, U> Map<S, Set<U>> apply(Map<S, Set<T>> mapset, Function<T, U> function) {
        Map<S, Set<U>> result = new HashMap<>(mapset.size());
        for (Map.Entry<S, Set<T>> entry : mapset.entrySet()) {
            result.put(entry.getKey(), apply(entry.getValue(), function));
        }
        return result;
    }

    public static <U, T> Set<U> apply(Set<T> set, Function<T, U> function) {
        Set<U> result = new HashSet<>(set.size());
        for (T t : set) {
            U u = function.apply(t);
            if (u != null) {
                result.add(u);
            }
        }
        return result;
    }

    public static <S, T, U> Map<T, U> build(Collection<S> col, Function<S, T> key, Function<S, U> value) {
        Map<T, U> result = new HashMap<>(col.size());
        for (S s : col) {
            T t = key.apply(s);
            U u = value.apply(s);
            if (t != null && u != null) {
                result.put(t, u);
            }
        }
        return result;
    }

    public static <S, T, U> Function<S, U> compose(final Function<S, T> f1, final Function<T, U> f2) {
        return s -> f2.apply(f1.apply(s));
    }

    public static <T, U> Function<T, U> map(final Map<T, U> map) {
        return map::get;
    }

    public static <S, T> boolean contains(Map<S, Set<T>> mapset, S key, T val) {
        Set<T> set = mapset.get(key);
        return set != null && set.contains(val);
    }

    public static <S, T> Set<T> flatten(Map<S, Set<T>> mapset) {
        Set<T> set = new HashSet<>();
        for (Set<T> s : mapset.values()) {
            set.addAll(s);
        }
        return set;
    }

    /**
     * Produces a map where each set value in <code>from</code> map has every element that's in <code>to</code>
     * map's set value removed. If <code>from</code> map is left with empty set value, entire set is removed.
     * @param from
     * @param to
     * @param <S>
     * @param <T>
     * @return
     */
    public static <S, T> Map<S, Set<T>> diff(Map<S, Set<T>> from, Map<S, Set<T>> to) {
        Map<S, Set<T>> diff = copyMapSet(from);
        remove(diff, to);
        return diff;
    }

    public static <S, T> void add(Map<S, Set<T>> from, Map<S, Set<T>> toAdd) {
        for (Map.Entry<S, Set<T>> entry : toAdd.entrySet()) {
            from.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).addAll(entry.getValue());
        }
    }

    public static <S, T> void retain(Map<S, Set<T>> from, Map<S, Set<T>> toRetain) {
        for (Iterator<Map.Entry<S, Set<T>>> iterator = from.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<S, Set<T>> entry = iterator.next();
            Set<T> s = toRetain.get(entry.getKey());
            if (s != null) {
                entry.getValue().retainAll(s);
            } else {
                iterator.remove();
            }
        }
    }

    /**
     * Removes all values from <code>toRemove</code> map from <code>from</code> map. After removal, set values
     * in <code>from</code> map may be smaller or removed entirely (if there are no more values in given set).
     * @param from
     * @param toRemove
     * @param <S>
     * @param <T>
     */
    public static <S, T> void remove(Map<S, Set<T>> from, Map<S, Set<T>> toRemove) {
        for (Map.Entry<S, Set<T>> entry : toRemove.entrySet()) {
            Set<T> s = from.get(entry.getKey());
            if (s != null) {
                s.removeAll(entry.getValue());
                if (s.isEmpty()) {
                    from.remove(entry.getKey());
                }
            }
        }
    }

    @SuppressWarnings({
     "unchecked", "rawtypes"
    })
    public static <S> S copy(S obj) {
        if (obj instanceof List) {
            List r = new ArrayList();
            for (Object o : (List) obj) {
                r.add(copy(o));
            }
            return (S) r;
        } else if (obj instanceof Set) {
            Set r = new HashSet();
            for (Object o : (Set) obj) {
                r.add(copy(o));
            }
            return (S) r;
        } else if (obj instanceof Map) {
            Map r = new HashMap();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) obj).entrySet()) {
                r.put(copy(e.getKey()), copy(e.getValue()));
            }
            return (S) r;
        }
        return obj;
    }

    @SuppressWarnings({
     "rawtypes", "unchecked"
    })
    public static <S> void copy(S s1, S s2) {
        if (s1 instanceof Collection) {
            for (Object o : (Collection) s1) {
                ((Collection) s2).add(copy(o));
            }
        } else if (s1 instanceof Map) {
            for (Map.Entry<?, ?> e : ((Map<?, ?>) s1).entrySet()) {
                ((Map) s2).put(copy(e.getKey()), copy(e.getValue()));
            }
        } else {
            throw new IllegalArgumentException("Source is not a Collection or a Map");
        }
    }

    public static <S, T> Map<S, Set<T>> copyMapSet(Map<S, Set<T>> from) {
        Map<S, Set<T>> to = new HashMap<>();
        copyMapSet(from, to);
        return to;
    }

    public static <S, T> void copyMapSet(Map<S, Set<T>> from, Map<S, Set<T>> to) {
        for (Map.Entry<S, Set<T>> entry : from.entrySet()) {
            to.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
    }

    public static <S, T> void addToMapSet(Map<S, Set<T>> map, S key, T value) {
        map.computeIfAbsent(key, k -> new HashSet<>()).add(value);
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

    public static <T> Set<T> diff(Set<T> s1, Set<T> s2) {
        Set<T> s = new HashSet<>(s1);
        s.removeAll(s2);
        return s;
    }

    public static <S, T> Set<T> map(Set<S> s, Function<S, T> mapper) {
        return s.stream().map(mapper).collect(toSet());
    }

    public static <S> Set<S> filter(Set<S> s, Predicate<S> predicate) {
        return s.stream().filter(predicate).collect(toSet());
    }

}
