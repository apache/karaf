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
 * <p>A <em>profile</em> is a container for configuration that can be applied to Karaf distribution.</p>
 *
 * <p>Profiles may inherit from other (single or multiple) profiles. An <em>overlay</em> profile is single
 * profile with all the configurations, attributes and files from parent profiles, while configurations,
 * attributes and files from <em>child</em> profile overwrites corresponding data from parent profiles.</p>
 *
 * <p>Configuration include:<ul>
 *     <li>Attributes</li>
 *     <li>ConfigAdmin configurations (PIDs) to put into <code>${karaf.etc}</code> directory</li>
 *     <li>Other resources to put into <code>${karaf.etc}</code> directory</li>
 * </ul></p>
 *
 * <p>Attributes are properties in special file <code>profile.cfg</code> (<code>profile</code> PID) and may specify:<ul>
 *     <li>OSGi bundles to install (prefix: <code>bundle.</code>)</li>
 *     <li>Karaf features to install (prefix: <code>feature.</code>)</li>
 *     <li>Feature XML repositories to use to resolve bundles and features (prefix: <code>repository.</code>)</li>
 *     <li>Identifiers of parent profiles (property name: <code>attribute.parents</code>)</li>
 *     <li>Indication of abstract profile (property name: <code>abstract</code>)</li>
 *     <li>Indication of hidden profile (property name: <code>hidden</code>)</li>
 *     <li>Different attributes (prefix: <code>attribute.</code>)</li>
 *     <li>Properties to be added to <code>etc/config.properties</code> (prefix: <code>config.</code>)</li>
 *     <li>Properties to be added to <code>etc/system.properties</code> (prefix: <code>system.</code>)</li>
 *     <li>Additional libraries to be added to <code>lib</code> (prefix: <code>library.</code>)</li>
 *     <li>Additional libraries to be added to <code>lib/boot</code> (prefix: <code>boot.</code>)</li>
 *     <li>Additional libraries to be added to <code>lib/endorsed</code> (prefix: <code>endorsed.</code>)</li>
 *     <li>Additional libraries to be added to <code>lib/ext</code> (prefix: <code>ext.</code>)</li>
 *     <li>Bundle override definitions to be added to <code>etc/overrides.properties</code> (prefix: <code>override.</code>)</li>
 *     <li>Optional {@link org.osgi.resource.Resource resources} to be used during resolution (prefix: <code>optional.</code>)</li>
 * </ul></p>
 */
public interface Profile extends ProfileConstants {

    /**
     * Returns an attribute map of this profile
     * @return
     */
    Map<String, String> getAttributes();

    /**
     * Returns a property map for additional properties to be added to <code>${karaf.etc}/config.properties</code>
     * @return
     */
    Map<String, String> getConfig();

    /**
     * Returns a property map for additional properties to be added to <code>${karaf.etc}/system.properties</code>
     * @return
     */
    Map<String, String> getSystem();

    /**
     * Returns a unique identifier of this profile
     * @return
     */
    String getId();

    /**
     * Returns a list of parent profile identifiers for this profile
     * @return
     */
    List<String> getParentIds();

    /**
     * Returns a list of bundles (bundle URIs) defined in this profile
     * @return
     */
    List<String> getBundles();

    /**
     * Returns a list of features (<code>feature-name[/feature-version]</code>) defined in this profile
     * @return
     */
    List<String> getFeatures();

    /**
     * Returns a list of features XML repositories (URIs) defined in this profile
     * @return
     */
    List<String> getRepositories();

    /**
     * Returns a list of libraries (to be added to <code>${karaf.home}/lib</code>) defined in this profile
     * @return
     */
    List<String> getLibraries();

    /**
     * Returns a list of boot libraries (to be added to <code>${karaf.home}/lib/boot</code>) defined in this profile
     * @return
     */
    List<String> getBootLibraries();

    /**
     * Returns a list of endorsed libraries (to be added to <code>${karaf.home}/lib/endorsed</code>) defined in this profile
     * @return
     */
    List<String> getEndorsedLibraries();

    /**
     * Returns a list of extension libraries (to be added to <code>${karaf.home}/lib/ext</code>) defined in this profile
     * @return
     */
    List<String> getExtLibraries();

    /**
     * Returns a list of bundle override definitions (to be added to <code>${karaf.etc}/overrides.properties</code>)
     * defined in this profile
     * @return
     */
    List<String> getOverrides();

    /**
     * Returns a list of optional {@link org.osgi.resource.Resource resources} (URIs) to be used during
     * resolution
     * @return
     */
    List<String> getOptionals();

    /**
     * Get the configuration file names that are available on this profile. This list should contain at least
     * <code>profile.cfg</code> file.
     *
     * @return The configuration file names in the profile.
     */
    Set<String> getConfigurationFileNames();

    /**
     * Get all file configurations. This list should contain at least
     * <code>profile.cfg</code> file.
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
     * Get all configuration properties.This list should contain at least
     * configuration from main profile file - <code>profile.cfg</code>.
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
     * Indicate if this profile is an overlay or not. An <em>overlay</em> profile includes configurations and
     * attributes of parent profiles, while descendant profiles always have priority over parent profiles.
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
