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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;

import org.apache.karaf.deployer.kar.KarArtifactInstaller;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.internal.model.Bundle;
import org.apache.karaf.features.internal.model.Features;
import org.apache.karaf.features.internal.model.Feature;
import org.apache.karaf.features.internal.model.JaxbUtil;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Installs kar dependencies into a server-under-construction in target/assembly
 *
 * @version $Revision: 1.1 $
 * @goal install-kars
 * @phase process-resources
 * @requiresDependencyResolution runtime
 * @inheritByDefault true
 * @description Install kar dependencies
 */
public class InstallKarsMojo extends MojoSupport {

    /**
     * Directory that resources are copied to during the build.
     *
     * @parameter expression="${project.build.directory}/assembly"
     * @required
     */
    protected String workDirectory;

    /**
     * features config file.
     *
     * @parameter expression="${project.build.directory}/assembly/etc/org.apache.karaf.features.cfg"
     * @required
     */
    protected File featuresCfgFile;

    /**
     * startup.properties file.
     *
     * @parameter expression="${project.build.directory}/assembly/etc/startup.properties"
     * @required
     */
    protected File startupPropertiesFile;

    /**
     * if false, unpack to system and add bundles to startup.properties
     * if true, unpack to local-repo and add feature to features config
     *
     * @parameter
     */
    protected boolean unpackToLocalRepo;

    /**
     * Directory that resources are copied to during the build.
     *
     * @parameter expression="${project.build.directory}/assembly/local-repo"
     * @required
     */
    protected String localRepoDirectory;

    /**
     * Directory that resources are copied to during the build.
     *
     * @parameter expression="${project.build.directory}/assembly/system"
     * @required
     */
    protected String systemDirectory;
    private String repoPath;

    public void execute() throws MojoExecutionException, MojoFailureException {
        KarArtifactInstaller installer = new KarArtifactInstaller();
        installer.setBasePath(workDirectory);
        repoPath = unpackToLocalRepo ? localRepoDirectory : systemDirectory;
        installer.setLocalRepoPath(repoPath);
        FeaturesService featuresService = new OfflineFeaturesService();
        installer.setFeaturesService(featuresService);
        installer.init();
        Collection<Artifact> dependencies = project.getDependencyArtifacts();
        StringBuilder buf = new StringBuilder();
        for (Artifact artifact: dependencies) {
            if ("kar".equals(artifact.getType()) && "compile".equals(artifact.getScope())) {
                File file = artifact.getFile();
                try {
                    installer.install(file);
                } catch (Exception e) {
                    buf.append("Could not install kar: ").append(artifact.toString()).append("\n");
                    buf.append(e.getMessage()).append("\n\n");
                }
            }
            if ("features".equals(artifact.getClassifier()) && "compile".equals(artifact.getScope())) {
                //TODO
            }
        }
        if (buf.length() > 0) {
            throw new MojoExecutionException("Could not unpack all dependencies:\n" + buf.toString());
        }
    }

    private class OfflineFeaturesService implements FeaturesService {
        private static final String FEATURES_REPOSITORIES = "featuresRepositories";

        public void validateRepository(URI uri) throws Exception {
        }

        public void addRepository(URI url) throws Exception {
            if (unpackToLocalRepo) {
                getLog().info("Adding feature repository to local-repo: " + url);
                if (featuresCfgFile.exists()) {
                    Properties properties = new Properties();
                    InputStream in = new FileInputStream(featuresCfgFile);
                    try {
                        properties.load(in);
                    } finally {
                        in.close();
                    }
                    String existingFeatureRepos = properties.containsKey(FEATURES_REPOSITORIES)? properties.getProperty(FEATURES_REPOSITORIES) + ",": "";
                    existingFeatureRepos = existingFeatureRepos + url.toString();
                    properties.setProperty(FEATURES_REPOSITORIES, existingFeatureRepos);
                    FileOutputStream out = new FileOutputStream(featuresCfgFile);
                    try {
                        properties.store(out, "Features Service config");
                    } finally {
                        out.close();
                    }
                }
            } else {
                getLog().info("Installing feature to system and startup.properties");
                Properties startupProperties = new Properties();
                if (startupPropertiesFile.exists()) {
                    InputStream in = new FileInputStream(startupPropertiesFile);
                    try {
                        startupProperties.load(in);
                    } finally {
                        in.close();
                    }
                } else {
                    if (!startupPropertiesFile.getParentFile().exists()) {
                        startupPropertiesFile.getParentFile().mkdirs();
                    }
                }
                DefaultRepositoryLayout layout = new DefaultRepositoryLayout();
                String[] bits = url.toString().split("[:/]");
                Artifact artifact = factory.createArtifactWithClassifier(bits[1], bits[2], bits[3], bits[4], bits[5]);
                String featuresPath = repoPath + "/" + layout.pathOf(artifact);
                File repoFile = new File(featuresPath);
                InputStream in = new FileInputStream(repoFile);
                Features features;
                try {
                    features = JaxbUtil.unmarshal(in, false);
                } finally {
                    in.close();
                }
                for (Feature feature: features.getFeature()) {
                    for (Bundle bundle: feature.getBundle()) {
                        String location = bundle.getLocation();
                        String startLevel = Integer.toString(bundle.getStartLevel());
                        bits = location.toString().split("[:/]");
                        if (bits.length < 4) {
                            getLog().warn("bad bundle: " + location);
                        } else {
                        Artifact bundleArtifact = factory.createArtifact(bits[1], bits[2], bits[3], null, bits.length == 4? "jar": bits[4]);
                        String bundlePath = layout.pathOf(bundleArtifact);
                        startupProperties.put(bundlePath, startLevel);
                        }
                    }
                }

                OutputStream out = new FileOutputStream(startupPropertiesFile);
                try {
                    startupProperties.store(out, "startup bundles");
                } finally {
                    out.close();
                }

            }
        }

        public void removeRepository(URI url) {
        }

        public void restoreRepository(URI url) throws Exception {
        }

        public Repository[] listRepositories() {
            return new Repository[0];
        }

        public void installFeature(String name) throws Exception {
        }

        public void installFeature(String name, String version) throws Exception {
        }

        public void installFeature(String name, String version, EnumSet<Option> options) throws Exception {
        }

        public void installFeature(org.apache.karaf.features.Feature f, EnumSet<Option> options) throws Exception {
        }

        public void installFeatures(Set<org.apache.karaf.features.Feature> features, EnumSet<Option> options) throws Exception {
        }

        public void uninstallFeature(String name) throws Exception {
        }

        public void uninstallFeature(String name, String version) throws Exception {
        }

        public Feature[] listFeatures() throws Exception {
            return new Feature[0];
        }

        public Feature[] listInstalledFeatures() {
            return new Feature[0];
        }

        public boolean isInstalled(org.apache.karaf.features.Feature f) {
            return false;
        }

        public org.apache.karaf.features.Feature getFeature(String name, String version) throws Exception {
            return null;
        }

        public org.apache.karaf.features.Feature getFeature(String name) throws Exception {
            return null;
        }
    }
}
