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
package org.apache.karaf.profile.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

import org.apache.karaf.features.FeaturePattern;
import org.apache.karaf.features.LocationPattern;
import org.apache.karaf.profile.Profile;

import static org.apache.karaf.profile.impl.Utils.assertNotNull;
import static org.apache.karaf.profile.impl.Utils.assertTrue;


/**
 * This immutable profile implementation.
 */
final class ProfileImpl implements Profile {

    private static final Pattern ALLOWED_PROFILE_NAMES_PATTERN = Pattern.compile("^[A-Za-z0-9]+[.A-Za-z0-9_-]*$");

    private final String profileId;
    private final Map<String, String> attributes;
    private final List<String> parents = new ArrayList<>();
    private final Map<String, byte[]> fileConfigurations = new HashMap<>();
    private final Map<String, Map<String, Object>> configurations = new HashMap<>();
    private final boolean isOverlay;
    private int hash;

    // Only the {@link ProfileBuilder} should construct this
    ProfileImpl(String profileId, List<String> parents, Map<String, byte[]> fileConfigs, boolean isOverlay) {

        assertNotNull(profileId, "profileId is null");
        assertNotNull(parents, "parents is null");
        assertNotNull(fileConfigs, "fileConfigs is null");
        assertTrue(ALLOWED_PROFILE_NAMES_PATTERN.matcher(profileId).matches(), "Profile id '" + profileId + "' is invalid. Profile id must be: upper-case or lower-case letters, numbers, and . _ or - characters");

        this.profileId = profileId;
        this.isOverlay = isOverlay;

        // Parents
        this.parents.addAll(parents);

        // File configurations and derived configurations
        for (Entry<String, byte[]> entry : fileConfigs.entrySet()) {
            String fileKey = entry.getKey();
            byte[] bytes = entry.getValue();
            fileConfigurations.put(fileKey, bytes);
            if (fileKey.endsWith(Profile.PROPERTIES_SUFFIX)) {
                String pid = fileKey.substring(0, fileKey.indexOf(Profile.PROPERTIES_SUFFIX));
                configurations.put(pid, Collections.unmodifiableMap(Utils.toProperties(bytes)));
            }
        }

        // Attributes are profile configuration properties with prefix "attribute." contained in "profile" PID
        attributes = getPrefixedMap(ATTRIBUTE_PREFIX);
    }

    @Override
    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    @Override
    public Map<String, String> getConfig() {
        return getPrefixedMap(CONFIG_PREFIX);
    }

    @Override
    public Map<String, String> getSystem() {
        return getPrefixedMap(SYSTEM_PREFIX);
    }

    @Override
    public String getId() {
        return profileId;
    }

    private Map<String, String> getPrefixedMap(String prefix) {
        Map<String, String> map = new HashMap<>();
        Map<String, Object> profileConfig = configurations.get(Profile.INTERNAL_PID);
        if (profileConfig != null) {
            int prefixLength = prefix.length();
            for (Entry<String, Object> entry : profileConfig.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith(prefix)) {
                    map.put(key.substring(prefixLength), entry.getValue().toString());
                }
            }
        }
        return map;
    }

    @Override
    public List<String> getParentIds() {
        return Collections.unmodifiableList(parents);
    }

    @Override
    public List<String> getBundles() {
        return getContainerConfigList(ConfigListType.BUNDLES);
    }

    @Override
    public List<String> getFeatures() {
        return getContainerConfigList(ConfigListType.FEATURES);
    }

    @Override
    public List<String> getRepositories() {
        return getContainerConfigList(ConfigListType.REPOSITORIES);
    }

    @Override
    public List<LocationPattern> getBlacklistedBundles() {
        return getContainerConfigList(ConfigListType.BLACKLISTED_BUNDLES).stream()
                .map(LocationPattern::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<FeaturePattern> getBlacklistedFeatures() {
        return getContainerConfigList(ConfigListType.BLACKLISTED_FEATURES).stream()
                .map(FeaturePattern::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<LocationPattern> getBlacklistedRepositories() {
        return getContainerConfigList(ConfigListType.BLACKLISTED_REPOSITORIES).stream()
                .map(LocationPattern::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getLibraries() {
        return getContainerConfigList(ConfigListType.LIBRARIES);
    }

    @Override
    public List<String> getBootLibraries() {
        return getContainerConfigList(ConfigListType.BOOT_LIBRARIES);
    }

    @Override
    public List<String> getEndorsedLibraries() {
        return getContainerConfigList(ConfigListType.ENDORSED_LIBRARIES);
    }

    @Override
    public List<String> getExtLibraries() {
        return getContainerConfigList(ConfigListType.EXT_LIBRARIES);
    }

    @Override
    public List<String> getOverrides() {
        return getContainerConfigList(ConfigListType.OVERRIDES);
    }

    @Override
    public List<String> getOptionals() {
        return getContainerConfigList(ConfigListType.OPTIONALS);
    }

    @Override
    public boolean isOverlay() {
        return isOverlay;
    }

    @Override
    public boolean isAbstract() {
        return parseBoolean(attributes.get(ABSTRACT));
    }

    @Override
    public boolean isHidden() {
        return parseBoolean(attributes.get(HIDDEN));
    }

    private Boolean parseBoolean(Object obj) {
        return obj instanceof Boolean ? (Boolean) obj : obj != null && Boolean.parseBoolean(obj.toString());
    }

    @Override
    public Set<String> getConfigurationFileNames() {
        return Collections.unmodifiableSet(fileConfigurations.keySet());
    }

    @Override
    public Map<String, byte[]> getFileConfigurations() {
        return Collections.unmodifiableMap(fileConfigurations);
    }

    @Override
    public byte[] getFileConfiguration(String fileName) {
        return fileConfigurations.get(fileName);
    }

    @Override
    public Map<String, Map<String, Object>> getConfigurations() {
        return Collections.unmodifiableMap(configurations);
    }

    @Override
    public Map<String, Object> getConfiguration(String pid) {
        Map<String, Object> config = configurations.get(pid);
        config = config != null ? config : Collections.emptyMap();
        return Collections.unmodifiableMap(config);
    }

    private List<String> getContainerConfigList(ConfigListType type) {
        Map<String, Object> containerProps = getConfiguration(Profile.INTERNAL_PID);
        List<String> rc = new ArrayList<>();
        String prefix = type.value + ".";
        for (Map.Entry<String, Object> e : containerProps.entrySet()) {
            if ((e.getKey()).startsWith(prefix)) {
                rc.add(e.getValue().toString());
            }
        }
        return rc;
    }

    @Override
    public int hashCode() {
        if (hash == 0) {
            CRC32 crc = new CRC32();
            crc.update(profileId.getBytes());
            List<String> keys = new ArrayList<>(fileConfigurations.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                crc.update(key.getBytes());
                crc.update(fileConfigurations.get(key));
            }
            hash = (int) crc.getValue();
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ProfileImpl)) return false;
        ProfileImpl other = (ProfileImpl) obj;

        // Equality based on identity
        return profileId.equals(other.profileId)
                && fileConfigurations.equals(other.fileConfigurations);
    }

    @Override
    public String toString() {
        return "Profile[id=" + profileId + ", attrs=" + getAttributes() + "]";
    }

    enum ConfigListType {
        BUNDLES("bundle"),
        BLACKLISTED_BUNDLES("blacklisted.bundle"),
        FEATURES("feature"),
        BLACKLISTED_FEATURES("blacklisted.feature"),
        LIBRARIES("library"),
        BOOT_LIBRARIES("boot"),
        ENDORSED_LIBRARIES("endorsed"),
        EXT_LIBRARIES("ext"),
        OPTIONALS("optional"),
        OVERRIDES("override"),
        REPOSITORIES("repository"),
        BLACKLISTED_REPOSITORIES("blacklisted.repository");

        private String value;

        ConfigListType(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }
    }

}
