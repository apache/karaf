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
package org.apache.karaf.profile;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.profile.impl.ProfileBuilderImpl;

/**
 * A profile builder.
 */
public interface ProfileBuilder {

    ProfileBuilder addAttribute(String key, String value);

    ProfileBuilder setAttributes(Map<String, String> attributes);

    ProfileBuilder from(Profile profile);
    
    ProfileBuilder identity(String profileId);

    List<String> getParents();
    
    ProfileBuilder addParent(String parentId);

    ProfileBuilder addParents(List<String> parentIds);

    ProfileBuilder setParents(List<String> parentIds);
    
    ProfileBuilder removeParent(String parentId);

    Set<String> getConfigurationKeys();

    /**
     * Return a copy of the configuration with the specified pid
     * or an empty map if it does not exist yet.
     * The copy should be used for updates and then used with
     * {@link #addConfiguration(String, java.util.Map)} to keep
     * the layout and comments.
     *
     * @param pid The configuration PID.
     * @return The copy of the configuration with the given PID.
     */
    Map<String, Object> getConfiguration(String pid);
    
    ProfileBuilder addConfiguration(String pid, Map<String, Object> config);

    ProfileBuilder addConfiguration(String pid, String key, Object value);

    ProfileBuilder setConfigurations(Map<String, Map<String, Object>> configs);

    ProfileBuilder deleteConfiguration(String pid);

    Set<String> getFileConfigurationKeys();
    
    byte[] getFileConfiguration(String key);
    
    ProfileBuilder addFileConfiguration(String fileName, byte[] data);
    
    ProfileBuilder setFileConfigurations(Map<String, byte[]> configs);

    ProfileBuilder deleteFileConfiguration(String fileName);
    
    ProfileBuilder setBundles(List<String> values);

    ProfileBuilder addBundle(String value);

    ProfileBuilder setFeatures(List<String> values);

    ProfileBuilder addFeature(String value);

    ProfileBuilder setRepositories(List<String> values);

    ProfileBuilder addRepository(String value);

    ProfileBuilder setBlacklistedBundles(List<String> values);

    ProfileBuilder addBlacklistedBundle(String value);

    ProfileBuilder setBlacklistedFeatures(List<String> values);

    ProfileBuilder addBlacklistedFeature(String value);

    ProfileBuilder setBlacklistedRepositories(List<String> values);

    ProfileBuilder addBlacklistedRepository(String value);

    ProfileBuilder setOverrides(List<String> values);
    
    ProfileBuilder setOptionals(List<String> values);
    
    ProfileBuilder setOverlay(boolean overlay);
    
    Profile getProfile();

    final class Factory {

        public static ProfileBuilder create() {
            return new ProfileBuilderImpl();
        }

        public static ProfileBuilder create(String profileId) {
            return new ProfileBuilderImpl().identity(profileId);
        }

        public static ProfileBuilder createFrom(Profile profile) {
            return new ProfileBuilderImpl().from(profile);
        }

        // Hide ctor
        private Factory() {
        }
    }
}
