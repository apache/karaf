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

import java.util.Iterator;
import java.util.Map;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.bundlerepository.impl.CapabilityImpl;
import org.apache.felix.bundlerepository.impl.RequirementImpl;
import org.osgi.framework.Version;

public class ConvertedResource implements Resource {

    private final org.osgi.service.obr.Resource resource;
    
    private Capability[] capabilities;
    private Requirement[] requirements;

    public ConvertedResource(org.osgi.service.obr.Resource resource) {
        this.resource = resource;
        
        // convert capabilities
        org.osgi.service.obr.Capability[] c = resource.getCapabilities();
        if (c != null) {
            capabilities = new Capability[c.length];
            for (int i = 0; i < c.length; i++) {
                CapabilityImpl cap = new CapabilityImpl(c[i].getName());
                Iterator iter = c[i].getProperties().entrySet().iterator();     
                int j = 0;
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    cap.addProperty((String) entry.getKey(), null, (String) entry.getValue());
                }
                
                capabilities[i] = cap;
            }
        }
        
        // convert requirements
        org.osgi.service.obr.Requirement[] r = resource.getRequirements();
        if (r != null) {
            requirements = new Requirement[r.length];
            for (int i = 0; i < r.length; i++) {
                RequirementImpl req = new RequirementImpl(r[i].getName());
                req.setFilter(r[i].getFilter());
                req.setOptional(r[i].isOptional());
                req.setExtend(r[i].isExtend());
                req.setMultiple(r[i].isMultiple());
                
                requirements[i] = req;                
            }
        }
    }

    public Capability[] getCapabilities() {
        return capabilities;
    }

    public Requirement[] getRequirements() {
        return requirements;
    }
    
    public String[] getCategories() {
        return resource.getCategories();
    }

    public String getId() {
        return resource.getId();
    }

    public String getPresentationName() {
        return resource.getPresentationName();
    }

    public Map getProperties() {
        return resource.getProperties();
    }

    public Long getSize() {
        return null;
    }

    public String getSymbolicName() {
        return resource.getSymbolicName();
    }

    public String getURI() {
        return resource.getURL().toString();
    }

    public Version getVersion() {
        return resource.getVersion();
    }

    public boolean isLocal() {
        return false;
    }
   
}
