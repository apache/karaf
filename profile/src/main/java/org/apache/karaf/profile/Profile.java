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
     * Get the configuration file names that are available on this profile
     */
    Set<String> getConfigurationFileNames();

    /**
     * Get all file configurations
     */
    Map<String, byte[]> getFileConfigurations();

    /**
     * Get the configuration file for the given name
     */
    byte[] getFileConfiguration(String fileName);

    /**
     * Get all configuration properties
     */
    Map<String, Map<String, String>> getConfigurations();

    /**
     * Get the configuration properties for the given PID
     * @return an empty map if the there is no configuration for the given pid
     */
    Map<String, String> getConfiguration(String pid);

    /**
     * Indicate if this profile is an overlay or not.
     */
    boolean isOverlay();

    /**
     * Returns true if this profile is Abstract. 
     * Abstract profiles should not be provisioned by default, they are intended to be inherited
     */
    boolean isAbstract();

    /**
     * Returns true if this profile is hidden.  
     * Hidden profiles are not listed by default.
     */
    boolean isHidden();

}
