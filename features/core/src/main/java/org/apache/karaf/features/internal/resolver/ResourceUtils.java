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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.features.internal.repository.XmlRepository;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

import static org.osgi.framework.namespace.IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE;
import static org.osgi.framework.namespace.IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE;
import static org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE;
import static org.osgi.resource.Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE;
import static org.osgi.resource.Namespace.RESOLUTION_MANDATORY;
import static org.osgi.resource.Namespace.RESOLUTION_OPTIONAL;
import static org.osgi.service.repository.ContentNamespace.CAPABILITY_URL_ATTRIBUTE;
import static org.osgi.service.repository.ContentNamespace.CONTENT_NAMESPACE;

public final class ResourceUtils {

    public static final String TYPE_SUBSYSTEM = "karaf.subsystem";

    public static final String TYPE_FEATURE = "karaf.feature";

    private ResourceUtils() {
    }

    public static String getType(Resource resource) {
        List<Capability> caps = resource.getCapabilities(null);
        for (Capability cap : caps) {
            if (cap.getNamespace().equals(IDENTITY_NAMESPACE)) {
                return cap.getAttributes().get(CAPABILITY_TYPE_ATTRIBUTE).toString();
            }
        }
        return null;
    }

    // TODO: Correct name should probably be toUrl
    public static String getUri(Resource resource) {
        List<Capability> caps = resource.getCapabilities(null);
        String location = null;
        String url = null;
        for (Capability cap : caps) {
            if (cap.getNamespace().equals(XmlRepository.REPO_NAMESPACE)) {
                location = cap.getAttributes().get(CAPABILITY_URL_ATTRIBUTE).toString();
            }
            if (cap.getNamespace().equals(CONTENT_NAMESPACE)) {
                url = cap.getAttributes().get(CAPABILITY_URL_ATTRIBUTE).toString();
            }
        }

        // If there is no location value, then just return the saved URL
        if (location == null) {
            return url;
        }

        // If the saved is absolute (i.e. well-formed), then return it
        try {
            new URL(url);
            return url;
        } catch (MalformedURLException e) {
            // The URL is relative, so return the absolute URL 
            return location + url;
        }
    }

    public static String getFeatureId(Resource resource) {
        List<Capability> caps = resource.getCapabilities(null);
        for (Capability cap : caps) {
            if (cap.getNamespace().equals(IDENTITY_NAMESPACE)) {
                Map<String, Object> attributes = cap.getAttributes();
                if (TYPE_FEATURE.equals(attributes.get(CAPABILITY_TYPE_ATTRIBUTE))) {
                    String name = (String) attributes.get(IDENTITY_NAMESPACE);
                    Version version = (Version) attributes.get(CAPABILITY_VERSION_ATTRIBUTE);
                    return version != null ? name + "/" + version : name;
                }
            }
        }
        return null;
    }

    public static RequirementImpl addIdentityRequirement(ResourceImpl resource, String name, String type, String range) {
        return addIdentityRequirement(resource, name, type, range, true);
    }

    public static RequirementImpl addIdentityRequirement(ResourceImpl resource, String name, String type, String range, boolean mandatory) {
        return addIdentityRequirement(resource, name, type, range != null ? new VersionRange(range) : null, mandatory);
    }

    public static RequirementImpl addIdentityRequirement(ResourceImpl resource, String name, String type, VersionRange range) {
        return addIdentityRequirement(resource, name, type, range, true);
    }

    public static RequirementImpl addIdentityRequirement(ResourceImpl resource, String name, String type, VersionRange range, boolean mandatory) {
        Map<String, String> dirs = new HashMap<>();
        Map<String, Object> attrs = new HashMap<>();
        if (!mandatory) {
            dirs.put(REQUIREMENT_RESOLUTION_DIRECTIVE, RESOLUTION_OPTIONAL);
        }
        if (name != null) {
            attrs.put(IDENTITY_NAMESPACE, name);
        }
        if (type != null) {
            attrs.put(CAPABILITY_TYPE_ATTRIBUTE, type);
        }
        if (range != null) {
            attrs.put(CAPABILITY_VERSION_ATTRIBUTE, range);
        }
        RequirementImpl requirement = new RequirementImpl(resource, IDENTITY_NAMESPACE, dirs, attrs);
        resource.addRequirement(requirement);
        return requirement;
    }

    public static void addIdentityRequirement(ResourceImpl resource, Resource required) {
        addIdentityRequirement(resource, required, true);
    }

    public static void addIdentityRequirement(ResourceImpl resource, Resource required, boolean mandatory) {
        for (Capability cap : required.getCapabilities(null)) {
            if (cap.getNamespace().equals(IDENTITY_NAMESPACE)) {
                Map<String, Object> attributes = cap.getAttributes();
                Map<String, String> dirs = new HashMap<>();
                dirs.put(REQUIREMENT_RESOLUTION_DIRECTIVE, mandatory ? RESOLUTION_MANDATORY : RESOLUTION_OPTIONAL);
                Map<String, Object> attrs = new HashMap<>();
                attrs.put(IDENTITY_NAMESPACE, attributes.get(IDENTITY_NAMESPACE));
                attrs.put(CAPABILITY_TYPE_ATTRIBUTE, attributes.get(CAPABILITY_TYPE_ATTRIBUTE));
                Version version = (Version) attributes.get(CAPABILITY_VERSION_ATTRIBUTE);
                if (version != null) {
                    attrs.put(CAPABILITY_VERSION_ATTRIBUTE, new VersionRange(version, true));
                }
                resource.addRequirement(new RequirementImpl(resource, IDENTITY_NAMESPACE, dirs, attrs));
            }
        }
    }

    public static String toFeatureRequirement(String feature) {
        String[] parts = feature.split("/");
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(IDENTITY_NAMESPACE, parts[0]);
        attrs.put(CAPABILITY_TYPE_ATTRIBUTE, TYPE_FEATURE);
        if (parts.length > 1) {
            attrs.put(CAPABILITY_VERSION_ATTRIBUTE, new VersionRange(parts[1]));
        }
        Map<String, String> dirs = new HashMap<>();
        dirs.put(Constants.FILTER_DIRECTIVE, SimpleFilter.convert(attrs).toString());
        return new RequirementImpl(null, IDENTITY_NAMESPACE, dirs, attrs).toString();
    }

    public static String toFeatureCapability(String feature) {
        String[] parts = feature.split("/");
        Map<String, String> dirs = new HashMap<>();
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(IDENTITY_NAMESPACE, parts[0]);
        attrs.put(CAPABILITY_TYPE_ATTRIBUTE, TYPE_FEATURE);
        if (parts.length > 1) {
            attrs.put(CAPABILITY_VERSION_ATTRIBUTE, VersionTable.getVersion(parts[1]));
        }
        return new CapabilityImpl(null, IDENTITY_NAMESPACE, dirs, attrs).toString();
    }
}
