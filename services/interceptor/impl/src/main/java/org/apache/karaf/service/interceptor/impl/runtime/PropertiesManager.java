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
package org.apache.karaf.service.interceptor.impl.runtime;

import java.util.Hashtable;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.osgi.framework.ServiceReference;

public class PropertiesManager {
    public Stream<String> unflattenStringValues(final Object it) {
        return String[].class.isInstance(it) ? Stream.of(String[].class.cast(it)) : Stream.of(String.class.cast(it));
    }

    public <T> Hashtable<String, Object> collectProperties(final ServiceReference<T> ref) {
        return Stream.of(ref.getPropertyKeys())
                .filter(it -> !ComponentProperties.INTERCEPTORS_PROPERTY.equals(it))
                .collect(Collector.of(Hashtable::new, (h, p) -> h.put(p, ref.getProperty(p)), (p1, p2) -> {
                    p1.putAll(p2);
                    return p1;
                }));
    }
}
