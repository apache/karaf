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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.utils.properties.InterpolationHelper;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration for the Maven URL resolver.
 */
public class MavenConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(MavenConfiguration.class);

    public static final String PID = "org.apache.karaf.url.mvn";

    /**
     * Framework property telling to wait for the configuration from ConfigAdmin before providing the URL handlers.
     */
    public static final String PROP_REQUIRE_CONFIG_ADMIN_CONFIG = PID + ".requireConfigAdminConfig";

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

    /**
     * The default repositories of a Karaf instance, used when they are not configured, as in
     * <code>etc/org.apache.karaf.url.mvn.cfg</code>.
     */
    static final String KARAF_DEFAULT_REPOSITORIES =
            "${karaf.home.uri}${karaf.default.repository}@id=system.repository@snapshots, "
            + "${karaf.data.uri}kar@id=kar.repository@multi@snapshots, "
            + "${karaf.base.uri}${karaf.default.repository}@id=child.system.repository@snapshots";

    /**
     * The remote repositories used when they are not configured and the Maven settings have none, as in
     * <code>etc/org.apache.karaf.url.mvn.cfg</code>.
     */
    static final String DEFAULT_REPOSITORIES = "https://repo1.maven.org/maven2@id=central, "
            + "https://repository.apache.org/content/groups/snapshots-group@id=apache@snapshots@noreleases";

    private File localRepository;
    private List<String> defaultRepositories;
    private List<String> repositories;
    private String updatePolicy;
    private String checksumPolicy;
    private int connectionTimeout;
    private int readTimeout;
    private boolean certificateCheck;
    private int retryCount;
    private MavenSettings settings;

    private final String propertyPrefix;
    private final InterpolationHelper.SubstitutionCallback substitutionCallback;

    public MavenConfiguration(BundleContext context, Dictionary<String, ?> properties) {
        this(context, properties, PID);
    }

    /**
     * @param context used to look up the framework properties, <code>null</code> to only use the system properties
     * @param properties the configuration (can be <code>null</code>)
     * @param propertyPrefix the prefix of the keys to read from <code>properties</code>
     */
    public MavenConfiguration(BundleContext context, Dictionary<String, ?> properties, String propertyPrefix) {
        this.propertyPrefix = propertyPrefix;
        this.substitutionCallback = new InterpolationHelper.BundleContextSubstitutionCallback(context);

        // Maven settings (settings.xml)
        settings = loadSettings(getProperty(context, properties, PROP_SETTINGS));

        // Local repository: the configured one, the one of the Maven settings, or ~/.m2/repository
        String localRepo = getProperty(context, properties, PROP_LOCAL_REPOSITORY);
        if (localRepo == null || localRepo.isEmpty()) {
            localRepo = settings.localRepository;
        }
        if (localRepo != null && !localRepo.isEmpty()) {
            localRepository = resolveFile(localRepo);
        } else {
            localRepository = new File(System.getProperty("user.home"), ".m2/repository");
        }

        // Default repositories (local file-based repos), the ones of the Karaf instance if they are not configured
        String defaultRepos = getProperty(context, properties, PROP_DEFAULT_REPOSITORIES);
        if (defaultRepos == null && substitutionCallback.getValue("karaf.home.uri") != null) {
            defaultRepos = substitute(PROP_DEFAULT_REPOSITORIES, KARAF_DEFAULT_REPOSITORIES);
        }
        defaultRepositories = parseRepositories(defaultRepos);

        // Remote repositories: as Pax URL, the ones of the active profiles of the Maven settings when they are not
        // configured, and appended to the configured ones when these start with +
        String repos = getProperty(context, properties, PROP_REPOSITORIES);
        if (repos == null) {
            repositories = settings.repositories.isEmpty()
                    ? parseRepositories(DEFAULT_REPOSITORIES) : settings.repositories;
        } else if (repos.trim().startsWith("+")) {
            repositories = new ArrayList<>(parseRepositories(repos.trim().substring(1)));
            repositories.addAll(settings.repositories);
        } else {
            repositories = parseRepositories(repos);
        }

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

    /**
     * @return the value of the property, with the variables substituted
     */
    private String getProperty(BundleContext context, Dictionary<String, ?> properties, String key) {
        String fullKey = propertyPrefix + "." + key;
        // Check dictionary first
        if (properties != null) {
            Object value = properties.get(fullKey);
            if (value == null) {
                value = properties.get(key);
            }
            if (value != null) {
                return substitute(fullKey, value.toString());
            }
        }
        // Fall back to system/framework properties
        String value = substitutionCallback.getValue(fullKey);
        if (value != null) {
            return substitute(fullKey, value);
        }
        return null;
    }

    private String substitute(String key, String value) {
        return InterpolationHelper.substVars(value, key, null, null, substitutionCallback);
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

    private static List<String> parseRepositories(String repos) {
        if (repos == null || repos.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String repo : repos.split(",")) {
            String trimmed = repo.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private File resolveFile(String path) {
        File file = new File(path);
        if (!file.isAbsolute()) {
            String karafHome = substitutionCallback.getValue("karaf.home");
            if (karafHome != null) {
                file = new File(karafHome, path);
            }
        }
        return file;
    }

    private static MavenSettings loadSettings(String location) {
        File file = MavenSettings.locate(location);
        if (file == null) {
            return MavenSettings.EMPTY;
        }
        try {
            LOG.debug("Using the Maven settings {}", file);
            return MavenSettings.load(file);
        } catch (IOException e) {
            LOG.warn("Could not read the Maven settings {}: {}", file, e.getMessage());
            return MavenSettings.EMPTY;
        }
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

    /**
     * @return the Maven settings, empty if there are none
     */
    MavenSettings getSettings() {
        return settings;
    }

}
