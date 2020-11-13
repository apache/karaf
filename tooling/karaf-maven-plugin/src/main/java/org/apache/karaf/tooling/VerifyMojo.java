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
package org.apache.karaf.tooling;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import aQute.bnd.osgi.Macro;
import aQute.bnd.osgi.Processor;
import org.apache.felix.resolver.Logger;
import org.apache.felix.resolver.ResolverImpl;
import org.apache.felix.utils.resource.ResourceBuilder;
import org.apache.felix.utils.resource.ResourceImpl;
import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.DeploymentEvent;
import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.LocationPattern;
import org.apache.karaf.features.internal.download.DownloadCallback;
import org.apache.karaf.features.internal.download.DownloadManager;
import org.apache.karaf.features.internal.download.Downloader;
import org.apache.karaf.features.internal.download.StreamProvider;
import org.apache.karaf.features.internal.model.*;
import org.apache.karaf.features.internal.resolver.ResourceUtils;
import org.apache.karaf.features.internal.service.Deployer;
import org.apache.karaf.features.internal.service.FeaturesProcessorImpl;
import org.apache.karaf.features.internal.service.FeaturesServiceConfig;
import org.apache.karaf.features.internal.service.State;
import org.apache.karaf.features.internal.service.StaticInstallSupport;
import org.apache.karaf.features.internal.util.MapUtils;
import org.apache.karaf.features.internal.util.MultiException;
import org.apache.karaf.profile.assembly.CustomDownloadManager;
import org.apache.karaf.tooling.utils.MavenUtil;
import org.apache.karaf.tooling.utils.MojoSupport;
import org.apache.karaf.tooling.utils.ReactorMavenResolver;
import org.apache.karaf.util.config.PropertiesLoader;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.repository.WorkspaceReader;
import org.ops4j.pax.url.mvn.MavenResolver;
import org.ops4j.pax.url.mvn.MavenResolvers;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Requirement;
import org.osgi.service.resolver.ResolutionException;

import static java.util.jar.JarFile.MANIFEST_NAME;

@Mojo(name = "verify", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class VerifyMojo extends MojoSupport {

    @Parameter(property = "descriptors")
    protected Set<String> descriptors;

    @Parameter(property = "blacklistedDescriptors")
    protected Set<String> blacklistedDescriptors;

    @Parameter(property = "featureProcessingInstructions")
    protected File featureProcessingInstructions;

    @Parameter(property = "features")
    protected List<String> features;

    @Parameter(property = "framework")
    protected Set<String> framework;

    @Parameter(property = "configuration")
    protected String configuration;

    @Parameter(property = "distribution", defaultValue = "org.apache.karaf:apache-karaf")
    protected String distribution;

    @Parameter(property = "javase")
    protected String javase;

    @Parameter(property = "dist-dir")
    protected String distDir;

    @Parameter(property = "karaf-version")
    protected String karafVersion;

    @Parameter(property = "additional-metadata")
    protected File additionalMetadata;

    @Parameter(property = "ignore-missing-conditions")
    protected boolean ignoreMissingConditions;

    @Parameter(property = "fail")
    protected String fail = "end";

    @Parameter(property = "verify-transitive")
    protected boolean verifyTransitive = false;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Component(role = WorkspaceReader.class, hint = "reactor")
    protected WorkspaceReader reactor;

    @Parameter(property = "skip", defaultValue = "${features.verify.skip}")
    protected boolean skip;

    @Parameter(readonly = true, defaultValue = "${project.groupId}")
    protected String selfGroupId;

    @Parameter(readonly = true, defaultValue = "${project.artifactId}")
    protected String selfArtifactId;

    protected MavenResolver resolver;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            return;
        }

        if (karafVersion == null) {
            karafVersion = org.apache.karaf.util.Version.karafVersion();
        }

        Hashtable<String, String> config = new Hashtable<>();
        String remoteRepositories = MavenUtil.remoteRepositoryList(project.getRemoteProjectRepositories());
        getLog().info("Using repositories: " + remoteRepositories);
        config.put("maven.repositories", remoteRepositories);
        config.put("maven.localRepository", localRepo.getBasedir());

        if (mavenSession.getRequest().getUserSettingsFile().exists()) {
            config.put("maven.settings", mavenSession.getRequest().getUserSettingsFile().toString());
        }

        // TODO: add more configuration bits ?
        resolver = new ReactorMavenResolver(reactor, MavenResolvers.createMavenResolver(config, "maven"));
        doExecute();
    }

    private String getVersion(String id, String def) {
        String v = getVersion(id);
        return v != null ? v : def;
    }

    private String getVersion(String id) {
        Artifact artifact = project.getArtifactMap().get(id);
        if (artifact != null) {
            return artifact.getBaseVersion();
        } else if (id.startsWith("org.apache.karaf")) {
            return karafVersion;
        } else {
            return null;
        }
    }

    private static Object invoke(Object object, String getter) throws MojoExecutionException {
        try {
            return object.getClass().getMethod(getter).invoke(object);
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to build remote repository from " + object.toString(), e);
        }
    }

    private static Object getPolicy(Object object, boolean snapshots) throws MojoExecutionException {
        return invoke(object, "getPolicy", new Class[] { Boolean.TYPE }, new Object[] { snapshots });
    }

    private static Object invoke(Object object, String getter, Class<?>[] types, Object[] params) throws MojoExecutionException {
        try {
            return object.getClass().getMethod(getter, types).invoke(object, params);
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to build remote repository from " + object.toString(), e);
        }
    }

    protected void doExecute() throws MojoExecutionException {
        System.setProperty("karaf.home", "target/karaf");
        System.setProperty("karaf.data", "target/karaf/data");

        Hashtable<String, String> properties = new Hashtable<>();

        if (additionalMetadata != null) {
            try (Reader reader = new FileReader(additionalMetadata)) {
                Properties metadata = new Properties();
                metadata.load(reader);
                for (Enumeration<?> e = metadata.propertyNames(); e.hasMoreElements(); ) {
                    Object key = e.nextElement();
                    Object val = metadata.get(key);
                    properties.put(key.toString(), val.toString());
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to load additional metadata from " + additionalMetadata, e);
            }
        }

        Set<String> allDescriptors = new LinkedHashSet<>();
        if (descriptors == null) {
            if (framework == null) {
                framework = Collections.singleton("framework");
            }
            descriptors = new LinkedHashSet<>();
            if (framework.contains("framework")) {
                allDescriptors.add("mvn:org.apache.karaf.features/framework/" + getVersion("org.apache.karaf.features:framework") + "/xml/features");
            }
            String filePrefix = null;
            if (System.getProperty("os.name").contains("Windows")) {
                filePrefix = "file:/";
            } else {
                filePrefix = "file:";
            }
            allDescriptors.add(filePrefix + project.getBuild().getDirectory() + File.separator 
                               + "feature"
                               + File.separator 
                               + "feature.xml");
        } else {
            allDescriptors.addAll(descriptors);
            if (framework != null && framework.contains("framework")) {
                allDescriptors.add("mvn:org.apache.karaf.features/framework/" + getVersion("org.apache.karaf.features:framework") + "/xml/features");
            }
        }

        // TODO: allow using external configuration ?
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(8);
        DownloadManager manager = new CustomDownloadManager(resolver, executor);
        final Map<String, Features> repositories;
        Map<String, List<Feature>> allFeatures = new HashMap<>();
        try {
            repositories = loadRepositories(manager, allDescriptors);
            for (String repoUri : repositories.keySet()) {
                List<Feature> features = repositories.get(repoUri).getFeature();
                // Ack features to inline configuration files urls
                for (Feature feature : features) {
                    for (org.apache.karaf.features.internal.model.Bundle bi : feature.getBundle()) {
                        String loc = bi.getLocation();
                        String nloc = null;
                        if (loc.contains("file:")) {
                            for (ConfigFile cfi : feature.getConfigfile()) {
                                if (cfi.getFinalname().substring(1)
                                        .equals(loc.substring(loc.indexOf("file:") + "file:".length()))) {
                                    nloc = cfi.getLocation();
                                }
                            }
                        }
                        if (nloc != null) {
                            Field field = bi.getClass().getDeclaredField("location");
                            field.setAccessible(true);
                            field.set(bi, loc.substring(0, loc.indexOf("file:")) + nloc);
                        }
                    }
                }
                allFeatures.put(repoUri, features);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to load features descriptors", e);
        }

        List<Feature> featuresToTest = new ArrayList<>();
        if (verifyTransitive) {
            for (List<Feature> features : allFeatures.values()) {
                featuresToTest.addAll(features);
            }
        } else {
            for (String uri : descriptors) {
                featuresToTest.addAll(allFeatures.get(uri));
            }
        }
        if (features != null && !features.isEmpty()) {
            Pattern pattern = getPattern(features);
            for (Iterator<Feature> iterator = featuresToTest.iterator(); iterator.hasNext();) {
                Feature feature = iterator.next();
                String id = feature.getName() + "/" + feature.getVersion();
                if (!pattern.matcher(id).matches()) {
                    iterator.remove();
                }
            }
        }

        for (String fmk : framework) {
            properties.put("feature.framework." + fmk, fmk);
        }
        Set<String> successes = new LinkedHashSet<>();
        Set<String> ignored = new LinkedHashSet<>();
        Set<String> skipped = new LinkedHashSet<>();
        Map<String, Exception> failures = new LinkedHashMap<>();
        for (Feature feature : featuresToTest) {
            String id = feature.getId();
            if (feature.isBlacklisted()) {
                skipped.add(id);
                getLog().info("Verification of feature " + id + " skipped");
                continue;
            }
            try {
                verifyResolution(new CustomDownloadManager(resolver, executor),
                                 repositories, Collections.singleton(id), properties);
                successes.add(id);
                getLog().info("Verification of feature " + id + " succeeded");
            } catch (Exception e) {
                if (e.getCause() instanceof ResolutionException || !getLog().isDebugEnabled()) {
                    getLog().warn(e.getMessage() + ": " + id);
                    getLog().warn(e.getCause().getMessage());
                } else {
                    getLog().warn(e);
                }
                failures.put(id, e);
                if ("first".equals(fail)) {
                    throw e;
                }
            }
            for (Conditional cond : feature.getConditional()) {
                Set<String> ids = new LinkedHashSet<>();
                ids.add(feature.getId());
                ids.addAll(cond.getCondition());
                String cid = String.join("+", ids);
                try {
                    verifyResolution(manager, repositories, ids, properties);
                    successes.add(cid);
                    getLog().info("Verification of feature " + cid + " succeeded");
                } catch (Exception e) {
                    if (ignoreMissingConditions && e.getCause() instanceof ResolutionException) {
                        boolean ignore = true;
                        Collection<Requirement> requirements = ((ResolutionException) e.getCause()).getUnresolvedRequirements();
                        for (Requirement req : requirements) {
                            ignore &= (IdentityNamespace.IDENTITY_NAMESPACE.equals(req.getNamespace())
                                    && ResourceUtils.TYPE_FEATURE.equals(req.getAttributes().get("type"))
                                    && cond.getCondition().contains(req.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE).toString()));
                        }
                        if (ignore) {
                            ignored.add(cid);
                            getLog().warn("Feature resolution failed for " + cid
                                    + "\nMessage: " + e.getCause().getMessage());
                            continue;
                        }
                    }
                    if (e.getCause() instanceof ResolutionException || !getLog().isDebugEnabled()) {
                        getLog().warn(e.getMessage());
                    } else {
                        getLog().warn(e);
                    }
                    failures.put(cid, e);
                    if ("first".equals(fail)) {
                        throw e;
                    }
                }
            }
        }
        int nb = successes.size() + ignored.size() + failures.size();
        getLog().info("Features verified: " + nb + ", failures: " + failures.size() + ", ignored: " + ignored.size() + ", skipped: " + skipped.size());
        if (!failures.isEmpty()) {
            getLog().info("Failures: " + String.join(", ", failures.keySet()));
        }
        if ("end".equals(fail) && !failures.isEmpty()) {
            throw new MojoExecutionException("Verification failures", new MultiException("Verification failures", new ArrayList<>(failures.values())));
        }
    }

    static Pattern getPattern(List<String> features) {
        StringBuilder sb = new StringBuilder();
        boolean prevIsNeg = false;
        for (String feature : features) {
            if (sb.length() > 0 && !prevIsNeg) {
                sb.append("|");
            }
            sb.append("(");
            feature = feature.trim();
            boolean negative = feature.startsWith("!");
            if (negative) {
                feature = feature.substring("!".length());
                sb.append("(?!");
            }
            String p = feature.replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*");
            sb.append(p);
            if (!feature.contains("/")) {
                sb.append("/.*");
            }
            if (negative) {
                sb.append(")");
            }
            prevIsNeg = negative;
        }
        for (String feature : features) {
            sb.append(")");
        }
        return Pattern.compile(sb.toString());
    }

    private void verifyResolution(DownloadManager manager, final Map<String, Features> repositories, Set<String> features, Hashtable<String, String> properties) throws MojoExecutionException {
        try {
            Bundle systemBundle = getSystemBundle(getMetadata(properties, "metadata#"));
            DummyDeployCallback callback = new DummyDeployCallback(systemBundle, repositories.values());
            Deployer deployer = new Deployer(manager, new ResolverImpl(new MavenResolverLog()), callback);


            // Install framework
            Deployer.DeploymentRequest request = Deployer.DeploymentRequest.defaultDeploymentRequest();

            for (String fmwk : framework) {
                MapUtils.addToMapSet(request.requirements, FeaturesService.ROOT_REGION, fmwk);
            }
            try {
                deployer.deploy(callback.getDeploymentState(), request);
            } catch (Exception e) {
                throw new MojoExecutionException("Unable to resolve framework features", e);
            }

            /*
            boolean resolveOptionalImports = getResolveOptionalImports(properties);

            DeploymentBuilder builder = new DeploymentBuilder(
                    manager,
                    null,
                    repositories.values(),
                    -1 // Disable url handlers
            );
            Map<String, Resource> downloadedResources = builder.download(
                    getPrefixedProperties(properties, "feature."),
                    getPrefixedProperties(properties, "bundle."),
                    getPrefixedProperties(properties, "fab."),
                    getPrefixedProperties(properties, "req."),
                    getPrefixedProperties(properties, "override."),
                    getPrefixedProperties(properties, "optional."),
                    getMetadata(properties, "metadata#")
            );

            for (String uri : getPrefixedProperties(properties, "resources.")) {
                builder.addResourceRepository(new MetadataRepository(new HttpMetadataProvider(uri)));
            }
            */

            // Install features
            for (String feature : features) {
                MapUtils.addToMapSet(request.requirements, FeaturesService.ROOT_REGION, feature);
            }
            try {
                deployer.deployFully(callback.getDeploymentState(), request);

                // TODO: find unused resources ?
            } catch (Exception e) {
                throw new MojoExecutionException("Feature resolution failed for " + features
                        + "\nMessage: " + (e instanceof ResolutionException ? e.getMessage() : e.toString())
                        + "\nRepositories: " + toString(new TreeSet<>(repositories.keySet()))
                        + "\nResources: " + toString(new TreeSet<>(manager.getProviders().keySet())), e);
            }
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Error verifying feature " + features + "\nMessage: " + e.getMessage(), e);
        }
    }

    private static String toString(Collection<String> collection) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (String s : collection) {
            sb.append("\t").append(s).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    private Bundle getSystemBundle(Map<String, Map<VersionRange, Map<String, String>>> metadata) throws Exception {
        URL configPropURL;
        if (configuration != null) {
            configPropURL = new URL(configuration);
        } else {
            Artifact karafDistro = project.getArtifactMap().get(distribution);
            if (karafDistro != null) {
                String dir = distDir;
                if ("kar".equals(karafDistro.getType()) && dir == null) {
                    dir = "resources";
                }
                if (dir == null) {
                    dir = karafDistro.getArtifactId() + "-" + karafDistro.getBaseVersion();
                }
                configPropURL = new URL("jar:file:" + karafDistro.getFile() + "!/" + dir + "/etc/config.properties");
            } else {
                String version = getVersion(distribution, "RELEASE");
                String[] dist = distribution.split(":");
                File distFile = resolver.resolve(dist[0], dist[1], null, "zip", version);
                String resolvedVersion = distFile.getName().substring(dist[1].length() + 1, distFile.getName().length() - 4);
                String dir = distDir;
                if (dir == null) {
                    dir = dist[1] + "-" + resolvedVersion;
                }
                configPropURL = new URL("jar:file:" + distFile + "!/" + dir + "/etc/config.properties");
            }
        }
        org.apache.felix.utils.properties.Properties configProps = PropertiesLoader.loadPropertiesFile(configPropURL, true);
//        copySystemProperties(configProps);
        if (javase == null) {
            configProps.put("java.specification.version", System.getProperty("java.specification.version"));
        } else {
            configProps.put("java.specification.version", javase);
        }
        configProps.substitute();

        Attributes attributes = new Attributes();
        attributes.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
        attributes.putValue(Constants.BUNDLE_SYMBOLICNAME, "system.bundle");
        attributes.putValue(Constants.BUNDLE_VERSION, "0.0.0");

        String exportPackages = configProps.getProperty("org.osgi.framework.system.packages");
        if (configProps.containsKey("org.osgi.framework.system.packages.extra")) {
            exportPackages += "," + configProps.getProperty("org.osgi.framework.system.packages.extra");
        }
        exportPackages = exportPackages.replaceAll(",\\s*,", ",");
        attributes.putValue(Constants.EXPORT_PACKAGE, exportPackages);

        String systemCaps = configProps.getProperty("org.osgi.framework.system.capabilities");
        attributes.putValue(Constants.PROVIDE_CAPABILITY, systemCaps);

        // TODO: support metadata overrides on system bundle
//        attributes = DeploymentBuilder.overrideAttributes(attributes, metadata);

        final Hashtable<String, String> headers = new Hashtable<>();
        for (Map.Entry<Object, Object> attr : attributes.entrySet()) {
            headers.put(attr.getKey().toString(), attr.getValue().toString());
        }

        final FakeBundleRevision resource = new FakeBundleRevision(headers, "system-bundle", 0l);
        return resource.getBundle();
    }


    public Map<String, Features> loadRepositories(DownloadManager manager, Set<String> uris) throws Exception {
        final Map<String, Features> loaded = new HashMap<>();
        final Downloader downloader = manager.createDownloader();

        FeaturesServiceConfig config = null;
        if (featureProcessingInstructions != null) {
            config = new FeaturesServiceConfig(featureProcessingInstructions.toURI().toString(), null);
        } else {
            config = new FeaturesServiceConfig();
        }
        FeaturesProcessorImpl processor = new FeaturesProcessorImpl(config);
        if (blacklistedDescriptors != null) {
            blacklistedDescriptors.forEach(lp -> processor.getInstructions().getBlacklistedRepositoryLocationPatterns()
                    .add(new LocationPattern(lp)));
        }
        processor.getInstructions().getBlacklistedRepositoryLocationPatterns()
                .add(new LocationPattern("mvn:" + selfGroupId + "/" + selfArtifactId));

        for (String repository : uris) {
            if (!processor.isRepositoryBlacklisted(repository)) {
                downloader.download(repository, new DownloadCallback() {
                    @Override
                    public void downloaded(final StreamProvider provider) throws Exception {
                        synchronized (loaded) {
                            // If provider was already loaded, no need to do it again.
                            if (loaded.containsKey(provider.getUrl())) {
                                return;
                            }
                        }
                        try (InputStream is = provider.open()) {
                            Features featuresModel;
                            if (JacksonUtil.isJson(provider.getUrl())) {
                                featuresModel = JacksonUtil.unmarshal(provider.getUrl());
                            } else {
                                featuresModel = JaxbUtil.unmarshal(provider.getUrl(), is, false);
                            }
                            processor.process(featuresModel);
                            synchronized (loaded) {
                                loaded.put(provider.getUrl(), featuresModel);
                                for (String innerRepository : featuresModel.getRepository()) {
                                    if (!processor.isRepositoryBlacklisted(innerRepository)) {
                                        downloader.download(innerRepository, this);
                                    }
                                }
                            }
                        }
                    }
                });
            }
        }
        downloader.await();
        return loaded;
    }

    public static Set<String> getPrefixedProperties(Map<String, String> properties, String prefix) {
        Set<String> result = new HashSet<>();
        for (String key : properties.keySet()) {
            if (key.startsWith(prefix)) {
                String url = properties.get(key);
                if (url == null || url.length() == 0) {
                    url = key.substring(prefix.length());
                }
                if (!url.isEmpty()) {
                    result.add(url);
                }
            }
        }
        return result;
    }

    public static Map<String, Map<VersionRange, Map<String, String>>> getMetadata(Map<String, String> properties, String prefix) {
        Map<String, Map<VersionRange, Map<String, String>>> result = new HashMap<>();
        for (String key : properties.keySet()) {
            if (key.startsWith(prefix)) {
                String val = properties.get(key);
                key = key.substring(prefix.length());
                String[] parts = key.split("#");
                if (parts.length == 3) {
                    Map<VersionRange, Map<String, String>> ranges =
                            result.computeIfAbsent(parts[0], k -> new HashMap<>());
                    String version = parts[1];
                    if (!version.startsWith("[") && !version.startsWith("(")) {
                        Processor processor = new Processor();
                        processor.setProperty("@", VersionTable.getVersion(version).toString());
                        Macro macro = new Macro(processor);
                        version = macro.process("${range;[==,=+)}");
                    }
                    VersionRange range = new VersionRange(version);
                    ranges.computeIfAbsent(range, k -> new HashMap<>()).put(parts[2], val);
                }
            }
        }
        return result;
    }

    public static class FakeBundleRevision extends ResourceImpl implements BundleRevision, BundleStartLevel {

        private final Bundle bundle;
        private int startLevel;

        public FakeBundleRevision(final Hashtable<String, String> headers, final String location, final long bundleId) throws BundleException {
            ResourceBuilder.build(this, location, headers);
            this.bundle = (Bundle) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class[] { Bundle.class },
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) {
                            switch (method.getName()) {
                                case "hashCode":
                                    return FakeBundleRevision.this.hashCode();
                                case "equals":
                                    return proxy == args[0];
                                case "toString":
                                    return bundle.getSymbolicName() + "/" + bundle.getVersion();
                                case "adapt":
                                    if (args.length == 1 && args[0] == BundleRevision.class) {
                                        return FakeBundleRevision.this;
                                    } else if (args.length == 1 && args[0] == BundleStartLevel.class) {
                                        return FakeBundleRevision.this;
                                    }
                                    break;
                                case "getHeaders":
                                    return headers;
                                case "getBundleId":
                                    return bundleId;
                                case "getLocation":
                                    return location;
                                case "getSymbolicName":
                                    String name = headers.get(Constants.BUNDLE_SYMBOLICNAME);
                                    int idx = name.indexOf(';');
                                    if (idx > 0) {
                                        name = name.substring(0, idx).trim();
                                    }
                                    return name;
                                case "getVersion":
                                    return new Version(headers.get(Constants.BUNDLE_VERSION));
                                case "getState":
                                    return Bundle.ACTIVE;
                                case "getLastModified":
                                    return 0l;
                            }
                            return null;
                        }
                    });
        }

        @Override
        public int getStartLevel() {
            return startLevel;
        }

        @Override
        public void setStartLevel(int startLevel) {
            this.startLevel = startLevel;
        }

        @Override
        public boolean isPersistentlyStarted() {
            return true;
        }

        @Override
        public boolean isActivationPolicyUsed() {
            return false;
        }

        @Override
        public String getSymbolicName() {
            return bundle.getSymbolicName();
        }

        @Override
        public Version getVersion() {
            return bundle.getVersion();
        }

        @Override
        public List<BundleCapability> getDeclaredCapabilities(String namespace) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<BundleRequirement> getDeclaredRequirements(String namespace) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getTypes() {
            throw new UnsupportedOperationException();
        }

        @Override
        public BundleWiring getWiring() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Bundle getBundle() {
            return bundle;
        }
    }

    public static class DummyDeployCallback extends StaticInstallSupport implements Deployer.DeployCallback {

        private final Bundle systemBundle;
        private final Deployer.DeploymentState dstate;
        private final AtomicLong nextBundleId = new AtomicLong(0);

        public DummyDeployCallback(Bundle sysBundle, Collection<Features> repositories) {
            systemBundle = sysBundle;
            dstate = new Deployer.DeploymentState();
            dstate.bundles = new HashMap<>();
            dstate.bundlesPerRegion = new HashMap<>();
            dstate.filtersPerRegion = new HashMap<>();
            dstate.state = new State();

            MapUtils.addToMapSet(dstate.bundlesPerRegion, FeaturesService.ROOT_REGION, 0l);
            dstate.bundles.put(0l, systemBundle);

            Collection<org.apache.karaf.features.Feature> features = new LinkedList<>();
            for (Features repo : repositories) {
                if (repo.isBlacklisted()) {
                    continue;
                }
                for (Feature f : repo.getFeature()) {
                    if (!f.isBlacklisted()) {
                        features.add(f);
                    }
                }
            }
            dstate.partitionFeatures(features);
        }

        public Deployer.DeploymentState getDeploymentState() {
            return dstate;
        }

        @Override
        public void saveState(State state) {
            dstate.state.replace(state);
        }

        @Override
        public void persistResolveRequest(Deployer.DeploymentRequest request) {
        }

        @Override
        public void installConfigs(org.apache.karaf.features.Feature feature) {
        }

        @Override
        public void deleteConfigs(org.apache.karaf.features.Feature feature) throws IOException, InvalidSyntaxException {
        }

        @Override
        public void installLibraries(org.apache.karaf.features.Feature feature) {
        }

        @Override
        public void callListeners(FeatureEvent featureEvent) {
        }

        @Override
        public void callListeners(DeploymentEvent deployEvent) {
        }

        @Override
        public Bundle installBundle(String region, String uri, InputStream is) throws BundleException {
            try {
                Hashtable<String, String> headers = new Hashtable<>();
                ZipInputStream zis = new ZipInputStream(is);
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (MANIFEST_NAME.equals(entry.getName())) {
                        Attributes attributes = new Manifest(zis).getMainAttributes();
                        for (Map.Entry<Object, Object> attr : attributes.entrySet()) {
                            headers.put(attr.getKey().toString(), attr.getValue().toString());
                        }
                    }
                }
                BundleRevision revision = new FakeBundleRevision(headers, uri, nextBundleId.incrementAndGet());
                Bundle bundle = revision.getBundle();
                MapUtils.addToMapSet(dstate.bundlesPerRegion, region, bundle.getBundleId());
                dstate.bundles.put(bundle.getBundleId(), bundle);
                return bundle;
            } catch (IOException e) {
                throw new BundleException("Unable to install bundle", e);
            }
        }

        @Override
        public void bundleBlacklisted(BundleInfo bundleInfo) {

        }

    }

    public class MavenResolverLog extends org.apache.felix.resolver.Logger {

        public MavenResolverLog() {
            super(Logger.LOG_DEBUG);
        }

        @Override
        protected void doLog(int level, String msg, Throwable throwable) {
            switch (level) {
            case LOG_DEBUG:
                getLog().debug(msg, throwable);
                break;
            case LOG_INFO:
                getLog().info(msg, throwable);
                break;
            case LOG_WARNING:
                getLog().warn(msg, throwable);
                break;
            case LOG_ERROR:
                getLog().error(msg, throwable);
                break;
            }
        }
    }
}
