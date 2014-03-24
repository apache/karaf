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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.internal.model.Bundle;
import org.apache.karaf.features.internal.model.Feature;
import org.apache.karaf.features.internal.model.Features;
import org.apache.karaf.features.internal.model.JaxbUtil;
import org.apache.karaf.kar.internal.Kar;
import org.apache.karaf.tooling.utils.MojoSupport;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Installs kar dependencies into a server-under-construction in target/assembly
 *
 * @goal install-kars
 * @phase process-resources
 * @requiresDependencyResolution runtime
 * @inheritByDefault true
 * @description Install kar dependencies
 */
public class InstallKarsMojo extends MojoSupport {

    /**
     * Base directory used to copy the resources during the build (working directory).
     *
     * @parameter default-value="${project.build.directory}/assembly"
     * @required
     */
    protected String workDirectory;

    /**
     * Features configuration file (etc/org.apache.karaf.features.cfg).
     *
     * @parameter default-value="${project.build.directory}/assembly/etc/org.apache.karaf.features.cfg"
     * @required
     */
    protected File featuresCfgFile;

    /**
     * startup.properties file.
     *
     * @parameter default-value="${project.build.directory}/assembly/etc/startup.properties"
     * @required
     */
    protected File startupPropertiesFile;

    /**
     * default start level for bundles in features that don't specify it.
     *
     * @parameter
     */
    protected int defaultStartLevel = 30;

    /**
     * if false, unpack to system and add bundles to startup.properties
     * if true, unpack to system and add feature to features config
     */
    protected boolean dontAddToStartup;

    /**
     * Directory used during build to construction the Karaf system repository.
     *
     * @parameter default-value="${project.build.directory}/assembly/system"
     * @required
     */
    protected File systemDirectory;

    /**
     * List of features from runtime-scope features xml and kars to be installed into system and listed in startup.properties.
     *
     * @parameter
     */
    private List<String> startupFeatures;

    /**
     * List of features from runtime-scope features xml and kars to be installed into system repo and listed in features service boot features.
     *
     * @parameter
     */
    private List<String> bootFeatures;

    /**
     * List of features from runtime-scope features xml and kars to be installed into system repo and not mentioned elsewhere.
     *
     * @parameter
     */
    private List<String> installedFeatures;

    /**
     * When a feature depends on another feature, try to find it in another referenced feature-file and install that one
     * too.
     *
     * @parameter
     */
    private boolean addTransitiveFeatures = true;

    private URI system;
    private Properties startupProperties = new Properties();
    private Set<Feature> featureSet = new HashSet<Feature>();
    private List<Dependency> missingDependencies = new ArrayList<Dependency>();

    // an access layer for available Aether implementation
    protected DependencyHelper dependencyHelper;

    /**
     * list of features to  install into local repo.
     */
    private List<Feature> localRepoFeatures = new ArrayList<Feature>();

    @SuppressWarnings("deprecation")
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        this.dependencyHelper = DependencyHelperFactory.createDependencyHelper(this.container, this.project, this.mavenSession, getLog());
        systemDirectory.mkdirs();
        system = systemDirectory.toURI();
        if (startupPropertiesFile.exists()) {
            try {
                InputStream in = new FileInputStream(startupPropertiesFile);
                try {
                    startupProperties.load(in);
                } finally {
                    in.close();
                }
            } catch (IOException e) {
                throw new MojoFailureException("Could not open existing startup.properties file at " + startupPropertiesFile, e);
            }
        } else {
            startupProperties.setHeader(Collections.singletonList("#Bundles to be started on startup, with startlevel"));
            if (!startupPropertiesFile.getParentFile().exists()) {
                startupPropertiesFile.getParentFile().mkdirs();
            }
        }

        FeaturesService featuresService = new OfflineFeaturesService();

        Collection<Artifact> dependencies = project.getDependencyArtifacts();
        StringBuilder buf = new StringBuilder();
        for (Artifact artifact : dependencies) {
            dontAddToStartup = "runtime".equals(artifact.getScope());
            if ("kar".equals(artifact.getType()) && acceptScope(artifact)) {
                File file = artifact.getFile();
                try {
                    Kar kar = new Kar(file.toURI());
                    kar.extract(new File(system.getPath()), new File(workDirectory));
                    for (URI repoUri : kar.getFeatureRepos()) {
                        featuresService.removeRepository(repoUri);
                        featuresService.addRepository(repoUri);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Could not install kar: " + artifact.toString() + "\n", e);
                    //buf.append("Could not install kar: ").append(artifact.toString()).append("\n");
                    //buf.append(e.getMessage()).append("\n\n");
                }
            }
            if ("features".equals(artifact.getClassifier()) && acceptScope(artifact)) {
                String uri = this.dependencyHelper.artifactToMvn(artifact);

                File source = artifact.getFile();
                DefaultRepositoryLayout layout = new DefaultRepositoryLayout();

                //remove timestamp version
                artifact = factory.createArtifactWithClassifier(artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion(), artifact.getType(), artifact.getClassifier());
                File target = new File(system.resolve(layout.pathOf(artifact)));

                if (!target.exists()) {
                    target.getParentFile().mkdirs();
                    try {
                        copy(source, target);
                    } catch (RuntimeException e) {
                        getLog().error("Could not copy features " + uri + " from source file " + source, e);
                    }

                    // for snapshot, generate the repository metadata in order to avoid override of snapshot from remote repositories
                    if (artifact.isSnapshot()) {
                        getLog().debug("Feature " + uri + " is a SNAPSHOT, generate the maven-metadata-local.xml file");
                        File metadataTarget = new File(target.getParentFile(), "maven-metadata-local.xml");
                        try {
                            MavenUtil.generateMavenMetadata(artifact, metadataTarget);
                        } catch (Exception e) {
                            getLog().warn("Could not create maven-metadata-local.xml", e);
                            getLog().warn("It means that this SNAPSHOT could be overwritten by an older one present on remote repositories");
                        }
                    }

                }
                try {
                    featuresService.addRepository(URI.create(uri));
                } catch (Exception e) {
                    buf.append("Could not install feature: ").append(artifact.toString()).append("\n");
                    buf.append(e.getMessage()).append("\n\n");
                }
            }
        }

        // install bundles listed in startup properties that weren't in kars into the system dir
        Set<?> keySet = startupProperties.keySet();
        for (Object keyObject : keySet) {
            String key = (String) keyObject;
            String path = this.dependencyHelper.pathFromMaven(key);
            File target = new File(system.resolve(path));
            if (!target.exists()) {
                install(key, target);
            }
        }

        // install bundles listed in install features not in system
        for (Feature feature : localRepoFeatures) {
            for (Bundle bundle : feature.getBundle()) {
                if (!bundle.isDependency()) {
                    String key = bundle.getLocation();
                    // remove wrap: protocol to resolve from maven
                    if (key.startsWith("wrap:")) {
                        key = key.substring(5);
                    }
                    String path = this.dependencyHelper.pathFromMaven(key);
                    File test = new File(system.resolve(path));
                    if (!test.exists()) {
                        File target = new File(system.resolve(path));
                        if (!target.exists()) {
                            install(key, target);
                            Artifact artifact = this.dependencyHelper.mvnToArtifact(key);
                            if (artifact.isSnapshot()) {
                                // generate maven-metadata-local.xml for the artifact
                                File metadataSource = new File(this.dependencyHelper.resolveById(key, getLog()).getParentFile(), "maven-metadata-local.xml");
                                File metadataTarget = new File(target.getParentFile(), "maven-metadata-local.xml");
                                metadataTarget.getParentFile().mkdirs();
                                try {
                                    if (!metadataSource.exists()) {
                                        // the maven-metadata-local.xml doesn't exist in the local repo, generate one
                                        MavenUtil.generateMavenMetadata(artifact, metadataTarget);
                                    } else {
                                        // copy the metadata to the target
                                        copy(metadataSource, metadataTarget);
                                    }
                                } catch (IOException ioException) {
                                    getLog().warn(ioException);
                                    getLog().warn("Unable to copy the maven-metadata-local.xml, it means that this SNAPSHOT will be overwritten by a remote one (if exist)");
                                }
                            }
                        }
                    }
                }
            }
        }

        try {
            OutputStream out = new FileOutputStream(startupPropertiesFile);
            try {
                startupProperties.save(out);
            } finally {
                out.close();
            }
        } catch (IOException e) {
            throw new MojoFailureException("Could not write startup.properties file at " + startupPropertiesFile, e);
        }
        if (buf.length() > 0) {
            throw new MojoExecutionException("Could not unpack all dependencies:\n" + buf.toString());
        }
    }

    private void install(String key, File target) throws MojoFailureException {
        File source = this.dependencyHelper.resolveById(key, getLog());
        target.getParentFile().mkdirs();
        copy(source, target);
    }

    private boolean acceptScope(Artifact artifact) {
        return "compile".equals(artifact.getScope()) || "runtime".equals(artifact.getScope());
    }

    private class OfflineFeaturesService implements FeaturesService {
        private static final String FEATURES_REPOSITORIES = "featuresRepositories";
        private static final String FEATURES_BOOT = "featuresBoot";

        @Override
        public void validateRepository(URI uri) throws Exception {
        }

        @Override
        public void addRepository(URI uri) throws Exception {
            if (dontAddToStartup) {
                getLog().info("Adding feature repository to system: " + uri);
                if (featuresCfgFile.exists()) {
                    Properties properties = new Properties();
                    InputStream in = new FileInputStream(featuresCfgFile);
                    try {
                        properties.load(in);
                    } finally {
                        in.close();
                    }
                    String existingFeatureRepos = retrieveProperty(properties, FEATURES_REPOSITORIES);
                    if (!existingFeatureRepos.contains(uri.toString())) {
                        existingFeatureRepos = existingFeatureRepos + uri.toString();
                        properties.put(FEATURES_REPOSITORIES, existingFeatureRepos);
                    }
                    Features repo = readFeatures(uri);
                    for (String innerRepository : repo.getRepository()) {
                        String innerRepositoryPath = dependencyHelper.pathFromMaven(innerRepository);
                        File innerRepositoryTargetInSystemRepository = new File(system.resolve(innerRepositoryPath));
                        if (!innerRepositoryTargetInSystemRepository.exists()) {
                            File innerRepositorySourceFile = dependencyHelper.resolveById(innerRepository, getLog());
                            innerRepositoryTargetInSystemRepository.getParentFile().mkdirs();
                            copy(innerRepositorySourceFile, innerRepositoryTargetInSystemRepository);

                            // add metadata for snapshot
                            Artifact innerRepositoryArtifact = dependencyHelper.mvnToArtifact(innerRepository);
                            if (innerRepositoryArtifact.isSnapshot()) {
                                getLog().debug("Feature repository " + innerRepository + " is a SNAPSHOT, generate the maven-metadata-local.xml file");
                                File metadataTarget = new File(innerRepositoryTargetInSystemRepository.getParentFile(), "maven-metadata-local.xml");
                                try {
                                    MavenUtil.generateMavenMetadata(innerRepositoryArtifact, metadataTarget);
                                } catch (Exception e) {
                                    getLog().warn("Could not create maven-metadata-local.xml", e);
                                    getLog().warn("It means that this SNAPSHOT could be overwritten by an older one present on remote repositories");
                                }
                            }
                        }
                    }
                    for (Feature feature : repo.getFeature()) {
                        featureSet.add(feature);
                        if (startupFeatures != null && startupFeatures.contains(feature.getName())) {
                            installFeature(feature, null);
                        } else if (bootFeatures != null && bootFeatures.contains(feature.getName())) {
                            localRepoFeatures.add(feature);
                            missingDependencies.addAll(feature.getDependencies());
                            String existingBootFeatures = retrieveProperty(properties, FEATURES_BOOT);
                            if (!existingBootFeatures.contains(feature.getName())) {
                                existingBootFeatures = existingBootFeatures + feature.getName();
                                properties.put(FEATURES_BOOT, existingBootFeatures);
                            }
                        } else if (installedFeatures != null && installedFeatures.contains(feature.getName())) {
                            localRepoFeatures.add(feature);
                            missingDependencies.addAll(feature.getDependencies());
                        }
                    }
                    if (addTransitiveFeatures) {
                        addMissingDependenciesToRepo();
                    }
                    FileOutputStream out = new FileOutputStream(featuresCfgFile);
                    try {
                        properties.save(out);
                    } finally {
                        out.close();
                    }
                }
            } else {
                getLog().info("Installing feature " + uri + " to system and startup.properties");
                Features features = readFeatures(uri);
                for (Feature feature : features.getFeature()) {
                    installFeature(feature, null);
                }
            }
        }

        private void addMissingDependenciesToRepo() {
            for (ListIterator<Dependency> iterator = missingDependencies.listIterator(); iterator.hasNext(); ) {
                Dependency dependency = iterator.next();
                Feature depFeature = lookupFeature(dependency);
                if (depFeature == null) {
                    continue;
                }
                localRepoFeatures.add(depFeature);
                iterator.remove();
                addAllMissingDependencies(iterator, depFeature);
            }
        }

        private void addAllMissingDependencies(ListIterator<Dependency> iterator, Feature depFeature) {
            for (Dependency dependency : depFeature.getDependencies()) {
                if (!missingDependencies.contains(dependency)) {
                    iterator.add(dependency);
                }
            }
        }

        @Override
        public void addRepository(URI uri, boolean install) throws Exception {
        }

        private String retrieveProperty(Properties properties, String key) {
            String val = properties.getProperty(key);
            return val != null && val.length() > 0 ? val + "," : "";
        }

        private Features readFeatures(URI uri) throws MojoExecutionException, XMLStreamException, JAXBException, IOException {
            File repoFile;
            if (uri.toString().startsWith("mvn:")) {
                URI featuresPath = system.resolve(dependencyHelper.pathFromMaven(uri.toString()));
                repoFile = new File(featuresPath);
            } else {
                repoFile = new File(uri);
            }
            InputStream in = new FileInputStream(repoFile);
            Features features;
            try {
                features = JaxbUtil.unmarshal(in, false);
            } finally {
                in.close();
            }
            return features;
        }

        @Override
        public void removeRepository(URI uri) {
        }

        @Override
        public void removeRepository(URI uri, boolean install) {
        }

        @Override
        public void restoreRepository(URI uri) throws Exception {
        }

        @Override
        public Repository[] listRepositories() {
            return new Repository[0];
        }

        @Override
        public URI getRepositoryUriFor(String name, String version) {
            return null;
        }

        @Override
        public String[] getRepositoryNames() {
            return new String[0];
        }

        @Override
        public void installFeature(String name) throws Exception {
        }

        @Override
        public void installFeature(String name, EnumSet<Option> options) throws Exception {
        }

        @Override
        public void installFeature(String name, String version) throws Exception {
        }

        @Override
        public void installFeature(String name, String version, EnumSet<Option> options) throws Exception {
        }

        @Override
        public void installFeature(org.apache.karaf.features.Feature feature, EnumSet<Option> options) throws Exception {
            List<String> comment = Arrays.asList(new String[]{"", "# feature: " + feature.getName() + " version: " + feature.getVersion()});
            for (BundleInfo bundle : feature.getBundles()) {
                String location = bundle.getLocation();
                String startLevel = Integer.toString(bundle.getStartLevel() == 0 ? defaultStartLevel : bundle.getStartLevel());
                if (startupProperties.containsKey(location)) {
                    int oldStartLevel = Integer.decode((String) startupProperties.get(location));
                    if (oldStartLevel > bundle.getStartLevel()) {
                        startupProperties.put(location, startLevel);
                    }
                } else {
                    if (comment == null) {
                        startupProperties.put(location, startLevel);
                    } else {
                        startupProperties.put(location, comment, startLevel);
                        comment = null;
                    }
                }
            }
        }

        private Feature lookupFeature(Dependency dependency) {
            for (Feature feature : featureSet) {
                if (featureSatisfiesDependency(feature, dependency)) {
                    return feature;
                }
            }
            return null;
        }

        private boolean featureSatisfiesDependency(Feature feature, Dependency dependency) {
            if (!feature.getName().equals(dependency.getName())) {
                return false;
            }
            return true;
        }

        @Override
        public void installFeatures(Set<org.apache.karaf.features.Feature> features, EnumSet<Option> options)
                throws Exception {
        }

        @Override
        public void uninstallFeature(String name) throws Exception {
        }

        @Override
        public void uninstallFeature(String name, EnumSet<Option> options) {
        }

        @Override
        public void uninstallFeature(String name, String version) throws Exception {
        }

        @Override
        public void uninstallFeature(String name, String version, EnumSet<Option> options) {
        }

        @Override
        public Feature[] listFeatures() throws Exception {
            return new Feature[0];
        }

        @Override
        public Feature[] listInstalledFeatures() {
            return new Feature[0];
        }

        @Override
        public boolean isInstalled(org.apache.karaf.features.Feature f) {
            return false;
        }

        @Override
        public org.apache.karaf.features.Feature getFeature(String name, String version) throws Exception {
            return null;
        }

        @Override
        public org.apache.karaf.features.Feature getFeature(String name) throws Exception {
            return null;
        }

        @Override
        public Repository getRepository(String repoName) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void refreshRepository(URI uri) throws Exception {
            // TODO Auto-generated method stub

        }
    }

}
