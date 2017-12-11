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
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import static org.apache.karaf.features.internal.resolver.ResourceUtils.TYPE_FEATURE;
import static org.apache.karaf.features.internal.resolver.ResourceUtils.addIdentityRequirement;

/**
 * <p>An OSGi {@link Resource} representing Karaf feature. It has requirements on all its non-dependency
 * (<code>dependency="false"</code>) bundles.</p>
 * <p>It'll also use arbitrary capabilities ({@code <feature>/<capability>}) and requirements
 * ({@code <feature>/<requirement>}).</p>
 * <p>Dependant features ({@code <feature>/<feature>}) without <code>dependency="true"</code> will also be added
 * as <code>osgi.identity</code> requirements with <code>type=karaf.feature</code>.</p>
 */
public final class FeatureResource extends ResourceImpl {

    public static final String REQUIREMENT_CONDITIONAL_DIRECTIVE = "condition";
    public static final String CONDITIONAL_TRUE = "true";
    private final Feature feature;

    private FeatureResource(Feature feature) {
        super(feature.getName(), TYPE_FEATURE, VersionTable.getVersion(feature.getVersion()));
        this.feature = feature;
    }

    /**
     * Constructs a {@link Resource} for conditional of a feature
     * @param feature
     * @param conditional
     * @param featureRange
     * @param locToRes
     * @return
     * @throws BundleException
     */
    public static FeatureResource build(Feature feature, Conditional conditional, String featureRange, Map<String, ? extends Resource> locToRes) throws BundleException {
        Feature fcond = conditional.asFeature();
        FeatureResource resource = build(fcond, featureRange, locToRes);
        for (String cond : conditional.getCondition()) {
            if (cond.startsWith("req:")) {
                // <conditional>/<condition>req:xxx</condition>
                // conditional feature will require all its bundles and will have all declared, generic
                // requirements
                cond = cond.substring("req:".length());
                List<Requirement> reqs = ResourceBuilder.parseRequirement(resource, cond);
                resource.addRequirements(reqs);
            } else {
                // <conditional>/<condition>xxx</condition>
                // conditional feature will require all its bundles and will require the features that are the
                // conditions with "condition:=true" directive
                org.apache.karaf.features.internal.model.Dependency dep = new org.apache.karaf.features.internal.model.Dependency();
                String[] p = cond.split("/");
                dep.setName(p[0]);
                if (p.length > 1) {
                    dep.setVersion(p[1]);
                }
                addDependency(resource, dep, featureRange, true);
            }
        }
        org.apache.karaf.features.internal.model.Dependency dep = new org.apache.karaf.features.internal.model.Dependency();
        dep.setName(feature.getName());
        dep.setVersion(feature.getVersion());
        // conditional feature will also require parent feature - also with "condition:=true" directive
        addDependency(resource, dep, featureRange, true);
        return resource;
    }

    /**
     * Constructs {@link Resource} for given non-conditional feature.
     * @param feature
     * @param featureRange
     * @param locToRes
     * @return
     * @throws BundleException
     */
    public static FeatureResource build(Feature feature, String featureRange, Map<String, ? extends Resource> locToRes) throws BundleException {
        FeatureResource resource = new FeatureResource(feature);
        for (BundleInfo info : feature.getBundles()) {
            if (!info.isDependency() && !info.isBlacklisted()) {
                Resource res = locToRes.get(info.getLocation());
                if (res == null) {
                    throw new IllegalStateException("Resource not found for url " + info.getLocation());
                }
                addIdentityRequirement(resource, res);
            }
        }
        for (Dependency dep : feature.getDependencies()) {
            if (!dep.isDependency()) {
                addDependency(resource, dep, featureRange);
            }
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
        addDependency(resource, dep, featureRange, false);
    }

    protected static void addDependency(FeatureResource resource, Dependency dep, String featureRange, boolean condition) {
        String name = dep.getName();
        String version = dep.getVersion();
        if (version.equals("0.0.0")) {
            version = null;
        } else if (!version.startsWith("[") && !version.startsWith("(")) {
            version = Macro.transform(featureRange, version);
        }
        addIdentityRequirement(resource, name, TYPE_FEATURE, version != null ? new VersionRange(version) : null, true, condition);
    }

    public Feature getFeature() {
        return feature;
    }
}
