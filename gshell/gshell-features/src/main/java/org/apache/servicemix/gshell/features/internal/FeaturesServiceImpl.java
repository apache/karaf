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
package org.apache.servicemix.gshell.features.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.geronimo.gshell.command.Command;
import org.apache.servicemix.gshell.features.Feature;
import org.apache.servicemix.gshell.features.FeaturesService;
import org.apache.servicemix.gshell.features.Repository;
import org.osgi.framework.BundleContext;
import org.osgi.service.obr.RepositoryAdmin;
import org.springframework.osgi.context.BundleContextAware;

/**
 * The Features service implementation.
 * Adding a repository url will load the features contained in this repository and
 * create dummy sub shells.  When invoked, these commands will prompt the user for
 * installing the needed bundles.
 *
 */
public class FeaturesServiceImpl implements FeaturesService, BundleContextAware {

    private BundleContext bundleContext;
    private RepositoryAdmin admin;
    private Set<URL> urls;
    private Map<URL, RepositoryImpl> repositories = new HashMap<URL, RepositoryImpl>();

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public RepositoryAdmin getAdmin() {
        return admin;
    }

    public void setAdmin(RepositoryAdmin admin) {
        this.admin = admin;
    }

    public void setUrls(String urls) throws MalformedURLException {
        String[] s = urls.split(",");
        this.urls = new HashSet<URL>();
        for (int i = 0; i < s.length; i++) {
            this.urls.add(new URL(s[i]));
        }
    }

    public void addRepository(URL url) throws Exception {
        RepositoryImpl repo = new RepositoryImpl(url);
        repo.load();
        Feature[] features = repo.getFeatures();
        for (int i = 0; i < features.length; i++) {
            CommandProxy cmd = new CommandProxy(features[i], bundleContext);
        }
        repositories.put(url, repo);
    }

    public void removeRepository(URL url) {
        Repository repo = repositories.remove(url);
        // TODO: ...
    }

    public Repository[] listRepositories() {
        return new Repository[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void start() throws Exception {
        if (urls != null) {
            for (URL url : urls) {
                addRepository(url);
            }
        }
    }

    public void stop() throws Exception {
        urls = new HashSet<URL>(repositories.keySet());
        while (!repositories.isEmpty()) {
            removeRepository(repositories.keySet().iterator().next());
        }
    }
}
