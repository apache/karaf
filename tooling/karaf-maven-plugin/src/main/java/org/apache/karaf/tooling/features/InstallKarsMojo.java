/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.tooling.features;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.felix.utils.properties.InterpolationHelper;
import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.features.internal.download.impl.DownloadManagerHelper;
import org.apache.karaf.features.internal.model.Bundle;
import org.apache.karaf.features.internal.model.Conditional;
import org.apache.karaf.features.internal.model.Config;
import org.apache.karaf.features.internal.model.ConfigFile;
import org.apache.karaf.features.internal.model.Dependency;
import org.apache.karaf.features.internal.model.Feature;
import org.apache.karaf.features.internal.model.Features;
import org.apache.karaf.features.internal.model.JaxbUtil;
import org.apache.karaf.features.internal.util.MapUtils;
import org.apache.karaf.kar.internal.Kar;
import org.apache.karaf.profile.Profile;
import org.apache.karaf.profile.ProfileBuilder;
import org.apache.karaf.profile.impl.Profiles;
import org.apache.karaf.tooling.url.CustomBundleURLStreamHandlerFactory;
import org.apache.karaf.tooling.utils.InternalMavenResolver;
import org.apache.karaf.tooling.utils.IoUtils;
import org.apache.karaf.tooling.utils.MojoSupport;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.ops4j.pax.url.mvn.MavenResolver;

/**
 * Installs kar dependencies into a server-under-construction in target/assembly
 */
@Mojo(name = "install-kars", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class InstallKarsMojo extends MojoSupport {

    /**
     * Base directory used to copy the resources during the build (working directory).
     */
    @Parameter(defaultValue = "${project.build.directory}/assembly")
    protected File workDirectory;

    /**
     * Features configuration file (etc/org.apache.karaf.features.cfg).
     */
    @Parameter(defaultValue = "${project.build.directory}/assembly/etc/org.apache.karaf.features.cfg")
    protected File featuresCfgFile;

    /**
     * startup.properties file.
     */
    @Parameter(defaultValue = "${project.build.directory}/assembly/etc/startup.properties")
    protected File startupPropertiesFile;

    /**
     * Directory used during build to construction the Karaf system repository.
     */
    @Parameter(defaultValue="${project.build.directory}/assembly/system")
    protected File systemDirectory;

    /**
     * default start level for bundles in features that don't specify it.
     */
    @Parameter
    protected int defaultStartLevel = 30;

    @Parameter
    private List<String> startupRepositories;
    @Parameter
    private List<String> bootRepositories;
    @Parameter
    private List<String> installedRepositories;

    /**
     * List of features from runtime-scope features xml and kars to be installed into system and listed in startup.properties.
     */
    @Parameter
    private List<String> startupFeatures;

    /**
     * List of features from runtime-scope features xml and kars to be installed into system repo and listed in features service boot features.
     */
    @Parameter
    private List<String> bootFeatures;

    /**
     * List of features from runtime-scope features xml and kars to be installed into system repo and not mentioned elsewhere.
     */
    @Parameter
    private List<String> installedFeatures;

    @Parameter
    private List<String> startupBundles;
    @Parameter
    private List<String> bootBundles;
    @Parameter
    private List<String> installedBundles;
    
    @Parameter
    private File profilesDirectory;

    @Parameter
    private List<String> bootProfiles;

    @Parameter
    private List<String> startupProfiles;

    @Parameter
    private List<String> installedProfiles;

    /**
     * Ignore the dependency attribute (dependency="[true|false]") on bundle
     */
    @Parameter(defaultValue = "true")
    protected boolean ignoreDependencyFlag;

    /**
     * Additional feature repositories
     */
    @Parameter
    protected List<String> featureRepositories;

    /**
     * Use reference: style urls in startup.properties
     */
    @Parameter(defaultValue = "false")
    protected boolean useReferenceUrls;

    @Parameter
    protected boolean installAllFeaturesByDefault = true;

    // an access layer for available Aether implementation
    protected DependencyHelper dependencyHelper;

    private static final String FEATURES_REPOSITORIES = "featuresRepositories";
    private static final String FEATURES_BOOT = "featuresBoot";

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        this.dependencyHelper = DependencyHelperFactory.createDependencyHelper(this.container, this.project, this.mavenSession, getLog());
        MavenResolver resolver = new InternalMavenResolver(dependencyHelper, getLog());
        CustomBundleURLStreamHandlerFactory.install(resolver);

        try {
            doExecute();
        }
        catch (MojoExecutionException | MojoFailureException e) {
            throw e;
        }
        catch (Exception e) {
            throw new MojoExecutionException("Unable to build assembly", e);
        }
        finally {
            CustomBundleURLStreamHandlerFactory.uninstall();
        }
    }

    protected void doExecute() throws Exception {
        this.dependencyHelper = DependencyHelperFactory.createDependencyHelper(this.container, this.project, this.mavenSession, getLog());

        // creating system directory
        getLog().info("Creating system directory");
        systemDirectory.mkdirs();

        IoUtils.deleteRecursive(new File(systemDirectory, "generated"));

        startupRepositories = nonNullList(startupRepositories);
        bootRepositories = nonNullList(bootRepositories);
        installedRepositories = nonNullList(installedRepositories);
        startupBundles = nonNullList(startupBundles);
        bootBundles = nonNullList(bootBundles);
        installedBundles = nonNullList(installedBundles);
        startupFeatures = nonNullList(startupFeatures);
        bootFeatures = nonNullList(bootFeatures);
        installedFeatures = nonNullList(installedFeatures);
        startupProfiles = nonNullList(startupProfiles);
        bootProfiles = nonNullList(bootProfiles);
        installedProfiles = nonNullList(installedProfiles);

        if (!startupProfiles.isEmpty() || !bootProfiles.isEmpty() || !installedProfiles.isEmpty()) {
            if (profilesDirectory == null) {
                throw new IllegalArgumentException("profilesDirectory must be specified");
            }
        }

        if (featureRepositories != null && !featureRepositories.isEmpty()) {
            getLog().warn("Use of featureRepositories is deprecated, use startupRepositories, bootRepositories or installedRepositories instead");
            startupRepositories.addAll(featureRepositories);
            bootRepositories.addAll(featureRepositories);
            installedRepositories.addAll(featureRepositories);
        }

        // Build optional features and known prerequisites
        Map<String, List<String>> prereqs = new HashMap<>();
        prereqs.put("blueprint:", Arrays.asList("deployer", "aries-blueprint"));
        prereqs.put("spring:", Arrays.asList("deployer", "spring"));
        prereqs.put("wrap:", Arrays.asList("wrap"));
        prereqs.put("war:", Arrays.asList("war"));


        // Loading kars and features repositories
        getLog().info("Loading kar and features repositories dependencies");
        for (Artifact artifact : project.getDependencyArtifacts()) {
            if ("kar".equals(artifact.getType())) {
                File karFile = artifact.getFile();
                getLog().info("Extracting " + artifact.toString() + " kar");
                try {
                    Kar kar = new Kar(karFile.toURI());
                    kar.extract(systemDirectory, workDirectory);
                    for (URI repositoryUri : kar.getFeatureRepos()) {
                        switch (artifact.getScope()) {
                        case "compile":
                            startupRepositories.add(repositoryUri.getPath());
                            break;
                        case "runtime":
                            bootRepositories.add(repositoryUri.getPath());
                            break;
                        case "provided":
                            installedRepositories.add(repositoryUri.getPath());
                            break;
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Can not install " + artifact.toString() + " kar", e);
                }
            } else if ("features".equals(artifact.getClassifier())) {
                String repositoryUri = dependencyHelper.artifactToMvn(artifact);
                switch (artifact.getScope()) {
                case "compile":
                    startupRepositories.add(repositoryUri);
                    break;
                case "runtime":
                    bootRepositories.add(repositoryUri);
                    break;
                case "provided":
                    installedRepositories.add(repositoryUri);
                    break;
                }
            } else if ("jar".equals(artifact.getType()) || "bundle".equals(artifact.getType())) {
                String bundleUri = dependencyHelper.artifactToMvn(artifact);
                switch (artifact.getScope()) {
                case "compile":
                    startupBundles.add(bundleUri);
                    break;
                case "runtime":
                    bootBundles.add(bundleUri);
                    break;
                case "provided":
                    installedBundles.add(bundleUri);
                    break;
                }
            }
        }

        // Load profiles
        Map<String, Profile> allProfiles;
        if (profilesDirectory != null) {
            allProfiles = Profiles.loadProfiles(profilesDirectory.toPath());
        } else {
            allProfiles = new HashMap<>();
        }
        // Generate profiles
        Profile startupProfile = generateProfile(allProfiles, startupProfiles, startupRepositories, startupFeatures, startupBundles);
        Profile bootProfile = generateProfile(allProfiles, bootProfiles, bootRepositories, bootFeatures, bootBundles);
        Profile installedProfile = generateProfile(allProfiles, installedProfiles, installedRepositories, installedFeatures, installedBundles);

        //
        // Compute overall profile
        //
        Profile overallProfile = ProfileBuilder.Factory.create(UUID.randomUUID().toString())
                .setParents(Arrays.asList(startupProfile.getId(), bootProfile.getId(), installedProfile.getId()))
                .getProfile();
        Profile overallOverlay = Profiles.getOverlay(overallProfile, allProfiles);
        Profile overallEffective = Profiles.getEffective(overallOverlay, false);

        Hashtable<String, String> agentProps = new Hashtable<>(overallEffective.getConfiguration("org.ops4j.pax.url.mvn"));
        final Map<String, String> properties = new HashMap<>();
        properties.put("karaf.default.repository", "system");
        InterpolationHelper.performSubstitution(agentProps, new InterpolationHelper.SubstitutionCallback() {
            @Override
            public String getValue(String key) {
                return properties.get(key);
            }
        }, false, false, true);

        //
        // Write all configuration files
        //
        for (Map.Entry<String, byte[]> config : overallEffective.getFileConfigurations().entrySet()) {
            Path configFile = workDirectory.toPath().resolve("etc/" + config.getKey());
            Files.write(configFile, config.getValue());
        }

        //
        // Compute startup
        //
        Profile startupOverlay = Profiles.getOverlay(startupProfile, allProfiles);
        Profile startupEffective = Profiles.getEffective(startupOverlay, false);
        // Load startup repositories
        Map<String, Features> startupRepositories = loadRepositories(startupEffective.getRepositories());
        // Compute startup feature dependencies
        Set<Feature> allStartupFeatures = new HashSet<>();
        for (Features repo : startupRepositories.values()) {
            allStartupFeatures.addAll(repo.getFeature());
        }
        Set<Feature> startupFeatures = new LinkedHashSet<>();
        if (startupEffective.getFeatures().isEmpty() && installAllFeaturesByDefault) {
            startupFeatures.addAll(allStartupFeatures);
        } else {
            for (String feature : startupEffective.getFeatures()) {
                addFeatures(startupFeatures, allStartupFeatures, feature);
            }
        }
        // Compute all bundles
        Map<String, Integer> allStartupBundles = new LinkedHashMap<>();
        for (Feature feature : startupFeatures) {
            for (Bundle bundleInfo : feature.getBundle()) {
                String bundleLocation = bundleInfo.getLocation();
                int bundleStartLevel = bundleInfo.getStartLevel() == 0 ? defaultStartLevel : bundleInfo.getStartLevel();
                if (allStartupBundles.containsKey(bundleLocation)) {
                    bundleStartLevel = Math.min(bundleStartLevel, allStartupBundles.get(bundleLocation));
                }
                allStartupBundles.put(bundleLocation, bundleStartLevel);
            }
            // Install config
            for (Config config : feature.getConfig()) {
                installConfig(config);
            }
            for (Conditional cond : feature.getConditional()) {
                for (Config config : cond.getConfig()) {
                    installConfig(config);
                }
            }
            // Install config files
            for (ConfigFile configFile : feature.getConfigfile()) {
                try (InputStream is = new URL(configFile.getLocation()).openStream()) {
                    String path = configFile.getFinalname();
                    if (path.startsWith("/")) {
                        path = path.substring(1);
                    }
                    Path output = workDirectory.toPath().resolve(path);
                    Files.copy(is, output, StandardCopyOption.REPLACE_EXISTING);  // TODO: be smarter about overwrites
                }
            }
            for (Conditional cond : feature.getConditional()) {
                for (ConfigFile configFile : cond.getConfigfile()) {
                    try (InputStream is = new URL(configFile.getLocation()).openStream()) {
                        Path output = workDirectory.toPath().resolve(configFile.getFinalname());
                        Files.copy(is, output, StandardCopyOption.REPLACE_EXISTING); // TODO: be smarter about overwrites
                    }
                }
            }
        }
        for (String bundleLocation : startupEffective.getBundles()) {
            int bundleStartLevel = defaultStartLevel;
            if (allStartupBundles.containsKey(bundleLocation)) {
                bundleStartLevel = Math.min(bundleStartLevel, allStartupBundles.get(bundleLocation));
            }
            allStartupBundles.put(bundleLocation, bundleStartLevel);
        }
        // Load startup.properties
        startupPropertiesFile.getParentFile().mkdirs();
        Properties startupProperties = new Properties(startupPropertiesFile);
        if (!startupPropertiesFile.exists()) {
            startupProperties.setHeader(Collections.singletonList("# Bundles to be started on startup, with startlevel"));
        }
        // Install bundles and update startup.properties
        Map<Integer, Set<String>> invertedStartupBundles = MapUtils.invert(allStartupBundles);
        for (Map.Entry<Integer, Set<String>> entry : invertedStartupBundles.entrySet()) {
            String startLevel = Integer.toString(entry.getKey());
            for (String location : new TreeSet<>(entry.getValue())) {
                location = installStartupArtifact(location, useReferenceUrls);
                if (location.startsWith("file:") && useReferenceUrls) {
                    location = "reference:" + location;
                }
                startupProperties.put(location, startLevel);
            }
        }
        // generate the startup.properties file
        startupProperties.save();


        //
        // Handle boot profiles
        //
        Profile bootOverlay = Profiles.getOverlay(bootProfile, allProfiles);
        Profile bootEffective = Profiles.getEffective(bootOverlay, false);
        // Load startup repositories
        Map<String, Features> bootRepositories = loadRepositories(bootEffective.getRepositories());
        // Compute startup feature dependencies
        Set<Feature> allBootFeatures = new HashSet<>();
        for (Features repo : bootRepositories.values()) {
            allBootFeatures.addAll(repo.getFeature());
        }
        // Install all repositories
        for (String repository : bootRepositories.keySet()) {
            installArtifact(repository);
        }
        // Generate a global feature
        Map<String, Dependency> generatedDep = new HashMap<>();
        Feature generated = new Feature();
        generated.setName(UUID.randomUUID().toString());
        // Add feature dependencies
        if (bootEffective.getFeatures().isEmpty()) {
            if (installAllFeaturesByDefault) {
                for (Features repo : bootRepositories.values()) {
                    for (Feature feature : repo.getFeature()) {
                        Dependency dep = generatedDep.get(feature.getName());
                        if (dep == null) {
                            dep = new Dependency();
                            dep.setName(feature.getName());
                            generated.getFeature().add(dep);
                            generatedDep.put(dep.getName(), dep);
                        }
                        dep.setDependency(false);
                    }
                }
            }
        } else {
            for (String dependency : bootEffective.getFeatures()) {
                Dependency dep = generatedDep.get(dependency);
                if (dep == null) {
                    dep = new Dependency();
                    dep.setName(dependency);
                    generated.getFeature().add(dep);
                    generatedDep.put(dep.getName(), dep);
                }
                dep.setDependency(false);
            }
        }
        // Add bundles
        for (String location : bootEffective.getBundles()) {
            location = location.replace("profile:", "file:etc/");
            Bundle bun = new Bundle();
            bun.setLocation(location);
            generated.getBundle().add(bun);
        }
        Features rep = new Features();
        rep.setName(UUID.randomUUID().toString());
        rep.getRepository().addAll(bootEffective.getRepositories());
        rep.getFeature().add(generated);
        allBootFeatures.add(generated);

        // Compute startup feature dependencies
        Set<Feature> bootFeatures = new HashSet<>();
        addFeatures(bootFeatures, allBootFeatures, generated.getName());
        for (Feature feature : bootFeatures) {
            // the feature is a startup feature, updating startup.properties file
            getLog().info("Feature " + feature.getName() + " is defined as a boot feature");
            // add the feature in the system folder
            Set<String> locations = new HashSet<>();
            for (Bundle bundle : feature.getBundle()) {
                if (!ignoreDependencyFlag || !bundle.isDependency()) {
                    locations.add(bundle.getLocation());
                }
            }
            for (Conditional cond : feature.getConditional()) {
                for (Bundle bundle : cond.getBundle()) {
                    if (!ignoreDependencyFlag || !bundle.isDependency()) {
                        locations.add(bundle.getLocation());
                    }
                }
            }
            for (String location : locations) {
                installArtifact(location);
                for (Map.Entry<String, List<String>> entry : prereqs.entrySet()) {
                    if (location.startsWith(entry.getKey())) {
                        for (String prereq : entry.getValue()) {
                            Dependency dep = generatedDep.get(prereq);
                            if (dep == null) {
                                dep = new Dependency();
                                dep.setName(prereq);
                                generated.getFeature().add(dep);
                                generatedDep.put(dep.getName(), dep);
                            }
                            dep.setPrerequisite(true);
                        }
                    }
                }
            }
            // Install config files
            for (ConfigFile configFile : feature.getConfigfile()) {
                installArtifact(configFile.getLocation());
            }
            for (Conditional cond : feature.getConditional()) {
                for (ConfigFile configFile : cond.getConfigfile()) {
                    installArtifact(configFile.getLocation());
                }
            }
        }

        // If there are bundles to install, we can't use the boot features only
        // so keep the generated feature
        if (!generated.getBundle().isEmpty()) {
            File output = new File(workDirectory, "etc/" + rep.getName() + ".xml");
            try (FileOutputStream os = new FileOutputStream(output)) {
                JaxbUtil.marshal(rep, os);
            }
            Properties featuresProperties = new Properties(featuresCfgFile);
            featuresProperties.put(FEATURES_REPOSITORIES, "file:${karaf.home}/etc/" + output.getName());
            featuresProperties.put(FEATURES_BOOT, generated.getName());
            featuresProperties.save();
        }
        else {
            String boot = "";
            for (Dependency dep : generatedDep.values()) {
                if (dep.isPrerequisite()) {
                    if (boot.isEmpty()) {
                        boot = "(";
                    } else {
                        boot = boot + ",";
                    }
                    boot = boot + dep.getName();
                }
            }
            if (!boot.isEmpty()) {
                boot = boot + ")";
            }
            // TODO: for dependencies, we'd need to resolve the features completely
            for (Dependency dep : generatedDep.values()) {
                if (!dep.isPrerequisite() && !dep.isDependency()) {
                    if (!boot.isEmpty()) {
                        boot = boot + ",";
                    }
                    boot = boot + dep.getName();
                }
            }
            String repos = "";
            for (String repo : new HashSet<>(rep.getRepository())) {
                if (!repos.isEmpty()) {
                    repos = repos + ",";
                }
                repos = repos + repo;
            }

            Properties featuresProperties = new Properties(featuresCfgFile);
            featuresProperties.put(FEATURES_REPOSITORIES, repos);
            featuresProperties.put(FEATURES_BOOT, boot);
            featuresProperties.save();
        }


        //
        // Handle installed profiles
        //
        Profile installedOverlay = Profiles.getOverlay(installedProfile, allProfiles);
        Profile installedEffective = Profiles.getEffective(installedOverlay, false);

        // Load startup repositories
        Map<String, Features> installedRepositories = loadRepositories(installedEffective.getRepositories());
        // Install all repositories
        for (String repository : installedRepositories.keySet()) {
            installArtifact(repository);
        }
        // Compute startup feature dependencies
        Set<Feature> allInstalledFeatures = new HashSet<>();
        for (Features repo : installedRepositories.values()) {
            allInstalledFeatures.addAll(repo.getFeature());
        }
        Set<Feature> installedFeatures = new LinkedHashSet<>();
        if (installedEffective.getFeatures().isEmpty() && installAllFeaturesByDefault) {
            installedFeatures.addAll(allInstalledFeatures);
        } else {
            // Add boot features for search
            allInstalledFeatures.addAll(allBootFeatures);
            for (String feature : installedEffective.getFeatures()) {
                addFeatures(installedFeatures, allInstalledFeatures, feature);
            }
        }
        for (Feature feature : installedFeatures) {
            for (Bundle bundle : feature.getBundle()) {
                if (!ignoreDependencyFlag || !bundle.isDependency()) {
                    installArtifact(bundle.getLocation());
                }
            }
            for (Conditional cond : feature.getConditional()) {
                for (Bundle bundle : cond.getBundle()) {
                    if (!ignoreDependencyFlag || !bundle.isDependency()) {
                        installArtifact(bundle.getLocation());
                    }
                }
            }
        }
        for (String location : installedEffective.getBundles()) {
            installArtifact(location);
        }
        // TODO: install config files
    }

    private Map<String, Features> loadRepositories(List<String> repositories) throws Exception {
        Map<String, Features> loaded = new HashMap<>();
        for (String repository : repositories) {
            doLoadRepository(loaded, repository);
        }
        return loaded;
    }

    private void doLoadRepository(Map<String, Features> loaded, String repository) throws Exception {
        if (!loaded.containsKey(repository)) {
            Features featuresModel = JaxbUtil.unmarshal(repository, false);
            loaded.put(repository, featuresModel);
            // recursively process the inner repositories
            for (String innerRepository : featuresModel.getRepository()) {
                doLoadRepository(loaded, innerRepository);
            }
        }
    }

    private Profile generateProfile(Map<String, Profile> allProfiles, List<String> profiles, List<String> repositories, List<String> features, List<String> bundles) {
        Profile profile = ProfileBuilder.Factory.create(UUID.randomUUID().toString())
                .setParents(profiles)
                .setRepositories(repositories)
                .setFeatures(features)
                .setBundles(bundles)
                .getProfile();
        allProfiles.put(profile.getId(), profile);
        return profile;
    }

    private List<String> nonNullList(List<String> list) {
        return list == null ? new ArrayList<String>() : list;
    }

    private void addFeatures(Set<Feature> startupFeatures, Set<Feature> features, String feature) {
        int nbFound = 0;
        for (Feature f : features) {
            String[] split = feature.split("/");
            if (split.length == 2) {
                if (f.getName().equals(split[0]) && f.getVersion().equals(split[1])) {
                    for (Dependency dep : f.getFeature()) {
                        addFeatures(startupFeatures, features, dep.getName());
                    }
                    startupFeatures.add(f);
                    nbFound++;
                }
            } else {
                if (feature.equals(f.getName())) {
                    for (Dependency dep : f.getFeature()) {
                        addFeatures(startupFeatures, features, dep.getName());
                    }
                    startupFeatures.add(f);
                    nbFound++;
                }
            }
        }
        if (nbFound == 0) {
            throw new IllegalStateException("Could not find matching feature for " + feature);
        }
    }

    private String installStartupArtifact(String location, boolean asFile) throws Exception {
        getLog().info("== Installing artifact " + location);
        String url;
        String path;
        if (location.startsWith("mvn:")) {
            url = location;
            path = dependencyHelper.pathFromMaven(location);
            if (asFile) {
                location = "file:" + path ;
            }
        } else {
            url = location.replace("profile:", "file:" + workDirectory.getAbsolutePath() + "/etc/");
            path = "generated/" + location.replaceAll("[^0-9a-zA-Z.\\-_]+", "_");
            location = "file:" + path;
        }
        File bundleSystemFile = new File(systemDirectory, path);
        if (!bundleSystemFile.exists()) {
            bundleSystemFile.getParentFile().mkdirs();
            try (InputStream is = new URL(url).openStream()) {
                Files.copy(is, bundleSystemFile.toPath());
            }
        }
        return location;
    }

    private void installArtifact(String location) throws Exception {
        getLog().info("== Installing artifact " + location);
        location = DownloadManagerHelper.stripUrl(location);
        location = DownloadManagerHelper.removeInlinedMavenRepositoryUrl(location);
        if (location.startsWith("mvn:")) {
            if (location.endsWith("/")) {
                // for bad formed URL (like in Camel for mustache-compiler), we remove the trailing /
                location = location.substring(0, location.length() - 1);
            }
            File inputFile = dependencyHelper.resolveById(location, getLog());
            File targetFile = new File(systemDirectory, dependencyHelper.pathFromMaven(location));
            copy(inputFile, targetFile);
            // add metadata for snapshot
            Artifact artifact = dependencyHelper.mvnToArtifact(location);
            if (artifact.isSnapshot()) {
                File metadataTarget = new File(targetFile.getParentFile(), "maven-metadata-local.xml");
                try {
                    MavenUtil.generateMavenMetadata(artifact, metadataTarget);
                } catch (Exception e) {
                    getLog().warn("Could not create maven-metadata-local.xml", e);
                    getLog().warn("It means that this SNAPSHOT could be overwritten by an older one present on remote repositories");
                }
            }
        } else {
            getLog().warn("Ignoring artifact " + location);
        }
    }

    private void installConfig(Config config) throws Exception {
        getLog().info("== Installing configuration " + config.getName());
        Path configFile = workDirectory.toPath().resolve("etc/" + config.getName());
        if (!Files.exists(configFile)) {
            Files.write(configFile, config.getValue().getBytes());
        } else if (config.isAppend()) {
            // TODO
        }
    }

}
