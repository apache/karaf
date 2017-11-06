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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.features.Repository;

/**
 * Implementation of {@link RepositoryCache} that makes use of {@link FeaturesProcessor} to alter feature
 * definitions after reading them from XML file.
 */
public class RepositoryCacheImpl implements RepositoryCache {

    private final Map<String, Repository> repositoryCache = new HashMap<>();
    private final FeaturesProcessor featuresProcessor;

    public RepositoryCacheImpl(FeaturesProcessor featuresProcessor) {
        this.featuresProcessor = featuresProcessor;
    }

    @Override
    public Repository create(URI uri, boolean validate) {
        RepositoryImpl repository = new RepositoryImpl(uri, validate);
        if (featuresProcessor != null) {
            repository.setBlacklisted(featuresProcessor.isRepositoryBlacklisted(uri));
            repository.processFeatures(featuresProcessor);
        }
        return repository;
    }

    @Override
    public void addRepository(Repository repository) {
        String repoUriSt = repository.getURI().toString();
        repositoryCache.put(repoUriSt, repository);
    }

    @Override
    public void removeRepository(URI repositoryUri) {
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

    @Override
    public Repository[] listRepositories() {
        return repositoryCache.values().toArray(new Repository[repositoryCache.size()]);
    }

    @Override
    public Repository[] listMatchingRepositories(Set<String> uris) {
        return repositoryCache.values().stream()
                .filter(r -> uris.contains(r.getURI().toString()))
                .toArray(Repository[]::new);
    }

    @Override
    public Repository getRepositoryByName(String name) {
        for (Repository repo : this.repositoryCache.values()) {
            if (name.equals(repo.getName())) {
                return repo;
            }
        }
        return null;
    }

    @Override
    public Repository getRepository(String uri) {
        return repositoryCache.get(uri);
    }

    /**
     * Returns a set containing the given repository and all its dependencies recursively
     */
    @Override
    public Set<Repository> getRepositoryClosure(Repository repo) {
        Set<Repository> closure = new HashSet<>();
        Deque<Repository> remaining = new ArrayDeque<>(Collections.singleton(repo));
        while (!remaining.isEmpty()) {
            Repository rep = remaining.removeFirst();
            if (closure.add(rep)) {
                for (URI uri : rep.getRepositories()) {
                    remaining.add(getRepository(uri.toString()));
                }
            }
        }
        return closure;
    }

}
