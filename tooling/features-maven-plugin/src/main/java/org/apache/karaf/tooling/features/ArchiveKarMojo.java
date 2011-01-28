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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.features.internal.Bundle;
import org.apache.karaf.features.internal.Feature;
import org.apache.karaf.features.internal.Features;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.jar.JarArchiver;

/**
 * assembles a kar archive from a features.xml file
 *
 * @version $Revision: 1.1 $
 * @goal archive-kar
 * @phase package
 * @execute phase="package"
 * @requiresDependencyResolution runtime
 * @inheritByDefault true
 * @description Assemble a kar archive from a features.xml file
 */
public class ArchiveKarMojo extends MojoSupport {


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
     * Location of resources directory for additional content to include in the car.
     * Note that it includes everything under classes so as to include maven-remote-resources goo
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

    
    //
    // Mojo
    //

    public void execute() throws MojoExecutionException, MojoFailureException {
        List<Artifact> bundles = readBundles();
        // Build the archive
        File archive = createArchive(bundles);

        // Attach the generated archive for install/deploy
        project.getArtifact().setFile(archive);
    }

    private List<Artifact> readBundles() throws MojoExecutionException {
        List<Artifact> bundles = new ArrayList<Artifact>();
        try {
            InputStream in = new FileInputStream(featuresFile);
            try {
                Features features = JaxbUtil.unmarshal(in, false);
                for (Feature feature : features.getFeature()) {
                    for (Bundle bundle : feature.getBundle()) {
                        if (bundle.isDependency() == null || !bundle.isDependency()) {
                            bundles.add(bundleToArtifact(bundle.getValue(), false));
                        }
                    }
                }
                return bundles;
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
            Artifact featureArtifact = factory.createArtifactWithClassifier(project.getGroupId(), project.getArtifactId(), project.getVersion(), "xml", "feature");
            jarArchiver.addFile(featuresFile, repositoryPath + layout.pathOf(featureArtifact));

            for (Artifact artifact: bundles) {
                resolver.resolve(artifact, remoteRepos, localRepo);
                File localFile = artifact.getFile();
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
