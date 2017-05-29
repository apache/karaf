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
package org.apache.karaf.features.internal.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.karaf.features.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryCache.class);
    private final Map<String, Repository> repositoryCache = new HashMap<>();
    private final Clause[] blacklisted;
    
    public RepositoryCache(String blacklisted) {
        this.blacklisted = loadBlacklist(blacklisted);
    }
    
    private Clause[] loadBlacklist(String blacklisted) {
        Set<String> blacklistStrings = Blacklist.loadBlacklist(blacklisted);
        return Parser.parseClauses(blacklistStrings.toArray(new String[blacklistStrings.size()]));
    }
    
    public Repository load(URI uri) throws Exception {
        return new RepositoryImpl(uri, blacklisted);
    }

    public Repository loadAndValidate(URI uri) throws Exception {
        RepositoryImpl repo = new RepositoryImpl(uri, blacklisted);
        repo.load(true);
        return repo;
    }

    public synchronized void addRepository(Repository repository) throws Exception {
        String repoUriSt = repository.getURI().toString();
        repositoryCache.put(repoUriSt, repository);
    }

    public synchronized  void removeRepository(URI repositoryUri) throws Exception {
        List<String> toRemove = new ArrayList<>();
        toRemove.add(repositoryUri.toString());
        while (!toRemove.isEmpty()) {
            Repository rep = repositoryCache.remove(toRemove.remove(0));
            if (rep != null) {
                for (URI u : rep.getRepositories()) {
                    toRemove.add(u.toString());
                }
            }
        }
    }

    public  synchronized Repository[] listRepositories() {
        return repositoryCache.values().toArray(new Repository[repositoryCache.size()]);
    }

    public  synchronized Repository[] listRequiredRepositories(Set<String> topLevelRepoUris) throws Exception {
        List<Repository> repos = new ArrayList<>();
        for (Map.Entry<String, Repository> entry : repositoryCache.entrySet()) {
            if (topLevelRepoUris.contains(entry.getKey())) {
                repos.add(entry.getValue());
            }
        }
        return repos.toArray(new Repository[repos.size()]);
    }

    public synchronized Repository getRepository(String name) throws Exception {
        for (Repository repo : this.repositoryCache.values()) {
            if (name.equals(repo.getName())) {
                return repo;
            }
        }
        return null;
    }

    public  synchronized Repository getRepository(URI uri) throws Exception {
        for (Repository repo : this.repositoryCache.values()) {
            if (repo.getURI().equals(uri)) {
                return repo;
            }
        }
        return null;
    }

    public String getRepositoryName(URI uri) throws Exception {
        Repository repo = getRepository(uri);
        return (repo != null) ? repo.getName() : null;
    }
    
    public synchronized void loadDependent(Set<String> topLevelRepoUris) {
        Set<String> loaded = new HashSet<>();
        List<String> toLoad = new ArrayList<>(topLevelRepoUris);
        while (!toLoad.isEmpty()) {
            String uri = toLoad.remove(0);
            Repository repo = repositoryCache.get(uri);
            try {
                if (repo == null) {
                    repo = load(URI.create(uri));
                    repositoryCache.put(uri, repo);
                }
                if (loaded.add(uri)) {
                    for (URI u : repo.getRepositories()) {
                        toLoad.add(u.toString());
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Can't load features repository {}", uri, e);
            }
        }
    }

    public Set<Repository> getRepositories(Repository repo) throws Exception {
        HashSet<Repository> repos = new HashSet<>();
        for (URI repoURI : repo.getRepositories()) {
            repos.add(load(repoURI));
        }
        return repos;
    }
    
    public Set<Repository> tranGetRepositories(Repository repo) throws Exception {
        HashSet<Repository> repos = new HashSet<>();
        repos.add(repo);
        Set<Repository> deps = getRepositories(repo);
        for (Repository depRepo : deps) {
            if (!repos.contains(depRepo)) {
                repos.addAll(tranGetRepositories(depRepo));
            }
        }
        return repos;
    }

}
