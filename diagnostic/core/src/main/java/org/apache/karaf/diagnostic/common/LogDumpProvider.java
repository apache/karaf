/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.diagnostic.common;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.karaf.diagnostic.core.DumpDestination;
import org.apache.karaf.diagnostic.core.DumpProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Dump provider which copies log files from data/log directory to
 * destination.
 */
public class LogDumpProvider implements DumpProvider {

    private final BundleContext bundleContext;

    public LogDumpProvider(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * Attach log entries from directory.
     */
    @Override
    public void createDump(DumpDestination destination) throws Exception {
        // get the ConfigAdmin service
        var ref = bundleContext.getServiceReference(ConfigurationAdmin.class);
        if (ref == null) {
            return;
        }

        // get the PAX Logging configuration
        var configurationAdmin = bundleContext.getService(ref);
        try {
            var configuration = configurationAdmin.getConfiguration("org.ops4j.pax.logging", null);

            // get the ".file" Pax Logging properties
            for (var entry : FrameworkUtil.asMap(configuration.getProcessedProperties(null)).entrySet()) {
                var property = entry.getKey();
                if (property.endsWith(".fileName")) {
                    // it's a file appender, get the file location
                    var location = Path.of((String) entry.getValue());
                    if (Files.exists(location)) {
                        try (var os = destination.add("log/" + location.getFileName())) {
                            Files.copy(location, os);
                        }
                    }
                }
            }
        } finally {
            bundleContext.ungetService(ref);
        }
    }
}
