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

/**
 * The immutable view of a profile
 */
public interface Profile {

    /**
     * The attribute key for the list of parents
     */
    String PARENTS = "parents";

    /**
     * The attribute key for the description of the profile
     */
    String DESCRIPTION = "description";

    /**
     * The attribute key for the abstract flag
     */
    String ABSTRACT = "abstract";

    /**
     * The attribute key for the hidden flag
     */
    String HIDDEN = "hidden";

    /**
     * Key indicating a deletion.
     * This value can appear as the value of a key in a configuration
     * or as a key itself.  If used as a key, the whole configuration
     * is flagged has been deleted from its parent when computing the
     * overlay.
     */
    String DELETED = "#deleted#";

    /**
     * The pid of the configuration holding internal profile attributes
     */
    String INTERNAL_PID = "profile";

    /**
     * The file suffix for a configuration
     */
    String PROPERTIES_SUFFIX = ".cfg";

    /**
     * The attribute prefix for in the agent configuration
     */
    String ATTRIBUTE_PREFIX = "attribute.";

    /**
     * The config prefix for in the agent configuration
     */
    String CONFIG_PREFIX = "config.";

    /**
     * The config prefix for in the agent configuration
     */
    String SYSTEM_PREFIX = "system.";

    Map<String, String> getAttributes();
    Map<String, String> getConfig();
    Map<String, String> getSystem();

    List<String> getParentIds();

    List<String> getLibraries();
    List<String> getBundles();
    List<String> getFeatures();
    List<String> getRepositories();
    List<String> getOverrides();
    List<String> getOptionals();

    String getId();

    /**
     * Get the configuration file names that are available on this profile.
     *
     * @return The configuration file names in the profile.
     */
    Set<String> getConfigurationFileNames();

    /**
     * Get all file configurations.
     *
     * @return The file configurations in the profile.
     */
    Map<String, byte[]> getFileConfigurations();

    /**
     * Get the configuration file for the given name.
     *
     * @param fileName The file configuration name to look for in the profile.
     * @return The file configuration in the profile.
     */
    byte[] getFileConfiguration(String fileName);

    /**
     * Get all configuration properties.
     *
     * @return The configurations in the profile.
     */
    Map<String, Map<String, Object>> getConfigurations();

    /**
     * Get the configuration properties for the given PID.
     *
     * @param pid The configuration PID to look for.
     * @return An empty map if the there is no configuration for the given pid.
     */
    Map<String, Object> getConfiguration(String pid);

    /**
     * Indicate if this profile is an overlay or not.
     *
     * @return True if the profile is an overlay, false else.
     */
    boolean isOverlay();

    /**
     * Return true if this profile is Abstract.
     * Abstract profiles should not be provisioned by default, they are intended to be inherited.
     *
     * @return True if the profile is abstract, false else.
     */
    boolean isAbstract();

    /**
     * Return true if this profile is hidden.
     * Hidden profiles are not listed by default.
     *
     * @return True if the profile is hidden, false else.
     */
    boolean isHidden();

}
