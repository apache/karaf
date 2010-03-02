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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.Version;
import org.osgi.service.obr.Capability;
import org.osgi.service.obr.Repository;
import org.osgi.service.obr.Requirement;

public class ResourceWrapper implements org.osgi.service.obr.Resource {

    final Resource resource;

    public ResourceWrapper(Resource resource) {
        this.resource = resource;
    }

    public Map getProperties() {
        return resource.getProperties();
    }

    public String getSymbolicName() {
        return resource.getSymbolicName();
    }

    public String getPresentationName() {
        return resource.getPresentationName();
    }

    public Version getVersion() {
        return resource.getVersion();
    }

    public String getId() {
        return resource.getId();
    }

    public URL getURL() {
        try {
            return new URL(resource.getURI());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public Requirement[] getRequirements() {
        return Wrapper.wrap(resource.getRequirements());
    }

    public Capability[] getCapabilities() {
        return Wrapper.wrap(resource.getCapabilities());
    }

    public String[] getCategories() {
        return resource.getCategories();
    }

    public Repository getRepository() {
        throw new UnsupportedOperationException();
    }
}
