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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.felix.utils.properties.TypedProperties;
import org.apache.karaf.profile.Profile;
import org.apache.karaf.profile.ProfileBuilder;

import static org.apache.karaf.profile.ProfileConstants.*;
import static org.apache.karaf.profile.impl.ProfileImpl.ConfigListType;

/**
 * The default {@link ProfileBuilder}
 */
public final class ProfileBuilderImpl implements ProfileBuilder {

	private String profileId;
	private Map<String, FileContent> fileMapping = new HashMap<>();
	private boolean isOverlay;
	
	@Override
	public ProfileBuilder from(Profile profile) {
		profileId = profile.getId();
		setFileConfigurations(profile.getFileConfigurations());
        return this;
	}

	@Override
	public ProfileBuilder identity(String profileId) {
		this.profileId = profileId;
		return this;
	}

	@Override
    public List<String> getParents() {
        Map<String, Object> config = getConfigurationInternal(Profile.INTERNAL_PID);
        String pspec = (String) config.get(PARENTS);
        String[] parentIds = pspec != null ? pspec.split(" ") : new String[0];
        return Arrays.asList(parentIds);
    }

    @Override
	public ProfileBuilder addParent(String parentId) {
        return addParentsInternal(Collections.singletonList(parentId), false);
	}

	@Override
	public ProfileBuilder addParents(List<String> parentIds) {
		return addParentsInternal(parentIds, false);
	}

    @Override
    public ProfileBuilder setParents(List<String> parentIds) {
        return addParentsInternal(parentIds, true);
    }

    private ProfileBuilder addParentsInternal(List<String> parentIds, boolean clear) {
        Set<String> currentIds = new LinkedHashSet<>(getParents());
        if (clear) {
            currentIds.clear();
        }
        if (parentIds != null) {
            currentIds.addAll(parentIds);
        }
        updateParentsAttribute(currentIds);
        return this;
    }
    
    @Override
	public ProfileBuilder removeParent(String profileId) {
        Set<String> currentIds = new LinkedHashSet<>(getParents());
        currentIds.remove(profileId);
        updateParentsAttribute(currentIds);
		return this;
	}

    private void updateParentsAttribute(Collection<String> parentIds) {
        Map<String, Object> config = getConfigurationInternal(Profile.INTERNAL_PID);
        config.remove(PARENTS);
        if (parentIds.size() > 0) {
            config.put(PARENTS, parentsAttributeValue(parentIds));
        }
        addConfiguration(Profile.INTERNAL_PID, config);
    }

    private String parentsAttributeValue(Collection<String> parentIds) {
	    return parentIds.isEmpty() ? "" : String.join(" ", parentIds);
    }
    
    @Override
    public Set<String> getFileConfigurationKeys() {
        return fileMapping.keySet();
    }

    @Override
    public byte[] getFileConfiguration(String key) {
        return fileMapping.get(key) == null ? null : fileMapping.get(key).bytes;
    }

    @Override
    public ProfileBuilder setFileConfigurations(Map<String, byte[]> configurations) {
        fileMapping = new HashMap<>();
        configurations.forEach((name, bytes) -> fileMapping.put(name, new FileContent(bytes, false)));
        return this;
    }

    @Override
    public ProfileBuilder addFileConfiguration(String fileName, byte[] data) {
        fileMapping.put(fileName, new FileContent(data, false));
        return this;
    }

    @Override
    public ProfileBuilder deleteFileConfiguration(String fileName) {
        fileMapping.remove(fileName);
        return this;
    }

	@Override
	public ProfileBuilder setConfigurations(Map<String, Map<String, Object>> configs) {
	    for (String pid : getConfigurationKeys()) {
	        deleteConfiguration(pid);
	    }
		for (Entry<String, Map<String, Object>> entry : configs.entrySet()) {
		    addConfiguration(entry.getKey(), new HashMap<>(entry.getValue()));
		}
		return this;
	}

    @Override
    public ProfileBuilder addConfiguration(String pid, Map<String, Object> config) {
        fileMapping.put(pid + Profile.PROPERTIES_SUFFIX, new FileContent(Utils.toBytes(config), true));
        return this;
    }

    @Override
    public ProfileBuilder addConfiguration(String pid, String key, Object value) {
        Map<String, Object> config = getConfigurationInternal(pid);
        config.put(key, value);
        return addConfiguration(pid, config);
    }

    @Override
    public Set<String> getConfigurationKeys() {
        Set<String> result = new HashSet<>();
        for (String fileKey : fileMapping.keySet()) {
            if (fileKey.endsWith(Profile.PROPERTIES_SUFFIX)) {
                String configKey = fileKey.substring(0, fileKey.indexOf(Profile.PROPERTIES_SUFFIX));
                result.add(configKey);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public Map<String, Object> getConfiguration(String pid) {
        return getConfigurationInternal(pid);
    }

    private Map<String, Object> getConfigurationInternal(String pid) {
        FileContent content = fileMapping.get(pid + Profile.PROPERTIES_SUFFIX);
        return Utils.toProperties(content == null ? null : content.bytes);
    }
    
    @Override
    public ProfileBuilder deleteConfiguration(String pid) {
        fileMapping.remove(pid + Profile.PROPERTIES_SUFFIX);
        return this;
    }
    
	@Override
	public ProfileBuilder setBundles(List<String> values) {
		addProfileConfiguration(ConfigListType.BUNDLES, values);
		return this;
	}

    @Override
    public ProfileBuilder addBundle(String value) {
        addProfileConfiguration(ConfigListType.BUNDLES, value);
        return this;
    }

    @Override
	public ProfileBuilder setFeatures(List<String> values) {
		addProfileConfiguration(ConfigListType.FEATURES, values);
		return this;
	}

    @Override
    public ProfileBuilder addFeature(String value) {
        addProfileConfiguration(ConfigListType.FEATURES, value);
        return this;
    }

    @Override
	public ProfileBuilder setRepositories(List<String> values) {
		addProfileConfiguration(ConfigListType.REPOSITORIES, values);
		return this;
	}

    @Override
    public ProfileBuilder addRepository(String value) {
        addProfileConfiguration(ConfigListType.REPOSITORIES, value);
        return this;
    }

    @Override
	public ProfileBuilder setOverrides(List<String> values) {
		addProfileConfiguration(ConfigListType.OVERRIDES, values);
		return this;
	}

    @Override
    public ProfileBuilder setOptionals(List<String> values) {
        addProfileConfiguration(ConfigListType.OPTIONALS, values);
        return this;
    }

    public ProfileBuilder setOverlay(boolean overlay) {
        this.isOverlay = overlay;
        addConfiguration(Profile.INTERNAL_PID, Profile.ATTRIBUTE_PREFIX + Profile.OVERLAY, Boolean.toString(overlay));
        return this;
    }

	@Override
    public ProfileBuilder addAttribute(String key, String value) {
        addConfiguration(Profile.INTERNAL_PID, Profile.ATTRIBUTE_PREFIX + key, value);
        return this;
    }

    @Override
    public ProfileBuilder setAttributes(Map<String, String> attributes) {
        Map<String, Object> config = getConfigurationInternal(Profile.INTERNAL_PID);
        for (String key : new ArrayList<>(config.keySet())) {
            if (key.startsWith(Profile.ATTRIBUTE_PREFIX)) {
                config.remove(key);
            }
        }
        for (Entry<String, String> entry : attributes.entrySet()) {
            config.put(Profile.ATTRIBUTE_PREFIX + entry.getKey(), entry.getValue());
        }
        addConfiguration(Profile.INTERNAL_PID, config);
        return null;
    }

    private void addProfileConfiguration(ConfigListType type, List<String> values) {
        String prefix = type + ".";
        Map<String, Object> config = getConfigurationInternal(Profile.INTERNAL_PID);
        for (String key : new ArrayList<>(config.keySet())) {
            if (key.startsWith(prefix)) {
                config.remove(key);
            }
        }
        for (String value : values) {
            config.put(prefix + value, value);
        }
        addConfiguration(Profile.INTERNAL_PID, config);
    }

    private void addProfileConfiguration(ConfigListType type, String value) {
        String prefix = type + ".";
        Map<String, Object> config = getConfigurationInternal(Profile.INTERNAL_PID);
        config.put(prefix + value, value);
        addConfiguration(Profile.INTERNAL_PID, config);
    }

    /**
     * Returns an immutable implementation of {@link Profile}
     * @return
     */
    @Override
    public Profile getProfile() {
        // reformatting all generated files.
        Map<String, byte[]> files = new LinkedHashMap<>();
        fileMapping.forEach((k, v) -> files.put(k, reformat(k, v)));
        return new ProfileImpl(profileId, getParents(), files, isOverlay);
    }

    /**
     * If some properties file has been marked as {@link FileContent#generated}, then we can add some comment hints.
     * @param name
     * @param fileContent
     * @return
     */
    private byte[] reformat(String name, FileContent fileContent) {
        if (!fileContent.generated || !(isOverlay && name.equals(INTERNAL_PID + PROPERTIES_SUFFIX))) {
            return fileContent.bytes;
        }

        TypedProperties properties = Utils.toProperties(fileContent.bytes);
        TypedProperties result = Utils.toProperties((byte[])null);

        String parents = null;
        Map<String, Object> attributes = new LinkedHashMap<>();
        Map<String, Object> repositories = new LinkedHashMap<>();
        Map<String, Object> features = new LinkedHashMap<>();
        Map<String, Object> bundles = new LinkedHashMap<>();
        Map<String, Object> libraries = new LinkedHashMap<>();
        Map<String, Object> bootLibraries = new LinkedHashMap<>();
        Map<String, Object> endorsedLibraries = new LinkedHashMap<>();
        Map<String, Object> extLibraries = new LinkedHashMap<>();
        Map<String, Object> config = new LinkedHashMap<>();
        Map<String, Object> system = new LinkedHashMap<>();
        Map<String, Object> overrides = new LinkedHashMap<>();
        Map<String, Object> optionals = new LinkedHashMap<>();
        for (String key : properties.keySet()) {
            Object v = properties.get(key);
            if (key.equals(PARENTS)) {
                parents = (String) v;
            } else if (key.startsWith(ATTRIBUTE_PREFIX)) {
                attributes.put(key, v);
            } else if (key.startsWith(REPOSITORY_PREFIX)) {
                repositories.put(key, v);
            } else if (key.startsWith(FEATURE_PREFIX)) {
                features.put(key, v);
            } else if (key.startsWith(BUNDLE_PREFIX)) {
                bundles.put(key, v);
            } else if (key.startsWith(LIB_PREFIX)) {
                libraries.put(key, v);
            } else if (key.startsWith(BOOT_PREFIX)) {
                bootLibraries.put(key, v);
            } else if (key.startsWith(ENDORSED_PREFIX)) {
                endorsedLibraries.put(key, v);
            } else if (key.startsWith(EXT_PREFIX)) {
                extLibraries.put(key, v);
            } else if (key.startsWith(CONFIG_PREFIX)) {
                config.put(key, v);
            } else if (key.startsWith(SYSTEM_PREFIX)) {
                system.put(key, v);
            } else if (key.startsWith(OVERRIDE_PREFIX)) {
                overrides.put(key, v);
            } else if (key.startsWith(OPTIONAL_PREFIX)) {
                optionals.put(key, v);
            }
        }

        result.setHeader(Arrays.asList("#", "# Profile generated by Karaf Assembly Builder", "#"));
        if (parents != null) {
            result.put(PARENTS, comment("Parent profiles"), parents);
        }
        addGroupOfProperties("Attributes", result, attributes);
        addGroupOfProperties("Feature XML repositories", result, repositories);
        addGroupOfProperties("Features", result, features);
        addGroupOfProperties("Bundles", result, bundles);
        addGroupOfProperties("Libraries", result, libraries);
        addGroupOfProperties("Boot libraries", result, bootLibraries);
        addGroupOfProperties("Endorsed libraries", result, endorsedLibraries);
        addGroupOfProperties("Extension libraries", result, extLibraries);
        addGroupOfProperties("Configuration properties for etc/config.properties", result, config);
        addGroupOfProperties("Configuration properties for etc/system.properties", result, system);
        addGroupOfProperties("Bundle overrides (deprecated)", result, overrides);
        addGroupOfProperties("Optional resources for resolution", result, optionals);

        return Utils.toBytes(result);
    }

    /**
     * Puts properties under single comment.
     * @param comment
     * @param properties
     * @param values
     */
    private void addGroupOfProperties(String comment, TypedProperties properties, Map<String, Object> values) {
        boolean first = true;
        for (Entry<String, Object> entry : values.entrySet()) {
            if (first) {
                first = false;
                properties.put(entry.getKey(), comment(comment), entry.getValue());
            } else {
                properties.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Helper method to generate comments above property groups in {@link TypedProperties}
     * @param comment
     * @return
     */
    private List<String> comment(String comment) {
        return Arrays.asList("", "# " + comment);
    }

    /**
     * We can distinguish between bytes read from external file and bytes from serialized
     * {@link org.apache.felix.utils.properties.TypedProperties}
     */
    static class FileContent {
        byte[] bytes;
        boolean generated;

        public FileContent(byte[] bytes, boolean generated) {
            this.bytes = bytes;
            this.generated = generated;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FileContent that = (FileContent) o;
            return Arrays.equals(bytes, that.bytes);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(bytes);
        }
    }

}
