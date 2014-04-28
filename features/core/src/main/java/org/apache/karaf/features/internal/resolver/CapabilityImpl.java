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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Constants;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

public class CapabilityImpl extends BaseClause implements Capability {

    private final Resource resource;
    private final String namespace;
    private final Map<String, String> dirs;
    private final Map<String, Object> attrs;
    private final Set<String> mandatory;

    public CapabilityImpl(Resource resource, String namespace,
                          Map<String, String> dirs, Map<String, Object> attrs) {
        this.namespace = namespace;
        this.resource = resource;
        this.dirs = dirs;
        this.attrs = attrs;

        // Handle mandatory directive
        Set<String> mandatory = Collections.emptySet();
        String value = this.dirs.get(Constants.MANDATORY_DIRECTIVE);
        if (value != null) {
            List<String> names = ResourceBuilder.parseDelimitedString(value, ",");
            mandatory = new HashSet<>(names.size());
            for (String name : names) {
                // If attribute exists, then record it as mandatory.
                if (this.attrs.containsKey(name)) {
                    mandatory.add(name);
                // Otherwise, report an error.
                } else {
                    throw new IllegalArgumentException("Mandatory attribute '" + name + "' does not exist.");
                }
            }
        }
        this.mandatory = mandatory;
    }

    public Resource getResource() {
        return resource;
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

    public boolean isAttributeMandatory(String name) {
        return !mandatory.isEmpty() && mandatory.contains(name);
    }

}
