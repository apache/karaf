/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.karaf.tooling.assembly;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists of artifacts, kars and features.
 */
class ArtifactLists {

    private final List<String> startupKars = new ArrayList<>();

    private final List<String> bootKars = new ArrayList<>();

    private final List<String> installedKars = new ArrayList<>();

    private final List<String> startupBundles = new ArrayList<>();

    private final List<String> bootBundles = new ArrayList<>();

    private final List<String> installedBundles = new ArrayList<>();

    private final List<String> startupRepositories = new ArrayList<>();

    private final List<String> bootRepositories = new ArrayList<>();

    private final List<String> installedRepositories = new ArrayList<>();

    void addStartupBundles(final List<String> startup) {
        this.startupBundles.addAll(startup);
    }

    void addBootBundles(final List<String> boot) {
        this.bootBundles.addAll(boot);
    }

    void addInstalledBundles(final List<String> installed) {
        this.installedBundles.addAll(installed);
    }

    void addStartupRepositories(final List<String> startup) {
        this.startupRepositories.addAll(startup);
    }

    void addBootRepositories(final List<String> boot) {
        this.bootRepositories.addAll(boot);
    }

    void addInstalledRepositories(final List<String> installed) {
        this.installedRepositories.addAll(installed);
    }

    List<String> getStartupBundles() {
        return startupBundles;
    }

    List<String> getBootBundles() {
        return bootBundles;
    }

    List<String> getInstalledBundles() {
        return installedBundles;
    }

    List<String> getStartupKars() {
        return startupKars;
    }

    List<String> getBootKars() {
        return bootKars;
    }

    List<String> getInstalledKars() {
        return installedKars;
    }

    void removeStartupKar(final String kar) {
        startupKars.remove(kar);
    }

    List<String> getStartupRepositories() {
        return startupRepositories;
    }

    List<String> getBootRepositories() {
        return bootRepositories;
    }

    List<String> getInstalledRepositories() {
        return installedRepositories;
    }
}
