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
import java.net.URI;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;

import org.apache.karaf.deployer.kar.KarArtifactInstaller;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Installs kar dependencies into a server-under-construction in target/assembly
 *
 * @version $Revision: 1.1 $
 * @goal install-kars
 * @phase process-resources
 * @execute phase="process-resources"
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
     * Directory that resources are copied to during the build.
     *
     * @parameter expression="${project.build.directory}/assembly/local-repo"
     * @required
     */
    protected String localRepoDirectory;

    public void execute() throws MojoExecutionException, MojoFailureException {
        KarArtifactInstaller installer = new KarArtifactInstaller();
        installer.setBasePath(workDirectory);
        installer.setLocalRepoPath(localRepoDirectory);
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
            if (featuresCfgFile.exists()) {
                Properties properties = new Properties();
                FileInputStream in = new FileInputStream(featuresCfgFile);
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

        public void installFeature(Feature f, EnumSet<Option> options) throws Exception {
        }

        public void installFeatures(Set<Feature> features, EnumSet<Option> options) throws Exception {
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

        public boolean isInstalled(Feature f) {
            return false;
        }

        public Feature getFeature(String name, String version) throws Exception {
            return null;
        }

        public Feature getFeature(String name) throws Exception {
            return null;
        }
    }
}
