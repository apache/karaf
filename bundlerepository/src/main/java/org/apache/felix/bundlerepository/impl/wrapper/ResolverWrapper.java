/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.bundlerepository.impl.wrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resolver;

public class ResolverWrapper implements org.osgi.service.obr.Resolver {

    private final Resolver resolver;

    public ResolverWrapper(Resolver resolver)
    {
        this.resolver = resolver;
    }

    public void add(org.osgi.service.obr.Resource resource) {
        resolver.add(Wrapper.unwrap(resource));
    }

    public org.osgi.service.obr.Resource[] getAddedResources() {
        return Wrapper.wrap(resolver.getAddedResources());
    }

    public org.osgi.service.obr.Resource[] getRequiredResources() {
        return Wrapper.wrap(resolver.getRequiredResources());
    }

    public org.osgi.service.obr.Resource[] getOptionalResources() {
        return Wrapper.wrap(resolver.getOptionalResources());
    }

    public org.osgi.service.obr.Requirement[] getReason(org.osgi.service.obr.Resource resource) {
        Reason[] r = resolver.getReason(Wrapper.unwrap(resource));
        if (r == null)
        {
            return null;
        }
        Requirement[] r2 = new Requirement[r.length];
        for (int reaIdx = 0; reaIdx < r.length; reaIdx++)
        {
            r2[reaIdx] = r[reaIdx].getRequirement();
        }
        return Wrapper.wrap(r2);
    }

    public org.osgi.service.obr.Requirement[] getUnsatisfiedRequirements() {
        Map map = getUnsatisfiedRequirementsMap();
        return (org.osgi.service.obr.Requirement[]) map.keySet().toArray(new org.osgi.service.obr.Requirement[map.size()]);
    }

    public org.osgi.service.obr.Resource[] getResources(org.osgi.service.obr.Requirement requirement) {
        Map map = getUnsatisfiedRequirementsMap();
        List l = (List) map.get(requirement);
        if (l == null)
        {
            return null;
        }
        return (org.osgi.service.obr.Resource[]) l.toArray(new org.osgi.service.obr.Resource[l.size()]);
    }

    public boolean resolve() {
        return resolver.resolve();
    }

    public void deploy(boolean start) {
        resolver.deploy(start ? Resolver.START : 0);
    }

    private Map getUnsatisfiedRequirementsMap() {
        Reason[] reasons = resolver.getUnsatisfiedRequirements();
        Map map = new HashMap();
        for (int i = 0; i < reasons.length; i++)
        {
            org.osgi.service.obr.Requirement req = Wrapper.wrap(reasons[i].getRequirement());
            org.osgi.service.obr.Resource res = Wrapper.wrap(reasons[i].getResource());
            List l = (List) map.get(req);
            if (l == null)
            {
                l = new ArrayList();
                map.put(req, l);
            }
            l.add(res);
        }
        return map;
    }
}
