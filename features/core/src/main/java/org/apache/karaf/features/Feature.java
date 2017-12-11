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

import java.util.List;

/**
 * A feature is a list of bundles associated identified by its name.
 */
public interface Feature extends Blacklisting {

    String DEFAULT_INSTALL_MODE = "auto";

    String getId();

    String getName();

    String getDescription();

    String getDetails();

    String getVersion();

    boolean hasVersion();

    String getResolver();

    String getInstall();

    boolean isHidden();

    List<Dependency> getDependencies();

    List<BundleInfo> getBundles();

//    Map<String, Map<String, String>> getConfigurations();
    List<ConfigInfo> getConfigurations();

    List<ConfigFileInfo> getConfigurationFiles();

    List<? extends Conditional> getConditional();

    int getStartLevel();

    List<? extends Capability> getCapabilities();

    List<? extends Requirement> getRequirements();

    Scoping getScoping();

    List<? extends Library> getLibraries();

    String getNamespace();

    List<String> getResourceRepositories();

    String getRepositoryUrl();

}
