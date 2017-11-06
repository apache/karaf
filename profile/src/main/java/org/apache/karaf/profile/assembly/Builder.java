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
package org.apache.karaf.profile.assembly;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.felix.resolver.ResolverImpl;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Library;
import org.apache.karaf.features.internal.download.DownloadCallback;
import org.apache.karaf.features.internal.download.DownloadManager;
import org.apache.karaf.features.internal.download.Downloader;
import org.apache.karaf.features.internal.download.StreamProvider;
import org.apache.karaf.features.internal.model.Bundle;
import org.apache.karaf.features.internal.model.Conditional;
import org.apache.karaf.features.internal.model.ConfigFile;
import org.apache.karaf.features.internal.model.Dependency;
import org.apache.karaf.features.internal.model.Feature;
import org.apache.karaf.features.internal.model.Features;
import org.apache.karaf.features.internal.model.JaxbUtil;
import org.apache.karaf.features.internal.repository.BaseRepository;
import org.apache.karaf.features.internal.resolver.ResourceBuilder;
import org.apache.karaf.features.internal.service.Blacklist;
import org.apache.karaf.features.internal.service.Deployer;
import org.apache.karaf.features.internal.util.MapUtils;
import org.apache.karaf.kar.internal.Kar;
import org.apache.karaf.profile.Profile;
import org.apache.karaf.profile.ProfileBuilder;
import org.apache.karaf.profile.impl.Profiles;
import org.apache.karaf.tools.utils.KarafPropertiesEditor;
import org.apache.karaf.tools.utils.model.KarafPropertyEdits;
import org.apache.karaf.util.config.PropertiesLoader;
import org.apache.karaf.util.maven.Parser;
import org.ops4j.pax.url.mvn.MavenResolver;
import org.ops4j.pax.url.mvn.MavenResolvers;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Resource;
import org.osgi.service.resolver.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.singletonList;
import static java.util.jar.JarFile.MANIFEST_NAME;
import static org.apache.karaf.features.internal.service.Blacklist.TYPE_REPOSITORY;
import static org.apache.karaf.profile.assembly.Builder.Stage.Startup;

public class Builder {

    private static final String STATIC_FEATURES_KAR = "mvn:org.apache.karaf.features/static/%s/kar";

    private static final Logger LOGGER = LoggerFactory.getLogger(Builder.class);

    private static final String FEATURES_REPOSITORIES = "featuresRepositories";
    private static final String FEATURES_BOOT = "featuresBoot";

    private static final String LIBRARY_CLAUSE_TYPE = "type";
    private static final String LIBRARY_CLAUSE_EXPORT = "export";
    private static final String LIBRARY_CLAUSE_DELEGATE = "delegate";
    public static final String ORG_OPS4J_PAX_URL_MVN_PID = "org.ops4j.pax.url.mvn";

    public enum Stage {
        Startup, Boot, Installed
    }

    public enum KarafVersion {
        v24, v3x, v4x
    }


    public enum BlacklistPolicy {
        Discard,
        Fail
    }

    static class RepositoryInfo {
        Stage stage;
        boolean addAll;
    }

    //
    // Input parameters
    //

    List<String> profilesUris = new ArrayList<>();
    boolean defaultAddAll = true;
    Stage defaultStage = Stage.Startup;
    Map<String, RepositoryInfo> kars = new LinkedHashMap<>();
    Map<String, Stage> profiles = new LinkedHashMap<>();
    Map<String, RepositoryInfo> repositories = new LinkedHashMap<>();
    Map<String, Stage> features = new LinkedHashMap<>();
    Map<String, Stage> bundles = new LinkedHashMap<>();
    List<String> blacklistedProfiles = new ArrayList<>();
    List<String> blacklistedFeatures = new ArrayList<>();
    List<String> blacklistedBundles = new ArrayList<>();
    List<String> blacklistedRepositories = new ArrayList<>();
    BlacklistPolicy blacklistPolicy = BlacklistPolicy.Discard;
    List<String> libraries = new ArrayList<>();
    String javase = "1.8";
    KarafVersion karafVersion = KarafVersion.v4x;
    String environment = null;
    boolean useReferenceUrls;
    boolean ignoreDependencyFlag;
    int defaultStartLevel = 50;
    Path homeDirectory;
    boolean offline;
    String localRepository;
    String mavenRepositories;
    Map<String, String> config = new LinkedHashMap<>();
    Map<String, String> system = new LinkedHashMap<>();
    List<String> pidsToExtract = new LinkedList<>();

    private ScheduledExecutorService executor;
    private DownloadManager manager;
    private Resolver resolver;
    private Path etcDirectory;
    private Path systemDirectory;
    private Map<String, Profile> allProfiles;
    private KarafPropertyEdits propertyEdits;
    private Map<String, String> translatedUrls;

    private Function<MavenResolver, MavenResolver> resolverWrapper = null;

    public static Builder newInstance() {
        return new Builder();
    }

    public Builder defaultStage(Stage stage) {
        this.defaultStage = stage;
        return this;
    }

    public Builder defaultAddAll(boolean addAll) {
        this.defaultAddAll = addAll;
        return this;
    }

    public Builder profilesUris(String... profilesUri) {
        Collections.addAll(this.profilesUris, profilesUri);
        return this;
    }

    public Builder libraries(String... libraries) {
        Collections.addAll(this.libraries, libraries);
        return this;
    }

    public Builder kars(String... kars) {
        return kars(defaultStage, defaultAddAll, kars);
    }

    public Builder kars(boolean addAll, String... kars) {
        return kars(defaultStage, addAll, kars);
    }

    public Builder kars(Stage stage, boolean addAll, String... kars) {
        for (String kar : kars) {
            RepositoryInfo info = new RepositoryInfo();
            info.stage = stage;
            info.addAll = addAll;
            this.kars.put(kar, info);
        }
        return this;
    }

    public Builder repositories(String... repositories) {
        return repositories(defaultStage, defaultAddAll, repositories);
    }

    public Builder repositories(boolean addAll, String... repositories) {
        return repositories(defaultStage, addAll, repositories);
    }

    public Builder repositories(Stage stage, boolean addAll, String... repositories) {
        for (String repository : repositories) {
            RepositoryInfo info = new RepositoryInfo();
            info.stage = stage;
            info.addAll = addAll;
            this.repositories.put(repository, info);
        }
        return this;
    }

    public Builder features(String... features) {
        return features(defaultStage, features);
    }

    public Builder features(Stage stage, String... features) {
        for (String feature : features) {
            this.features.put(feature, stage);
        }
        return this;
    }

    public Builder bundles(String... bundles) {
        return bundles(defaultStage, bundles);
    }

    public Builder bundles(Stage stage, String... bundles) {
        for (String bundle : bundles) {
            this.bundles.put(bundle, stage);
        }
        return this;
    }

    public Builder profiles(String... profiles) {
        return profiles(defaultStage, profiles);
    }

    public Builder profiles(Stage stage, String... profiles) {
        for (String profile : profiles) {
            this.profiles.put(profile, stage);
        }
        return this;
    }

    public Builder homeDirectory(Path homeDirectory) {
        if (homeDirectory == null) {
            throw new IllegalArgumentException("homeDirectory is null");
        }
        this.homeDirectory = homeDirectory;
        return this;
    }

    public Builder javase(String javase) {
        if (javase == null) {
            throw new IllegalArgumentException("javase is null");
        }
        this.javase = javase;
        return this;
    }

    public Builder environment(String environment) {
        this.environment = environment;
        return this;
    }

    public Builder useReferenceUrls() {
        return useReferenceUrls(true);
    }

    public Builder useReferenceUrls(boolean useReferenceUrls) {
        this.useReferenceUrls = useReferenceUrls;
        return this;
    }

    public Builder karafVersion(KarafVersion karafVersion) {
        this.karafVersion = karafVersion;
        return this;
    }

    public Builder defaultStartLevel(int defaultStartLevel) {
        this.defaultStartLevel = defaultStartLevel;
        return this;
    }

    public Builder ignoreDependencyFlag() {
        return ignoreDependencyFlag(true);
    }

    public Builder ignoreDependencyFlag(boolean ignoreDependencyFlag) {
        this.ignoreDependencyFlag = ignoreDependencyFlag;
        return this;
    }

    public Builder offline(boolean offline) {
        this.offline = offline;
        return this;
    }

    public Builder offline() {
        return offline(true);
    }

    public Builder localRepository(String localRepository) {
        this.localRepository = localRepository;
        return this;
    }

    public Builder mavenRepositories(String mavenRepositories) {
        this.mavenRepositories = mavenRepositories;
        return this;
    }

    public Builder resolverWrapper(Function<MavenResolver, MavenResolver> wrapper) {
        this.resolverWrapper = wrapper;
        return this;
    }

    public Builder staticFramework() {
        // TODO: load this from resources
        return staticFramework("4.0.0-SNAPSHOT");
    }

    public Builder staticFramework(String version) {
        String staticFeaturesKar = String.format(STATIC_FEATURES_KAR, version);
        return this.defaultStage(Startup).useReferenceUrls().kars(Startup, true, staticFeaturesKar);
    }

    public Builder blacklistProfiles(Collection<String> profiles) {
        this.blacklistedProfiles.addAll(profiles);
        return this;
    }

    public Builder blacklistFeatures(Collection<String> features) {
        this.blacklistedFeatures.addAll(features);
        return this;
    }

    public Builder blacklistBundles(Collection<String> bundles) {
        this.blacklistedBundles.addAll(bundles);
        return this;
    }

    public Builder blacklistRepositories(Collection<String> repositories) {
        this.blacklistedRepositories.addAll(repositories);
        return this;
    }

    public Builder blacklistPolicy(BlacklistPolicy policy) {
        this.blacklistPolicy = policy;
        return this;
    }

    /**
     * Specify a set of edits to apply when moving etc files.
     * @param propertyEdits the edits.
     * @return this.
     */
    public Builder propertyEdits(KarafPropertyEdits propertyEdits) {
        this.propertyEdits = propertyEdits;
        return this;
    }

    public Builder pidsToExtract(List<String> pidsToExtract) {
        if (pidsToExtract != null) {
            for (String pid : pidsToExtract) {
                this.pidsToExtract.add(pid.trim());
            }
        }
        return this;
    }

    /**
     * Specify a set of url mappings to use instead of
     * downloading from the original urls.
     * @param translatedUrls the urls translations.
     * @return this.
     */
    public Builder translatedUrls(Map<String, String> translatedUrls) {
        this.translatedUrls = translatedUrls;
        return this;
    }

    public Builder config(String key, String value) {
        this.config.put(key, value);
        return this;
    }

    public Builder system(String key, String value) {
        this.system.put(key, value);
        return this;
    }

    public List<String> getBlacklistedProfiles() {
        return blacklistedProfiles;
    }

    public List<String> getBlacklistedFeatures() {
        return blacklistedFeatures;
    }

    public List<String> getBlacklistedBundles() {
        return blacklistedBundles;
    }

    public List<String> getBlacklistedRepositories() {
        return blacklistedRepositories;
    }

    public BlacklistPolicy getBlacklistPolicy() {
        return blacklistPolicy;
    }

    public List<String> getPidsToExtract() {
        return pidsToExtract;
    }

    public void generateAssembly() throws Exception {
        if (javase == null) {
            throw new IllegalArgumentException("javase is not set");
        }
        if (homeDirectory == null) {
            throw new IllegalArgumentException("homeDirectory is not set");
        }
        try {
            doGenerateAssembly();
        } finally {
            if (executor != null) {
                executor.shutdownNow();
            }
        }
    }

    private void doGenerateAssembly() throws Exception {
        systemDirectory = homeDirectory.resolve("system");
        etcDirectory = homeDirectory.resolve("etc");

        LOGGER.info("Generating karaf assembly: " + homeDirectory);

        //
        // Create download manager
        //
        Dictionary<String, String> props = new Hashtable<>();
        if (offline) {
            props.put(ORG_OPS4J_PAX_URL_MVN_PID + "offline", "true");
        }
        if (localRepository != null) {
            props.put(Builder.ORG_OPS4J_PAX_URL_MVN_PID + ".localRepository", localRepository);
        }
        if (mavenRepositories != null) {
            props.put(Builder.ORG_OPS4J_PAX_URL_MVN_PID + ".repositories", mavenRepositories);
        }
        MavenResolver resolver = MavenResolvers.createMavenResolver(props, ORG_OPS4J_PAX_URL_MVN_PID);
        if (resolverWrapper != null) {
            resolver = resolverWrapper.apply(resolver);
        }
        executor = Executors.newScheduledThreadPool(8);
        manager = new CustomDownloadManager(resolver, executor, null, translatedUrls);
        this.resolver = new ResolverImpl(new Slf4jResolverLog(LOGGER));

        //
        // Unzip kars
        //
        LOGGER.info("Unzipping kars");
        Map<String, RepositoryInfo> repositories = new LinkedHashMap<>(this.repositories);
        Downloader downloader = manager.createDownloader();
        for (String kar : kars.keySet()) {
            downloader.download(kar, null);
        }
        downloader.await();
        for (String karUri : kars.keySet()) {
            Kar kar = new Kar(manager.getProviders().get(karUri).getFile().toURI());
            kar.extract(systemDirectory.toFile(), homeDirectory.toFile());
            RepositoryInfo info = kars.get(karUri);
            for (URI repositoryUri : kar.getFeatureRepos()) {
                repositories.put(repositoryUri.toString(), info);
            }
        }

        //
        // Propagate feature installation from repositories
        //
        LOGGER.info("   Loading repositories");
        Map<String, Stage> features = new LinkedHashMap<>(this.features);
        Map<String, Features> karRepositories = loadRepositories(manager, repositories.keySet(), false);
        for (String repo : repositories.keySet()) {
            RepositoryInfo info = repositories.get(repo);
            if (info.addAll) {
                for (Feature feature : karRepositories.get(repo).getFeature()) {
                    features.put(feature.getId(), info.stage);
                }
            }
        }

        //
        // Load profiles
        //
        LOGGER.info("Loading profiles");
        allProfiles = new HashMap<>();
        for (String profilesUri : profilesUris) {
            String uri = profilesUri;
            if (uri.startsWith("jar:") && uri.contains("!/")) {
                uri = uri.substring("jar:".length(), uri.indexOf("!/"));
            }
            if (!uri.startsWith("file:")) {
                downloader = manager.createDownloader();
                downloader.download(uri, null);
                downloader.await();
                StreamProvider provider = manager.getProviders().get(uri);
                profilesUri = profilesUri.replace(uri, provider.getFile().toURI().toString());
            }
            URI profileURI = URI.create(profilesUri);
            Path profilePath;
            try {
                profilePath = Paths.get(profileURI);
            } catch (FileSystemNotFoundException e) {
                // file system does not exist, try to create it
                FileSystem fs = FileSystems.newFileSystem(profileURI, new HashMap<>(), Builder.class.getClassLoader());
                profilePath = fs.provider().getPath(profileURI);
            }
            allProfiles.putAll(Profiles.loadProfiles(profilePath));
            // Handle blacklisted profiles
            if (!blacklistedProfiles.isEmpty()) {
                if (blacklistPolicy == BlacklistPolicy.Discard) {
                    // Override blacklisted profiles with empty ones
                    for (String profile : blacklistedProfiles) {
                        allProfiles.put(profile, ProfileBuilder.Factory.create(profile).getProfile());
                    }
                } else {
                    // Remove profiles completely
                    allProfiles.keySet().removeAll(blacklistedProfiles);
                }
            }
        }

        // Generate profiles
        Profile startupProfile = generateProfile(Stage.Startup, profiles, repositories, features, bundles);
        profiles.put(startupProfile.getId(), Stage.Boot);
        Profile bootProfile = generateProfile(Stage.Boot, profiles, repositories, features, bundles);
        Profile installedProfile = generateProfile(Stage.Installed, profiles, repositories, features, bundles);

        //
        // Compute overall profile
        //
        ProfileBuilder builder = ProfileBuilder.Factory.create(UUID.randomUUID().toString())
                .setParents(Arrays.asList(startupProfile.getId(), bootProfile.getId(), installedProfile.getId()));
        config.forEach((k ,v) -> builder.addConfiguration(Profile.INTERNAL_PID, Profile.CONFIG_PREFIX + k, v));
        system.forEach((k ,v) -> builder.addConfiguration(Profile.INTERNAL_PID, Profile.SYSTEM_PREFIX + k, v));
        Profile overallProfile = builder
                .getProfile();
        Profile overallOverlay = Profiles.getOverlay(overallProfile, allProfiles, environment);
        Profile overallEffective = Profiles.getEffective(overallOverlay, false);

        manager = new CustomDownloadManager(resolver, executor, overallEffective, translatedUrls);

//        Hashtable<String, String> agentProps = new Hashtable<>(overallEffective.getConfiguration(ORG_OPS4J_PAX_URL_MVN_PID));
//        final Map<String, String> properties = new HashMap<>();
//        properties.put("karaf.default.repository", "system");
//        InterpolationHelper.performSubstitution(agentProps, properties::get, false, false, true);

        //
        // Write config and system properties
        //
        Path configPropertiesPath = etcDirectory.resolve("config.properties");
        Properties configProperties = new Properties(configPropertiesPath.toFile());
        configProperties.putAll(overallEffective.getConfig());
        configProperties.save();

        Path systemPropertiesPath = etcDirectory.resolve("system.properties");
        Properties systemProperties = new Properties(systemPropertiesPath.toFile());
        systemProperties.putAll(overallEffective.getSystem());
        systemProperties.save();

        //
        // Download libraries
        //
        // TODO: handle karaf 2.x and 3.x libraries
        LOGGER.info("Downloading libraries");
        downloader = manager.createDownloader();
        downloadLibraries(downloader, configProperties, overallEffective.getLibraries(), "");
        downloadLibraries(downloader, configProperties, libraries, "");
        downloader.await();
        // Reformat clauses
        reformatClauses(configProperties, Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
        reformatClauses(configProperties, Constants.FRAMEWORK_BOOTDELEGATION);
        configProperties.save();

        //
        // Write all configuration files
        //
        LOGGER.info("Writing configurations");
        for (Map.Entry<String, byte[]> config : overallEffective.getFileConfigurations().entrySet()) {
            Path configFile = etcDirectory.resolve(config.getKey());
            LOGGER.info("   adding config file: {}", homeDirectory.relativize(configFile));
            Files.createDirectories(configFile.getParent());
            Files.write(configFile, config.getValue());
        }

        // 'improve' configuration files.
        if (propertyEdits != null) {
            KarafPropertiesEditor editor = new KarafPropertiesEditor();
            editor.setInputEtc(etcDirectory.toFile())
                    .setOutputEtc(etcDirectory.toFile())
                    .setEdits(propertyEdits);
            editor.run();
        }

        //
        // Handle overrides
        //
        if (!overallEffective.getOverrides().isEmpty()) {
            Path overrides = etcDirectory.resolve("overrides.properties");
            List<String> lines = new ArrayList<>();
            lines.add("#");
            lines.add("# Generated by the karaf assembly builder");
            lines.add("#");
            lines.addAll(overallEffective.getOverrides());
            LOGGER.info("Generating {}", homeDirectory.relativize(overrides));
            Files.write(overrides, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }

        //
        // Handle blacklist
        //
        if (!blacklistedFeatures.isEmpty() || !blacklistedBundles.isEmpty()) {
            Path blacklist = etcDirectory.resolve("blacklisted.properties");
            List<String> lines = new ArrayList<>();
            lines.add("#");
            lines.add("# Generated by the karaf assembly builder");
            lines.add("#");
            if (!blacklistedFeatures.isEmpty()) {
                lines.add("");
                lines.add("# Features");
                lines.addAll(blacklistedFeatures);
            }
            if (!blacklistedBundles.isEmpty()) {
                lines.add("");
                lines.add("# Bundles");
                lines.addAll(blacklistedBundles);
            }
            LOGGER.info("Generating {}", homeDirectory.relativize(blacklist));
            Files.write(blacklist, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }

        //
        // Startup stage
        //
        Profile startupEffective = startupStage(startupProfile);

        //
        // Boot stage
        //
        Set<Feature> allBootFeatures = bootStage(bootProfile, startupEffective);

        //
        // Installed stage
        //
        installStage(installedProfile, allBootFeatures);
    }

    private void reformatClauses(Properties config, String key) {
        String val = config.getProperty(key);
        if (val != null && !val.isEmpty()) {
            List<String> comments = config.getComments(key);
            Clause[] clauses = org.apache.felix.utils.manifest.Parser.parseHeader(val);
            Set<String> strings = new LinkedHashSet<>();
            for (Clause clause : clauses) {
                strings.add(clause.toString());
            }
            List<String> lines = new ArrayList<>();
            lines.add("");
            int index = 0;
            for (String string : strings) {
                String s = "    " + string;
                if (index++ < strings.size() - 1) {
                    s += ", ";
                }
                lines.add(s);
            }
            config.put(key, comments, lines);
        }
    }

    void downloadLibraries(Downloader downloader, final Properties config, Collection<String> libraries, String indent) throws MalformedURLException {
        Clause[] clauses = org.apache.felix.utils.manifest.Parser.parseClauses(libraries.toArray(new String[libraries.size()]));
        for (final Clause clause : clauses) {
            final String filename;
            final String library;
            if (clause.getDirective("url") != null) {
                filename = clause.getName();
                library = clause.getDirective("url");
            } else {
                filename = null;
                library = clause.getName();
            }
            final String type = clause.getDirective(LIBRARY_CLAUSE_TYPE) != null
                    ? clause.getDirective(LIBRARY_CLAUSE_TYPE) : Library.TYPE_DEFAULT;
            if (!javase.startsWith("1.") && (Library.TYPE_ENDORSED.equals(type) || Library.TYPE_EXTENSION.equals(type))) {
                LOGGER.warn("Ignoring library " + library + " of type " + type + " which is only supported for Java 1.8.");
                continue;
            }
            final String path;
            switch (type) {
            case Library.TYPE_ENDORSED:  path = "lib/endorsed"; break;
            case Library.TYPE_EXTENSION: path = "lib/ext"; break;
            case Library.TYPE_BOOT:      path = "lib/boot"; break;
            default:                     path = "lib"; break;
            }
            downloader.download(library, provider -> {
                    synchronized (provider) {
                        Path input = provider.getFile().toPath();
                        String name = filename != null ? filename : input.getFileName().toString();
                        Path libOutput = homeDirectory.resolve(path).resolve(name);
                        LOGGER.info("{}   adding library: {}", indent, homeDirectory.relativize(libOutput));
                        Files.copy(input, libOutput, StandardCopyOption.REPLACE_EXISTING);
                        if (provider.getUrl().startsWith("mvn:")) {
                            // copy boot library in system repository
                            if (type.equals(Library.TYPE_BOOT)) {
                                String mvnPath = Parser.pathFromMaven(provider.getUrl());
                                Path sysOutput = systemDirectory.resolve(mvnPath);
                                Files.createDirectories(sysOutput.getParent());
                                Files.copy(input, sysOutput, StandardCopyOption.REPLACE_EXISTING);
                                libOutput = homeDirectory.resolve(path).resolve(name);
                                // copy the file
                                LOGGER.info("{}   adding maven library: {}", indent, provider.getUrl());
                                Files.copy(input, libOutput, StandardCopyOption.REPLACE_EXISTING);
                                /* a symlink could be used instead

                                if (Files.notExists(libOutput, LinkOption.NOFOLLOW_LINKS)) {
                                    try {
                                        Files.createSymbolicLink(libOutput, libOutput.getParent().relativize(sysOutput));
                                    } catch (FileSystemException e) {
                                        Files.copy(input, libOutput, StandardCopyOption.REPLACE_EXISTING);
                                    }
                                }
                                */
                            }
                        }
                    }
                    boolean export = Boolean.parseBoolean(clause.getDirective(LIBRARY_CLAUSE_EXPORT));
                    boolean delegate = Boolean.parseBoolean(clause.getDirective(LIBRARY_CLAUSE_DELEGATE));
                    if (export || delegate) {
                        Map<String, String> headers = getHeaders(provider);
                        String packages = headers.get(Constants.EXPORT_PACKAGE);
                        if (packages != null) {
                            Clause[] clauses1 = org.apache.felix.utils.manifest.Parser.parseHeader(packages);
                            if (export) {
                                String val = config.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
                                for (Clause clause1 : clauses1) {
                                    val += "," + clause1.toString();
                                }
                                config.setProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, val);
                            }
                            if (delegate) {
                                String val = config.getProperty(Constants.FRAMEWORK_BOOTDELEGATION);
                                for (Clause clause1 : clauses1) {
                                    val += "," + clause1.getName();
                                }
                                config.setProperty(Constants.FRAMEWORK_BOOTDELEGATION, val);
                            }
                        }
                    }
            });
        }
    }

    private void installStage(Profile installedProfile, Set<Feature> allBootFeatures) throws Exception {
        LOGGER.info("Install stage");
        //
        // Handle installed profiles
        //
        Profile installedOverlay = Profiles.getOverlay(installedProfile, allProfiles, environment);
        Profile installedEffective = Profiles.getEffective(installedOverlay, false);

        Downloader downloader = manager.createDownloader();

        // Load startup repositories
        LOGGER.info("   Loading repositories");
        Map<String, Features> installedRepositories = loadRepositories(manager, installedEffective.getRepositories(), true);
        // Compute startup feature dependencies
        Set<Feature> allInstalledFeatures = new HashSet<>();
        for (Features repo : installedRepositories.values()) {
            allInstalledFeatures.addAll(repo.getFeature());
        }
        
        // Add boot features for search
        allInstalledFeatures.addAll(allBootFeatures);
        FeatureSelector selector = new FeatureSelector(allInstalledFeatures);
        Set<Feature> installedFeatures = selector.getMatching(installedEffective.getFeatures());
        ArtifactInstaller installer = new ArtifactInstaller(systemDirectory, downloader, blacklistedBundles);
        for (Feature feature : installedFeatures) {
            LOGGER.info("   Feature {} is defined as an installed feature", feature.getId());
            for (Bundle bundle : feature.getBundle()) {
                if (!ignoreDependencyFlag || !bundle.isDependency()) {
                    installer.installArtifact(bundle.getLocation().trim());
                }
            }
            // Install config files
            for (ConfigFile configFile : feature.getConfigfile()) {
                installer.installArtifact(configFile.getLocation().trim());
            }
            for (Conditional cond : feature.getConditional()) {
                for (Bundle bundle : cond.getBundle()) {
                    if (!ignoreDependencyFlag || !bundle.isDependency()) {
                        installer.installArtifact(bundle.getLocation().trim());
                    }
                }
            }
        }
        for (String location : installedEffective.getBundles()) {
            installer.installArtifact(location);
        }
        downloader.await();
    }

    private Set<Feature> bootStage(Profile bootProfile, Profile startupEffective) throws Exception {
        LOGGER.info("Boot stage");
        //
        // Handle boot profiles
        //
        Profile bootOverlay = Profiles.getOverlay(bootProfile, allProfiles, environment);
        Profile bootEffective = Profiles.getEffective(bootOverlay, false);
        // Load startup repositories
        LOGGER.info("   Loading repositories");
        Map<String, Features> bootRepositories = loadRepositories(manager, bootEffective.getRepositories(), true);
        // Compute startup feature dependencies
        Set<Feature> allBootFeatures = new HashSet<>();
        for (Features repo : bootRepositories.values()) {
            allBootFeatures.addAll(repo.getFeature());
        }
        // Generate a global feature
        Map<String, Dependency> generatedDep = new HashMap<>();
        Feature generated = new Feature();
        generated.setName(UUID.randomUUID().toString());
        // Add feature dependencies
        for (String dependency : bootEffective.getFeatures()) {
            Dependency dep = generatedDep.get(dependency);
            if (dep == null) {
                dep = createDependency(dependency);
                generated.getFeature().add(dep);
                generatedDep.put(dep.getName(), dep);
            }
            dep.setDependency(false);
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

        Downloader downloader = manager.createDownloader();

        // Compute startup feature dependencies
        FeatureSelector selector = new FeatureSelector(allBootFeatures);
        Set<Feature> bootFeatures = selector.getMatching(singletonList(generated.getName()));
        for (Feature feature : bootFeatures) {
            // the feature is a startup feature, updating startup.properties file
            LOGGER.info("   Feature " + feature.getId() + " is defined as a boot feature");
            // add the feature in the system folder
            Set<String> locations = new HashSet<>();
            for (Bundle bundle : feature.getBundle()) {
                if (!ignoreDependencyFlag || !bundle.isDependency()) {
                    locations.add(bundle.getLocation().trim());
                }
            }
            for (Conditional cond : feature.getConditional()) {
                for (Bundle bundle : cond.getBundle()) {
                    if (!ignoreDependencyFlag || !bundle.isDependency()) {
                        locations.add(bundle.getLocation().trim());
                    }
                }
            }

            // Build optional features and known prerequisites
            Map<String, List<String>> prereqs = new HashMap<>();
            prereqs.put("blueprint:", Arrays.asList("deployer", "aries-blueprint"));
            prereqs.put("spring:", Arrays.asList("deployer", "spring"));
            prereqs.put("wrap:", Arrays.asList("wrap"));
            prereqs.put("war:", Arrays.asList("war"));
            ArtifactInstaller installer = new ArtifactInstaller(systemDirectory, downloader, blacklistedBundles);
            for (String location : locations) {
                installer.installArtifact(location);
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

            new ConfigInstaller(etcDirectory, pidsToExtract)
                .installConfigs(feature, downloader, installer);
            // Install libraries
            List<String> libraries = new ArrayList<>();
            for (Library library : feature.getLibraries()) {
                String lib = library.getLocation() +
                        ";type:=" + library.getType() +
                        ";export:=" + library.isExport() +
                        ";delegate:=" + library.isDelegate();
                libraries.add(lib);
            }
            Path configPropertiesPath = etcDirectory.resolve("config.properties");
            Properties configProperties = new Properties(configPropertiesPath.toFile());
            downloadLibraries(downloader, configProperties, libraries, "   ");
            downloader.await();
            // Reformat clauses
            reformatClauses(configProperties, Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
            reformatClauses(configProperties, Constants.FRAMEWORK_BOOTDELEGATION);
            configProperties.save();
        }

        // If there are bundles to install, we can't use the boot features only
        // so keep the generated feature
        Path featuresCfgFile = etcDirectory.resolve("org.apache.karaf.features.cfg");
        if (!generated.getBundle().isEmpty()) {
            File output = etcDirectory.resolve(rep.getName() + ".xml").toFile();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            JaxbUtil.marshal(rep, baos);
            ByteArrayInputStream bais;
            String repoUrl;
            if (karafVersion == KarafVersion.v24) {
                String str = baos.toString();
                str = str.replace("http://karaf.apache.org/xmlns/features/v1.3.0", "http://karaf.apache.org/xmlns/features/v1.2.0");
                str = str.replaceAll(" dependency=\".*?\"", "");
                str = str.replaceAll(" prerequisite=\".*?\"", "");
                for (Feature f : rep.getFeature()) {
                    for (Dependency d : f.getFeature()) {
                        if (d.isPrerequisite()) {
                            if (!startupEffective.getFeatures().contains(d.getName())) {
                                LOGGER.warn("Feature " + d.getName() + " is a prerequisite and should be installed as a startup feature.");                }
                        }
                    }
                }
                bais = new ByteArrayInputStream(str.getBytes());
                repoUrl = "file:etc/" + output.getName();
            } else {
                bais = new ByteArrayInputStream(baos.toByteArray());
                repoUrl = "file:${karaf.home}/etc/" + output.getName();
            }
            Files.copy(bais, output.toPath());
            Properties featuresProperties = new Properties(featuresCfgFile.toFile());
            featuresProperties.put(FEATURES_REPOSITORIES, repoUrl);
            featuresProperties.put(FEATURES_BOOT, generated.getName());
            featuresProperties.save();
        }
        else {
            String repos = getRepos(rep);
            String boot = getBootFeatures(generatedDep);

            Properties featuresProperties = new Properties(featuresCfgFile.toFile());
            featuresProperties.put(FEATURES_REPOSITORIES, repos);
            featuresProperties.put(FEATURES_BOOT, boot);
            reformatClauses(featuresProperties, FEATURES_REPOSITORIES);
            reformatClauses(featuresProperties, FEATURES_BOOT);
            featuresProperties.save();
        }
        downloader.await();
        return allBootFeatures;
    }



    private String getRepos(Features rep) {
        StringBuilder repos = new StringBuilder();
        for (String repo : new HashSet<>(rep.getRepository())) {
            if (repos.length() > 0) {
                repos.append(",");
            }
            repos.append(repo);
        }
        return repos.toString();
    }

    private String getBootFeatures(Map<String, Dependency> generatedDep) {
        StringBuilder boot = new StringBuilder();
        for (Dependency dep : generatedDep.values()) {
            if (dep.isPrerequisite()) {
                if (boot.length() == 0) {
                    boot.append("(");
                } else {
                    boot.append(",");
                }
                boot.append(dep.getName());
            }
        }
        if (boot.length() > 0) {
            boot.append(")");
        }
        // TODO: for dependencies, we'd need to resolve the features completely
        for (Dependency dep : generatedDep.values()) {
            if (!dep.isPrerequisite() && !dep.isDependency()) {
                if (boot.length() > 0) {
                    boot.append(",");
                }
                boot.append(dep.getName());
                if (!Feature.DEFAULT_VERSION.equals(dep.getVersion())) {
                    if (karafVersion == KarafVersion.v4x) {
                        boot.append("/");
                    } else {
                        boot.append(";version=");
                    }
                    boot.append(dep.getVersion());
                }
            }
        }
        return boot.toString();
    }

    private Dependency createDependency(String dependency) {
        Dependency dep;
        dep = new Dependency();
        String[] split = dependency.split("/");
        dep.setName(split[0]);
        if (split.length > 1) {
            dep.setVersion(split[1]);
        }
        return dep;
    }

    private Profile startupStage(Profile startupProfile) throws Exception {
        LOGGER.info("Startup stage");
        //
        // Compute startup
        //
        Profile startupOverlay = Profiles.getOverlay(startupProfile, allProfiles, environment);
        Profile startupEffective = Profiles.getEffective(startupOverlay, false);
        // Load startup repositories
        LOGGER.info("   Loading repositories");
        Map<String, Features> startupRepositories = loadRepositories(manager, startupEffective.getRepositories(), false);

        //
        // Resolve
        //
        LOGGER.info("   Resolving features");
        Map<String, Integer> bundles =
                resolve(manager,
                        resolver,
                        startupRepositories.values(),
                        startupEffective.getFeatures(),
                        startupEffective.getBundles(),
                        startupEffective.getOverrides(),
                        startupEffective.getOptionals());

        //
        // Generate startup.properties
        //
        Properties startup = new Properties();
        startup.setHeader(Collections.singletonList("# Bundles to be started on startup, with startlevel"));
        Map<Integer, Set<String>> invertedStartupBundles = MapUtils.invert(bundles);
        for (Map.Entry<Integer, Set<String>> entry : new TreeMap<>(invertedStartupBundles).entrySet()) {
            String startLevel = Integer.toString(entry.getKey());
            for (String location : new TreeSet<>(entry.getValue())) {
                if (useReferenceUrls) {
                    if (location.startsWith("mvn:")) {
                        location = "file:" + Parser.pathFromMaven(location);
                    }
                    if (location.startsWith("file:")) {
                        location = "reference:" + location;
                    }
                }
                if (location.startsWith("file:") && karafVersion == KarafVersion.v24) {
                    location = location.substring("file:".length());
                }
                startup.put(location, startLevel);
            }
        }
        Path startupProperties = etcDirectory.resolve("startup.properties");
        startup.save(startupProperties.toFile());
        return startupEffective;
    }

    private List<String> getStaged(Stage stage, Map<String, Stage> data) {
        List<String> staged = new ArrayList<>();
        for (String s : data.keySet()) {
            if (data.get(s) == stage) {
                staged.add(s);
            }
        }
        return staged;
    }

    private List<String> getStagedRepositories(Stage stage, Map<String, RepositoryInfo> data) {
        List<String> staged = new ArrayList<>();
        for (String s : data.keySet()) {
            if (data.get(s).stage == stage ||
                    data.get(s).stage == Stage.Startup && stage == Stage.Boot) {
                // For boot stage, we also want the startup repositories
                staged.add(s);
            }
        }
        return staged;
    }

    private Map<String, Features> loadRepositories(DownloadManager manager, Collection<String> repositories, final boolean install) throws Exception {
        final Map<String, Features> loaded = new HashMap<>();
        final Downloader downloader = manager.createDownloader();
        final List<String> blacklist = new ArrayList<>();
        blacklist.addAll(blacklistedBundles);
        blacklist.addAll(blacklistedFeatures);
        final List<String> blacklistRepos = new ArrayList<>();
        blacklistRepos.addAll(blacklistedRepositories);
        final Blacklist blacklistOther = new Blacklist(blacklist);
        final Blacklist repoBlacklist = new Blacklist(blacklistRepos);
        for (String repository : repositories) {
            downloader.download(repository, new DownloadCallback() {
                @Override
                public void downloaded(final StreamProvider provider) throws Exception {
                    String url = provider.getUrl();
                    if (repoBlacklist.isRepositoryBlacklisted(url)) {
                        LOGGER.info("      feature repository " + url + " is blacklisted");
                        return;
                    }
                    synchronized (loaded) {
                        if (!loaded.containsKey(provider.getUrl())) {
                            if (install) {
                                synchronized (provider) {
                                    Path path = ArtifactInstaller.pathFromProviderUrl(systemDirectory, url);
                                    Files.createDirectories(path.getParent());
                                    LOGGER.info("      adding feature repository: " + url);
                                    Files.copy(provider.getFile().toPath(), path, StandardCopyOption.REPLACE_EXISTING);
                                }
                            }
                            try (InputStream is = provider.open()) {
                                Features featuresModel = JaxbUtil.unmarshal(url, is, false);
                                if (blacklistPolicy == BlacklistPolicy.Discard) {
                                    blacklistOther.blacklist(featuresModel);
                                }
                                loaded.put(provider.getUrl(), featuresModel);
                                for (String innerRepository : featuresModel.getRepository()) {
                                    downloader.download(innerRepository, this);
                                }
                            }
                        }
                    }
                }
            });
        }
        downloader.await();
        return loaded;
    }

    private Profile generateProfile(Stage stage, Map<String, Stage> profiles, Map<String, RepositoryInfo> repositories, Map<String, Stage> features, Map<String, Stage> bundles) {
        Profile profile = ProfileBuilder.Factory.create(UUID.randomUUID().toString())
                .setParents(getStaged(stage, profiles))
                .setRepositories(getStagedRepositories(stage, repositories))
                .setFeatures(getStaged(stage, features))
                .setBundles(getStaged(stage, bundles))
                .getProfile();
        allProfiles.put(profile.getId(), profile);
        return profile;
    }

    private Map<String, Integer> resolve(
                    DownloadManager manager,
                    Resolver resolver,
                    Collection<Features> repositories,
                    Collection<String> features,
                    Collection<String> bundles,
                    Collection<String> overrides,
                    Collection<String> optionals) throws Exception {
        BundleRevision systemBundle = getSystemBundle();
        AssemblyDeployCallback callback = new AssemblyDeployCallback(manager, this, systemBundle, repositories);
        Deployer deployer = new Deployer(manager, resolver, callback);

        // Install framework
        Deployer.DeploymentRequest request = createDeploymentRequest();
        // Add overrides
        request.overrides.addAll(overrides);
        // Add optional resources
        final List<Resource> resources = new ArrayList<>();
        Downloader downloader = manager.createDownloader();
        for (String optional : optionals) {
            downloader.download(optional, provider -> {
                    Resource resource = ResourceBuilder.build(provider.getUrl(), getHeaders(provider));
                    synchronized (resources) {
                        resources.add(resource);
                    }
            });
        }
        downloader.await();
        request.globalRepository = new BaseRepository(resources);
        // Install features
        for (String feature : features) {
            MapUtils.addToMapSet(request.requirements, FeaturesService.ROOT_REGION, feature);
        }
        for (String bundle : bundles) {
            MapUtils.addToMapSet(request.requirements, FeaturesService.ROOT_REGION, "bundle:" + bundle);
        }
        Set<String> prereqs = new HashSet<>();
        while (true) {
            try {
                deployer.deploy(callback.getDeploymentState(), request);
                break;
            } catch (Deployer.PartialDeploymentException e) {
                if (!prereqs.containsAll(e.getMissing())) {
                    prereqs.addAll(e.getMissing());
                } else {
                    throw new Exception("Deployment aborted due to loop in missing prerequisites: " + e.getMissing());
                }
            }
        }

        return callback.getStartupBundles();
    }

    private Deployer.DeploymentRequest createDeploymentRequest() {
        Deployer.DeploymentRequest request = new Deployer.DeploymentRequest();
        request.bundleUpdateRange = FeaturesService.DEFAULT_BUNDLE_UPDATE_RANGE;
        request.featureResolutionRange = FeaturesService.DEFAULT_FEATURE_RESOLUTION_RANGE;
        request.serviceRequirements = FeaturesService.SERVICE_REQUIREMENTS_DEFAULT;
        request.overrides = new HashSet<>();
        request.requirements = new HashMap<>();
        request.stateChanges = new HashMap<>();
        request.options = EnumSet.noneOf(FeaturesService.Option.class);
        return request;
    }

    @SuppressWarnings("rawtypes")
    private BundleRevision getSystemBundle() throws Exception {
        Path configPropPath = etcDirectory.resolve("config.properties");
        Properties configProps = PropertiesLoader.loadPropertiesOrFail(configPropPath.toFile());
        configProps.put("java.specification.version", javase);
        configProps.substitute();

        Attributes attributes = new Attributes();
        attributes.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
        attributes.putValue(Constants.BUNDLE_SYMBOLICNAME, "system.bundle");
        attributes.putValue(Constants.BUNDLE_VERSION, "0.0.0");

        String exportPackages = configProps.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES);
        if (configProps.containsKey(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA)) {
            exportPackages += "," + configProps.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
        }
        exportPackages = exportPackages.replaceAll(",\\s*,", ",");
        attributes.putValue(Constants.EXPORT_PACKAGE, exportPackages);

        String systemCaps = configProps.getProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES);
        attributes.putValue(Constants.PROVIDE_CAPABILITY, systemCaps);

        final Hashtable<String, String> headers = new Hashtable<>();
        for (Map.Entry attr : attributes.entrySet()) {
            headers.put(attr.getKey().toString(), attr.getValue().toString());
        }

        return new FakeBundleRevision(headers, "system-bundle", 0L);
    }

    @SuppressWarnings("rawtypes")
    private Map<String, String> getHeaders(StreamProvider provider) throws IOException {
        try (
                ZipInputStream zis = new ZipInputStream(provider.open())
        ) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (MANIFEST_NAME.equals(entry.getName())) {
                    Attributes attributes = new Manifest(zis).getMainAttributes();
                    Map<String, String> headers = new HashMap<>();
                    for (Map.Entry attr : attributes.entrySet()) {
                        headers.put(attr.getKey().toString(), attr.getValue().toString());
                    }
                    return headers;
                }
            }
        }
        throw new IllegalArgumentException("Resource " + provider.getUrl() + " does not contain a manifest");
    }

}
