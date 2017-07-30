/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.log.core.internal;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;


/**
 * An array that only keeps the last N elements added
 */
public class CircularBuffer<T> {

    private T[] elements;
    private transient int start;
    private transient int end;
    private transient boolean full;
    private final int maxElements;
    private Class<?> type;

    public CircularBuffer(int size, Class<?> type) {
        if (size <= 0) {
            throw new IllegalArgumentException("The size must be greater than 0");
        }
        this.type = type;
        maxElements = size;
        clear();
    }

    private int size() {
        if (end == start) {
            return full ? maxElements : 0;
        } else if (end < start) {
            return maxElements - start + end;
        } else {
            return end - start;
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized void clear() {
        start = 0;
        end = 0;
        full = false;
        elements = (T[])Array.newInstance(type, maxElements);
    }

    public synchronized void add(T element) {
        if (null == element) {
             throw new NullPointerException("Attempted to add null object to buffer");
        }
        if (full) {
            increaseStart();
        }
        elements[end] = element;
        increaseEnd();
        
    }

    private void increaseStart() {
        start++;
        if (start >= maxElements) {
            start = 0;
        }
    }

    private void increaseEnd() {
        end++;
        if (end >= maxElements) {
            end = 0;
        }
        if (end == start) {
            full = true;
        }
    }

    public synchronized Iterable<T> getElements() {
        return getElements(size());
    }

    public synchronized Iterable<T> getElements(int nb) {
        int s = size();
        nb = Math.min(Math.max(0, nb), s);
        List<T> result = new ArrayList<T>();
        for (int i = 0; i < nb; i++) {
            result.add(elements[(i + s - nb + start) % maxElements]);
        }
        return result;
    }


}
