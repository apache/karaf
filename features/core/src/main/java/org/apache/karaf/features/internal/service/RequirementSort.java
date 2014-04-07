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


import org.apache.karaf.features.internal.resolver.RequirementImpl;
import org.apache.karaf.features.internal.resolver.SimpleFilter;
import org.osgi.framework.Constants;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RequirementSort  {

    /**
     * Sorts {@link Resource} based on their {@link Requirement}s and {@link Capability}s.
     * @param resources
     * @return
     */
    public static <T extends Resource> Collection<T> sort(Collection<T> resources) {
        Set<T> sorted = new LinkedHashSet<T>();
        Set<T> visited = new LinkedHashSet<T>();
        for (T r : resources) {
            visit(r, resources, visited, sorted);
        }
        return sorted;
    }


    private static <T extends Resource> void visit(T resource, Collection<T> resources, Set<T> visited, Set<T> sorted) {
        if (visited.contains(resource)) {
            return;
        }
        visited.add(resource);
        for (T r : collectDependencies(resource, resources)) {
            visit(r, resources, visited, sorted);
        }
        sorted.add(resource);
    }

    /**
     * Finds the dependencies of the current resource.
     * @param resource
     * @param allResources
     * @return
     */
    private static <T extends Resource> Set<T> collectDependencies(T resource, Collection<T> allResources) {
        Set<T> result = new LinkedHashSet<T>();
        List<Requirement> requirements = resource.getRequirements(null);
        for (Requirement requirement : requirements) {
            boolean isSatisfied = false;
            for (Resource r : result) {
                for (Capability capability : r.getCapabilities(null)) {
                    if (isSatisfied(requirement, capability)) {
                        isSatisfied = true;
                        break;
                    }
                }
            }

            for (T r : allResources) {
                if (!isSatisfied) {
                    for (Capability capability : r.getCapabilities(null)) {
                        if (isSatisfied(requirement, capability)) {
                            result.add(r);
                            break;
                        }
                    }
                }
            }
        }
        return result;
    }

    private static boolean isSatisfied(Requirement requirement, Capability capability) {
        RequirementImpl br;
        if (requirement instanceof RequirementImpl) {
            br = (RequirementImpl) requirement;
        } else {
            String filter = requirement.getDirectives().get(Constants.FILTER_DIRECTIVE);
            SimpleFilter sf = (filter != null)
                    ? SimpleFilter.parse(filter)
                    : new SimpleFilter(null, null, SimpleFilter.MATCH_ALL);
            br = new RequirementImpl(null, requirement.getNamespace(), requirement.getDirectives(), requirement.getAttributes(), sf);
        }
        return br.matches(capability);
    }
}
