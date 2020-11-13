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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URI;
import java.util.Objects;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.internal.model.Features;
import org.apache.karaf.features.internal.model.JacksonUtil;
import org.apache.karaf.features.internal.model.JaxbUtil;

/**
 * The repository implementation.
 */
public class RepositoryImpl implements Repository {

    /** {@link URI original URI} of the resource where feature declarations were loaded from */
    private final URI uri;

    /** Transformed {@link Features model} of the repository */
    private Features features;

    private boolean blacklisted;

    public RepositoryImpl(URI uri) {
        this(uri, false);
    }

    public RepositoryImpl(URI uri, boolean validate) {
        this.uri = uri;
        load(validate);
    }

    /**
     * Constructs a repository without any downloading
     * @param uri
     * @param features
     * @param blacklisted
     */
    public RepositoryImpl(URI uri, Features features, boolean blacklisted) {
        this.uri = uri;
        this.features = features;
        this.blacklisted = blacklisted;
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public String getName() {
        return features.getName();
    }

    @Override
    public URI[] getRepositories() {
        return features.getRepository().stream()
                .map(String::trim)
                .map(URI::create)
                .toArray(URI[]::new);
    }

    @Override
    public URI[] getResourceRepositories() {
        return features.getResourceRepository().stream()
                .map(String::trim)
                .map(URI::create)
                .toArray(URI[]::new);
    }

    @Override
    public Feature[] getFeatures() {
        return features.getFeature()
                .toArray(new Feature[features.getFeature().size()]);
    }

    public Features getFeaturesInternal() {
        return features;
    }

    @Override
    public boolean isBlacklisted() {
        return blacklisted;
    }

    public void setBlacklisted(boolean blacklisted) {
        this.blacklisted = blacklisted;
        features.setBlacklisted(blacklisted);
    }

    private void load(boolean validate) {
        if (features == null) {
            try (InputStream inputStream = new InterruptibleInputStream(uri.toURL().openStream())) {
                if (JacksonUtil.isJson(uri.toASCIIString())) {
                    features = JacksonUtil.unmarshal(uri.toASCIIString());
                } else {
                    features = JaxbUtil.unmarshal(uri.toASCIIString(), inputStream, validate);
                }
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage() + " : " + uri, e);
            }
        }
    }

    /**
     * An extension point to alter {@link Features JAXB model of features}
     * @param processor
     */
    public void processFeatures(FeaturesProcessor processor) {
        processor.process(features);
        if (blacklisted) {
            // all features of blacklisted repository are blacklisted too
            for (org.apache.karaf.features.internal.model.Feature feature : features.getFeature()) {
                feature.setBlacklisted(true);
            }
        }
    }

    static class InterruptibleInputStream extends FilterInputStream {
        InterruptibleInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedIOException();
            }
            return super.read(b, off, len);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RepositoryImpl that = (RepositoryImpl) o;
        return Objects.equals(uri, that.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri);
    }

    @Override
    public String toString() {
        return getURI().toString();
    }

}
