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

import java.util.Map;

import org.apache.karaf.features.internal.util.StringArrayMap;
import org.osgi.framework.Constants;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class RequirementImpl extends BaseClause implements Requirement {

    private final Resource resource;
    private final String namespace;
    private final SimpleFilter filter;
    private final boolean optional;
    private final Map<String, String> dirs;
    private final Map<String, Object> attrs;

    public RequirementImpl(
            Resource resource, String namespace,
            Map<String, String> dirs, Map<String, Object> attrs, SimpleFilter filter) {
        this.resource = resource;
        this.namespace = namespace.intern();
        this.dirs = StringArrayMap.reduceMemory(dirs);
        this.attrs = StringArrayMap.reduceMemory(attrs);
        this.filter = filter;
        // Find resolution import directives.
        optional = Constants.RESOLUTION_OPTIONAL.equals(this.dirs.get(Constants.RESOLUTION_DIRECTIVE));
    }

    public RequirementImpl(
            Resource resource, String namespace,
            Map<String, String> dirs, Map<String, Object> attrs) {
        this(resource, namespace, dirs, attrs, SimpleFilter.convert(attrs));
    }

    public String getNamespace() {
        return namespace;
    }

    public Map<String, String> getDirectives() {
        return dirs;
    }

    public Map<String, Object> getAttributes() {
        return attrs;
    }

    public Resource getResource() {
        return resource;
    }

    public boolean matches(Capability cap) {
        return CapabilitySet.matches(cap, getFilter());
    }

    public boolean isOptional() {
        return optional;
    }

    public SimpleFilter getFilter() {
        return filter;
    }

}
