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
package org.apache.karaf.services.url.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;

import org.osgi.framework.BundleContext;

/**
 * Configuration for the Maven URL resolver.
 */
public class MavenConfiguration {

    public static final String PID = "org.apache.karaf.url.mvn";

    public static final String PROP_LOCAL_REPOSITORY = "localRepository";
    public static final String PROP_DEFAULT_REPOSITORIES = "defaultRepositories";
    public static final String PROP_REPOSITORIES = "repositories";
    public static final String PROP_UPDATE_POLICY = "globalUpdatePolicy";
    public static final String PROP_CHECKSUM_POLICY = "globalChecksumPolicy";
    public static final String PROP_SETTINGS = "settings";
    public static final String PROP_CONNECTION_TIMEOUT = "socket.connectionTimeout";
    public static final String PROP_READ_TIMEOUT = "socket.readTimeout";
    public static final String PROP_TIMEOUT = "timeout";
    public static final String PROP_CERTIFICATE_CHECK = "certificateCheck";
    public static final String PROP_RETRY_COUNT = "connection.retryCount";

    private File localRepository;
    private List<String> defaultRepositories;
    private List<String> repositories;
    private String updatePolicy;
    private String checksumPolicy;
    private int connectionTimeout;
    private int readTimeout;
    private boolean certificateCheck;
    private int retryCount;

    public MavenConfiguration(BundleContext context, Dictionary<String, ?> properties) {
        // Local repository
        String localRepo = getProperty(context, properties, PROP_LOCAL_REPOSITORY);
        if (localRepo != null && !localRepo.isEmpty()) {
            localRepository = resolveFile(context, localRepo);
        } else {
            localRepository = new File(System.getProperty("user.home"), ".m2/repository");
        }

        // Default repositories (local file-based repos)
        String defaultRepos = getProperty(context, properties, PROP_DEFAULT_REPOSITORIES);
        defaultRepositories = parseRepositories(context, defaultRepos);

        // Remote repositories
        String repos = getProperty(context, properties, PROP_REPOSITORIES);
        repositories = parseRepositories(context, repos);

        // Policies
        updatePolicy = getProperty(context, properties, PROP_UPDATE_POLICY);
        checksumPolicy = getProperty(context, properties, PROP_CHECKSUM_POLICY);

        // Timeouts
        int defaultTimeout = getIntProperty(context, properties, PROP_TIMEOUT, 5000);
        connectionTimeout = getIntProperty(context, properties, PROP_CONNECTION_TIMEOUT, defaultTimeout);
        readTimeout = getIntProperty(context, properties, PROP_READ_TIMEOUT, 30000);

        // SSL
        certificateCheck = getBooleanProperty(context, properties, PROP_CERTIFICATE_CHECK, true);

        // Retry
        retryCount = getIntProperty(context, properties, PROP_RETRY_COUNT, 3);
    }

    private String getProperty(BundleContext context, Dictionary<String, ?> properties, String key) {
        String fullKey = PID + "." + key;
        // Check dictionary first
        if (properties != null) {
            Object value = properties.get(fullKey);
            if (value == null) {
                value = properties.get(key);
            }
            if (value != null) {
                return substituteVars(context, value.toString());
            }
        }
        // Fall back to system/framework properties
        String value = context.getProperty(fullKey);
        if (value == null) {
            value = System.getProperty(fullKey);
        }
        if (value != null) {
            return substituteVars(context, value);
        }
        return null;
    }

    private int getIntProperty(BundleContext context, Dictionary<String, ?> properties, String key, int defaultValue) {
        String value = getProperty(context, properties, key);
        if (value != null && !value.isEmpty()) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return defaultValue;
    }

    private boolean getBooleanProperty(BundleContext context, Dictionary<String, ?> properties, String key, boolean defaultValue) {
        String value = getProperty(context, properties, key);
        if (value != null && !value.isEmpty()) {
            return Boolean.parseBoolean(value.trim());
        }
        return defaultValue;
    }

    private List<String> parseRepositories(BundleContext context, String repos) {
        if (repos == null || repos.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String repo : repos.split(",")) {
            String trimmed = repo.trim();
            if (!trimmed.isEmpty()) {
                trimmed = substituteVars(context, trimmed);
                result.add(trimmed);
            }
        }
        return result;
    }

    private File resolveFile(BundleContext context, String path) {
        path = substituteVars(context, path);
        File file = new File(path);
        if (!file.isAbsolute()) {
            String karafHome = context.getProperty("karaf.home");
            if (karafHome != null) {
                file = new File(karafHome, path);
            }
        }
        return file;
    }

    private String substituteVars(BundleContext context, String value) {
        if (value == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < value.length()) {
            if (value.charAt(i) == '$' && i + 1 < value.length() && value.charAt(i + 1) == '{') {
                int end = value.indexOf('}', i + 2);
                if (end > 0) {
                    String varName = value.substring(i + 2, end);
                    String varValue = context.getProperty(varName);
                    if (varValue == null) {
                        varValue = System.getProperty(varName);
                    }
                    if (varValue != null) {
                        result.append(varValue);
                    } else {
                        result.append(value, i, end + 1);
                    }
                    i = end + 1;
                } else {
                    result.append(value.charAt(i));
                    i++;
                }
            } else {
                result.append(value.charAt(i));
                i++;
            }
        }
        return result.toString();
    }

    public File getLocalRepository() {
        return localRepository;
    }

    public List<String> getDefaultRepositories() {
        return defaultRepositories;
    }

    public List<String> getRepositories() {
        return repositories;
    }

    public String getUpdatePolicy() {
        return updatePolicy;
    }

    public String getChecksumPolicy() {
        return checksumPolicy;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public boolean isCertificateCheck() {
        return certificateCheck;
    }

    public int getRetryCount() {
        return retryCount;
    }

}
