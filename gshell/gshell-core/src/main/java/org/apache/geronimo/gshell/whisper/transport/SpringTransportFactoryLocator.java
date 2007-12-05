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
package org.apache.geronimo.gshell.whisper.transport;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring based implementation of the TransportFactory locator
 */
public class SpringTransportFactoryLocator<T extends TransportFactory> implements TransportFactoryLocator<T> {

    private List<T> factories;
    private Map<String, T> factoryMap;

    public List<T> getFactories() {
        return factories;
    }

    public void setFactories(List<T> factories) {
        this.factories = factories;
        this.factoryMap = new HashMap<String, T>();
        for (T factory : factories) {
            factoryMap.put(factory.getScheme(), factory);
        }
    }

    public T locate(URI location) throws TransportException {
        assert location != null;

        String scheme = location.getScheme();

        if (scheme == null) {
            throw new InvalidLocationException(location);
        }

        T factory = factoryMap.get(scheme);
        if (factory == null) {
            throw new LookupException(scheme);
        }
        return factory;
    }
    
}
