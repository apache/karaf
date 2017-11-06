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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Conditional;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesNamespaces;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Library;
import org.apache.karaf.features.ScopeFilter;
import org.apache.karaf.features.internal.download.DownloadManager;
import org.apache.karaf.features.internal.download.Downloader;
import org.apache.karaf.features.internal.download.StreamProvider;
import org.apache.karaf.features.internal.repository.BaseRepository;
import org.apache.karaf.features.internal.resolver.CapabilityImpl;
import org.apache.karaf.features.internal.resolver.FeatureResource;
import org.apache.karaf.features.internal.resolver.RequirementImpl;
import org.apache.karaf.features.internal.resolver.ResolverUtil;
import org.apache.karaf.features.internal.resolver.ResourceBuilder;
import org.apache.karaf.features.internal.resolver.ResourceImpl;
import org.apache.karaf.features.internal.resolver.ResourceUtils;
import org.apache.karaf.features.internal.resolver.SimpleFilter;
import org.apache.karaf.features.internal.service.Overrides;
import org.apache.karaf.features.internal.util.StringArrayMap;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import static java.util.jar.JarFile.MANIFEST_NAME;
import static org.apache.karaf.features.internal.resolver.ResourceUtils.TYPE_FEATURE;
import static org.apache.karaf.features.internal.resolver.ResourceUtils.TYPE_SUBSYSTEM;
import static org.apache.karaf.features.internal.resolver.ResourceUtils.addIdentityRequirement;
import static org.apache.karaf.features.internal.resolver.ResourceUtils.getUri;
import static org.apache.karaf.features.internal.resolver.ResourceUtils.toFeatureRequirement;
import static org.apache.karaf.features.internal.util.MapUtils.addToMapSet;
import static org.eclipse.equinox.region.RegionFilter.VISIBLE_ALL_NAMESPACE;
import static org.osgi.framework.namespace.IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE;
import static org.osgi.framework.namespace.IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE;
import static org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE;
import static org.osgi.resource.Namespace.REQUIREMENT_FILTER_DIRECTIVE;

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
    private final boolean mandatory;
    private final List<Subsystem> children = new ArrayList<>();
    private final Map<String, Set<String>> importPolicy;
    private final Map<String, Set<String>> exportPolicy;
    private final List<Resource> installable = new ArrayList<>();
    private final Map<String, DependencyInfo> dependencies = new HashMap<>();
    private final List<Requirement> dependentFeatures = new ArrayList<>();

    private final List<String> bundles = new ArrayList<>();

    public Subsystem(String name) {
        super(name, TYPE_SUBSYSTEM, Version.emptyVersion);
        this.name = name;
        this.parent = null;
        this.acceptDependencies = true;
        this.feature = null;
        this.importPolicy = SHARE_NONE_POLICY;
        this.exportPolicy = SHARE_NONE_POLICY;
        this.mandatory = true;
    }

    public Subsystem(String name, Feature feature, Subsystem parent, boolean mandatory) {
        super(name, TYPE_SUBSYSTEM, Version.emptyVersion);
        this.name = name;
        this.parent = parent;
        this.acceptDependencies = feature.getScoping() != null && feature.getScoping().acceptDependencies();
        this.feature = feature;
        this.mandatory = mandatory;
        if (feature.getScoping() != null) {
            this.importPolicy = createPolicy(feature.getScoping().getImports());
            this.importPolicy.put(IDENTITY_NAMESPACE, Collections.singleton(SUBSYSTEM_OR_FEATURE_FILTER));
            this.exportPolicy = createPolicy(feature.getScoping().getExports());
            this.exportPolicy.put(IDENTITY_NAMESPACE, Collections.singleton(SUBSYSTEM_OR_FEATURE_FILTER));
        } else {
            this.importPolicy = SHARE_ALL_POLICY;
            this.exportPolicy = SHARE_ALL_POLICY;
        }

        addIdentityRequirement(this,
                feature.getName(),
                TYPE_FEATURE,
                new VersionRange(VersionTable.getVersion(feature.getVersion()), true));
    }

    public Subsystem(String name, Subsystem parent, boolean acceptDependencies, boolean mandatory) {
        super(name, TYPE_SUBSYSTEM, Version.emptyVersion);
        this.name = name;
        this.parent = parent;
        this.acceptDependencies = acceptDependencies;
        this.feature = null;
        this.mandatory = mandatory;
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
        String childName = getName() + "/" + name;
        Subsystem as = new Subsystem(childName, this, acceptDependencies, true);
        children.add(as);
        // Add a requirement to force its resolution
        ResourceUtils.addIdentityRequirement(this, childName, TYPE_SUBSYSTEM, (VersionRange) null);
        // Add it to repo
        installable.add(as);
        return as;
    }

    public void addSystemResource(Resource resource) {
        installable.add(resource);
    }

    public void requireFeature(String name, String range, boolean mandatory) {
        if (mandatory) {
            ResourceUtils.addIdentityRequirement(this, name, TYPE_FEATURE, range);
        } else {
            ResourceImpl res = new ResourceImpl();
            ResourceUtils.addIdentityRequirement(res, name, TYPE_FEATURE, range, false);
            dependentFeatures.addAll(res.getRequirements(null));
        }
    }

    public void require(String requirement) throws BundleException {
        int idx = requirement.indexOf(':');
        String type, req;
        if (idx >= 0) {
            type = requirement.substring(0, idx);
            req = requirement.substring(idx + 1);
        } else {
            type = "feature";
            req = requirement;
        }
        switch (type) {
        case "feature":
            addRequirement(toFeatureRequirement(req));
            break;
        case "requirement":
            addRequirement(req);
            break;
        case "bundle":
            bundles.add(req);
            break;
        }
    }

    protected void addRequirement(String requirement) throws BundleException {
        for (Requirement req : ResourceBuilder.parseRequirement(this, requirement)) {
            Object range = req.getAttributes().get(CAPABILITY_VERSION_ATTRIBUTE);
            if (range instanceof String) {
                req.getAttributes().put(CAPABILITY_VERSION_ATTRIBUTE, new VersionRange((String) range));
            }
            addRequirement(req);
        }
    }

    public Map<String, BundleInfo> getBundleInfos() {
        Map<String, BundleInfo> infos = new HashMap<>();
        for (DependencyInfo di : dependencies.values()) {
            infos.put(di.getLocation(), di);
        }
        return infos;
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public void build(Collection<Feature> features) throws Exception {
        doBuild(features, true);
    }

    private void doBuild(Collection<Feature> features, boolean mandatory) throws Exception {
        for (Subsystem child : children) {
            child.doBuild(features, true);
        }
        if (feature != null) {
            for (Dependency dep : feature.getDependencies()) {
                Subsystem ss = this;
                while (!ss.isAcceptDependencies()) {
                    ss = ss.getParent();
                }
                ss.requireFeature(dep.getName(), dep.getVersion(), false);
            }
            for (Conditional cond : feature.getConditional()) {
                Feature fcond = cond.asFeature();
                String ssName = this.name + "#" + (fcond.hasVersion() ? fcond.getName() + "-" + fcond.getVersion() : fcond.getName());
                Subsystem fs = getChild(ssName);
                if (fs == null) {
                    fs = new Subsystem(ssName, fcond, this, true);
                    fs.doBuild(features, false);
                    installable.add(fs);
                    children.add(fs);
                }
            }
        }
        List<Requirement> processed = new ArrayList<>();
        while (true) {
            List<Requirement> requirements = getRequirements(IDENTITY_NAMESPACE);
            requirements.addAll(dependentFeatures);
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
                                    fs = new Subsystem(ssName, feature, this, mandatory && !SubsystemResolveContext.isOptional(requirement));
                                    fs.build(features);
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
    }

    public Set<String> collectPrerequisites() {
        Set<String> prereqs = new HashSet<>();
        doCollectPrerequisites(prereqs);
        return prereqs;
    }

    private void doCollectPrerequisites(Set<String> prereqs) {
        for (Subsystem child : children) {
            child.doCollectPrerequisites(prereqs);
        }
        if (feature != null) {
            boolean match = false;
            for (String prereq : prereqs) {
                String[] p = prereq.split("/");
                if (feature.getName().equals(p[0])
                        && VersionRange.parseVersionRange(p[1]).contains(Version.parseVersion(feature.getVersion()))) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                for (Dependency dep : feature.getDependencies()) {
                    if (dep.isPrerequisite()) {
                        prereqs.add(dep.toString());
                    }
                }
            }
        }
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public void downloadBundles(DownloadManager manager,
                                Set<String> overrides,
                                String featureResolutionRange,
                                final String serviceRequirements,
                                RepositoryManager repos) throws Exception {
        for (Subsystem child : children) {
            child.downloadBundles(manager, overrides, featureResolutionRange, serviceRequirements, repos);
        }
        final Map<String, ResourceImpl> bundles = new ConcurrentHashMap<>();
        final Downloader downloader = manager.createDownloader();
        final Map<BundleInfo, Conditional> infos = new HashMap<>();
        if (feature != null) {
            for (Conditional cond : feature.getConditional()) {
                for (final BundleInfo bi : cond.getBundles()) {
                    infos.put(bi, cond);
                }
            }
            for (BundleInfo bi : feature.getBundles()) {
                infos.put(bi, null);
            }
        }
        boolean removeServiceRequirements;
        if (FeaturesService.SERVICE_REQUIREMENTS_DISABLE.equals(serviceRequirements)) {
            removeServiceRequirements = true;
        } else if (feature != null && FeaturesService.SERVICE_REQUIREMENTS_DEFAULT.equals(serviceRequirements)) {
            removeServiceRequirements = FeaturesNamespaces.URI_1_0_0.equals(feature.getNamespace())
                                     || FeaturesNamespaces.URI_1_1_0.equals(feature.getNamespace())
                                     || FeaturesNamespaces.URI_1_2_0.equals(feature.getNamespace())
                                     || FeaturesNamespaces.URI_1_2_1.equals(feature.getNamespace());
        } else {
            removeServiceRequirements = false;
        }
        for (Map.Entry<BundleInfo, Conditional> entry : infos.entrySet()) {
            final BundleInfo bi = entry.getKey();
            final String loc = bi.getLocation();
            downloader.download(loc, provider -> {
                bundles.put(loc, createResource(loc, getMetadata(provider), removeServiceRequirements));
            });
        }
        for (Clause bundle : Parser.parseClauses(this.bundles.toArray(new String[this.bundles.size()]))) {
            final String loc = bundle.getName();
            downloader.download(loc, provider -> {
                bundles.put(loc, createResource(loc, getMetadata(provider), removeServiceRequirements));
            });
        }
        for (String override : overrides) {
            final String loc = Overrides.extractUrl(override);
            downloader.download(loc, provider -> {
                bundles.put(loc, createResource(loc, getMetadata(provider), removeServiceRequirements));
            });
        }
        if (feature != null) {
            for (Library library : feature.getLibraries()) {
                if (library.isExport()) {
                    final String loc = library.getLocation();
                    downloader.download(loc, provider -> {
                        bundles.put(loc, createResource(loc, getMetadata(provider), removeServiceRequirements));
                    });
                }
            }
        }
        downloader.await();
        Overrides.override(bundles, overrides);
        if (feature != null) {
            // Add conditionals
            Map<Conditional, Resource> resConds = new HashMap<>();
            for (Conditional cond : feature.getConditional()) {
                FeatureResource resCond = FeatureResource.build(feature, cond, featureResolutionRange, bundles);
                addIdentityRequirement(this, resCond, false);
                addIdentityRequirement(resCond, this, true);
                installable.add(resCond);
                resConds.put(cond, resCond);
            }
            // Add features
            FeatureResource resFeature = FeatureResource.build(feature, featureResolutionRange, bundles);
            addIdentityRequirement(resFeature, this);
            installable.add(resFeature);
            // Add dependencies
            for (Map.Entry<BundleInfo, Conditional> entry : infos.entrySet()) {
                final BundleInfo bi = entry.getKey();
                final String loc = bi.getLocation();
                final Conditional cond = entry.getValue();
                ResourceImpl res = bundles.get(loc);
                int sl = bi.getStartLevel() <= 0 ? feature.getStartLevel() : bi.getStartLevel();
                if (cond != null) {
                    addIdentityRequirement(res, resConds.get(cond), true);
                }
                boolean mandatory = !bi.isDependency() && cond == null;
                if (bi.isDependency()) {
                    addDependency(res, mandatory, bi.isStart(), sl, bi.isBlacklisted());
                } else {
                    doAddDependency(res, mandatory, bi.isStart(), sl, bi.isBlacklisted());
                }
            }
            for (Library library : feature.getLibraries()) {
                if (library.isExport()) {
                    final String loc = library.getLocation();
                    ResourceImpl res = bundles.get(loc);
                    addDependency(res, false, false, 0, false);
                }
            }
            for (String uri : feature.getResourceRepositories()) {
                BaseRepository repo = repos.getRepository(feature.getRepositoryUrl(), uri);
                for (Resource resource : repo.getResources()) {
                    ResourceImpl res = cloneResource(resource);
                    addDependency(res, false, true, 0, false);
                }
            }
        }
        for (Clause bundle : Parser.parseClauses(this.bundles.toArray(new String[this.bundles.size()]))) {
            final String loc = bundle.getName();
            boolean dependency = Boolean.parseBoolean(bundle.getAttribute("dependency"));
            boolean start = bundle.getAttribute("start") == null || Boolean.parseBoolean(bundle.getAttribute("start"));
            boolean blacklisted = bundle.getAttribute("blacklisted") != null && Boolean.parseBoolean(bundle.getAttribute("blacklisted"));
            int startLevel = 0;
            try {
                startLevel = Integer.parseInt(bundle.getAttribute("start-level"));
            } catch (NumberFormatException e) {
                // Ignore
            }
            if (dependency) {
                addDependency(bundles.get(loc), false, start, startLevel, blacklisted);
            } else {
                doAddDependency(bundles.get(loc), true, start, startLevel, blacklisted);
                addIdentityRequirement(this, bundles.get(loc));
            }
        }
        // Compute dependencies
        for (DependencyInfo info : dependencies.values()) {
            installable.add(info.resource);
            addIdentityRequirement(info.resource, this, info.mandatory);
        }
    }

    ResourceImpl cloneResource(Resource resource) {
        ResourceImpl res = new ResourceImpl();
        for (Capability cap : resource.getCapabilities(null)) {
            res.addCapability(new CapabilityImpl(res, cap.getNamespace(),
                    new StringArrayMap<>(cap.getDirectives()), new StringArrayMap<>(cap.getAttributes())));
        }
        for (Requirement req : resource.getRequirements(null)) {
            SimpleFilter sf;
            if (req instanceof RequirementImpl) {
                sf = ((RequirementImpl) req).getFilter();
            } else if (req.getDirectives().containsKey(REQUIREMENT_FILTER_DIRECTIVE)) {
                sf = SimpleFilter.parse(req.getDirectives().get(REQUIREMENT_FILTER_DIRECTIVE));
            } else {
                sf = SimpleFilter.convert(req.getAttributes());
            }
            res.addRequirement(new RequirementImpl(res, req.getNamespace(),
                    new StringArrayMap<>(req.getDirectives()), new StringArrayMap<>(req.getAttributes()), sf));
        }
        return res;
    }

    Map<String, String> getMetadata(StreamProvider provider) throws IOException {
        try (
                ZipInputStream zis = new ZipInputStream(provider.open())
        ) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (MANIFEST_NAME.equals(entry.getName())) {
                    Attributes attributes = new Manifest(zis).getMainAttributes();
                    Map<java.lang.String, java.lang.String> headers = new HashMap<>();
                    for (Map.Entry attr : attributes.entrySet()) {
                        headers.put(attr.getKey().toString(), attr.getValue().toString());
                    }
                    return headers;
                }
            }
        }
        throw new IllegalArgumentException("Resource " + provider.getUrl() + " does not contain a manifest");
    }

    void addDependency(ResourceImpl resource, boolean mandatory, boolean start, int startLevel, boolean blacklisted) {
        if (isAcceptDependencies()) {
            doAddDependency(resource, mandatory, start, startLevel, blacklisted);
        } else {
            parent.addDependency(resource, mandatory, start, startLevel, blacklisted);
        }
    }

    private void doAddDependency(ResourceImpl resource, boolean mandatory, boolean start, int startLevel, boolean blacklisted) {
        String id = ResolverUtil.getSymbolicName(resource) + "|" + ResolverUtil.getVersion(resource);
        DependencyInfo info = new DependencyInfo(resource, mandatory, start, startLevel, blacklisted);
        dependencies.merge(id, info, this::merge);
    }

    private DependencyInfo merge(DependencyInfo di1, DependencyInfo di2) {
        DependencyInfo info = new DependencyInfo();
        if (di1.resource != di2.resource) {
            Requirement r1 = getFirstIdentityReq(di1.resource);
            Requirement r2 = getFirstIdentityReq(di2.resource);
            if (r1 == null) {
                info.resource = di1.resource;
            } else if (r2 == null) {
                info.resource = di2.resource;
            } else {
                String id = ResolverUtil.getSymbolicName(di1.resource) + "/" + ResolverUtil.getVersion(di1.resource);
                throw new IllegalStateException("Resource " + id + " is duplicated on subsystem " + this.toString() + ". First resource requires " + (r1 != null ? r1 : "nothing") + " while the second requires " + (r2 != null ? r2 : "nothing"));
            }
        } else {
            info.resource = di1.resource;
        }
        info.mandatory = di1.mandatory | di2.mandatory;
        info.start = di1.start | di2.start;
        if (di1.startLevel > 0 && di2.startLevel > 0) {
            info.startLevel = Math.min(di1.startLevel, di2.startLevel);
        } else {
            info.startLevel = Math.max(di1.startLevel, di2.startLevel);
        }
        return info;
    }

    private RequirementImpl getFirstIdentityReq(ResourceImpl resource) {
        for (Requirement r : resource.getRequirements(null)) {
            if (IDENTITY_NAMESPACE.equals(r.getNamespace())) {
                return (RequirementImpl) r;
            }
        }
        return null;
    }

    class DependencyInfo implements BundleInfo {
        ResourceImpl resource;
        boolean mandatory;
        boolean start;
        int startLevel;
        boolean blacklisted;
        boolean overriden;

        public DependencyInfo() {
        }

        public DependencyInfo(ResourceImpl resource, boolean mandatory, boolean start, int startLevel, boolean blacklisted) {
            this.resource = resource;
            this.mandatory = mandatory;
            this.start = start;
            this.startLevel = startLevel;
            this.blacklisted = blacklisted;
        }

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
        public String getOriginalLocation() {
            // resource is already overriden
            return getUri(resource);
        }

        @Override
        public boolean isDependency() {
            return !mandatory;
        }

        @Override
        public boolean isBlacklisted() {
            return blacklisted;
        }

        @Override
        public boolean isOverriden() {
            return overriden;
        }

        public void setOverriden(boolean overriden) {
            this.overriden = overriden;
        }

        @Override
        public String toString() {
            return "DependencyInfo{" +
                    "resource=" + resource +
                    '}';
        }
    }

    Map<String, Set<String>> createPolicy(List<? extends ScopeFilter> filters) {
        Map<String, Set<String>> policy = new HashMap<>();
        for (ScopeFilter filter : filters) {
            addToMapSet(policy, filter.getNamespace(), filter.getFilter());
        }
        return policy;
    }

    ResourceImpl createResource(String uri, Map<String, String> headers, boolean removeServiceRequirements) throws Exception {
        try {
            return ResourceBuilder.build(uri, headers, removeServiceRequirements);
        } catch (BundleException e) {
            throw new Exception("Unable to create resource for bundle " + uri, e);
        }
    }

    @Override
    public String toString() {
        return getName();
    }
}
