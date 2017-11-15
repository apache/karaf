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
package org.apache.karaf.profile;

public interface ProfileConstants {

    /**
     * The attribute prefix for the profile configuration (<code>profile.cfg</code>)
     */
    String ATTRIBUTE_PREFIX = "attribute.";

    /**
     * The attribute key for whitespace-separated list of parent profile IDs
     */
    String PARENTS = ATTRIBUTE_PREFIX + "parents";

    /**
     * The attribute key for the description of the profile
     */
    String DESCRIPTION = "description";

    /**
     * The attribute key for the <em>abstract</em> flag
     */
    String ABSTRACT = "abstract";

    /**
     * The attribute key for the <em>hidden</em> flag
     */
    String HIDDEN = "hidden";

    /**
     * <p>Key indicating a deletion.</p>
     * <p>This value can appear as the value of a key in a configuration
     * or as a key itself.  If used as a key, the whole configuration
     * is flagged as deleted from its parent when computing the overlay.</p>
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
     * The prefix for attributes that are targeted for <code>${karaf.etc}/config.properties</code> file
     */
    String CONFIG_PREFIX = "config.";

    /**
     * The prefix for attributes that are targeted for <code>${karaf.etc}/system.properties</code> file
     */
    String SYSTEM_PREFIX = "system.";

    /**
     * The prefix for attributes that specify URIs of features XML files
     */
    String REPOSITORY_PREFIX = "repository.";

    /**
     * The prefix for attributes that specify feature names (<code>name[/version]</code>) to install/use
     */
    String FEATURE_PREFIX = "feature.";

    /**
     * The prefix for attributes that specify bundle URIs to install
     */
    String BUNDLE_PREFIX = "bundle.";

    /**
     * The prefix for attributes that specify additional libraries to add to <code>${karaf.home}/lib</code>.
     * These are native libraries only. JARs that should be available in app classpath should go to
     * <code>${karaf.home}/lib/boot</code> and use {@link #BOOT_PREFIX}.
     */
    String LIB_PREFIX = "library.";

    /**
     * The prefix for attributes that specify additional endorsed libraries to add to
     * <code>${karaf.home}/lib/endorsed</code>
     */
    String ENDORSED_PREFIX = "endorsed.";

    /**
     * The prefix for attributes that specify additional extension libraries to add to
     * <code>${karaf.home}/lib/ext</code>
     */
    String EXT_PREFIX = "ext.";

    /**
     * The prefix for attributes that specify additional endorsed libraries to add to
     * <code>${karaf.home}/lib/boot</code>
     */
    String BOOT_PREFIX = "boot.";

    /**
     * The prefix for attributes that specify bundle overrides
     * (see {@link org.apache.karaf.features.internal.service.Overrides}). In version 4.2 it's better to use
     * {@link org.apache.karaf.features.internal.service.FeaturesProcessor} configuration.
     */
    String OVERRIDE_PREFIX = "override.";

    /**
     * The prefix for attributes that specify optional resources
     */
    String OPTIONAL_PREFIX = "optional.";

}
