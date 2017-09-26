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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.karaf.features.internal.resolver.CapabilitySet;
import org.apache.karaf.features.internal.resolver.RequirementImpl;
import org.apache.karaf.features.internal.resolver.SimpleFilter;
import org.osgi.framework.Constants;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class BaseRepository implements Repository {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final List<Resource> resources;
    protected final Map<String, CapabilitySet> capSets;

    public BaseRepository() {
        this.resources = new ArrayList<>();
        this.capSets = new HashMap<>();
    }

    public BaseRepository(Collection<Resource> resources) {
        this();
        for (Resource resource : resources) {
            addResource(resource);
        }
    }

    protected void addResource(Resource resource) {
        for (Capability cap : resource.getCapabilities(null)) {
            String ns = cap.getNamespace();
            capSets.computeIfAbsent(ns, n -> new CapabilitySet(Collections.singletonList(n))).addCapability(cap);
        }
        resources.add(resource);
    }

    public List<Resource> getResources() {
        return resources;
    }

    @Override
    public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
        Map<Requirement, Collection<Capability>> result = new HashMap<>();
        for (Requirement requirement : requirements) {
            CapabilitySet set = capSets.get(requirement.getNamespace());
            if (set != null) {
                SimpleFilter sf;
                if (requirement instanceof RequirementImpl) {
                    sf = ((RequirementImpl) requirement).getFilter();
                } else {
                    String filter = requirement.getDirectives().get(Constants.FILTER_DIRECTIVE);
                    sf = (filter != null)
                            ? SimpleFilter.parse(filter)
                            : SimpleFilter.MATCH_ALL_FILTER;
                }
                result.put(requirement, set.match(sf, true));
            } else {
                result.put(requirement, Collections.emptyList());
            }
        }
        return result;
    }
}
