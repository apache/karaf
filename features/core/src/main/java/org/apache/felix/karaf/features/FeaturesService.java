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
package org.apache.felix.karaf.features;

import java.net.URI;

/**
 * The service managing features repositories.
 */
public interface FeaturesService {

    void addRepository(URI url) throws Exception;

    void removeRepository(URI url);

    Repository[] listRepositories();

    void installFeature(String name) throws Exception;
    
    void installFeature(String name, String version) throws Exception;

    void uninstallFeature(String name) throws Exception;
    
    void uninstallFeature(String name, String version) throws Exception;

    Feature[] listFeatures() throws Exception;

    Feature[] listInstalledFeatures();

    boolean isInstalled(Feature f);

}
