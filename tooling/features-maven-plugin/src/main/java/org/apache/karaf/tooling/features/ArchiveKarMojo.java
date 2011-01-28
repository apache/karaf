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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.features.internal.Bundle;
import org.apache.karaf.features.internal.Feature;
import org.apache.karaf.features.internal.FeaturesRoot;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.FileUtils;

/**
 * assembles a kar archive from a features.xml file
 *
 * @version $Revision: 1.1 $
 * @goal archive-kar
 * @phase compile
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
     * The Jar archiver.
     *
     * @component role="org.codehaus.plexus.archiver.manager.ArchiverManager"
     * @required
     * @readonly
     */
    private ArchiverManager archiverManager = null;

    /**
     * The module base directory.
     *
     * @parameter expression="${project.basedir}"
     * @required
     * @readonly
     */
    private File baseDirectory = null;

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
     * The Geronimo repository where modules will be packaged up from.
     *
     * @parameter expression="${project.build.directory}/repository"
     * @required
     */
    private File targetRepository = null;

    /**
     * Location of resources directory for additional content to include in the car.
     *
     * @parameter expression="${project.build.directory}/resources"
     */
    private File resourcesDir;


    /**
     * The features file to use as instructions
     *
     * @parameter default-value="${project.build.directory}/feature/feature.xml"
     */
    private File featuresFile;
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
                FeaturesRoot features = JaxbUtil.unmarshal(FeaturesRoot.class, in, false);
                for (Feature feature : features.getFeature()) {
                    for (Bundle bundle : feature.getBundle()) {
                        if (bundle.isDependency() == null || !bundle.isDependency()) {
                            bundles.add(bundleToArtifact(bundle.getValue()));
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

    private File getArtifactInRepositoryDir() {
        //
        // HACK: Generate the filename in the repo... really should delegate this to the repo impl
        //

        String groupId = project.getGroupId().replace('.', '/');
        String artifactId = project.getArtifactId();
        String version = project.getVersion();
        String type = "car";


        File dir = new File(targetRepository, groupId);
        dir = new File(dir, artifactId);
        dir = new File(dir, version);
        dir = new File(dir, artifactId + "-" + version + "." + type);

        return dir;
    }


    /**
     * Generates the configuration archive.
     * @param bundles
     */
    private File createArchive(List<Artifact> bundles) throws MojoExecutionException {
        File archiveFile = getArchiveFile(outputDirectory, finalName, null);

        GeronimoArchiver archiver = new GeronimoArchiver(archiverManager);
        archiver.setArchiver(jarArchiver);
        archiver.setOutputFile(archiveFile);

        try {

            for (Artifact artifact: bundles) {
                resolver.resolve(artifact, remoteRepos, localRepo);
                File localFile = artifact.getFile();
                //TODO isn't there a maven class that can do this better?
                String dir = artifact.getGroupId().replace('.', '/') + "/" + artifact.getArtifactId() + "/" + artifact.getVersion() + "/";
                String name = artifact.getArtifactId() + "-" + artifact.getVersion() + (artifact.getClassifier() != null ? "-" + artifact.getClassifier() : "") + "." + artifact.getType();
                String targetFileName = dir + name;
                jarArchiver.addFile(localFile, targetFileName);
            }

            // Include the generated artifact contents
            File artifactDirectory = this.getArtifactInRepositoryDir();

            if (artifactDirectory.exists()) {
                archiver.addArchivedFileSet(artifactDirectory);
            }

            if (resourcesDir.isDirectory()) {
                archiver.getArchiver().addDirectory(resourcesDir);
            }

            //
            // HACK: Include legal files here for sanity
            //

            //
            // NOTE: Would be nice to share this with the copy-legal-files mojo
            //
            String[] includes = {
                    "LICENSE.txt",
                    "LICENSE",

                    "NOTICE.txt",
                    "NOTICE",
                    "DISCLAIMER.txt",
                    "DISCLAIMER"
            };

            archiver.getArchiver().addDirectory(baseDirectory, "META-INF/", includes, new String[0]);

            //For no plan car, do nothing
//            if (artifactDirectory.exists()) {
//
//                JarFile includedJarFile = new JarFile(artifactDirectory) ;
//
//                if (includedJarFile.getEntry("META-INF/MANIFEST.MF") != null) {
//                    JarArchiver.FilesetManifestConfig mergeFilesetManifestConfig = new JarArchiver.FilesetManifestConfig();
//                    mergeFilesetManifestConfig.setValue("merge");
//                    archiver.getArchiver().setFilesetmanifest(mergeFilesetManifestConfig);
//                } else {
//                    //File configFile = new File(new File(getArtifactInRepositoryDir(), "META-INF"), "imports.txt");
//                    ZipEntry importTxtEntry = includedJarFile.getEntry("META-INF/imports.txt");
//                    if (importTxtEntry != null) {
//                        StringBuilder imports = new StringBuilder("org.apache.geronimo.kernel.osgi,");
//                        if (boot) {
//                            archive.addManifestEntry(Constants.BUNDLE_ACTIVATOR, BootActivator.class.getName());
//                            imports.append("org.apache.geronimo.system.osgi,");
//                        } else {
//                            archive.addManifestEntry(Constants.BUNDLE_ACTIVATOR, ConfigurationActivator.class.getName());
//                        }
//                        archive.addManifestEntry(Constants.BUNDLE_NAME, project.getName());
//                        archive.addManifestEntry(Constants.BUNDLE_VENDOR, project.getOrganization().getName());
//                        ArtifactVersion version = project.getArtifact().getSelectedVersion();
//                        String versionString = "" + version.getMajorVersion() + "." + version.getMinorVersion() + "." + version.getIncrementalVersion();
//                        if (version.getQualifier() != null) {
//                            versionString += "." + version.getQualifier();
//                        }
//                        archive.addManifestEntry(Constants.BUNDLE_VERSION, versionString);
//                        archive.addManifestEntry(Constants.BUNDLE_MANIFESTVERSION, "2");
//                        archive.addManifestEntry(Constants.BUNDLE_DESCRIPTION, project.getDescription());
//                        // NB, no constant for this one
//                        archive.addManifestEntry("Bundle-License", ((License) project.getLicenses().get(0)).getUrl());
//                        archive.addManifestEntry(Constants.BUNDLE_DOCURL, project.getUrl());
//                        archive.addManifestEntry(Constants.BUNDLE_SYMBOLICNAME, project.getGroupId() + "." + project.getArtifactId());
//                        Reader in = new InputStreamReader(includedJarFile.getInputStream(importTxtEntry));
//                        char[] buf = new char[1024];
//                        try {
//                            int i;
//                            while ((i = in.read(buf)) > 0) {
//                                imports.append(buf, 0, i);
//                            }
//                        } finally {
//                            in.close();
//                        }
//                        // do we have any additional processing directives?
//                        if (instructions != null) {
//                            String explicitImports = (String) instructions.get(Constants.IMPORT_PACKAGE);
//                            // if there is an Import-Package instructions, then add these imports to the
//                            // list
//                            if (explicitImports != null) {
//                                // if specified on multiple lines, remove the line-ends.
//                                explicitImports = explicitImports.replaceAll("[\r\n]", "");
//                                imports.append(',');
//                                imports.append(explicitImports);
//                            }
//                            String requiredBundles = (String) instructions.get(Constants.REQUIRE_BUNDLE);
//                            if (requiredBundles != null) {
//                                requiredBundles = requiredBundles.replaceAll("[\r\n]", "");
//                                archive.addManifestEntry(Constants.REQUIRE_BUNDLE, requiredBundles);
//                            }
//                        }
//                        archive.addManifestEntry(Constants.IMPORT_PACKAGE, imports.toString());
//                        archive.addManifestEntry(Constants.DYNAMICIMPORT_PACKAGE, "*");
//                    }
//                }
//            }
            archiver.createArchive(project, archive);

            return archiveFile;
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to create archive", e);
        } finally {
            archiver.cleanup();
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

    private static class GeronimoArchiver extends MavenArchiver {

        private ArchiverManager archiverManager;
        private List<File> tmpDirs = new ArrayList<File>();

        public GeronimoArchiver(ArchiverManager archiverManager) {
            this.archiverManager = archiverManager;
        }

        public void addArchivedFileSet(File archiveFile) throws ArchiverException {
            UnArchiver unArchiver;
            try {
                unArchiver = archiverManager.getUnArchiver(archiveFile);
            } catch (NoSuchArchiverException e) {
                throw new ArchiverException(
                        "Error adding archived file-set. UnArchiver not found for: " + archiveFile,
                        e);
            }

            File tempDir = FileUtils.createTempFile("archived-file-set.", ".tmp", null);

            tempDir.mkdirs();

            tmpDirs.add(tempDir);

            unArchiver.setSourceFile(archiveFile);
            unArchiver.setDestDirectory(tempDir);

            try {
                unArchiver.extract();
            } catch (IOException e) {
                throw new ArchiverException("Error adding archived file-set. Failed to extract: "
                        + archiveFile, e);
            }

            getArchiver().addDirectory(tempDir, null, null, null);
        }

        public void cleanup() {
            for (File dir : tmpDirs) {
                try {
                    FileUtils.deleteDirectory(dir);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            tmpDirs.clear();
        }

    }

}
