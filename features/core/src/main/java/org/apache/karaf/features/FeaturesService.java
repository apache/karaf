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
package org.apache.karaf.features;

import java.net.URI;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.osgi.namespace.service.ServiceNamespace;

/**
 * The service managing features repositories.
 */
public interface FeaturesService {

    String ROOT_REGION = "root";

    SnapshotUpdateBehavior DEFAULT_UPDATE_SNAPSHOTS = SnapshotUpdateBehavior.Crc;

    String DEFAULT_FEATURE_RESOLUTION_RANGE = "${range;[====,====]}";
    String DEFAULT_BUNDLE_UPDATE_RANGE = "${range;[==,=+)}";

    String UPDATEABLE_URIS = "mvn:.*SNAPSHOT|(?!mvn:).*";

    int DEFAULT_DOWNLOAD_THREADS = 8;
    long DEFAULT_SCHEDULE_DELAY = 250;
    int DEFAULT_SCHEDULE_MAX_RUN = 9;
    long DEFAULT_REPOSITORY_EXPIRATION = 60000; // 1 minute
    boolean DEFAULT_AUTO_REFRESH = true;

    boolean DEFAULT_CONFIG_CFG_STORE = true;
    boolean DEFAULT_DIGRAPH_MBEAN = true;

    enum Option {
        NoFailOnFeatureNotFound,
        NoAutoRefreshManagedBundles,
        NoAutoRefreshUnmanagedBundles,
        NoAutoRefreshBundles,
        NoAutoStartBundles,
        NoAutoManageBundles,
        Simulate,
        Verbose,
        Upgrade,
        DisplayFeaturesWiring,
        DisplayAllWiring,
        DeleteConfigurations
    }

    /**
     * Configuration options for handling requirements from {@link ServiceNamespace#SERVICE_NAMESPACE} namespace
     */
    enum ServiceRequirementsBehavior {
        /** Remove and do not consider any {@link ServiceNamespace#SERVICE_NAMESPACE} requirements */
        Disable("disable"),
        /** Consider {@link ServiceNamespace#SERVICE_NAMESPACE} requirements only for <code>http://karaf.apache.org/xmlns/features/v1.2.1</code> XSD and below */
        Default("default"),
        /** Always consider {@link ServiceNamespace#SERVICE_NAMESPACE} requirements */
        Enforce("enforce");

        private String value;

        ServiceRequirementsBehavior(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static ServiceRequirementsBehavior fromString(String serviceRequirements) {
            return Arrays.stream(values()).filter(sub -> sub.value.equalsIgnoreCase(serviceRequirements)).findFirst().orElse(Default);
        }
    }

    /**
     * Configuration options for checking whether update'able bundle should really be updated
     */
    enum SnapshotUpdateBehavior {
        /** Never update */
        None("none"),
        /** Update if CRC differs */
        Crc("crc"),
        /** Always update */
        Always("always");

        private String value;

        SnapshotUpdateBehavior(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static SnapshotUpdateBehavior fromString(String updateSnapshots) {
            return Arrays.stream(values()).filter(sub -> sub.value.equals(updateSnapshots)).findFirst().orElse(Crc);
        }
    }

    /**
     * Validate repository contents.
     *
     * @param uri Repository uri.
     * @throws Exception When validation fails.
     */
    void validateRepository(URI uri) throws Exception;

    boolean isRepositoryUriBlacklisted(URI uri);

    Feature[] repositoryProvidedFeatures(URI uri) throws Exception;

    void addRepository(URI uri) throws Exception;

    void addRepository(URI uri, boolean install) throws Exception;

    void removeRepository(URI uri) throws Exception;

    void removeRepository(URI uri, boolean uninstall) throws Exception;

    void restoreRepository(URI uri) throws Exception;

    Repository[] listRequiredRepositories() throws Exception;

    Repository[] listRepositories() throws Exception;

    Repository getRepository(String repoName) throws Exception;

    Repository getRepository(URI uri) throws Exception;

    String getRepositoryName(URI uri) throws Exception;

    void setResolutionOutputFile(String outputFile);

    void installFeature(String name) throws Exception;

    void installFeature(String name, EnumSet<Option> options) throws Exception;

    void installFeature(String name, String version) throws Exception;

    void installFeature(String name, String version, EnumSet<Option> options) throws Exception;

    void installFeature(Feature f, EnumSet<Option> options) throws Exception;

    void installFeatures(Set<String> features, EnumSet<Option> options) throws Exception;

    void installFeatures(Set<String> features, String region, EnumSet<Option> options) throws Exception;

    void addRequirements(Map<String, Set<String>> requirements, EnumSet<Option> options) throws Exception;

    void uninstallFeature(String name, EnumSet<Option> options) throws Exception;

    void uninstallFeature(String name) throws Exception;

    void uninstallFeature(String name, String version, EnumSet<Option> options) throws Exception;

    void uninstallFeature(String name, String version) throws Exception;

    void uninstallFeatures(Set<String> features, EnumSet<Option> options) throws Exception;

    void uninstallFeatures(Set<String> features, String region, EnumSet<Option> options) throws Exception;

    void removeRequirements(Map<String, Set<String>> requirements, EnumSet<Option> options) throws Exception;

    void updateFeaturesState(Map<String, Map<String, FeatureState>> stateChanges, EnumSet<Option> options) throws Exception;

    void updateReposAndRequirements(Set<URI> repos,
                                    Map<String, Set<String>> requirements,
                                    EnumSet<Option> options) throws Exception;

    Repository createRepository(URI uri) throws Exception;

    Feature[] listFeatures() throws Exception;

    Feature[] listRequiredFeatures() throws Exception;

    Feature[] listInstalledFeatures() throws Exception;

    Map<String, Set<String>> listRequirements();

    boolean isRequired(Feature f);

    boolean isInstalled(Feature f);

    Feature getFeature(String name, String version) throws Exception;

    Feature getFeature(String name) throws Exception;

    Feature[] getFeatures(String name, String version) throws Exception;

    Feature[] getFeatures(String name) throws Exception;

    void refreshRepositories(Set<URI> uris) throws Exception;

    void refreshRepository(URI uri) throws Exception;

    URI getRepositoryUriFor(String name, String version);

    String[] getRepositoryNames();

    void registerListener(FeaturesListener listener);

    void unregisterListener(FeaturesListener listener);

    void registerListener(DeploymentListener listener);

    void unregisterListener(DeploymentListener listener);

    FeatureState getState(String featureId);

    String getFeatureXml(Feature feature);

    void refreshFeatures(EnumSet<Option> options) throws Exception;

}
