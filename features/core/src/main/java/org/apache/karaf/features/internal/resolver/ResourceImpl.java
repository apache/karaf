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
package org.apache.karaf.features.internal.resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.karaf.features.internal.util.StringArrayMap;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

/**
 */
public class ResourceImpl implements Resource {

    protected final List<Capability> caps;
    protected final List<Requirement> reqs;

    /**
     * CAUTION: This constructor does not ensure that the resource
     * has the required identity capability
     */
    public ResourceImpl() {
        caps = new ArrayList<>(0);
        reqs = new ArrayList<>(0);
    }

    public ResourceImpl(String name, String type, Version version) {
        caps = new ArrayList<>(1);
        Map<String, String> dirs = new StringArrayMap<>(0);
        Map<String, Object> attrs = new StringArrayMap<>(3);
        attrs.put(IdentityNamespace.IDENTITY_NAMESPACE, name);
        attrs.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, type);
        attrs.put(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, version);
        CapabilityImpl identity = new CapabilityImpl(this, IdentityNamespace.IDENTITY_NAMESPACE, dirs, attrs);
        caps.add(identity);
        reqs = new ArrayList<>(0);
    }

    public void addCapability(Capability capability) {
        assert capability.getResource() == this;
        caps.add(capability);
    }

    public void addCapabilities(Collection<? extends Capability> capabilities) {
        for (Capability cap : capabilities) {
            assert cap.getResource() == this;
        }
        caps.addAll(capabilities);
    }

    public void addRequirement(Requirement requirement) {
        assert requirement.getResource() == this;
        reqs.add(requirement);
    }

    public void addRequirements(Collection<? extends Requirement> requirements) {
        for (Requirement req : requirements) {
            assert req.getResource() == this;
        }
        reqs.addAll(requirements);
    }

    public List<Capability> getCapabilities(String namespace) {
        List<Capability> result = caps;
        if (namespace != null) {
            result = new ArrayList<>();
            for (Capability cap : caps) {
                if (cap.getNamespace().equals(namespace)) {
                    result.add(cap);
                }
            }
        }
        return result;
    }

    public List<Requirement> getRequirements(String namespace) {
        List<Requirement> result = reqs;
        if (namespace != null) {
            result = new ArrayList<>();
            for (Requirement req : reqs) {
                if (req.getNamespace().equals(namespace)) {
                    result.add(req);
                }
            }
        }
        return result;
    }

    @Override
    public String toString() {
        Capability cap = getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0);
        return cap.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE) + "/"
                + cap.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
    }

}
