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
package org.apache.karaf.features.internal.region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.resolver.Util;
import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Conditional;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.ScopeFilter;
import org.apache.karaf.features.internal.download.DownloadCallback;
import org.apache.karaf.features.internal.download.DownloadManager;
import org.apache.karaf.features.internal.download.Downloader;
import org.apache.karaf.features.internal.download.StreamProvider;
import org.apache.karaf.features.internal.resolver.FeatureResource;
import org.apache.karaf.features.internal.resolver.RequirementImpl;
import org.apache.karaf.features.internal.resolver.ResourceBuilder;
import org.apache.karaf.features.internal.resolver.ResourceImpl;
import org.apache.karaf.features.internal.resolver.ResourceUtils;
import org.apache.karaf.features.internal.service.Overrides;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import static org.apache.karaf.features.internal.resolver.ResourceUtils.TYPE_FEATURE;
import static org.apache.karaf.features.internal.resolver.ResourceUtils.TYPE_SUBSYSTEM;
import static org.apache.karaf.features.internal.resolver.ResourceUtils.addIdentityRequirement;
import static org.apache.karaf.features.internal.resolver.ResourceUtils.getUri;
import static org.apache.karaf.features.internal.util.MapUtils.addToMapSet;
import static org.eclipse.equinox.region.RegionFilter.VISIBLE_ALL_NAMESPACE;
import static org.osgi.framework.namespace.IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE;
import static org.osgi.framework.namespace.IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE;
import static org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE;

public class Subsystem extends ResourceImpl {

    private static final String ALL_FILTER = "(|(!(all=*))(all=*))";

    private static final String SUBSYSTEM_FILTER = String.format("(%s=%s)", CAPABILITY_TYPE_ATTRIBUTE, TYPE_SUBSYSTEM);

    private static final String FEATURE_FILTER = String.format("(%s=%s)", CAPABILITY_TYPE_ATTRIBUTE, TYPE_FEATURE);

    private static final String SUBSYSTEM_OR_FEATURE_FILTER = String.format("(|%s%s)", SUBSYSTEM_FILTER, FEATURE_FILTER);

    // Everything is visible
    private static final Map<String, Set<String>> SHARE_ALL_POLICY =
            Collections.singletonMap(
                    VISIBLE_ALL_NAMESPACE,
                    Collections.singleton(ALL_FILTER));

    // Nothing (but systems) is visible
    private static final Map<String, Set<String>> SHARE_NONE_POLICY =
            Collections.singletonMap(
                    IDENTITY_NAMESPACE,
                    Collections.singleton(SUBSYSTEM_FILTER));

    private final String name;
    private final boolean acceptDependencies;
    private final Subsystem parent;
    private final Feature feature;
    private final List<Subsystem> children = new ArrayList<Subsystem>();
    private final Map<String, Set<String>> importPolicy;
    private final Map<String, Set<String>> exportPolicy;
    private final List<Resource> installable = new ArrayList<Resource>();
    private final Map<String, DependencyInfo> dependencies = new HashMap<String, DependencyInfo>();

    public Subsystem(String name) {
        super(name, TYPE_SUBSYSTEM, Version.emptyVersion);
        this.name = name;
        this.parent = null;
        this.acceptDependencies = true;
        this.feature = null;
        this.importPolicy = SHARE_NONE_POLICY;
        this.exportPolicy = SHARE_NONE_POLICY;
    }

    public Subsystem(String name, Feature feature, Subsystem parent) {
        super(name, TYPE_SUBSYSTEM, Version.emptyVersion);
        this.name = name;
        this.parent = parent;
        this.acceptDependencies = feature.getScoping() != null && feature.getScoping().acceptDependencies();
        this.feature = feature;
        if (feature.getScoping() != null) {
            this.importPolicy = createPolicy(feature.getScoping().getImports());
            this.importPolicy.put(IDENTITY_NAMESPACE, Collections.singleton(SUBSYSTEM_OR_FEATURE_FILTER));
            this.exportPolicy = createPolicy(feature.getScoping().getExports());
            this.exportPolicy.put(IDENTITY_NAMESPACE, Collections.singleton(SUBSYSTEM_OR_FEATURE_FILTER));
        } else {
            this.importPolicy = SHARE_ALL_POLICY;
            this.exportPolicy = SHARE_ALL_POLICY;
        }

        Map<String, String> dirs = new HashMap<String, String>();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(IDENTITY_NAMESPACE, feature.getName());
        attrs.put(CAPABILITY_TYPE_ATTRIBUTE, TYPE_FEATURE);
        attrs.put(CAPABILITY_VERSION_ATTRIBUTE, new VersionRange(VersionTable.getVersion(feature.getVersion()), true));
        Requirement requirement = new RequirementImpl(this, IDENTITY_NAMESPACE, dirs, attrs);
        addRequirement(requirement);
    }

    public Subsystem(String name, Subsystem parent, boolean acceptDependencies) {
        super(name, TYPE_SUBSYSTEM, Version.emptyVersion);
        this.name = name;
        this.parent = parent;
        this.acceptDependencies = acceptDependencies;
        this.feature = null;
        this.importPolicy = SHARE_ALL_POLICY;
        this.exportPolicy = SHARE_NONE_POLICY;
    }

    public List<Resource> getInstallable() {
        return installable;
    }

    public String getName() {
        return name;
    }

    public Subsystem getParent() {
        return parent;
    }

    public Collection<Subsystem> getChildren() {
        return children;
    }

    public Subsystem getChild(String name) {
        for (Subsystem child : children) {
            if (child.getName().equals(name)) {
                return child;
            }
        }
        return null;
    }

    public boolean isAcceptDependencies() {
        return acceptDependencies;
    }

    public Map<String, Set<String>> getImportPolicy() {
        return importPolicy;
    }

    public Map<String, Set<String>> getExportPolicy() {
        return exportPolicy;
    }

    public Feature getFeature() {
        return feature;
    }

    public Subsystem createSubsystem(String name, boolean acceptDependencies) {
        if (feature != null) {
            throw new UnsupportedOperationException("Can not create application subsystems inside a feature subsystem");
        }
        // Create subsystem
        Subsystem as = new Subsystem(getName() + "/" + name, this, acceptDependencies);
        children.add(as);
        // Add a requirement to force its resolution
        Capability identity = as.getCapabilities(IDENTITY_NAMESPACE).iterator().next();
        Object bsn = identity.getAttributes().get(IDENTITY_NAMESPACE);
        Requirement requirement = new RequirementImpl(this, IDENTITY_NAMESPACE,
                Collections.<String,String>emptyMap(),
                Collections.singletonMap(IDENTITY_NAMESPACE, bsn));
        addRequirement(requirement);
        // Add it to repo
        installable.add(as);
        return as;
    }

    public void addSystemResource(Resource resource) {
        installable.add(resource);
    }

    public void requireFeature(String name, String range) {
        ResourceUtils.addIdentityRequirement(this, name, TYPE_FEATURE, range);
    }

    public Map<String, BundleInfo> getBundleInfos() {
        Map<String, BundleInfo> infos = new HashMap<String, BundleInfo>();
        for (DependencyInfo di : dependencies.values()) {
            infos.put(di.getLocation(), di);
        }
        return infos;
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public void preResolve(Collection<Feature> features,
                           DownloadManager manager,
                           Set<String> overrides,
                           String featureResolutionRange) throws Exception {
        for (Subsystem child : children) {
            child.preResolve(features, manager, overrides, featureResolutionRange);
        }
        List<Requirement> processed = new ArrayList<Requirement>();
        while (true) {
            List<Requirement> requirements = getRequirements(IDENTITY_NAMESPACE);
            requirements.removeAll(processed);
            if (requirements.isEmpty()) {
                break;
            }
            for (Requirement requirement : requirements) {
                String name = (String) requirement.getAttributes().get(IDENTITY_NAMESPACE);
                String type = (String) requirement.getAttributes().get(CAPABILITY_TYPE_ATTRIBUTE);
                VersionRange range = (VersionRange) requirement.getAttributes().get(CAPABILITY_VERSION_ATTRIBUTE);
                if (TYPE_FEATURE.equals(type)) {
                    for (Feature feature : features) {
                        if (feature.getName().equals(name)
                                && (range == null || range.contains(VersionTable.getVersion(feature.getVersion())))) {
                            if (feature != this.feature) {
                                String ssName = this.name + "#" + (feature.hasVersion() ? feature.getName() + "-" + feature.getVersion() : feature.getName());
                                Subsystem fs = getChild(ssName);
                                if (fs == null) {
                                    fs = new Subsystem(ssName, feature, this);
                                    fs.preResolve(features, manager, overrides, featureResolutionRange);
                                    installable.add(fs);
                                    children.add(fs);
                                }
                            }
                        }
                    }
                }
                processed.add(requirement);
            }
        }
        if (feature != null) {
            final Map<String, ResourceImpl> bundles = new ConcurrentHashMap<String, ResourceImpl>();
            final Downloader downloader = manager.createDownloader();
            final Map<BundleInfo, Boolean> infos = new HashMap<BundleInfo, Boolean>();
            for (Conditional cond : feature.getConditional()) {
                for (final BundleInfo bi : cond.getBundles()) {
                    infos.put(bi, false);
                }
            }
            for (BundleInfo bi : feature.getBundles()) {
                infos.put(bi, true);
            }
            for (Map.Entry<BundleInfo, Boolean> entry : infos.entrySet()) {
                final BundleInfo bi = entry.getKey();
                final String loc = bi.getLocation();
                downloader.download(loc, new DownloadCallback() {
                    @Override
                    public void downloaded(StreamProvider provider) throws Exception {
                        ResourceImpl res = createResource(loc, provider.getMetadata());
                        bundles.put(loc, res);
                    }
                });
            }
            for (String override : overrides) {
                final String loc = Overrides.extractUrl(override);
                downloader.download(loc, new DownloadCallback() {
                    @Override
                    public void downloaded(StreamProvider provider) throws Exception {
                        ResourceImpl res = createResource(loc, provider.getMetadata());
                        bundles.put(loc, res);
                    }
                });
            }
            downloader.await();
            Overrides.override(bundles, overrides);
            for (Map.Entry<BundleInfo, Boolean> entry : infos.entrySet()) {
                final BundleInfo bi = entry.getKey();
                final String loc = bi.getLocation();
                final boolean mandatory = entry.getValue();
                ResourceImpl res = bundles.get(loc);
                if (bi.isDependency()) {
                    addDependency(res, false, bi.isStart(), bi.getStartLevel());
                } else {
                    doAddDependency(res, mandatory, bi.isStart(), bi.getStartLevel());
                }
            }
            for (Dependency dep : feature.getDependencies()) {
                Subsystem ss = this;
                while (!ss.isAcceptDependencies()) {
                    ss = ss.getParent();
                }
                ss.requireFeature(dep.getName(), dep.getVersion());
            }
            for (Conditional cond : feature.getConditional()) {
                FeatureResource resCond = FeatureResource.build(feature, cond, featureResolutionRange, bundles);
                addIdentityRequirement(this, resCond, false);
                installable.add(resCond);
            }

            FeatureResource res = FeatureResource.build(feature, featureResolutionRange, bundles);
            addIdentityRequirement(res, this);
            installable.add(res);
        }
        // Compute dependencies
        for (DependencyInfo info : dependencies.values()) {
            installable.add(info.resource);
            addIdentityRequirement(info.resource, this, info.mandatory);
       }
    }

    void addDependency(ResourceImpl resource, boolean mandatory, boolean start, int startLevel) {
        if (isAcceptDependencies()) {
            doAddDependency(resource, mandatory, start, startLevel);
        } else {
            parent.addDependency(resource, mandatory, start, startLevel);
        }
    }

    private void doAddDependency(ResourceImpl resource, boolean mandatory, boolean start, int startLevel) {
        String id = Util.getSymbolicName(resource) + "|" + Util.getVersion(resource);
        DependencyInfo info = dependencies.get(id);
        if (info == null) {
            info = new DependencyInfo();
            dependencies.put(id, info);
        }
        info.resource = resource;
        info.mandatory |= mandatory;
        info.start |= start;
        if (info.startLevel > 0 && startLevel > 0) {
            info.startLevel = Math.min(info.startLevel, startLevel);
        } else {
            info.startLevel = Math.max(info.startLevel, startLevel);
        }
    }

    class DependencyInfo implements BundleInfo {
        ResourceImpl resource;
        boolean mandatory;
        boolean start;
        int startLevel;

        @Override
        public boolean isStart() {
            return start;
        }

        @Override
        public int getStartLevel() {
            return startLevel;
        }

        @Override
        public String getLocation() {
            return getUri(resource);
        }

        @Override
        public boolean isDependency() {
            return !mandatory;
        }
    }

    Map<String, Set<String>> createPolicy(List<? extends ScopeFilter> filters) {
        Map<String, Set<String>> policy = new HashMap<String, Set<String>>();
        for (ScopeFilter filter : filters) {
            addToMapSet(policy, filter.getNamespace(), filter.getFilter());
        }
        return policy;
    }

    ResourceImpl createResource(String uri, Map<String, String> headers) throws Exception {
        try {
            return ResourceBuilder.build(uri, headers);
        } catch (BundleException e) {
            throw new Exception("Unable to create resource for bundle " + uri, e);
        }
    }

    @Override
    public String toString() {
        return getName();
    }
}
