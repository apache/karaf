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

import org.apache.karaf.deployer.kar.KarArtifactInstaller;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.ConfigFileInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.internal.RepositoryImpl;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.jar.JarArchiver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Assembles and creates a KAR archive.
 *
 * @goal create-kar
 * @phase package
 * @requiresDependencyResolution runtime
 * @inheritByDefault true
 * @description Assemble and create a KAR archive from a feature.xml file
 */
public class CreateKarMojo extends MojoSupport {

    /**
     * The Maven archive configuration to use.
     * <p/>
     * See <a href="http://maven.apache.org/ref/current/maven-archiver/apidocs/org/apache/maven/archiver/MavenArchiveConfiguration.html">the Javadocs for MavenArchiveConfiguration</a>
     *
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * The Jar archiver.
     *
     * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="jar"
     * @required
     * @readonly
     */
    private JarArchiver jarArchiver = null;

    /**
     * Directory containing the generated archive.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File outputDirectory = null;

    /**
     * Name of the generated archive.
     *
     * @parameter expression="${project.build.finalName}"
     * @required
     */
    private String finalName = null;

    /**
     * Ignore the dependency flag on the bundles in the features XML
     *
     * @parameter default-value="false"
     */
    private boolean ignoreDependencyFlag;

    /**
     * Location of resources directory for additional content to include in the KAR.
     * Note that it includes everything under classes so as to include maven-remote-resources
     *
     * @parameter expression="${project.build.directory}/classes"
     */
    private File resourcesDir;

    /**
     * The features file to use as instructions
     *
     * @parameter default-value="${project.build.directory}/feature/feature.xml"
     */
    private File featuresFile;

    /**
     * The internal repository in the kar.
     *
     * @parameter default-value="${repositoryPath}"
     */
    private String repositoryPath = "repository/";

    public void execute() throws MojoExecutionException, MojoFailureException {
        List<Artifact> resources = readResources();
        // build the archive
        File archive = createArchive(resources);

        // attach the generated archive to install/deploy
        projectHelper.attachArtifact(project, "kar", null, archive);
    }

    /**
     * Read and load the bundles and configuration files contained in the features file.
     *
     * @return a list of resources artifact.
     * @throws MojoExecutionException
     */
    private List<Artifact> readResources() throws MojoExecutionException {
        List<Artifact> resources = new ArrayList<Artifact>();
        try {
            RepositoryImpl featuresRepo = new RepositoryImpl(featuresFile.toURI());
            Feature[] features = featuresRepo.getFeatures();
            for (Feature feature : features) {
                for (BundleInfo bundle : feature.getBundles()) {
                    if (ignoreDependencyFlag || (!ignoreDependencyFlag && !bundle.isDependency())) {
                        resources.add(resourceToArtifact(bundle.getLocation(), false));
                    }
                }
                for (ConfigFileInfo configFile : feature.getConfigurationFiles()) {
                    resources.add(resourceToArtifact(configFile.getLocation(), false));
                }
            }
            return resources;
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Could not interpret features XML file", e);
        }
    }

    private File createArchive(List<Artifact> bundles) throws MojoExecutionException {
        ArtifactRepositoryLayout layout = new DefaultRepositoryLayout();
        File archiveFile = getArchiveFile(outputDirectory, finalName, null);

        MavenArchiver archiver = new MavenArchiver();
        MavenArchiveConfiguration configuration = new MavenArchiveConfiguration();
        archiver.setArchiver(jarArchiver);
        archiver.setOutputFile(archiveFile);

        try {
            // include the features XML file
            Artifact featureArtifact = factory.createArtifactWithClassifier(project.getGroupId(), project.getArtifactId(), project.getVersion(), "xml", KarArtifactInstaller.FEATURES_CLASSIFIER);
            jarArchiver.addFile(featuresFile, repositoryPath + layout.pathOf(featureArtifact));

            for (Artifact artifact : bundles) {
                resolver.resolve(artifact, remoteRepos, localRepo);
                File localFile = artifact.getFile();
                // TODO this may not be reasonable, but... resolved snapshot artifacts have timestamped versions
                // which do not work in startup.properties
                artifact.setVersion(artifact.getBaseVersion());
                String targetFileName = repositoryPath + layout.pathOf(artifact);
                jarArchiver.addFile(localFile, targetFileName);
            }

            if (resourcesDir.isDirectory()) {
                archiver.getArchiver().addDirectory(resourcesDir);
            }

            archiver.createArchive(project, configuration);

            return archiveFile;
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to create kar archive", e);
        }
    }

    protected static File getArchiveFile(final File basedir, final String finalName, String classifier) {
        if (classifier == null) {
            classifier = "";
        } else if (classifier.trim().length() > 0 && !classifier.startsWith("-")) {
            classifier = "-" + classifier;
        }
        return new File(basedir, finalName + classifier + ".kar");
    }

}
