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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Conditional;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.internal.util.Macro;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

/**
*/
public class FeatureResource extends ResourceImpl {

    private final Feature feature;

    public static Resource build(Feature feature, Conditional conditional, String featureRange, Map<String, Resource> locToRes) throws BundleException {
        Feature fcond = conditional.asFeature(feature.getName(), feature.getVersion());
        FeatureResource resource = (FeatureResource) build(fcond, featureRange, locToRes);
        for (Dependency dep : conditional.getCondition()) {
            addDependency(resource, dep, featureRange);
        }
        org.apache.karaf.features.internal.model.Dependency dep = new org.apache.karaf.features.internal.model.Dependency();
        dep.setName(feature.getName());
        dep.setVersion(feature.getVersion());
        addDependency(resource, dep, featureRange);
        return resource;
    }

    public static Resource build(Feature feature, String featureRange, Map<String, Resource> locToRes) throws BundleException {
        FeatureResource resource = new FeatureResource(feature);
        Map<String, String> dirs = new HashMap<String, String>();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(FeatureNamespace.FEATURE_NAMESPACE, feature.getName());
        attrs.put(FeatureNamespace.CAPABILITY_VERSION_ATTRIBUTE, VersionTable.getVersion(feature.getVersion()));
        resource.addCapability(new CapabilityImpl(resource, FeatureNamespace.FEATURE_NAMESPACE, dirs, attrs));
        for (BundleInfo info : feature.getBundles()) {
            if (!info.isDependency()) {
                Resource res = locToRes.get(info.getLocation());
                if (res == null) {
                    throw new IllegalStateException("Resource not found for url " + info.getLocation());
                }
                List<Capability> caps = res.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
                if (caps.size() != 1) {
                    throw new IllegalStateException("Resource does not have a single " + IdentityNamespace.IDENTITY_NAMESPACE + " capability");
                }
                dirs = new HashMap<String, String>();
                attrs = new HashMap<String, Object>();
                attrs.put(IdentityNamespace.IDENTITY_NAMESPACE, caps.get(0).getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE));
                attrs.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, caps.get(0).getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE));
                attrs.put(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, new VersionRange((Version) caps.get(0).getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE), true));
                resource.addRequirement(new RequirementImpl(resource, IdentityNamespace.IDENTITY_NAMESPACE, dirs, attrs));
            }
        }
        for (Dependency dep : feature.getDependencies()) {
            addDependency(resource, dep, featureRange);
        }
        for (org.apache.karaf.features.Capability cap : feature.getCapabilities()) {
            resource.addCapabilities(ResourceBuilder.parseCapability(resource, cap.getValue()));
        }
        for (org.apache.karaf.features.Requirement req : feature.getRequirements()) {
            resource.addRequirements(ResourceBuilder.parseRequirement(resource, req.getValue()));
        }
        return resource;
    }

    protected static void addDependency(FeatureResource resource, Dependency dep, String featureRange) {
        Map<String, String> dirs;
        Map<String, Object> attrs;
        String name = dep.getName();
        String version = dep.getVersion();
        if (version.equals("0.0.0")) {
            version = null;
        } else if (!version.startsWith("[") && !version.startsWith("(")) {
            version = Macro.transform(featureRange, version);
        }
        dirs = new HashMap<String, String>();
        attrs = new HashMap<String, Object>();
        attrs.put(FeatureNamespace.FEATURE_NAMESPACE, name);
        if (version != null) {
            attrs.put(FeatureNamespace.CAPABILITY_VERSION_ATTRIBUTE, new VersionRange(version));
        }
        resource.addRequirement(new RequirementImpl(resource, FeatureNamespace.FEATURE_NAMESPACE, dirs, attrs));
    }

    public FeatureResource(Feature feature) {
        super(feature.getName(), FeatureNamespace.TYPE_FEATURE, VersionTable.getVersion(feature.getVersion()));
        this.feature = feature;
    }

    public Feature getFeature() {
        return feature;
    }
}
