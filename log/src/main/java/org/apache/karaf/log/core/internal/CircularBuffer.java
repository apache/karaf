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

import org.ops4j.pax.logging.spi.PaxLoggingEvent;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;


/**
 * An array that only keeps the last N elements added.
 * <p>
 * It is likely that it writes way more than it reads (add vs getElements) since logs should be continuous appended
 * but their query should be quite rare so we want to optimize the append path.
 * <p>
 * Important: it can happen a small inconsistency between add() and getElements() but the fact getElements()
 * sorts the data makes it hurtless and it avoids to have a lock in this buffer which must keep a "0-overhead"
 * on the runtime.
 */
public class CircularBuffer {

    private final AtomicInteger currentIdx = new AtomicInteger(0);
    private final AtomicReferenceArray<PaxLoggingEvent> buffer;

    public CircularBuffer(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("The size must be greater than 0");
        }
        this.buffer = new AtomicReferenceArray<>(size);
    }

    public int maxSize() {
        return buffer.length();
    }

    public void add(final PaxLoggingEvent element) {
        if (null == element) {
            throw new NullPointerException("Attempted to add null object to buffer");
        }
        doAdd(element);
    }

    public List<PaxLoggingEvent> getElements(final int requestedCount) {
        final int max = Math.min(buffer.length(), requestedCount);
        final int from = (currentIdx.get() % buffer.length()) - max;
        return collectEvents(max, idx -> {
            int index = from + idx;
            while (index < 0) {
                index = index + buffer.length();
            }
            while (index >= buffer.length()) {
                index = index - buffer.length();
            }
            return buffer.get(index);
        });
    }

    private List<PaxLoggingEvent> collectEvents(final int max, final IntFunction<PaxLoggingEvent> mapper) {
        return IntStream.range(0, max)
                .mapToObj(mapper)
                .filter(Objects::nonNull) // not initialized yet
                .sorted(comparing(PaxLoggingEvent::getTimeStamp)) // not critical but better when dumped
                .collect(toList());
    }

    private void doAdd(final PaxLoggingEvent element) {
        final int idx = currentIdx.getAndUpdate(value -> {
            final int newValue = value + 1;
            if (newValue >= buffer.length()) {
                return 0;
            }
            return newValue;
        }) % buffer.length();
        buffer.set(idx, element);
    }
}
