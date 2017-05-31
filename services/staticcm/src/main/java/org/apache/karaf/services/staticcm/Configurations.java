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
package org.apache.karaf.services.staticcm;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.utils.properties.InterpolationHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;

public class Configurations {

    public static List<Configuration> loadConfigurations(BundleContext context, File directory) throws IOException {
        Map<String, Map<String, String>> configs = new HashMap<>();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".cfg")) {
                    try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
                        final Properties p = new Properties();
                        in.mark(1);
                        boolean isXml = in.read() == '<';
                        in.reset();
                        if (isXml) {
                            p.loadFromXML(in);
                        } else {
                            p.load(in);
                        }
                        Map<String, String> strMap = new HashMap<>();
                        for (Object k : p.keySet()) {
                            strMap.put(k.toString(), p.getProperty(k.toString()));
                        }
                        configs.put(file.getName(), strMap);
                    }
                }
            }
        }
        return createConfigurations(context, configs);
    }

    public static List<Configuration> createConfigurations(BundleContext context, Map<String, Map<String, String>> configs) {
        List<Configuration> configurations = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> entry : configs.entrySet()) {
            String pid[] = parsePid(entry.getKey());
            Map<String, String> cfg = entry.getValue();
            InterpolationHelper.performSubstitution(cfg, context);
            cfg.put(Constants.SERVICE_PID, pid[0]);
            configurations.add(new StaticConfigurationImpl(pid[0], pid[1], new Hashtable<>(cfg)));
        }
        return configurations;
    }

    private static String[] parsePid(String path) {
        String pid = path.substring(0, path.lastIndexOf('.'));
        int n = pid.indexOf('-');
        if (n > 0) {
            String factoryPid = pid.substring(n + 1);
            pid = pid.substring(0, n);
            return new String[] { pid, factoryPid };
        } else {
            return new String[] { pid, null };
        }
    }

}
