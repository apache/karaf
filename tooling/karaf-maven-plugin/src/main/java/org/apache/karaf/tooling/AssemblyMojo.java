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
package org.apache.karaf.tooling;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import org.apache.karaf.profile.assembly.Builder;
import org.apache.karaf.tooling.utils.IoUtils;
import org.apache.karaf.tooling.utils.MavenUtil;
import org.apache.karaf.tooling.utils.MojoSupport;
import org.apache.karaf.tooling.utils.ReactorMavenResolver;
import org.apache.karaf.tools.utils.model.KarafPropertyEdits;
import org.apache.karaf.tools.utils.model.io.stax.KarafPropertyInstructionsModelStaxReader;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.aether.repository.WorkspaceReader;
import org.osgi.framework.Constants;

/**
 * Creates a customized Karaf distribution by installing features and setting up
 * configuration files. The plugin gets features from feature.xml files and KAR
 * archives declared as dependencies or as files configured with the
 * featureRespositories parameter. It picks up other files, such as config files,
 * from ${project.build.directory}/classes. Thus, a file in src/main/resources/etc
 * will be copied by the resource plugin to ${project.build.directory}/classes/etc,
 * and then added to the assembly by this goal.
 */
@Mojo(name = "assembly", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class AssemblyMojo extends MojoSupport {

    /**
     * Base directory used to overwrite resources in generated assembly after the build (resource directory).
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/resources/assembly")
    protected File sourceDirectory;

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

    @Parameter
    private List<String> blacklistedRepositories;

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
    private List<String> blacklistedFeatures;

    @Parameter
    private List<String> startupBundles;
    @Parameter
    private List<String> bootBundles;
    @Parameter
    private List<String> installedBundles;
    @Parameter
    private List<String> blacklistedBundles;
    
    @Parameter
    private String profilesUri;

    @Parameter
    private List<String> bootProfiles;

    @Parameter
    private List<String> startupProfiles;

    @Parameter
    private List<String> installedProfiles;

    @Parameter
    private List<String> blacklistedProfiles;

    @Parameter
    private Builder.BlacklistPolicy blacklistPolicy = Builder.BlacklistPolicy.Discard;

    /**
     * Ignore the dependency attribute (dependency="[true|false]") on bundle
     */
    @Parameter(defaultValue = "false")
    protected boolean ignoreDependencyFlag;

    /**
     * Additional feature repositories
     */
    @Parameter
    protected List<String> featureRepositories;

    @Parameter
    protected List<String> libraries;

    /**
     * Use reference: style urls in startup.properties
     */
    @Parameter(defaultValue = "false")
    protected boolean useReferenceUrls;

    /**
     * Include project build output directory in the assembly
     */
    @Parameter(defaultValue = "true")
    protected boolean includeBuildOutputDirectory;

    @Parameter
    protected boolean installAllFeaturesByDefault = true;

    @Parameter
    protected Builder.KarafVersion karafVersion = Builder.KarafVersion.v4x;

    /**
     * Specify the version of Java SE to be assumed for osgi.ee.
     */
    @Parameter(defaultValue = "1.8")
    protected String javase;

    /**
     * Specify which framework to use
     * (one of framework, framework-logback, static-framework, static-framework-logback).
     */
    @Parameter
    protected String framework;

    /**
     * Specify an XML file that instructs this goal to apply edits to
     * one or more standard Karaf property files.
     * The contents of this file are documented in detail on
     * <a href="karaf-property-instructions-model.html">this page</a>.
     * This allows you to
     * customize these files without making copies in your resources
     * directories. Here's a simple example:
     * <pre>
     * {@literal
      <property-edits xmlns="http://karaf.apache.org/tools/property-edits/1.0.0">
         <edits>
          <edit>
            <file>config.properties</file>
            <operation>put</operation>
            <key>karaf.framework</key>
            <value>equinox</value>
          </edit>
          <edit>
            <file>config.properties</file>
            <operation>extend</operation>
            <key>org.osgi.framework.system.capabilities</key>
            <value>my-magic-capability</value>
          </edit>
          <edit>
            <file>config.properties</file>
            <operation prepend='true'>extend</operation>
            <key>some-other-list</key>
            <value>my-value-goes-first</value>
            </edit>
         </edits>
      </property-edits>
    }
     </pre>
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/karaf/assembly-property-edits.xml")
    protected String propertyFileEdits;

    /**
     * Glob specifying which configuration pids in the selected boot features
     * should be extracted to the etc directory.
     */
    @Parameter
    protected List<String> pidsToExtract = Collections.singletonList("*");

    /**
     * Specify a set of translated urls to use instead of downloading the artifacts
     * from their original locations.  The given set will be extended with already
     * built artifacts from the maven project.
     */
    @Parameter
    protected Map<String, String> translatedUrls;

    @Parameter
    protected Map<String, String> config;

    @Parameter
    protected Map<String, String> system;

    @Component(role = WorkspaceReader.class, hint = "reactor")
    protected WorkspaceReader reactor;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            doExecute();
        }
        catch (MojoExecutionException | MojoFailureException e) {
            throw e;
        }
        catch (Exception e) {
            throw new MojoExecutionException("Unable to build assembly", e);
        }
    }

    protected void doExecute() throws Exception {
        startupRepositories = nonNullList(startupRepositories);
        bootRepositories = nonNullList(bootRepositories);
        installedRepositories = nonNullList(installedRepositories);
        startupBundles = nonNullList(startupBundles);
        bootBundles = nonNullList(bootBundles);
        installedBundles = nonNullList(installedBundles);
        blacklistedBundles = nonNullList(blacklistedBundles);
        startupFeatures = nonNullList(startupFeatures);
        bootFeatures = nonNullList(bootFeatures);
        installedFeatures = nonNullList(installedFeatures);
        blacklistedFeatures = nonNullList(blacklistedFeatures);
        startupProfiles = nonNullList(startupProfiles);
        bootProfiles = nonNullList(bootProfiles);
        installedProfiles = nonNullList(installedProfiles);
        blacklistedProfiles = nonNullList(blacklistedProfiles);
        blacklistedRepositories = nonNullList(blacklistedRepositories);

        if (!startupProfiles.isEmpty() || !bootProfiles.isEmpty() || !installedProfiles.isEmpty()) {
            if (profilesUri == null) {
                throw new IllegalArgumentException("profilesDirectory must be specified");
            }
        }

        if (featureRepositories != null && !featureRepositories.isEmpty()) {
            getLog().warn("Use of featureRepositories is deprecated, use startupRepositories, bootRepositories or installedRepositories instead");
            startupRepositories.addAll(featureRepositories);
            bootRepositories.addAll(featureRepositories);
            installedRepositories.addAll(featureRepositories);
        }

        StringBuilder remote = new StringBuilder();
        for (Object obj : project.getRemoteProjectRepositories()) {
            if (remote.length() > 0) {
                remote.append(",");
            }
            remote.append(invoke(obj, "getUrl"));
            remote.append("@id=").append(invoke(obj, "getId"));
            if (!((Boolean) invoke(getPolicy(obj, false), "isEnabled"))) {
                remote.append("@noreleases");
            }
            if ((Boolean) invoke(getPolicy(obj, true), "isEnabled")) {
                remote.append("@snapshots");
            }
        }
        getLog().info("Using repositories: " + remote.toString());

        Builder builder = Builder.newInstance();
        builder.offline(mavenSession.isOffline());
        builder.localRepository(localRepo.getBasedir());
        builder.mavenRepositories(remote.toString());
        builder.resolverWrapper((resolver) -> new ReactorMavenResolver(reactor, resolver));
        builder.javase(javase);

        // Set up config and system props
        if (config != null) {
            config.forEach(builder::config);
        }
        if (system != null) {
            system.forEach(builder::system);
        }

        // Set up blacklisted items
        builder.blacklistBundles(blacklistedBundles);
        builder.blacklistFeatures(blacklistedFeatures);
        builder.blacklistProfiles(blacklistedProfiles);
        builder.blacklistRepositories(blacklistedRepositories);
        builder.blacklistPolicy(blacklistPolicy);

        if (propertyFileEdits != null) {
            File file = new File(propertyFileEdits);
            if (file.exists()) {
                KarafPropertyEdits edits;
                try (InputStream editsStream = new FileInputStream(propertyFileEdits)) {
                    KarafPropertyInstructionsModelStaxReader kipmsr = new KarafPropertyInstructionsModelStaxReader();
                    edits = kipmsr.read(editsStream, true);
                }
                builder.propertyEdits(edits);
            }
        }
        builder.pidsToExtract(pidsToExtract);

        Map<String, String> urls = new HashMap<>();
        List<Artifact> artifacts = new ArrayList<>(project.getAttachedArtifacts());
        artifacts.add(project.getArtifact());
        for (Artifact artifact : artifacts) {
            if (artifact.getFile() != null && artifact.getFile().exists()) {
                String mvnUrl = "mvn:" + artifact.getGroupId() + "/" + artifact.getArtifactId()
                        + "/" + artifact.getVersion();
                String type = artifact.getType();
                if ("bundle".equals(type)) {
                    type = "jar";
                }
                if (!"jar".equals(type) || artifact.getClassifier() != null) {
                    mvnUrl += "/" + type;
                    if (artifact.getClassifier() != null) {
                        mvnUrl += "/" + artifact.getClassifier();
                    }
                }
                urls.put(mvnUrl, artifact.getFile().toURI().toString());
            }
        }
        if (translatedUrls != null) {
            urls.putAll(translatedUrls);
        }
        builder.translatedUrls(urls);

        // creating system directory
        getLog().info("Creating work directory");
        builder.homeDirectory(workDirectory.toPath());
        IoUtils.deleteRecursive(workDirectory);
        workDirectory.mkdirs();

        List<String> startupKars = new ArrayList<>();
        List<String> bootKars = new ArrayList<>();
        List<String> installedKars = new ArrayList<>();

        // Loading kars and features repositories
        getLog().info("Loading kar and features repositories dependencies");
        for (Artifact artifact : project.getDependencyArtifacts()) {
            Builder.Stage stage;
            switch (artifact.getScope()) {
            case "compile":
                stage = Builder.Stage.Startup;
                break;
            case "runtime":
                stage = Builder.Stage.Boot;
                break;
            case "provided":
                stage = Builder.Stage.Installed;
                break;
            default:
                continue;
            }
            String uri = artifactToMvn(artifact);
            String type = getType(artifact);
            if ("kar".equals(type)) {
                switch (stage) {
                case Startup:   startupKars.add(uri); break;
                case Boot:      bootKars.add(uri); break;
                case Installed: installedKars.add(uri); break;
                }
            } else if ("features".equals(type)) {
                switch (stage) {
                case Startup:   startupRepositories.add(uri); break;
                case Boot:      bootRepositories.add(uri); break;
                case Installed: installedRepositories.add(uri); break;
                }
            } else if ("bundle".equals(type)) {
                switch (stage) {
                case Startup:   startupBundles.add(uri); break;
                case Boot:      bootBundles.add(uri); break;
                case Installed: installedBundles.add(uri); break;
                }
            }
        }

        builder.karafVersion(karafVersion)
               .useReferenceUrls(useReferenceUrls)
               .defaultAddAll(installAllFeaturesByDefault)
               .ignoreDependencyFlag(ignoreDependencyFlag);
        if (profilesUri != null) {
            builder.profilesUris(profilesUri);
        }
        if (libraries != null) {
            builder.libraries(libraries.toArray(new String[libraries.size()]));
        }
        // Startup
        boolean hasFrameworkKar = false;
        for (String kar : startupKars) {
            if (kar.startsWith("mvn:org.apache.karaf.features/framework/")
                    || kar.startsWith("mvn:org.apache.karaf.features/static/")) {
                hasFrameworkKar = true;
                startupKars.remove(kar);
                if (framework == null) {
                    framework = kar.startsWith("mvn:org.apache.karaf.features/framework/")
                            ? "framework" : "static-framework";
                }
                builder.kars(Builder.Stage.Startup, false, kar);
                break;
            }
        }
        if (!hasFrameworkKar) {
            Properties versions = new Properties();
            try (InputStream is = getClass().getResourceAsStream("versions.properties")) {
                versions.load(is);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            String realKarafVersion = versions.getProperty("karaf-version");
            String kar;
            switch (framework) {
                case "framework":
                    kar = "mvn:org.apache.karaf.features/framework/" + realKarafVersion + "/xml/features";
                    break;
                case "framework-logback":
                    kar = "mvn:org.apache.karaf.features/framework/" + realKarafVersion + "/xml/features";
                    break;
                case "static-framework":
                    kar = "mvn:org.apache.karaf.features/static/" + realKarafVersion + "/xml/features";
                    break;
                case "static-framework-logback":
                    kar = "mvn:org.apache.karaf.features/static/" + realKarafVersion + "/xml/features";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported framework: " + framework);
            }
            builder.kars(Builder.Stage.Startup, false, kar);
        }
        if (!startupFeatures.contains(framework)) {
            builder.features(Builder.Stage.Startup, framework);
        }
        builder.defaultStage(Builder.Stage.Startup)
               .kars(toArray(startupKars))
               .repositories(startupFeatures.isEmpty() && startupProfiles.isEmpty() && installAllFeaturesByDefault, toArray(startupRepositories))
               .features(toArray(startupFeatures))
               .bundles(toArray(startupBundles))
               .profiles(toArray(startupProfiles));
        // Boot
        builder.defaultStage(Builder.Stage.Boot)
                .kars(toArray(bootKars))
                .repositories(bootFeatures.isEmpty() && bootProfiles.isEmpty() && installAllFeaturesByDefault, toArray(bootRepositories))
                .features(toArray(bootFeatures))
                .bundles(toArray(bootBundles))
                .profiles(toArray(bootProfiles));
        // Installed
        builder.defaultStage(Builder.Stage.Installed)
                .kars(toArray(installedKars))
                .repositories(installedFeatures.isEmpty() && installedProfiles.isEmpty() && installAllFeaturesByDefault, toArray(installedRepositories))
                .features(toArray(installedFeatures))
                .bundles(toArray(installedBundles))
                .profiles(toArray(installedProfiles));

        // Generate the assembly
        builder.generateAssembly();

        // Include project classes content
        if (includeBuildOutputDirectory)
            IoUtils.copyDirectory(new File(project.getBuild().getOutputDirectory()), workDirectory);

        // Overwrite assembly dir contents
        if (sourceDirectory.exists())
            IoUtils.copyDirectory(sourceDirectory, workDirectory);

        // Chmod the bin/* scripts
        File[] files = new File(workDirectory, "bin").listFiles();
        if( files!=null ) {
            for (File file : files) {
                if( !file.getName().endsWith(".bat") ) {
                    try {
                        Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rwxr-xr-x"));
                    } catch (Throwable ignore) {
                        // we tried our best, perhaps the OS does not support posix file perms.
                    }
                }
            }
        }
    }

    private Object invoke(Object object, String getter) throws MojoExecutionException {
        try {
            return object.getClass().getMethod(getter).invoke(object);
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to build remote repository from " + object.toString(), e);
        }
    }

    private Object getPolicy(Object object, boolean snapshots) throws MojoExecutionException {
        return invoke(object, "getPolicy", new Class[] { Boolean.TYPE }, new Object[] { snapshots });
    }

    private Object invoke(Object object, String getter, Class[] types, Object[] params) throws MojoExecutionException {
        try {
            return object.getClass().getMethod(getter, types).invoke(object, params);
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to build remote repository from " + object.toString(), e);
        }
    }

    private String getType(Artifact artifact) {
        // Identify kars
        if ("kar".equals(artifact.getType())) {
            return "kar";
        }
        if ("zip".equals(artifact.getType())) {
            try (ZipFile zip = new ZipFile(artifact.getFile())) {
                if (zip.getEntry("META-INF/KARAF.MF") != null) {
                    return "kar";
                }
            } catch (IOException e) {
                // Ignore
            }
        }
        // Identify features
        if ("features".equals(artifact.getClassifier())) {
            return "features";
        }
        if ("xml".equals(artifact.getType())) {
            try (InputStream is = new FileInputStream(artifact.getFile())) {
                XMLInputFactory xif = XMLInputFactory.newFactory();
                xif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
                XMLStreamReader r = xif.createXMLStreamReader(is);
                r.nextTag();
                QName name = r.getName();
                if (name.getLocalPart().equals("features")
                        && (name.getNamespaceURI().isEmpty()
                                || name.getNamespaceURI().startsWith("http://karaf.apache.org/xmlns/features/"))) {
                    return "features";
                }

            } catch (Exception e) {
                // Ignore
            }
        }
        // Identify bundles
        if ("bundle".equals(artifact.getType())) {
            return "bundle";
        }
        if ("jar".equals(artifact.getType())) {
            try (JarFile jar = new JarFile(artifact.getFile())) {
                Manifest manifest = jar.getManifest();
                if (manifest != null
                        && manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME) != null) {
                    return "bundle";
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        return "unknown";
    }

    private String artifactToMvn(Artifact artifact) throws MojoExecutionException {
        String uri;

        String groupId = artifact.getGroupId();
        String artifactId = artifact.getArtifactId();
        String version = artifact.getBaseVersion();
        String type = artifact.getArtifactHandler().getExtension();
        String classifier = artifact.getClassifier();

        if (MavenUtil.isEmpty(classifier)) {
            if ("jar".equals(type)) {
                uri = String.format("mvn:%s/%s/%s", groupId, artifactId, version);
            } else {
                uri = String.format("mvn:%s/%s/%s/%s", groupId, artifactId, version, type);
            }
        } else {
            uri = String.format("mvn:%s/%s/%s/%s/%s", groupId, artifactId, version, type, classifier);
        }
        return uri;
    }

    private String[] toArray(List<String> strings) {
        return strings.toArray(new String[strings.size()]);
    }

    private List<String> nonNullList(List<String> list) {
        final List<String> nonNullList = list == null ? new ArrayList<>() : list;
        return nonNullList.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

}
