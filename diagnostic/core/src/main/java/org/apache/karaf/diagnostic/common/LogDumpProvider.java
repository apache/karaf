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
import java.util.Map;

import org.apache.karaf.diagnostic.core.DumpDestination;
import org.apache.karaf.diagnostic.core.DumpProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Dump provider which copies log files from data/log directory to
 * destination.
 */
public class LogDumpProvider implements DumpProvider {

    private static final String PAX_LOGGING_PID = "org.ops4j.pax.logging";
    private static final String KARAF_LOGGING_PID = "org.apache.karaf.logging";

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

        var configurationAdmin = bundleContext.getService(ref);
        try {
            // get the PAX Logging configuration
            var paxLogging = getProperties(configurationAdmin, PAX_LOGGING_PID);
            if (paxLogging != null) {
                // get the ".fileName" Pax Logging properties
                for (var entry : paxLogging.entrySet()) {
                    var property = entry.getKey();
                    if (property.endsWith(".fileName")) {
                        // it's a file appender, get the file location
                        copy((String) entry.getValue(), destination);
                    }
                }
            } else {
                // no Pax Logging (Apache Karaf Light): get the Karaf logging service file
                var karafLogging = getProperties(configurationAdmin, KARAF_LOGGING_PID);
                var file = karafLogging != null ? (String) karafLogging.get("file") : null;
                if (file == null && System.getProperty("karaf.log") != null) {
                    file = System.getProperty("karaf.log") + "/karaf.log";
                }
                copy(file, destination);
            }
        } finally {
            bundleContext.ungetService(ref);
        }
    }

    private static Map<String, Object> getProperties(ConfigurationAdmin configurationAdmin, String pid) throws Exception {
        // listConfigurations() doesn't create the configuration if it doesn't exist (unlike getConfiguration())
        var configurations = configurationAdmin.listConfigurations("(" + Constants.SERVICE_PID + "=" + pid + ")");
        if (configurations == null || configurations.length == 0) {
            return null;
        }
        return FrameworkUtil.asMap(configurations[0].getProcessedProperties(null));
    }

    private static void copy(String file, DumpDestination destination) throws Exception {
        if (file == null) {
            return;
        }
        var location = Path.of(file);
        if (Files.exists(location)) {
            try (var os = destination.add("log/" + location.getFileName())) {
                Files.copy(location, os);
            }
        }
    }
}
