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
package org.apache.karaf.features.internal.repository;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.stream.XMLStreamException;

import org.apache.karaf.features.internal.resolver.CapabilitySet;
import org.apache.karaf.features.internal.resolver.SimpleFilter;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import static org.osgi.framework.namespace.IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE;
import static org.osgi.framework.namespace.IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE;
import static org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE;

/**
 * Repository conforming to the OSGi Repository specification.
 * The content of the URL can be gzipped.
 */
public class XmlRepository extends BaseRepository {

    protected final String url;
    protected final long expiration;
    protected final boolean ignoreFailures;
    protected final Map<String, XmlLoader> loaders = new HashMap<>();
    protected final ReadWriteLock lock = new ReentrantReadWriteLock();

    public XmlRepository(String url, long expiration, boolean ignoreFailures) {
        this.url = url;
        this.expiration = expiration;
        this.ignoreFailures = ignoreFailures;
    }

    @Override
    public List<Resource> getResources() {
        checkAndLoadCache();
        return super.getResources();
    }

    @Override
    public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
        checkAndLoadCache();
        return super.findProviders(requirements);
    }

    public String getUrl() {
        return url;
    }

    protected Map<String, XmlLoader> getLoaders() {
        return loaders;
    }

    @Override
    protected void addResource(Resource resource) {
        List<Capability> identities = resource.getCapabilities(IDENTITY_NAMESPACE);
        if (identities.isEmpty()) {
            throw new IllegalStateException("Invalid resource: a capability with 'osgi.identity' namespace is required");
        } else if (identities.size() > 1) {
            throw new IllegalStateException("Invalid resource: multiple 'osgi.identity' capabilities found");
        }
        Capability identity = identities.get(0);
        Object name = identity.getAttributes().get(IDENTITY_NAMESPACE);
        Object type = identity.getAttributes().get(CAPABILITY_TYPE_ATTRIBUTE);
        Object vers = identity.getAttributes().get(CAPABILITY_VERSION_ATTRIBUTE);
        if (!String.class.isInstance(name)
                || !String.class.isInstance(type)
                || !Version.class.isInstance(vers)) {
            throw new IllegalStateException("Invalid osgi.identity capability: " + identity);
        }
        if (!hasResource((String) type, (String) name, (Version) vers)) {
            super.addResource(resource);
        }
    }

    private boolean hasResource(String type, String name, Version version) {
        CapabilitySet set = capSets.get(IDENTITY_NAMESPACE);
        if (set != null) {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put(CAPABILITY_TYPE_ATTRIBUTE, type);
            attrs.put(IDENTITY_NAMESPACE, name);
            attrs.put(CAPABILITY_VERSION_ATTRIBUTE, version);
            SimpleFilter sf = SimpleFilter.convert(attrs);
            return !set.match(sf, true).isEmpty();
        } else {
            return false;
        }
    }

    private void checkAndLoadCache() {
        try {
            if (checkAndLoadReferrals(url, Integer.MAX_VALUE)) {
                lock.writeLock().lock();
                try {
                    resources.clear();
                    capSets.clear();
                    populate(loaders.get(url).xml, Integer.MAX_VALUE);
                } finally {
                    lock.writeLock().unlock();
                }
            }
        } catch (Exception e) {
            if (ignoreFailures) {
                logger.warn("Ignoring failure: " + e.getMessage(), e);
            } else {
                throw e;
            }
        }
    }

    private void populate(StaxParser.XmlRepository xml, int hopCount) {
        if (hopCount > 0) {
            for (Resource resource : xml.resources) {
                addResource(resource);
            }
            for (StaxParser.Referral referral : xml.referrals) {
                populate(loaders.get(referral.url).xml, Math.min(referral.depth, hopCount - 1));
            }
        }
    }

    private boolean checkAndLoadReferrals(String url, int hopCount) {
        boolean modified = false;
        if (hopCount > 0) {
            XmlLoader loader = loaders.computeIfAbsent(url, u -> new XmlLoader(u, expiration));
            modified = loader.checkAndLoadCache();
            for (StaxParser.Referral referral : loader.xml.referrals) {
                modified |= checkAndLoadReferrals(referral.url, Math.min(referral.depth, hopCount - 1));
            }
        }
        return modified;
    }

    protected static class XmlLoader extends UrlLoader {

        protected StaxParser.XmlRepository xml;

        public XmlLoader(String url, long expiration) {
            super(url, expiration);
        }

        public XmlLoader(String url, long expiration, StaxParser.XmlRepository xml) {
            super(url, expiration);
            this.xml = xml;
        }

        @Override
        protected boolean doRead(InputStream is) throws IOException {
            try {
                StaxParser.XmlRepository oldXml = xml;
                xml = StaxParser.parse(URI.create(getUrl()), is, oldXml);
                return oldXml != xml;
            } catch (XMLStreamException e) {
                throw new IOException("Unable to read xml repository", e);
            }
        }
    }

}
