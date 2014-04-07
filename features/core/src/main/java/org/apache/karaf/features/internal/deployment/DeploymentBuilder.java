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
package org.apache.karaf.features.internal.deployment;

import java.io.IOException;
import java.io.InputStream;
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

import org.apache.felix.resolver.ResolverImpl;
import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Conditional;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.internal.repository.AggregateRepository;
import org.apache.karaf.features.internal.repository.StaticRepository;
import org.apache.karaf.features.internal.resolver.FeatureNamespace;
import org.apache.karaf.features.internal.resolver.FeatureResource;
import org.apache.karaf.features.internal.resolver.RequirementImpl;
import org.apache.karaf.features.internal.resolver.ResolveContextImpl;
import org.apache.karaf.features.internal.resolver.ResourceBuilder;
import org.apache.karaf.features.internal.resolver.ResourceImpl;
import org.apache.karaf.features.internal.resolver.Slf4jResolverLog;
import org.apache.karaf.features.internal.service.Overrides;
import org.apache.karaf.features.internal.util.Macro;
import org.apache.karaf.features.internal.util.MultiException;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.ResolveContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class DeploymentBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentBuilder.class);

    public static final String REQ_PROTOCOL = "req:";

    private final Collection<Repository> repositories;

    private final List<org.osgi.service.repository.Repository> resourceRepos;

    String featureRange = "${range;[====,====]}";

    Downloader downloader;
    ResourceImpl requirements;
    Map<String, Resource> resources;
    Map<String, StreamProvider> providers;

    Set<Feature> featuresToRegister = new HashSet<Feature>();

    public DeploymentBuilder(Downloader downloader,
                             Collection<Repository> repositories) {
        this.downloader = downloader;
        this.repositories = repositories;
        this.resourceRepos = new ArrayList<org.osgi.service.repository.Repository>();
    }

    public void addResourceRepository(org.osgi.service.repository.Repository repository) {
        resourceRepos.add(repository);
    }

    public Map<String, StreamProvider> getProviders() {
        return providers;
    }

    public void setFeatureRange(String featureRange) {
        this.featureRange = featureRange;
    }

    public Map<String, Resource> download(
                         Set<String> features,
                         Set<String> bundles,
                         Set<String> reqs,
                         Set<String> overrides,
                         Set<String> optionals)
                throws IOException, MultiException, InterruptedException, ResolutionException, BundleException {
        this.resources = new ConcurrentHashMap<String, Resource>();
        this.providers = new ConcurrentHashMap<String, StreamProvider>();
        this.requirements = new ResourceImpl("dummy", "dummy", Version.emptyVersion);
        // First, gather all bundle resources
        for (String feature : features) {
            registerMatchingFeatures(feature);
        }
        for (String bundle : bundles) {
            downloadAndBuildResource(bundle);
        }
        for (String req : reqs) {
            buildRequirement(req);
        }
        for (String override : overrides) {
            // TODO: ignore download failures for overrides
            downloadAndBuildResource(Overrides.extractUrl(override));
        }
        for (String optional : optionals) {
            downloadAndBuildResource(optional);
        }
        // Wait for all resources to be created
        downloader.await();
        // Do override replacement
        Overrides.override(resources, overrides);
        // Build features resources
        for (Feature feature : featuresToRegister) {
            Resource resource = FeatureResource.build(feature, featureRange, resources);
            resources.put("feature:" + feature.getName() + "/" + feature.getVersion(), resource);
        }
        // Build requirements
        for (String feature : features) {
            requireFeature(feature, requirements);
        }
        for (String bundle : bundles) {
            requireResource(bundle);
        }
        for (String req : reqs) {
            requireResource(REQ_PROTOCOL + req);
        }
        return resources;
    }

    public Collection<Resource> resolve(List<Resource> systemBundles,
                                        boolean resolveOptionalImports) throws ResolutionException {
        // Resolve
        for (int i = 0; i < systemBundles.size(); i++) {
            resources.put("system-bundle-" + i, systemBundles.get(i));
        }

        List<org.osgi.service.repository.Repository> repos = new ArrayList<org.osgi.service.repository.Repository>();
        repos.add(new StaticRepository(resources.values()));
        repos.addAll(resourceRepos);

        ResolverImpl resolver = new ResolverImpl(new Slf4jResolverLog(LOGGER));
        ResolveContext context = new ResolveContextImpl(
                Collections.<Resource>singleton(requirements),
                Collections.<Resource>emptySet(),
                new AggregateRepository(repos),
                resolveOptionalImports);

        Map<Resource, List<Wire>> resolution = resolver.resolve(context);
        return resolution.keySet();
    }

    public void requireFeature(String feature, ResourceImpl resource) throws IOException {
        // Find name and version range
        String[] split = feature.split("/");
        String name = split[0].trim();
        String version = (split.length > 1) ? split[1].trim() : null;
        if (version != null && !version.equals("0.0.0") && !version.startsWith("[") && !version.startsWith("(")) {
            version = Macro.transform(featureRange, version);
        }
        VersionRange range = version != null ? new VersionRange(version) : VersionRange.ANY_VERSION;
        // Add requirement
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(IdentityNamespace.IDENTITY_NAMESPACE, name);
        attrs.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, FeatureNamespace.TYPE_FEATURE);
        attrs.put(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, range);
        resource.addRequirement(
                new RequirementImpl(resource, IdentityNamespace.IDENTITY_NAMESPACE,
                        Collections.<String, String>emptyMap(), attrs)
        );
    }

    public void requireResource(String location) {
        Resource res = resources.get(location);
        if (res == null) {
            throw new IllegalStateException("Could not find resource for " + location);
        }
        List<Capability> caps = res.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
        if (caps.size() != 1) {
            throw new IllegalStateException("Resource does not have a single " + IdentityNamespace.IDENTITY_NAMESPACE + " capability");
        }
        Capability cap = caps.get(0);
        // Add requirement
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(IdentityNamespace.IDENTITY_NAMESPACE, cap.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE));
        attrs.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, cap.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE));
        attrs.put(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, new VersionRange((Version) cap.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE), true));
        requirements.addRequirement(
                new RequirementImpl(requirements, IdentityNamespace.IDENTITY_NAMESPACE,
                        Collections.<String, String>emptyMap(), attrs));

    }

    public void registerMatchingFeatures(String feature) throws IOException {
        // Find name and version range
        String[] split = feature.split("/");
        String name = split[0].trim();
        String version = (split.length > 1)
                ? split[1].trim() : Version.emptyVersion.toString();
        // Register matching features
        registerMatchingFeatures(name, new VersionRange(version));
    }

    public void registerMatchingFeatures(String name, String version) throws IOException {
        if (version != null && !version.equals("0.0.0") && !version.startsWith("[") && !version.startsWith("(")) {
            version = Macro.transform(featureRange, version);
        }
        registerMatchingFeatures(name, version != null ? new VersionRange(version) : VersionRange.ANY_VERSION);
    }

    public void registerMatchingFeatures(String name, VersionRange range) throws IOException {
        for (Repository repo : repositories) {
            Feature[] features;
            try {
                features = repo.getFeatures();
            } catch (Exception e) {
                // This should not happen as the repository has been loaded already
                throw new IllegalStateException(e);
            }
            for (Feature f : features) {
                if (name.equals(f.getName())) {
                    Version v = VersionTable.getVersion(f.getVersion());
                    if (range.contains(v)) {
                        featuresToRegister.add(f);
                        for (Dependency dep : f.getDependencies()) {
                            registerMatchingFeatures(dep.getName(), dep.getVersion());
                        }
                        for (BundleInfo bundle : f.getBundles()) {
                            downloadAndBuildResource(bundle.getLocation());
                        }
                        for (Conditional cond : f.getConditional()) {
                            Feature c = cond.asFeature(f.getName(), f.getVersion());
                            featuresToRegister.add(c);
                            for (BundleInfo bundle : c.getBundles()) {
                                downloadAndBuildResource(bundle.getLocation());
                            }
                        }
                    }
                }
            }
        }
    }

    public void buildRequirement(String requirement) {
        try {
            String location = REQ_PROTOCOL + requirement;
            ResourceImpl resource = new ResourceImpl(location, "dummy", Version.emptyVersion);
            for (Requirement req : ResourceBuilder.parseRequirement(resource, requirement)) {
                resource.addRequirement(req);
            }
            resources.put(location, resource);
        } catch (BundleException e) {
            throw new IllegalArgumentException("Error parsing requirement: " + requirement, e);
        }
    }

    public void downloadAndBuildResource(final String location) throws IOException {
        if (!resources.containsKey(location)) {
            downloader.download(location, new Downloader.DownloadCallback() {
                @Override
                public void downloaded(StreamProvider provider) throws Exception {
                    manageResource(location, provider);
                }
            });
        }
    }

    private void manageResource(String location, StreamProvider provider) throws Exception {
        if (!resources.containsKey(location)) {
            Attributes attributes = getAttributes(location, provider);
            Resource resource = createResource(location, attributes);
            resources.put(location, resource);
            providers.put(location, provider);
        }
    }

    private Resource createResource(String uri, Attributes attributes) throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        for (Map.Entry attr : attributes.entrySet()) {
            headers.put(attr.getKey().toString(), attr.getValue().toString());
        }
        try {
            return ResourceBuilder.build(uri, headers);
        } catch (BundleException e) {
            throw new Exception("Unable to create resource for bundle " + uri, e);
        }
    }

    protected Attributes getAttributes(String uri, StreamProvider provider) throws Exception {
        InputStream is = provider.open();
        try {
            ZipInputStream zis = new ZipInputStream(is);
            ZipEntry entry;
            while ( (entry = zis.getNextEntry()) != null ) {
                if ("META-INF/MANIFEST.MF".equals(entry.getName())) {
                    return new Manifest(zis).getMainAttributes();
                }
            }
        } finally {
            is.close();
        }
        throw new IllegalArgumentException("Resource " + uri + " does not contain a manifest");
    }

}
