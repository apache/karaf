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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.karaf.features.FeaturesListener;
import org.apache.karaf.features.RepositoriesListener;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.RepositoryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage repositories of karaf. The repositories will be read from the uri given and cached in memory.
 * state.repositories only contains the top level repos
 * repositoryCache contains all repositories
 */
public class Repositories {
    private static final Logger LOGGER = LoggerFactory.getLogger(Repositories.class);

    private final Map<String, Repository> repositoryCache = new HashMap<>();
    private String blacklisted;
    private List<RepositoriesListener> listeners;
    private Set<String> repoUris;

    public Repositories(String blacklisted, Set<String> repoUris) {
        this.blacklisted = blacklisted;
        this.listeners = new CopyOnWriteArrayList<>();
        this.repoUris = new HashSet<>(repoUris);
        for (String uri : repoUris) {
            try {
                Repository repo = load(URI.create(uri));
                add(repo);
            } catch (IOException e) {

            }
        }
    }
    
    public Set<String> getUris() {
        return Collections.unmodifiableSet(repoUris);
    }

    public void add(Repository repository) {
        String uri = repository.getURI().toString();
        synchronized (this) {
            repositoryCache.put(uri, repository);
            repoUris.add(uri);
        }
        callListeners(new RepositoryEvent(repository, RepositoryEvent.EventType.RepositoryAdded, false));
    }
    
    public Repository load(URI uri) throws IOException {
        RepositoryImpl repo = new RepositoryImpl(uri, blacklisted);
        repo.load();
        return repo;
    }
    

    public Repository loadAndValidate(URI uri) throws IOException {
        RepositoryImpl repo = new RepositoryImpl(uri, blacklisted);
        repo.load(true);
        return repo;
    }
    
    public void loadDependent() {
        List<String> uris = new ArrayList<>(repoUris);
        // * first load dependent repositories
        Set<String> loaded = new HashSet<>();
        List<String> toLoad = new ArrayList<>(uris);
        while (!toLoad.isEmpty()) {
            String uriSt = toLoad.remove(0);
            URI uri = URI.create(uriSt);
            try {
                Repository repo = getByURI(uri);
                if (repo == null) {
                    repo = load(uri);
                    synchronized (this) {
                        repositoryCache.put(uriSt, repo);
                    }
                }
                if (loaded.add(uriSt)) {
                    for (URI u : repo.getRepositories()) {
                        toLoad.add(u.toString());
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Can't load features repository {}", uri, e);
            }
        }
    }
    
    public void validate(URI uri) throws Exception {
        throw new UnsupportedOperationException();
    }

    public void remove(Repository repository) throws Exception {
        synchronized (this) {
            if (!repoUris.remove(repository.getURI().toString())) {
                return;
            }
            List<String> toRemove = new ArrayList<>();
            toRemove.add(repository.getURI().toString());
            while (!toRemove.isEmpty()) {
                Repository rep = repositoryCache.remove(toRemove.remove(0));
                if (rep != null) {
                    for (URI u : rep.getRepositories()) {
                        toRemove.add(u.toString());
                    }
                }
            }
        }
        callListeners(new RepositoryEvent(repository, RepositoryEvent.EventType.RepositoryRemoved, false));
    }
    
    public synchronized Repository[] list() throws Exception {
        return repositoryCache.values().toArray(new Repository[repositoryCache.size()]);
    }
    
    public synchronized Repository[] listRequired() throws Exception {
        List<Repository> repos = new ArrayList<>();
        for (Map.Entry<String, Repository> entry : repositoryCache.entrySet()) {
            if (repoUris.contains(entry.getKey())) {
                repos.add(entry.getValue());
            }
        }
        return repos.toArray(new Repository[repos.size()]);
    }

    
    public synchronized Repository getByName(String name) throws Exception {
        for (Repository repo : this.repositoryCache.values()) {
            if (name.equals(repo.getName())) {
                return repo;
            }
        }
        return null;
    }

    public synchronized Repository getByURI(URI uri) throws Exception {
        return this.repositoryCache.get(uri.toString());
    }
    
    public synchronized void registerListener(RepositoriesListener listener) {
        for (Repository repository : this.repositoryCache.values()) {
            listener.repositoryEvent(new RepositoryEvent(repository, RepositoryEvent.EventType.RepositoryAdded, true));
        }
        this.listeners.add(listener);
    }
    
    public synchronized void unregisterListener(FeaturesListener listener) {
        listeners.remove(listener);
    }

    public synchronized List<Repository> getAll() {
        return new ArrayList<>(this.repositoryCache.values());
    }


    protected void callListeners(RepositoryEvent event) {
        for (RepositoriesListener listener : listeners) {
            listener.repositoryEvent(event);
        }
    }

}
