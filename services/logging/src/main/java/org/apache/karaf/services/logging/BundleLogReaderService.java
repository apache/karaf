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
package org.apache.karaf.services.logging;

import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;

/**
 * {@link LogReaderService} returned to a bundle, tracking the listeners it adds so that they can be removed when the
 * bundle releases the service.
 */
public class BundleLogReaderService implements LogReaderService {

    private final LogReaderServiceImpl logReaderService;
    private final Set<LogListener> listeners = ConcurrentHashMap.newKeySet();

    public BundleLogReaderService(LogReaderServiceImpl logReaderService) {
        this.logReaderService = logReaderService;
    }

    @Override
    public void addLogListener(LogListener listener) {
        if (listener != null) {
            listeners.add(listener);
            logReaderService.addLogListener(listener);
        }
    }

    @Override
    public void removeLogListener(LogListener listener) {
        if (listener != null) {
            listeners.remove(listener);
            logReaderService.removeLogListener(listener);
        }
    }

    @Override
    public Enumeration<LogEntry> getLog() {
        return logReaderService.getLog();
    }

    public void close() {
        for (LogListener listener : listeners) {
            logReaderService.removeLogListener(listener);
        }
        listeners.clear();
    }
}
