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
import java.util.Set;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.internal.model.Features;
import org.apache.karaf.features.internal.model.JaxbUtil;

/**
 * The repository implementation.
 */
public class RepositoryImpl implements Repository {

    private final URI uri;
    private final Clause[] blacklisted;
    private Features features;

    public RepositoryImpl(URI uri) {
        this(uri, (Clause[]) null);
    }

    public RepositoryImpl(URI uri, String blacklisted) {
        this.uri = uri;
        Set<String> blacklistStrings = Blacklist.loadBlacklist(blacklisted);
        this.blacklisted = Parser.parseClauses(blacklistStrings.toArray(new String[blacklistStrings.size()]));
    }

    public RepositoryImpl(URI uri, Clause[] blacklisted) {
        this.uri = uri;
        this.blacklisted = blacklisted != null ? blacklisted : new Clause[0];
    }

    public URI getURI() {
        return uri;
    }

    public String getName() throws IOException {
        load();
        return features.getName();
    }

    public URI[] getRepositories() throws IOException {
        load();
        return features.getRepository().stream()
                .map(String::trim)
                .map(URI::create)
                .toArray(URI[]::new);
    }

    public URI[] getResourceRepositories() throws IOException {
        load();
        return features.getResourceRepository().stream()
                .map(String::trim)
                .map(URI::create)
                .toArray(URI[]::new);
    }

    public Feature[] getFeatures() throws IOException {
        load();
        return features.getFeature()
                .toArray(new Feature[features.getFeature().size()]);
    }


    public void load() throws IOException {
        load(false);
    }

    public void load(boolean validate) throws IOException {
        if (features == null) {
            try (
                    InputStream inputStream = new InterruptibleInputStream(uri.toURL().openStream())
            ) {
                features = JaxbUtil.unmarshal(uri.toASCIIString(), inputStream, validate);
                Blacklist.blacklist(features, blacklisted);
            } catch (Exception e) {
                throw new IOException(e.getMessage() + " : " + uri, e);
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
    public int hashCode() {
        return uri.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RepositoryImpl other = (RepositoryImpl)obj;
        if (uri == null) {
            if (other.uri != null)
                return false;
        } else if (!uri.equals(other.uri))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return getURI().toString();
    }
}

