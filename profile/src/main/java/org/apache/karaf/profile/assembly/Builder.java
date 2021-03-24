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

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.felix.resolver.ResolverImpl;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.properties.Properties;
import org.apache.felix.utils.repository.BaseRepository;
import org.apache.felix.utils.resource.ResourceBuilder;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.FeaturePattern;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Library;
import org.apache.karaf.features.LocationPattern;
import org.apache.karaf.features.internal.download.DownloadCallback;
import org.apache.karaf.features.internal.download.DownloadManager;
import org.apache.karaf.features.internal.download.Downloader;
import org.apache.karaf.features.internal.download.StreamProvider;
import org.apache.karaf.features.internal.download.impl.DownloadManagerHelper;
import org.apache.karaf.features.internal.model.*;
import org.apache.karaf.features.internal.model.processing.FeaturesProcessing;
import org.apache.karaf.features.internal.service.Blacklist;
import org.apache.karaf.features.internal.service.Deployer;
import org.apache.karaf.features.internal.service.FeaturesProcessor;
import org.apache.karaf.features.internal.service.FeaturesProcessorImpl;
import org.apache.karaf.features.internal.service.Overrides;
import org.apache.karaf.features.internal.util.MapUtils;
import org.apache.karaf.features.internal.util.MultiException;
import org.apache.karaf.kar.internal.Kar;
import org.apache.karaf.profile.Profile;
import org.apache.karaf.profile.ProfileBuilder;
import org.apache.karaf.profile.impl.Profiles;
import org.apache.karaf.tools.utils.KarafPropertiesEditor;
import org.apache.karaf.tools.utils.model.KarafPropertyEdits;
import org.apache.karaf.util.ThreadUtils;
import org.apache.karaf.util.Version;
import org.apache.karaf.util.config.PropertiesLoader;
import org.apache.karaf.util.maven.Parser;
import org.ops4j.pax.url.mvn.MavenResolver;
import org.ops4j.pax.url.mvn.MavenResolvers;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.jar.JarFile.MANIFEST_NAME;
import static java.util.stream.Collectors.toMap;
import static org.apache.karaf.profile.assembly.Builder.Stage.Startup;

/**
 * A builder-like class to create instances of {@link Profile profiles}.
 */
public class Builder {

    private static final String STATIC_FEATURES_KAR = "mvn:org.apache.karaf.features/static/%s/kar";

    private static final Logger LOGGER = LoggerFactory.getLogger(Builder.class);

    private static final String FEATURES_REPOSITORIES = "featuresRepositories";
    private static final String FEATURES_BOOT = "featuresBoot";

    private static final String LIBRARY_CLAUSE_TYPE = "type";
    private static final String LIBRARY_CLAUSE_EXPORT = "export";
    private static final String LIBRARY_CLAUSE_DELEGATE = "delegate";
    private static final String START_LEVEL = "start-level";

    public static final String ORG_OPS4J_PAX_URL_MVN_PID = "org.ops4j.pax.url.mvn";

    /**
     * <p>An indication of <em>stage</em> for bundles/features/repositories/kars/profiles.</p>
     */
    public enum Stage {
        /**
         * Karaf runtime is in <em>startup</em> stage when it installs OSGi bundles into OSGi framework before
         * passing this responsibility to {@link FeaturesService}. A list of bundles to install is defined
         * in <code>${karaf.etc}/startup.properties</code>.
         */
        Startup,
        /**
         * Karaf runtime is in <em>boot</em> stage when it installs OSGi bundles using Karaf features. Features
         * (and features XML repositories) are defined in <code>${karaf.etc}/org.apache.karaf.features.cfg</code>.
         * Repositories and features available in startup stage should be <em>visible</em> in boot stage as well, as
         * this is the stage where term <em>Karaf feature</em> gets its meaning.
         */
        Boot,
        /**
         * <em>Installed</em> stage is just a space where bundles and features may be installed after starting
         * Karaf runtime (e.g., using Karaf shell commands, JMX or UI).
         */
        Installed;

        /**
         * Get a {@link Stage} corresponding to Maven scope.
         * @param scope
         * @return
         */
        public static Stage fromMavenScope(String scope) {
            switch (scope) {
                case "compile":
                    return Builder.Stage.Startup;
                case "runtime":
                    return Builder.Stage.Boot;
                case "provided":
                    return Builder.Stage.Installed;
                default:
                    return null;
            }
        }
    }

    /**
     * <p>An identifiier of Karaf version <em>family</em>. Each version family may have special methods
     * or requirements for generating/preparing configuration.</p>
     */
    public enum KarafVersion {
        v24, v3x, v4x
    }

    /**
     * <p>An idenfifier for supported Java version. This version is used for example in
     * <code>${karaf.etc}/jre.properties</code> to define system packages for given Java version. Only
     * supported versions are defined.</p>
     */
    public enum JavaVersion {
        Java16("1.6", 1),
        Java17("1.7", 2),
        Java18("1.8", 3),
        Java9("9", 4),
        Java10("10", 5),
        Java11("11", 6);

        private String version;
        private int ordinal;

        JavaVersion(String version, int ordinal) {
            this.version = version;
            this.ordinal = ordinal;
        }

        public static JavaVersion from(String version) {
            for (JavaVersion value : values()) {
                if (value.version.equals(version)) {
                    return value;
                }
            }
            throw new IllegalArgumentException("Java version \"" + version + "\" is not supported");
        }

        public boolean supportsEndorsedAndExtLibraries() {
            return this.ordinal < Java9.ordinal;
        }
    }

    /**
     * TODOCUMENT
     */
    public enum BlacklistPolicy {
        Discard,
        Fail
    }

    /**
     * Configuration of features XML repository (standalone or inside KAR). <code>addAll</code> may configure
     * given repository to install all defined features if no explicit feature is specified.
     */
    static class RepositoryInfo {
        Stage stage;
        boolean addAll;

        public RepositoryInfo(Stage stage, boolean addAll) {
            this.stage = stage;
            this.addAll = addAll;
        }
    }

    /**
     * Class similar to {@link FeaturePattern} but simplified for profile name matching
     */
    private static class ProfileNamePattern {
        private String name;
        private Pattern namePattern;


        public ProfileNamePattern(String profileName) {
            if (profileName == null) {
                throw new IllegalArgumentException("Profile name to match should not be null");
            }
            name = profileName;
            if (name.contains("*")) {
                namePattern = LocationPattern.toRegExp(name);
            }
        }

        /**
         * Returns <code>if this feature pattern</code> matches given feature/version
         * @param profileName
         * @return
         */
        public boolean matches(String profileName) {
            if (profileName == null) {
                return false;
            }
            if (namePattern != null) {
                return namePattern.matcher(profileName).matches();
            } else {
                return name.equals(profileName);
            }
        }
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
    List<String> blacklistedProfileNames = new ArrayList<>();
    List<String> blacklistedFeatureIdentifiers = new ArrayList<>();
    List<String> blacklistedBundleURIs = new ArrayList<>();
    List<String> blacklistedRepositoryURIs = new ArrayList<>();
    BlacklistPolicy blacklistPolicy = BlacklistPolicy.Discard;
    List<String> libraries = new ArrayList<>();
    JavaVersion javase = JavaVersion.Java18;
    KarafVersion karafVersion = KarafVersion.v4x;
    String environment = null;
    boolean useReferenceUrls;
    boolean ignoreDependencyFlag;
    int defaultStartLevel = 50;
    Path homeDirectory;
    Path featuresProcessingLocation;
    boolean offline;
    String localRepository;
    String mavenRepositories;
    Map<String, String> config = new LinkedHashMap<>();
    Map<String, String> system = new LinkedHashMap<>();
    List<String> pidsToExtract = new LinkedList<>();
    boolean writeProfiles;
    String generateConsistencyReport;
    String consistencyReportProjectName;
    String consistencyReportProjectVersion;
    // KARAF-7074: for recursive/inner features, we should use parallelism by default
    int resolverParallelism = Math.max(2, Runtime.getRuntime().availableProcessors());

    private ScheduledExecutorService executor;
    private DownloadManager manager;
    private Resolver resolver;
    private Path etcDirectory;
    private Path systemDirectory;
    private Map<String, Profile> allProfiles;
    private KarafPropertyEdits propertyEdits;
    private FeaturesProcessing featuresProcessing = new FeaturesProcessing();
    private Map<String, String> translatedUrls;
    private Blacklist blacklist;
    private String generatedBootFeatureName;

    private Function<MavenResolver, MavenResolver> resolverWrapper = Function.identity();

    public static Builder newInstance() {
        return new Builder();
    }

    /**
     * Sets the {@link Stage} used by next builder invocations.
     * @param stage
     * @return
     */
    public Builder defaultStage(Stage stage) {
        this.defaultStage = stage;
        return this;
    }

    /**
     * Sets default <em>add all</em> flag for KARs and repositories.
     * @param addAll
     * @return
     */
    public Builder defaultAddAll(boolean addAll) {
        this.defaultAddAll = addAll;
        return this;
    }

    /**
     * Configure a list of profile URIs to be used for profile import
     * @param profilesUri
     * @return
     */
    public Builder profilesUris(String... profilesUri) {
        Collections.addAll(this.profilesUris, profilesUri);
        return this;
    }

    /**
     * Configure libraries to use. Each library may contain OSGi header-like directives: <code>type</code>,
     * <code>url</code>, <code>export</code> and <code>delegate</code>.
     * @param libraries
     * @return
     */
    public Builder libraries(String... libraries) {
        Collections.addAll(this.libraries, libraries);
        return this;
    }

    /**
     * Configure KARs to use at current {@link #defaultStage stage} with default <em>add all</em> flag
     * @param kars
     * @return
     */
    public Builder kars(String... kars) {
        return kars(defaultStage, defaultAddAll, kars);
    }

    /**
     * Configure KARs to use at current {@link #defaultStage stage} with given <em>add all</em> flag
     * @param addAll
     * @param kars
     * @return
     */
    public Builder kars(boolean addAll, String... kars) {
        return kars(defaultStage, addAll, kars);
    }

    /**
     * Configure KARs to use at given stage with given <em>add all</em> flag
     * @param stage
     * @param addAll
     * @param kars
     * @return
     */
    public Builder kars(Stage stage, boolean addAll, String... kars) {
        for (String kar : kars) {
            this.kars.put(kar, new RepositoryInfo(stage, addAll));
        }
        return this;
    }

    /**
     * Configure features XML repositories to use at current {@link #defaultStage stage} with default <em>add all</em> flag
     * @param repositories
     * @return
     */
    public Builder repositories(String... repositories) {
        return repositories(defaultStage, defaultAddAll, repositories);
    }

    /**
     * Configure features XML repositories to use at current {@link #defaultStage stage} with given <em>add all</em> flag
     * @param addAll
     * @param repositories
     * @return
     */
    public Builder repositories(boolean addAll, String... repositories) {
        return repositories(defaultStage, addAll, repositories);
    }

    /**
     * Configure features XML repositories to use at given stage with given <em>add all</em> flag
     * @param stage
     * @param addAll
     * @param repositories
     * @return
     */
    public Builder repositories(Stage stage, boolean addAll, String... repositories) {
        for (String repository : repositories) {
            this.repositories.put(repository, new RepositoryInfo(stage, addAll));
        }
        return this;
    }

    /**
     * Configure features to use at current {@link #defaultStage stage}. Each feature may be specified as
     * <code>name</code> or <code>name/version</code> (no version ranges allowed).
     * @param features
     * @return
     */
    public Builder features(String... features) {
        return features(defaultStage, features);
    }

    /**
     * Configure features to use at given stage. Each feature may be specified as <code>name</code> or
     * <code>name/version</code> (no version ranges allowed).
     * @param stage
     * @param features
     * @return
     */
    public Builder features(Stage stage, String... features) {
        for (String feature : features) {
            this.features.put(feature, stage);
        }
        return this;
    }

    /**
     * Configure bundle URIs to use at current {@link #defaultStage stage}.
     * @param bundles
     * @return
     */
    public Builder bundles(String... bundles) {
        return bundles(defaultStage, bundles);
    }

    /**
     * Configure bundle URIs to use at given stage.
     * @param stage
     * @param bundles
     * @return
     */
    public Builder bundles(Stage stage, String... bundles) {
        for (String bundle : bundles) {
            this.bundles.put(bundle, stage);
        }
        return this;
    }

    /**
     * Configure profiles to use at current {@link #defaultStage stage}.
     * @param profiles
     * @return
     */
    public Builder profiles(String... profiles) {
        return profiles(defaultStage, profiles);
    }

    /**
     * Configure profiles to use at given stage.
     * @param stage
     * @param profiles
     * @return
     */
    public Builder profiles(Stage stage, String... profiles) {
        for (String profile : profiles) {
            this.profiles.put(profile, stage);
        }
        return this;
    }

    /**
     * Configure target directory, where distribution is being assembled.
     * @param homeDirectory
     * @return
     */
    public Builder homeDirectory(Path homeDirectory) {
        if (homeDirectory == null) {
            throw new IllegalArgumentException("homeDirectory is null");
        }
        this.homeDirectory = homeDirectory;
        return this;
    }

    /**
     * Configure Java version to use. This version will be resolved in several property placeholders inside
     * <code>${karaf.etc}/config.properties</code> and <code>${karaf.etc}/jre.properties</code>.
     * @param javase
     * @return
     */
    public Builder javase(String javase) {
        if (javase == null) {
            throw new IllegalArgumentException("javase is null");
        }
        this.javase = JavaVersion.from(javase);
        return this;
    }

    /**
     * Set environment to use that may be used to select different variant of PID configuration file, e.g.,
     * <code>org.ops4j.pax.url.mvn.cfg#docker</code>.
     * @param environment
     * @return
     */
    public Builder environment(String environment) {
        this.environment = environment;
        return this;
    }

    /**
     * Configure builder to generate <code>reference:</code>-like URIs in <code>${karaf.etc}/startup.properties</code>.
     * Bundles declared in this way are not copied (by Felix) to <code>data/cache</code> directory, but are
     * used from original location.
     * @return
     */
    public Builder useReferenceUrls() {
        return useReferenceUrls(true);
    }

    /**
     * Configure builder to use (when <code>true</code>) <code>reference:</code>-like URIs in
     * <code>${karaf.etc}/startup.properties</code>.
     * @param useReferenceUrls
     * @return
     */
    public Builder useReferenceUrls(boolean useReferenceUrls) {
        this.useReferenceUrls = useReferenceUrls;
        return this;
    }

    public Builder resolverParallelism(final int resolverParallelism) {
        this.resolverParallelism = resolverParallelism;
        return this;
    }

    /**
     * Configure builder to copy generated and configured profiles into <code>${karaf.etc}/profiles</code>
     * directory.
     * @param writeProfiles
     */
    public void writeProfiles(boolean writeProfiles) {
        this.writeProfiles = writeProfiles;
    }

    /**
     * Configure builder to generate consistency report
     * @param generateConsistencyReport
     */
    public void generateConsistencyReport(String generateConsistencyReport) {
        this.generateConsistencyReport = generateConsistencyReport;
    }

    /**
     * Configure project name to be used in consistency report
     * @param consistencyReportProjectName
     */
    public void setConsistencyReportProjectName(String consistencyReportProjectName) {
        this.consistencyReportProjectName = consistencyReportProjectName;
    }

    /**
     * Configure project version to be used in consistency report
     * @param consistencyReportProjectVersion
     */
    public void setConsistencyReportProjectVersion(String consistencyReportProjectVersion) {
        this.consistencyReportProjectVersion = consistencyReportProjectVersion;
    }

    /**
     * Configure Karaf version to target. This impacts the way some configuration files are generated.
     * @param karafVersion
     * @return
     */
    public Builder karafVersion(KarafVersion karafVersion) {
        this.karafVersion = karafVersion;
        return this;
    }

    /**
     * Sets default start level for bundles declared in <code>${karaf.etc}/startup.properties</code>.
     * @param defaultStartLevel
     * @return
     */
    public Builder defaultStartLevel(int defaultStartLevel) {
        this.defaultStartLevel = defaultStartLevel;
        return this;
    }

    /**
     * <p>Configures custom location for a file with features processing instructions. Normally this file is generated
     * by the builder if any of blacklisted options are configured.</p>
     * <p>If custom location is provided and it's not <code>etc/org.apache.karaf.features.xml</code>, it is copied</p>
     * <p>If custom location is provided and it's <code>etc/org.apache.karaf.features.xml</code>, it's left as is</p>
     * <p>Any additional blacklisting/overrides configuration via Maven configuration causes overwrite of original
     * content.</p>
     * @param featuresProcessing
     */
    public Builder setFeaturesProcessing(Path featuresProcessing) {
        this.featuresProcessingLocation = featuresProcessing;
        return this;
    }

    /**
     * Ignore the dependency attribute (dependency="[true|false]") on bundles, effectively forcing their
     * installation.
     */
    public Builder ignoreDependencyFlag() {
        return ignoreDependencyFlag(true);
    }

    /**
     * Configures builder to ignore (or not) <code>dependency</code> flag on bundles declared
     * in features XML file.
     * @param ignoreDependencyFlag
     * @return
     */
    public Builder ignoreDependencyFlag(boolean ignoreDependencyFlag) {
        this.ignoreDependencyFlag = ignoreDependencyFlag;
        return this;
    }

    /**
     * Configures builder to use offline pax-url-aether resolver
     * @return
     */
    public Builder offline() {
        return offline(true);
    }

    /**
     * Configures whether pax-url-aether resolver should work in offline mode
     * @param offline
     * @return
     */
    public Builder offline(boolean offline) {
        this.offline = offline;
        return this;
    }

    /**
     * Configures local Maven repository to use by pax-url-aether. By default, assembly mojo sets the value
     * read from current Maven build.
     * @param localRepository
     * @return
     */
    public Builder localRepository(String localRepository) {
        this.localRepository = localRepository;
        return this;
    }

    /**
     * Configures comma-separated list of remote Maven repositories to use by pax-url-aether.
     * By default, assembly mojo sets the repositories from current Maven build.
     * @param mavenRepositories
     * @return
     */
    public Builder mavenRepositories(String mavenRepositories) {
        this.mavenRepositories = mavenRepositories;
        return this;
    }

    /**
     * Configures a function that may alter/replace {@link MavenResolver} used to resolve <code>mvn:</code> URIs.
     * @param wrapper
     * @return
     */
    public Builder resolverWrapper(Function<MavenResolver, MavenResolver> wrapper) {
        this.resolverWrapper = wrapper;
        return this;
    }

    /**
     * Short-hand builder configuration to use standard Karaf static KAR at current Karaf version
     * @return
     */
    public Builder staticFramework() {
        return staticFramework(Version.karafVersion());
    }

    /**
     * Short-hand builder configuration to use standard Karaf static KAR at given Karaf version
     * @param version
     * @return
     */
    public Builder staticFramework(String version) {
        String staticFeaturesKar = String.format(STATIC_FEATURES_KAR, version);
        return this.defaultStage(Startup).useReferenceUrls().kars(Startup, true, staticFeaturesKar);
    }

    /**
     * Configure a list of blacklisted profile names (possibly using <code>*</code> glob)
     * @param profiles
     * @return
     */
    public Builder blacklistProfiles(Collection<String> profiles) {
        this.blacklistedProfileNames.addAll(profiles);
        return this;
    }

    /**
     * Configure a list of blacklisted feature names (see {@link FeaturePattern})
     * @param features
     * @return
     */
    public Builder blacklistFeatures(Collection<String> features) {
        this.blacklistedFeatureIdentifiers.addAll(features);
        return this;
    }

    /**
     * Configure a list of blacklisted bundle URIs (see {@link LocationPattern})
     * @param bundles
     * @return
     */
    public Builder blacklistBundles(Collection<String> bundles) {
        this.blacklistedBundleURIs.addAll(bundles);
        return this;
    }


    public Builder extraProtocols(Collection<String> protocols) {
        DownloadManagerHelper.setExtraProtocols(protocols);
        return this;
    }


    /**
     * Configure a list of blacklisted features XML repository URIs (see {@link LocationPattern})
     * @param repositories
     * @return
     */
    public Builder blacklistRepositories(Collection<String> repositories) {
        this.blacklistedRepositoryURIs.addAll(repositories);
        return this;
    }

    /**
     * TODOCUMENT
     * @param policy
     * @return
     */
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

    /**
     * Configures a list of PIDs (or PID patterns) to copy to <code>${karaf.etc}</code> from features, when
     * assembling a distribution
     * @param pidsToExtract
     * @return
     */
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

    /**
     * Configures additional properties to add to <code>${karaf.etc}/config.properties</code>
     * @param key
     * @param value
     * @return
     */
    public Builder config(String key, String value) {
        this.config.put(key, value);
        return this;
    }

    /**
     * Configures additional properties to add to <code>${karaf.etc}/system.properties</code>
     * @param key
     * @param value
     * @return
     */
    public Builder system(String key, String value) {
        this.system.put(key, value);
        return this;
    }

    public List<String> getBlacklistedProfileNames() {
        return blacklistedProfileNames;
    }

    public List<String> getBlacklistedFeatureIdentifiers() {
        return blacklistedFeatureIdentifiers;
    }

    public List<String> getBlacklistedBundleURIs() {
        return blacklistedBundleURIs;
    }

    public List<String> getBlacklistedRepositoryURIs() {
        return blacklistedRepositoryURIs;
    }

    public BlacklistPolicy getBlacklistPolicy() {
        return blacklistPolicy;
    }

    public List<String> getPidsToExtract() {
        return pidsToExtract;
    }

    /**
     * Main method to generate custom Karaf distribution using configuration provided with builder-like methods.
     * @throws Exception
     */
    public void generateAssembly() throws Exception {
        if (javase == null) {
            throw new IllegalArgumentException("javase is not set");
        }
        if (homeDirectory == null) {
            throw new IllegalArgumentException("homeDirectory is not set");
        }
        try {
            executor = Executors.newScheduledThreadPool(8, ThreadUtils.namedThreadFactory("builder"));

            systemDirectory = homeDirectory.resolve("system");
            etcDirectory = homeDirectory.resolve("etc");

            doGenerateAssembly();
        } finally {
            if (executor != null) {
                executor.shutdownNow();
            }
        }
    }

    private void doGenerateAssembly() throws Exception {
        LOGGER.info("Generating Karaf assembly: " + homeDirectory);

        //
        // Create download manager - combination of pax-url-aether and a resolver wrapper that may
        // alter the way pax-url-aether resolver works
        //
        MavenResolver resolver = createMavenResolver();
        manager = new CustomDownloadManager(resolver, executor, null, translatedUrls);
        this.resolver = new ResolverImpl(new Slf4jResolverLog(LOGGER), resolverParallelism);

        //
        // Unzip KARs
        //
        LOGGER.info("Unzipping kars");
        Downloader downloader = manager.createDownloader();
        for (String kar : kars.keySet()) {
            downloader.download(kar, null);
        }
        downloader.await();
        // each KAR is extracted and all features XML repositories found there are added to the same
        // stage as the KAR and with the same "add all" flag as the KAR itself
        for (String karUri : kars.keySet()) {
            LOGGER.info("   processing KAR: " + karUri);
            Kar kar = new Kar(manager.getProviders().get(karUri).getFile().toURI());
            kar.extract(systemDirectory.toFile(), homeDirectory.toFile());
            RepositoryInfo info = kars.get(karUri);
            for (URI repositoryUri : kar.getFeatureRepos()) {
                LOGGER.info("      found repository: " + repositoryUri);
                repositories.put(repositoryUri.toString(), info);
            }
        }

        //
        // Load profiles
        //
        LOGGER.info("Loading profiles from:");
        profilesUris.forEach(p -> LOGGER.info("   " + p));
        allProfiles = loadExternalProfiles(profilesUris);
        if (allProfiles.size() > 0) {
            StringBuilder sb = new StringBuilder();
            LOGGER.info("   Found profiles: " + String.join(", ", allProfiles.keySet()));
        }

        // Generate initial profile to collect overrides and blacklisting instructions
        Profile initialProfile = ProfileBuilder.Factory.create("initial")
                .setParents(new ArrayList<>(profiles.keySet()))
                .getProfile();
        Profile initialOverlay = Profiles.getOverlay(initialProfile, allProfiles, environment);
        Profile initialEffective = Profiles.getEffective(initialOverlay, false);

        //
        // Handle blacklist - we'll use SINGLE instance of Blacklist for all further downloads
        //
        blacklist = processBlacklist(initialEffective);

        //
        // Configure blacklisting and overriding features processor
        //

        boolean needFeaturesProcessorFileCopy = false;
        String existingProcessorDefinitionURI = null;
        Path existingProcessorDefinition = etcDirectory.resolve("org.apache.karaf.features.xml");
        if (existingProcessorDefinition.toFile().isFile()) {
            existingProcessorDefinitionURI = existingProcessorDefinition.toFile().toURI().toString();
            LOGGER.info("Found existing features processor configuration: {}", homeDirectory.relativize(existingProcessorDefinition));
        }
        if (featuresProcessingLocation != null && featuresProcessingLocation.toFile().isFile()
                && !featuresProcessingLocation.equals(existingProcessorDefinition)) {
            if (existingProcessorDefinitionURI != null) {
                LOGGER.warn("Explicitly configured {} will be used for features processor configuration.", homeDirectory.relativize(featuresProcessingLocation));
            } else {
                LOGGER.info("Found features processor configuration: {}", homeDirectory.relativize(featuresProcessingLocation));
            }
            existingProcessorDefinitionURI = featuresProcessingLocation.toFile().toURI().toString();
            // when there are no other (configured via Maven for example) processing instructions (e.g., blacklisting)
            // we don't have to generate this file and may take original content
            needFeaturesProcessorFileCopy = true;
        }

        // now we can configure blacklisting features processor which may have already defined (in XML)
        // configuration for bundle replacements or feature overrides.
        FeaturesProcessorImpl processor = new FeaturesProcessorImpl(existingProcessorDefinitionURI, null, blacklist, new HashSet<>());

        // add overrides from initialProfile
        Set<String> overrides = processOverrides(initialEffective.getOverrides());
        processor.addOverrides(overrides);

        //
        // Propagate feature installation from repositories
        //
        LOGGER.info("Loading repositories");
        Map<String, Features> karRepositories = loadRepositories(manager, repositories.keySet(), false, processor);
        for (String repo : repositories.keySet()) {
            RepositoryInfo info = repositories.get(repo);
            if (info.addAll) {
                LOGGER.info("   adding all non-blacklisted features from repository: " + repo + " (stage: " + info.stage + ")");
                for (Feature feature : karRepositories.get(repo).getFeature()) {
                    if (feature.isBlacklisted()) {
                        LOGGER.info("      feature {}/{} is blacklisted - skipping.", feature.getId(), feature.getVersion());
                    } else {
                        features.put(feature.getId(), info.stage);
                    }
                }
            }
        }

        //
        // Generate profiles. If user has configured additional profiles, they'll be used as parents
        // of the generated ones.
        //
        Profile startupProfile = generateProfile(Stage.Startup, profiles, repositories, features, bundles);
        allProfiles.put(startupProfile.getId(), startupProfile);

        // generated startup profile should be used (together with configured startup and boot profiles) as parent
        // of the generated boot profile - similar visibility rule (boot stage requires startup stage) is applied
        // for repositories and features
        profiles.put(startupProfile.getId(), Stage.Boot);
        Profile bootProfile = generateProfile(Stage.Boot, profiles, repositories, features, bundles);
        allProfiles.put(bootProfile.getId(), bootProfile);

        Profile installedProfile = generateProfile(Stage.Installed, profiles, repositories, features, bundles);
        allProfiles.put(installedProfile.getId(), installedProfile);

        //
        // Compute "overlay" profile - a single profile with all parent profiles included (when there's the same
        // file in both profiles, parent profile's version has lower priority)
        //
        ProfileBuilder builder = ProfileBuilder.Factory.create(UUID.randomUUID().toString())
                .setParents(Arrays.asList(startupProfile.getId(), bootProfile.getId(), installedProfile.getId()));
        config.forEach((k ,v) -> builder.addConfiguration(Profile.INTERNAL_PID, Profile.CONFIG_PREFIX + k, v));
        system.forEach((k ,v) -> builder.addConfiguration(Profile.INTERNAL_PID, Profile.SYSTEM_PREFIX + k, v));
        // profile with all the parents configured and stage-agnostic blacklisting configuration added
        blacklistedRepositoryURIs.forEach(builder::addBlacklistedRepository);
        blacklistedFeatureIdentifiers.forEach(builder::addBlacklistedFeature);
        blacklistedBundleURIs.forEach(builder::addBlacklistedBundle);
        // final profilep
        Profile overallProfile = builder.getProfile();

        // profile with parents included and "flattened" using inheritance rules (child files overwrite parent
        // files and child PIDs are merged with parent PIDs and same properties are taken from child profiles)
        Profile overallOverlay = Profiles.getOverlay(overallProfile, allProfiles, environment);

        // profile with property placeholders resolved or left unchanged (if there's no property value available,
        // so property placeholders are preserved - like ${karaf.base})
        Profile overallEffective = Profiles.getEffective(overallOverlay, false);

        if (writeProfiles) {
            Path profiles = etcDirectory.resolve("profiles");
            LOGGER.info("Adding profiles to {}", homeDirectory.relativize(profiles));
            allProfiles.forEach((id, profile) -> {
                try {
                    Profiles.writeProfile(profiles, profile);
                } catch (IOException e) {
                    LOGGER.warn("Problem writing profile {}: {}", id, e.getMessage());
                }
            });
        }

        manager = new CustomDownloadManager(resolver, executor, overallEffective, translatedUrls);

//        Hashtable<String, String> profileProps = new Hashtable<>(overallEffective.getConfiguration(ORG_OPS4J_PAX_URL_MVN_PID));
//        final Map<String, String> properties = new HashMap<>();
//        properties.put("karaf.default.repository", "system");
//        InterpolationHelper.performSubstitution(profileProps, properties::get, false, false, true);

        //
        // Write config and system properties
        //
        LOGGER.info("Configuring etc/config.properties and etc/system.properties");

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
        downloader = manager.createDownloader();
        LOGGER.info("Downloading libraries for generated profiles");
        downloadLibraries(downloader, configProperties, overallEffective.getLibraries(), "");
        LOGGER.info("Downloading additional libraries");
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
            if (Files.exists(configFile)) {
                LOGGER.info("   not changing existing config file: {}", homeDirectory.relativize(configFile));
            } else {
                LOGGER.info("   adding config file: {}", homeDirectory.relativize(configFile));
                Files.createDirectories(configFile.getParent());
                Files.write(configFile, config.getValue());
            }
        }

        if (processor.hasInstructions()) {
            Path featuresProcessingXml = etcDirectory.resolve("org.apache.karaf.features.xml");
            if (hasOwnInstructions() || overrides.size() > 0) {
                // just generate new etc/org.apache.karaf.features.xml file (with external config + builder config)
                try (FileOutputStream fos = new FileOutputStream(featuresProcessingXml.toFile())) {
                    LOGGER.info("Generating features processor configuration: {}", homeDirectory.relativize(featuresProcessingXml));
                    processor.writeInstructions(fos);
                }
            } else if (needFeaturesProcessorFileCopy) {
                // we may simply copy configured features processor XML configuration
                LOGGER.info("Copying features processor configuration: {} -> {}", homeDirectory.relativize(featuresProcessingLocation), homeDirectory.relativize(featuresProcessingXml));
                Files.copy(featuresProcessingLocation, featuresProcessingXml, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        //
        // Startup stage
        //
        Profile startupEffective = startupStage(startupProfile, processor);

        //
        // Boot stage
        //
        Set<Feature> allBootFeatures = bootStage(bootProfile, startupEffective, processor);

        //
        // Installed stage
        //
        Set<Feature> allInstalledFeatures = installStage(installedProfile, allBootFeatures, processor);

        // 'improve' configuration files.
        if (propertyEdits != null) {
            KarafPropertiesEditor editor = new KarafPropertiesEditor();
            editor.setInputEtc(etcDirectory.toFile())
            .setOutputEtc(etcDirectory.toFile())
            .setEdits(propertyEdits);
            editor.run();
        }

        if (generateConsistencyReport != null) {
            File directory = new File(generateConsistencyReport);
            if (directory.isFile()) {
                LOGGER.warn("Can't generate consistency report into {} - it's not a directory", generateConsistencyReport);
            } else {
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                if (directory.isDirectory()) {
                    LOGGER.info("Writing bundle report");
                    generateConsistencyReport(karRepositories, allInstalledFeatures, installedProfile, new File(directory, "bundle-report.xml"));
                    Files.copy(getClass().getResourceAsStream("/bundle-report.xslt"),
                            directory.toPath().resolve("bundle-report.xslt"),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /**
     * Produces human readable XML with <em>feature consistency report</em>.
     * @param repositories
     * @param allInstalledFeatures
     * @param installedProfile
     * @param result
     */
    public void generateConsistencyReport(Map<String, Features> repositories, Set<Feature> allInstalledFeatures, Profile installedProfile, File result) {
        Profile installedOverlay = Profiles.getOverlay(installedProfile, allProfiles, environment);
        Profile installedEffective = Profiles.getEffective(installedOverlay, false);

        List<String> installFeatures = new ArrayList<>();
        installFeatures.add(generatedBootFeatureName);
        installFeatures.addAll(installedEffective.getFeatures());

        FeatureSelector selector = new FeatureSelector(allInstalledFeatures);
        Set<Feature> effectiveInstalledFeatures = selector.getMatching(installFeatures);

        if (result == null) {
            return;
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(result))) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<?xml-stylesheet type=\"text/xsl\" href=\"bundle-report.xslt\"?>\n");
            writer.write("<consistency-report xmlns=\"urn:apache:karaf:consistency:1.0\" project=\"" + consistencyReportProjectName + "\" version=\"" + consistencyReportProjectVersion + "\">\n");

            ReportFlavor[] flavors = new ReportFlavor[] {
                    all,
                    notBlacklisted,
                    new ReportFlavor() {
                        @Override
                        public String name() {
                            return "installed";
                        }

                        @Override
                        public boolean include(Features repository) {
                            return !repository.isBlacklisted();
                        }

                        @Override
                        public boolean include(Feature feature) {
                            return !feature.isBlacklisted()
                                    && effectiveInstalledFeatures.contains(feature);
                        }

                        @Override
                        public boolean include(BundleInfo bundle) {
                            return !bundle.isBlacklisted();
                        }
                    }
            };

            for (ReportFlavor flavor : flavors) {
                writer.write("<report flavor=\"" + flavor.name() + "\">\n");

                Map<String, String> featureId2repository = new HashMap<>();
                // list of feature IDs containing given bundle URIs
                Map<String, Set<String>> bundle2featureId = new TreeMap<>(new URIAwareComparator());
                // map of groupId/artifactId to full URI list to detect "duplicates"
                Map<String, List<String>> ga2uri = new TreeMap<>();
                Set<String> haveDuplicates = new HashSet<>();

                // collect closure of bundles and features
                repositories.forEach((name, features) -> {
                    if (flavor.include(features)) {
                        features.getFeature().forEach(feature -> {
                            if (flavor.include(feature)) {
                                featureId2repository.put(feature.getId(), name);
                                feature.getBundle().forEach(bundle -> {
                                    // normal bundles of feature
                                    if (flavor.include(bundle)) {
                                        bundle2featureId.computeIfAbsent(bundle.getLocation().trim(), k -> new TreeSet<>()).add(feature.getId());
                                    }
                                });
                                feature.getConditional().forEach(cond -> cond.asFeature().getBundles().forEach(bundle -> {
                                    // conditional bundles of feature
                                    if (flavor.include(bundle)) {
                                        bundle2featureId.computeIfAbsent(bundle.getLocation().trim(), k -> new TreeSet<>()).add(feature.getId());
                                    }
                                }));
                            }
                        });
                    }
                });
                // collect bundle URIs - for now, only wrap:mvn: and mvn: are interesting
                bundle2featureId.keySet().forEach(uri -> {
                    String originalUri = uri;
                    if (uri.startsWith("wrap:mvn:")) {
                        uri = uri.substring(5);
                        if (uri.indexOf(";") > 0) {
                            uri = uri.substring(0, uri.indexOf(";"));
                        }
                        if (uri.indexOf("$") > 0) {
                            uri = uri.substring(0, uri.indexOf("$"));
                        }
                    }
                    if (uri.startsWith("mvn:")) {
                        try {
                            LocationPattern pattern = new LocationPattern(uri);
                            String ga = String.format("%s/%s", pattern.getGroupId(), pattern.getArtifactId());
                            ga2uri.computeIfAbsent(ga, k -> new LinkedList<>()).add(originalUri);
                        } catch (IllegalArgumentException ignored) {
                        /*
                            <!-- hibernate-validator-osgi-karaf-features-5.3.4.Final-features.xml -->
                            <feature name="hibernate-validator-paranamer" version="5.3.4.Final">
                                <feature>hibernate-validator</feature>
                                <bundle>wrap:mvn:com.thoughtworks.paranamer:paranamer:2.8</bundle>
                            </feature>
                         */
                        }
                    }
                });
                ga2uri.values().forEach(l -> {
                    if (l.size() > 1) {
                        haveDuplicates.addAll(l);
                    }
                });
                writer.write("    <duplicates>\n");
                ga2uri.forEach((key, uris) -> {
                    if (uris.size() > 1) {
                        try {
                            writer.write(String.format("        <duplicate ga=\"%s\">\n", key));
                            for (String uri : uris) {
                                writer.write(String.format("            <bundle uri=\"%s\">\n", sanitize(uri)));
                                for (String fid : bundle2featureId.get(uri)) {
                                    writer.write(String.format("                <feature repository=\"%s\">%s</feature>\n", featureId2repository.get(fid), fid));
                                }
                                writer.write("            </bundle>\n");
                            }
                            writer.write("        </duplicate>\n");
                        } catch (IOException ignored) {
                        }
                    }
                });
                writer.write("    </duplicates>\n");
                writer.write("    <bundles>\n");
                for (String uri : bundle2featureId.keySet()) {
                    writer.write(String.format("        <bundle uri=\"%s\" duplicate=\"%b\">\n", sanitize(uri), haveDuplicates.contains(uri)));
                    for (String fid : bundle2featureId.get(uri)) {
                        writer.write(String.format("            <feature>%s</feature>\n", fid));
                    }
                    writer.write("        </bundle>\n");
                }
                writer.write("    </bundles>\n");
                writer.write("</report>\n");
            }
            writer.write("</consistency-report>\n");
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private interface ReportFlavor {
        String name();
        boolean include(Features repository);
        boolean include(Feature feature);
        boolean include(BundleInfo bundle);
    }

    private ReportFlavor all = new ReportFlavor() {
        @Override
        public String name() {
            return "all";
        }

        @Override
        public boolean include(Features repository) {
            return true;
        }

        @Override
        public boolean include(Feature feature) {
            return true;
        }

        @Override
        public boolean include(BundleInfo bundle) {
            return true;
        }
    };

    private ReportFlavor notBlacklisted = new ReportFlavor() {
        @Override
        public String name() {
            return "available";
        }

        @Override
        public boolean include(Features repository) {
            return !repository.isBlacklisted();
        }

        @Override
        public boolean include(Feature feature) {
            return !feature.isBlacklisted();
        }

        @Override
        public boolean include(BundleInfo bundle) {
            return !bundle.isBlacklisted();
        }
    };

    /**
     * Sanitize before putting to XML
     * @param uri
     * @return
     */
    public String sanitize(String uri) {
        return uri.replaceAll("&", "&amp;").replaceAll(">", "&lt;").replaceAll("<", "&gt;").replaceAll("\"", "&quot;");
    }

    /**
     * Similar to {@link FeaturesProcessorImpl#hasInstructions()}, we check if there are any builder configuration
     * options for blacklisted repos/features/bundles or overwrites.
     * @return
     */
    private boolean hasOwnInstructions() {
        int count = 0;
        count += blacklistedRepositoryURIs.size();
        count += blacklistedFeatureIdentifiers.size();
        count += blacklistedBundleURIs.size();

        return count > 0;
    }

    /**
     * Checks existing (etc/overrides.properties) and configured (in profiles) overrides definitions
     * @param profileOverrides
     * @return
     */
    private Set<String> processOverrides(List<String> profileOverrides) {
        Set<String> result = new LinkedHashSet<>();
        Path existingOverridesLocation = etcDirectory.resolve("overrides.properties");
        if (existingOverridesLocation.toFile().isFile()) {
            LOGGER.warn("Found {} which is deprecated, please use new feature processor configuration.", homeDirectory.relativize(existingOverridesLocation));
            result.addAll(Overrides.loadOverrides(existingOverridesLocation.toFile().toURI().toString()));
        }
        result.addAll(profileOverrides);

        return result;
    }

    /**
     * Checks existing and configured blacklisting definitions
     * @param initialProfile
     * @return
     * @throws IOException
     */
    private Blacklist processBlacklist(Profile initialProfile) throws IOException {
        Blacklist existingBlacklist = null;
        Blacklist blacklist = new Blacklist();
        Path existingBLacklistedLocation = etcDirectory.resolve("blacklisted.properties");
        if (existingBLacklistedLocation.toFile().isFile()) {
            LOGGER.warn("Found {} which is deprecated, please use new feature processor configuration.", homeDirectory.relativize(existingBLacklistedLocation));
            existingBlacklist = new Blacklist(Files.readAllLines(existingBLacklistedLocation));
        }
        for (String br : blacklistedRepositoryURIs) {
            // from Maven/Builder configuration
            try {
                blacklist.blacklistRepository(new LocationPattern(br));
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Blacklisted features XML repository URI is invalid: {}, ignoring", br);
            }
        }
        for (LocationPattern br : initialProfile.getBlacklistedRepositories()) {
            // from profile configuration
            blacklist.blacklistRepository(br);
        }
        for (String bf : blacklistedFeatureIdentifiers) {
            // from Maven/Builder configuration
            blacklist.blacklistFeature(new FeaturePattern(bf));
        }
        for (FeaturePattern bf : initialProfile.getBlacklistedFeatures()) {
            // from profile configuration
            blacklist.blacklistFeature(bf);
        }
        for (String bb : blacklistedBundleURIs) {
            // from Maven/Builder configuration
            try {
                blacklist.blacklistBundle(new LocationPattern(bb));
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Blacklisted bundle URI is invalid: {}, ignoring", bb);
            }
        }
        for (LocationPattern bb : initialProfile.getBlacklistedBundles()) {
            // from profile configuration
            blacklist.blacklistBundle(bb);
        }
        if (existingBlacklist != null) {
            blacklist.merge(existingBlacklist);
        }

        return blacklist;
    }

    private MavenResolver createMavenResolver() {
        Dictionary<String, String> props = new Hashtable<>();
        if (offline) {
            props.put(ORG_OPS4J_PAX_URL_MVN_PID + "offline", "true");
        }
        if (localRepository != null) {
            props.put(ORG_OPS4J_PAX_URL_MVN_PID + ".localRepository", localRepository);
        }
        if (mavenRepositories != null) {
            props.put(ORG_OPS4J_PAX_URL_MVN_PID + ".repositories", mavenRepositories);
        }
        MavenResolver resolver = MavenResolvers.createMavenResolver(props, ORG_OPS4J_PAX_URL_MVN_PID);
        return resolverWrapper.apply(resolver);
    }

    /**
     * Loads all profiles declared in profile URIs. These will be used in addition to generated
     * <em>startup</em>, <em>boot</em> and <em>installed</em> profiles.
     */
    private Map<String, Profile> loadExternalProfiles(List<String> profilesUris) throws IOException, MultiException, InterruptedException {
        Map<String, Profile> profiles = new LinkedHashMap<>();
        Map<String, Profile> filteredProfiles = new LinkedHashMap<>();

        for (String profilesUri : profilesUris) {
            String uri = profilesUri;
            if (uri.startsWith("jar:") && uri.contains("!/")) {
                uri = uri.substring("jar:".length(), uri.indexOf("!/"));
            }
            if (!uri.startsWith("file:")) {
                Downloader downloader = manager.createDownloader();
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
            profiles.putAll(Profiles.loadProfiles(profilePath));
            // Handle blacklisted profiles
            List<ProfileNamePattern> blacklistedProfilePatterns = blacklistedProfileNames.stream()
                    .map(ProfileNamePattern::new).collect(Collectors.toList());

            for (String profileName : profiles.keySet()) {
                boolean blacklisted = false;
                for (ProfileNamePattern pattern : blacklistedProfilePatterns) {
                    if (pattern.matches(profileName)) {
                        LOGGER.info("   blacklisting profile {} from {}", profileName, profilePath);
                        // TODO review blacklist policy options
                        if (blacklistPolicy == BlacklistPolicy.Discard) {
                            // Override blacklisted profiles with empty one
                            filteredProfiles.put(profileName, ProfileBuilder.Factory.create(profileName).getProfile());
                        } else {
                            // Remove profile completely
                        }
                        // no need to check other patterns
                        blacklisted = true;
                        break;
                    }
                }
                if (!blacklisted) {
                    filteredProfiles.put(profileName, profiles.get(profileName));
                }
            }
        }

        return filteredProfiles;
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
            if (!javase.supportsEndorsedAndExtLibraries() && (Library.TYPE_ENDORSED.equals(type) || Library.TYPE_EXTENSION.equals(type))) {
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
                synchronized (downloader) {
                    Path input = provider.getFile().toPath();
                    String name = filename != null ? filename : input.getFileName().toString();
                    Path libOutput = homeDirectory.resolve(path).resolve(name);
                    if (!libOutput.toFile().getParentFile().isDirectory()) {
                        libOutput.toFile().getParentFile().mkdirs();
                    }
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
                    synchronized (config) {
                        Map<String, String> headers = getHeaders(provider);
                        String packages = headers.get(Constants.EXPORT_PACKAGE);
                        if (packages != null) {
                            Clause[] clauses1 = org.apache.felix.utils.manifest.Parser.parseHeader(packages);
                            if (export) {
                                StringBuilder val = new StringBuilder(config.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA));
                                for (Clause clause1 : clauses1) {
                                    val.append(",").append(clause1.toString());
                                }
                                config.setProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, val.toString());
                            }
                            if (delegate) {
                                StringBuilder val = new StringBuilder(config.getProperty(Constants.FRAMEWORK_BOOTDELEGATION));
                                for (Clause clause1 : clauses1) {
                                    val.append(",").append(clause1.getName());
                                }
                                config.setProperty(Constants.FRAMEWORK_BOOTDELEGATION, val.toString());
                            }
                        }
                    }
                }
            });
        }
    }

    private Set<Feature> installStage(Profile installedProfile, Set<Feature> allBootFeatures, FeaturesProcessor processor) throws Exception {
        LOGGER.info("Install stage");
        //
        // Handle installed profiles
        //
        Profile installedOverlay = Profiles.getOverlay(installedProfile, allProfiles, environment);
        Profile installedEffective = Profiles.getEffective(installedOverlay, false);

        Downloader downloader = manager.createDownloader();

        // Load startup repositories
        LOGGER.info("   Loading installed repositories");
        Map<String, Features> installedRepositories = loadRepositories(manager, installedEffective.getRepositories(), true, processor);
        // Compute startup feature dependencies
        Set<Feature> allInstalledFeatures = new HashSet<>();
        for (Features repo : installedRepositories.values()) {
            allInstalledFeatures.addAll(repo.getFeature());
        }

        // Add boot features for search
        allInstalledFeatures.addAll(allBootFeatures);
        FeatureSelector selector = new FeatureSelector(allInstalledFeatures);
        Set<Feature> installedFeatures = selector.getMatching(installedEffective.getFeatures());
        ArtifactInstaller installer = new ArtifactInstaller(systemDirectory, downloader, blacklist);
        for (Feature feature : installedFeatures) {
            if (feature.isBlacklisted()) {
                LOGGER.info("   Feature " + feature.getId() + " is blacklisted, ignoring");
                continue;
            }
            LOGGER.info("   Feature {} is defined as an installed feature", feature.getId());
            for (Bundle bundle : feature.getBundle()) {

                if (!ignoreDependencyFlag || !bundle.isDependency()) {
                    installer.installArtifact(bundle);
                }
            }
            // Install config files
            for (ConfigFile configFile : feature.getConfigfile()) {
                installer.installArtifact(configFile.getLocation().trim());
            }
            for (Conditional cond : feature.getConditional()) {
                if (cond.isBlacklisted()) {
                    LOGGER.info("   Conditionial " + cond.getConditionId() + " is blacklisted, ignoring");
                }
                for (Bundle bundle : cond.getBundle()) {
                    if (!ignoreDependencyFlag || !bundle.isDependency()) {
                        installer.installArtifact(bundle);
                    }
                }
            }
        }
        for (String location : installedEffective.getBundles()) {
            installer.installArtifact(location);
        }
        downloader.await();
        return allInstalledFeatures;
    }

    private Set<Feature> bootStage(Profile bootProfile, Profile startupEffective, FeaturesProcessor processor) throws Exception {
        LOGGER.info("Boot stage");
        //
        // Handle boot profiles
        //
        Profile bootOverlay = Profiles.getOverlay(bootProfile, allProfiles, environment);
        Profile bootEffective = Profiles.getEffective(bootOverlay, false);
        // Load startup repositories
        LOGGER.info("   Loading boot repositories");
        Map<String, Features> bootRepositories = loadRepositories(manager, bootEffective.getRepositories(), true, processor);
        // Compute startup feature dependencies
        Set<Feature> allBootFeatures = new HashSet<>();
        for (Features repo : bootRepositories.values()) {
            allBootFeatures.addAll(repo.getFeature());
        }
        // Generate a global feature
        Map<String, Dependency> generatedDep = new HashMap<>();
        generatedBootFeatureName = UUID.randomUUID().toString();
        Feature generated = new Feature();
        generated.setName(generatedBootFeatureName);
        // Add feature dependencies
        for (String nameOrPattern : bootEffective.getFeatures()) {
            // KARAF-5273: feature may be a pattern
            for (String dependency : FeatureSelector.getMatchingFeatures(nameOrPattern, bootRepositories.values())) {
                Dependency dep = generatedDep.get(dependency);
                if (dep == null) {
                    dep = createDependency(dependency);
                    generated.getFeature().add(dep);
                    generatedDep.put(dep.getName(), dep);
                }
                dep.setDependency(false);
            }
        }
        // Add bundles
        for (String location : bootEffective.getBundles()) {
            location = location.replace("profile:", "file:etc/");
            int intLevel = -100;
            if (location.contains(START_LEVEL)) {
                //extract start-level for this bundle
                String level = location.substring(location.indexOf(START_LEVEL));
                level = level.substring(START_LEVEL.length() + 1);
                if (level.startsWith("\"")) {
                    level = level.substring(1, level.length() - 1);
                }
                intLevel = Integer.parseInt(level);
                LOGGER.debug("bundle start-level: " + level);
                location = location.substring(0, location.indexOf(START_LEVEL) - 1);
                LOGGER.debug("new bundle location after strip start-level: " + location);
            }
            Bundle bun = new Bundle();
            if (intLevel > 0) {
                bun.setStartLevel(intLevel);
            }
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
            if (feature.isBlacklisted()) {
                LOGGER.info("   Feature " + feature.getId() + " is blacklisted, ignoring");
                continue;
            }
            LOGGER.info("   Feature " + feature.getId() + " is defined as a boot feature");
            // add the feature in the system folder
            Set<BundleInfo> bundleInfos = new HashSet<>();
            for (Bundle bundle : feature.getBundle()) {
                if (!ignoreDependencyFlag || !bundle.isDependency()) {
                    bundleInfos.add(bundle);
                }
            }
            for (Conditional cond : feature.getConditional()) {
                if (cond.isBlacklisted()) {
                    LOGGER.info("   Conditionial " + cond.getConditionId() + " is blacklisted, ignoring");
                }
                for (Bundle bundle : cond.getBundle()) {
                    if (!ignoreDependencyFlag || !bundle.isDependency()) {
                        bundleInfos.add(bundle);
                    }
                }
            }

            // Build optional features and known prerequisites
            Map<String, List<String>> prereqs = new HashMap<>();
            prereqs.put("blueprint:", Arrays.asList("deployer", "aries-blueprint"));
            prereqs.put("spring:", Arrays.asList("deployer", "spring"));
            prereqs.put("wrap:", Collections.singletonList("wrap"));
            prereqs.put("war:", Collections.singletonList("war"));
            ArtifactInstaller installer = new ArtifactInstaller(systemDirectory, downloader, blacklist);
            for (BundleInfo bundleInfo : bundleInfos) {
                installer.installArtifact(bundleInfo);
                for (Map.Entry<String, List<String>> entry : prereqs.entrySet()) {
                    if (bundleInfo.getLocation().trim().startsWith(entry.getKey())) {
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
                repoUrl = "file:${karaf.etc}/" + output.getName();
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

    private Profile startupStage(Profile startupProfile, FeaturesProcessor processor) throws Exception {
        LOGGER.info("Startup stage");
        //
        // Compute startup
        //
        Profile startupOverlay = Profiles.getOverlay(startupProfile, allProfiles, environment);
        Profile startupEffective = Profiles.getEffective(startupOverlay, false);
        // Load startup repositories
        LOGGER.info("   Loading startup repositories");
        Map<String, Features> startupRepositories = loadRepositories(manager, startupEffective.getRepositories(), false, processor);

        //
        // Resolve
        //
        LOGGER.info("   Resolving startup features and bundles");
        LOGGER.info("      Features: " + String.join(", ", startupEffective.getFeatures()));
        LOGGER.info("      Bundles: " + String.join(", ", startupEffective.getBundles()));

        Map<String, Integer> bundles =
                resolve(manager,
                        resolver,
                        startupRepositories.values(),
                        startupEffective.getFeatures(),
                        startupEffective.getBundles(),
                        startupEffective.getOptionals(),
                        processor);

        //
        // Generate startup.properties
        //
        Properties startup = new Properties();
        startup.setHeader(Collections.singletonList("# Bundles to be started on startup, with startlevel"));
        Map<Integer, Set<String>> invertedStartupBundles = MapUtils.invert(bundles);
        for (Map.Entry<Integer, Set<String>> entry : new TreeMap<>(invertedStartupBundles).entrySet()) {
            String startLevel = Integer.toString(entry.getKey());
            // ensure input order is respected whatever hashmap/set was in the middle of the processing
            final List<String> value = new ArrayList<>(entry.getValue());
            value.sort(comparing(bnd -> startupEffective.getBundles().indexOf(bnd)));
            for (String location : value) {
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

    /**
     * Gets a list of objects (bundle URIs, profile IDs, feature IDs) configured for given stage
     * @param stage
     * @param data
     * @return
     */
    private List<String> getStaged(Stage stage, Map<String, Stage> data) {
        List<String> staged = new ArrayList<>();
        for (String s : data.keySet()) {
            if (data.get(s) == stage) {
                staged.add(s);
            }
        }
        return staged;
    }

    /**
     * Gets a list of features XML repository URIs configured for given stage. There's one special rule - startup
     * repositories are added as boot repositories as well.
     * @param stage
     * @param data
     * @return
     */
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

    private Map<String, Features> loadRepositories(DownloadManager manager, Collection<String> repositories, final boolean install, FeaturesProcessor processor) throws Exception {
        final Map<String, Features> loaded = new HashMap<>();
        final Downloader downloader = manager.createDownloader();
        for (String repository : repositories) {
            downloader.download(repository, new DownloadCallback() {
                @Override
                public void downloaded(final StreamProvider provider) throws Exception {
                    String url = provider.getUrl();
                    if (processor.isRepositoryBlacklisted(url)) {
                        LOGGER.info("   feature repository " + url + " is blacklisted");
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
                                Features featuresModel;
                                if (JacksonUtil.isJson(url)) {
                                    featuresModel = JacksonUtil.unmarshal(url);
                                } else {
                                    featuresModel = JaxbUtil.unmarshal(url, is, false);
                                }
                                // always process according to processor configuration
                                featuresModel.setBlacklisted(processor.isRepositoryBlacklisted(url));
                                processor.process(featuresModel);

                                loaded.put(provider.getUrl(), featuresModel);
                                for (String innerRepository : featuresModel.getRepository()) {
                                    if (processor.isRepositoryBlacklisted(innerRepository)) {
                                        LOGGER.info("   referenced feature repository " + innerRepository + " is blacklisted");
                                        continue;
                                    }
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

    /**
     * Generate internal profile (for the purpose of custom assembly builder) for given <code>stage</code>.
     * @param stage a {@link Stage} for which the profile is being generated
     * @param parentProfiles all profiles for given stage will be used as parent profiles
     * @param repositories repositories to use in generated profile
     * @param features features to declare in generated profile
     * @param bundles bundles to declare in generated profile
     * @return
     */
    private Profile generateProfile(Stage stage, Map<String, Stage> parentProfiles, Map<String, RepositoryInfo> repositories, Map<String, Stage> features, Map<String, Stage> bundles) {
        String name = "generated-" + stage.name().toLowerCase();
        List<String> stagedParentProfiles = getStaged(stage, parentProfiles);

        if (stagedParentProfiles.isEmpty()) {
            LOGGER.info("Generating {} profile", name);
        } else {
            LOGGER.info("Generating {} profile with parents: {}", name, String.join(", ", stagedParentProfiles));
        }

        return ProfileBuilder.Factory.create(name)
                .setParents(stagedParentProfiles)
                .setRepositories(getStagedRepositories(stage, repositories))
                .setFeatures(getStaged(stage, features))
                .setBundles(getStaged(stage, bundles))
                .getProfile();
    }

    /**
     * <p>Resolves set of features and bundles using OSGi resolver to calculate startup stage bundles.</p>
     * <p>Startup stage means that <em>current</em> state of the OSGi framework is just single system bundle installed
     * and bundles+features are being resolved against this single <em>bundle 0</em>.</p>
     *
     * @param manager {@link DownloadManager} to help downloading bundles and resources
     * @param resolver OSGi resolver which will resolve features and bundles in framework with only system bundle installed
     * @param repositories all available (not only to-be-installed) features
     * @param features feature identifiers to resolve
     * @param bundles bundle locations to resolve
     * @param optionals optional URI locations that'll be available through {@link org.osgi.service.repository.Repository},
     * used in resolution process
     * @param processor {@link FeaturesProcessor} to process repositories/features/bundles
     * @return map from bundle URI to bundle start-level
     * @throws Exception
     */
    private Map<String, Integer> resolve(
                    DownloadManager manager,
                    Resolver resolver,
                    Collection<Features> repositories,
                    Collection<String> features,
                    Collection<String> bundles,
                    Collection<String> optionals,
                    FeaturesProcessor processor) throws Exception {

        // System bundle will be single bundle installed with bundleId == 0
        BundleRevision systemBundle = getSystemBundle();
        if (resolverParallelism > 1) {
            return doResolve(manager, resolver, repositories, features, bundles, optionals, processor, systemBundle);
        }
        // let a chance to be sequential in case order is important with the current framework
        return features.stream()
                .flatMap(it -> {
                    try {
                        return doResolve(manager, resolver, repositories, singletonList(it), bundles, optionals, processor, systemBundle).entrySet().stream();
                    } catch (final RuntimeException e) {
                        throw e;
                    } catch (final Exception e) {
                        throw new IllegalStateException(e);
                    }
                })
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));
    }

    private Map<String, Integer> doResolve(DownloadManager manager,
                                           Resolver resolver,
                                           Collection<Features> repositories,
                                           Collection<String> features,
                                           Collection<String> bundles,
                                           Collection<String> optionals,
                                           FeaturesProcessor processor,
                                           BundleRevision systemBundle) throws Exception {
        // Static distribution building callback and deployer that's used to deploy/collect startup-stage artifacts
        AssemblyDeployCallback callback = new AssemblyDeployCallback(manager, this, systemBundle, repositories, processor);
        Deployer deployer = new Deployer(manager, resolver, callback);

        // Install framework
        Deployer.DeploymentRequest request = Deployer.DeploymentRequest.defaultDeploymentRequest();

        // Add optional resources available through OSGi resource repository
        request.globalRepository = repositoryOfOptionalResources(manager, optionals);

        // Specify feature requirements
        for (String feature : features) {
            // KARAF-5273: feature may be a pattern
            for (String featureName : FeatureSelector.getMatchingFeatures(feature, repositories)) {
                MapUtils.addToMapSet(request.requirements, FeaturesService.ROOT_REGION, featureName);
            }
        }
        // Specify bundle requirements
        for (String bundle : bundles) {
            MapUtils.addToMapSet(request.requirements, FeaturesService.ROOT_REGION, "bundle:" + bundle);
        }

        deployer.deployFully(callback.getDeploymentState(), request);

        return callback.getStartupBundles();
    }

    /**
     * Optional resource URIs will be made available through OSGi {@link Repository}
     * @param manager
     * @param optionals
     * @return
     * @throws Exception
     */
    private Repository repositoryOfOptionalResources(DownloadManager manager, Collection<String> optionals)
            throws Exception {
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
        return new BaseRepository(resources);
    }

    /**
     * Prepares {@link BundleRevision} that represents System Bundle (a.k.a. <em>bundle 0</em>)
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    private BundleRevision getSystemBundle() throws Exception {
        Path configPropPath = etcDirectory.resolve("config.properties");
        Properties configProps = PropertiesLoader.loadPropertiesOrFail(configPropPath.toFile());
        configProps.put("java.specification.version", javase.version);
        configProps.substitute();

        Attributes attributes = new Attributes();
        attributes.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
        attributes.putValue(Constants.BUNDLE_SYMBOLICNAME, "system.bundle");
        attributes.putValue(Constants.BUNDLE_VERSION, "0.0.0");

        String exportPackages = configProps.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES, "");
        if ("".equals(exportPackages.trim())) {
            throw new IllegalArgumentException("\"org.osgi.framework.system.packages\" property should specify system bundle" +
                    " packages. It can't be empty, please check etc/config.properties of the assembly.");
        }
        if (configProps.containsKey(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA)) {
            exportPackages += "," + configProps.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
        }
        exportPackages = exportPackages.replaceAll(",\\s*,", ",");
        attributes.putValue(Constants.EXPORT_PACKAGE, exportPackages);

        String systemCaps = configProps.getProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES, "");
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
