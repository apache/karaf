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

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.karaf.deployer.kar.KarArtifactInstaller;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.ConfigFileInfo;
import org.apache.karaf.features.internal.model.Feature;
import org.apache.karaf.features.internal.model.Features;
import org.apache.karaf.features.internal.model.JaxbUtil;
import org.apache.karaf.tooling.utils.MojoSupport;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.jar.JarArchiver;

/**
 * assembles a kar archive
 *
 * @goal features-create-kar
 * @phase package
 * @requiresDependencyResolution runtime
 * @inheritByDefault true
 * @description Assemble a kar archive from a features.xml file
 */
public class CreateKarMojo extends MojoSupport {

    /**
     * The maven archive configuration to use.
     * <p/>
     * See <a href="http://maven.apache.org/ref/current/maven-archiver/apidocs/org/apache/maven/archiver/MavenArchiveConfiguration.html">the Javadocs for MavenArchiveConfiguration</a>.
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
     * Location of resources directory for additional content to include in the kar.
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
     * The wrapper repository in the kar.
     *
     * @parameter default-value="${repositoryPath}"
     */
    private String repositoryPath = "repository/";


    //
    // Mojo
    //

    public void execute() throws MojoExecutionException, MojoFailureException {
        List<Artifact> resources = readResources();
        // Build the archive
        File archive = createArchive(resources);

        // Attach the generated archive for install/deploy
        projectHelper.attachArtifact(project, "kar", null, archive);
    }

    /**
     * Read bundles and configuration files in the features file.
     *
     * @return
     * @throws MojoExecutionException
     */
    private List<Artifact> readResources() throws MojoExecutionException {
        List<Artifact> resources = new ArrayList<Artifact>();
        try {
            InputStream in = new FileInputStream(featuresFile);
            try {
                Features features = JaxbUtil.unmarshal(in, false);
                for (Feature feature : features.getFeature()) {
                    for (BundleInfo bundle : feature.getBundles()) {
                        if (!bundle.isDependency()) {
                            resources.add(resourceToArtifact(bundle.getLocation(), false));
                        }
                    }
                    for (ConfigFileInfo configFile : feature.getConfigurationFiles()) {
                        resources.add(resourceToArtifact(configFile.getLocation(), false));
                    }
                }
                return resources;
            } finally {
                in.close();
            }
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Could not interpret features.xml", e);
        }
    }

    /**
     * Generates the configuration archive.
     *
     * @param bundles
     */
    private File createArchive(List<Artifact> bundles) throws MojoExecutionException {
        ArtifactRepositoryLayout layout = new DefaultRepositoryLayout();
        File archiveFile = getArchiveFile(outputDirectory, finalName, null);

        MavenArchiver archiver = new MavenArchiver();
        archiver.setArchiver(jarArchiver);
        archiver.setOutputFile(archiveFile);

        try {
            //TODO should .kar be a bundle?
//            archive.addManifestEntry(Constants.BUNDLE_NAME, project.getName());
//            archive.addManifestEntry(Constants.BUNDLE_VENDOR, project.getOrganization().getName());
//            ArtifactVersion version = project.getArtifact().getSelectedVersion();
//            String versionString = "" + version.getMajorVersion() + "." + version.getMinorVersion() + "." + version.getIncrementalVersion();
//            if (version.getQualifier() != null) {
//                versionString += "." + version.getQualifier();
//            }
//            archive.addManifestEntry(Constants.BUNDLE_VERSION, versionString);
//            archive.addManifestEntry(Constants.BUNDLE_MANIFESTVERSION, "2");
//            archive.addManifestEntry(Constants.BUNDLE_DESCRIPTION, project.getDescription());
//            // NB, no constant for this one
//            archive.addManifestEntry("Bundle-License", ((License) project.getLicenses().get(0)).getUrl());
//            archive.addManifestEntry(Constants.BUNDLE_DOCURL, project.getUrl());
//            //TODO this might need some help
//            archive.addManifestEntry(Constants.BUNDLE_SYMBOLICNAME, project.getArtifactId());

            //include the feature.xml
            Artifact featureArtifact = factory.createArtifactWithClassifier(project.getGroupId(), project.getArtifactId(), project.getVersion(), "xml", KarArtifactInstaller.FEATURE_CLASSIFIER);
            jarArchiver.addFile(featuresFile, repositoryPath + layout.pathOf(featureArtifact));

            if (featureArtifact.isSnapshot()) {
                // the artifact is a snapshot, create the maven-metadata-local.xml
                getLog().debug("Feature artifact is a SNAPSHOT, handling the maven-metadata-local.xml");
                File metadataTarget = new File(featuresFile.getParentFile(), "maven-metadata-local.xml");
                getLog().debug("Looking for " + metadataTarget.getAbsolutePath());
                if (!metadataTarget.exists()) {
                    // the maven-metadata-local.xml doesn't exist, create it
                    getLog().debug(metadataTarget.getAbsolutePath() + " doesn't exist, create it");
                    Metadata metadata = new Metadata();
                    metadata.setGroupId(featureArtifact.getGroupId());
                    metadata.setArtifactId(featureArtifact.getArtifactId());
                    metadata.setVersion(featureArtifact.getVersion());
                    metadata.setModelVersion("1.1.0");

                    Versioning versioning = new Versioning();
                    versioning.setLastUpdatedTimestamp(new Date(System.currentTimeMillis()));
                    Snapshot snapshot = new Snapshot();
                    snapshot.setLocalCopy(true);
                    versioning.setSnapshot(snapshot);
                    SnapshotVersion snapshotVersion = new SnapshotVersion();
                    snapshotVersion.setClassifier(featureArtifact.getClassifier());
                    snapshotVersion.setVersion(featureArtifact.getVersion());
                    snapshotVersion.setExtension(featureArtifact.getType());
                    snapshotVersion.setUpdated(versioning.getLastUpdated());
                    versioning.addSnapshotVersion(snapshotVersion);

                    metadata.setVersioning(versioning);

                    MetadataXpp3Writer metadataWriter = new MetadataXpp3Writer();
                    try {
                        Writer writer = new FileWriter(metadataTarget);
                        metadataWriter.write(writer, metadata);
                    } catch (Exception e) {
                        getLog().warn("Could not create maven-metadata-local.xml", e);
                        getLog().warn("It means that this SNAPSHOT could be overwritten by an older one present on remote repositories");
                    }
                }
                getLog().debug("Adding file " + metadataTarget.getAbsolutePath() + " in the jar path " + repositoryPath + layout.pathOf(featureArtifact).substring(0, layout.pathOf(featureArtifact).lastIndexOf('/')) + "/maven-metadata-local.xml");
                jarArchiver.addFile(metadataTarget, repositoryPath + layout.pathOf(featureArtifact).substring(0, layout.pathOf(featureArtifact).lastIndexOf('/')) + "/maven-metadata-local.xml");
            }

            for (Artifact artifact : bundles) {
                resolver.resolve(artifact, remoteRepos, localRepo);
                File localFile = artifact.getFile();

                if (artifact.isSnapshot()) {
                    // the artifact is a snapshot, create the maven-metadata-local.xml
                    File metadataTarget = new File(localFile.getParentFile(), "maven-metadata-local.xml");
                    if (!metadataTarget.exists()) {
                        // the maven-metadata-local.xml doesn't exist, create it
                        try {
                            MavenUtil.generateMavenMetadata(artifact, metadataTarget);
                        } catch (Exception e) {
                            getLog().warn("Could not create maven-metadata-local.xml", e);
                            getLog().warn("It means that this SNAPSHOT could be overwritten by an older one present on remote repositories");
                        }
                    }
                    jarArchiver.addFile(metadataTarget, repositoryPath + layout.pathOf(artifact).substring(0, layout.pathOf(artifact).lastIndexOf('/')) + "/maven-metadata-local.xml");
                }

                //TODO this may not be reasonable, but... resolved snapshot artifacts have timestamped versions
                //which do not work in startup.properties.
                artifact.setVersion(artifact.getBaseVersion());
                String targetFileName = repositoryPath + layout.pathOf(artifact);
                jarArchiver.addFile(localFile, targetFileName);
            }

            if (resourcesDir.isDirectory()) {
                archiver.getArchiver().addDirectory(resourcesDir);
            }
            archiver.createArchive(project, archive);

            return archiveFile;
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to create archive", e);
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
