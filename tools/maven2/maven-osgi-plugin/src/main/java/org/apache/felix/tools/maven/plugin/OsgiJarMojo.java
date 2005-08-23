package org.apache.felix.tools.maven.plugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="tbennett@apache.org">Timothy Bennett</a>
 * @goal felix-jar
 * @phase package
 * @requiresDependencyResolution runtime
 * @description build a OSGi bundle jar
 */
public class OsgiJarMojo extends AbstractMojo {
    private static final String[] DEFAULT_EXCLUDES = new String[]{"**/package.html"};
    private static final String[] DEFAULT_INCLUDES = new String[]{"**/**"};

    /**
     * @todo Change type to File
     *
     * @parameter expression="${project.build.directory}"
     * @required
     * @readonly
     */
    private String basedir;

    /**
     * @parameter alias="jarName" expression="${project.build.finalName}"
     * @required
     */
    private String finalName;

    /**
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    private String outputDirectory;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter
     */
    private String manifestFile;

    /**
     * @parameter expression="${org.apache.felix.tools.maven.plugin.OsgiManifest}"
     */
    private OsgiManifest osgiManifest;

    /**
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * @todo Add license files in META-INF directory.
     */
    public void execute() throws MojoExecutionException {
        File jarFile = new File(basedir, finalName + ".jar");

        MavenArchiver archiver = new MavenArchiver();
        archiver.setOutputFile(jarFile);

        /*
            TODO: Decide if we accept merging of entire manifest.mf files
            Here's a big question to make a final decision at some point: Do accept
            merging of manifest entries located in some file somewhere in the project
            directory?  If so, do we allow both file and configuration based entries
            to be specified simultaneously and how do we merge these?

            For now... I'm going to disable support for file-based manifest entries.
        */
        //if (manifestFile != null) {
        //    File file = new File(project.getBasedir().getAbsolutePath(), manifestFile);
        //    getLog().info("Manifest file: " + file.getAbsolutePath() + " will be used");
        //    archive.setManifestFile(file);
        //} else {
        //    getLog().info("No manifest file specified. Default will be used.");
        //}

        // Look for any OSGi specified manifest entries in the maven-felix-plugin configuration
        // section of the POM.  If we find some, then add them to the target artifact's manifest.
        if (osgiManifest != null) {
            Map entries = osgiManifest.getEntries();
            if (entries.size() != 0) {
                getLog().info("OSGi bundle manifest entries have been specified." +
                        " Bundle manifest will be modified with the following entries: " + entries.toString());
                archive.addManifestEntries(entries);
            } else {
                getLog().info("No OSGi bundle manifest entries have been specified.  Bundle manifest will not be modified");
            }
        } else {
            getLog().info("No OSGi bundle manifest entries have been specified.  Bundle manifest will not be modified");
        }

        /*
            We are going to iterate thru the POM's specified jar dependencies.  If a dependency
            has a scope of either RUNTIME or COMPILE, then we'll jar them up inside the
            OSGi bundle artifact.

            We are also going to automatically construct the Bundle-Classpath manifest entry.
        */
        StringBuffer bundleClasspath = new StringBuffer();
        Set artifacts = project.getArtifacts();
        for (Iterator iter = artifacts.iterator(); iter.hasNext();) {
            Artifact artifact = (Artifact) iter.next();
            if (!Artifact.SCOPE_PROVIDED.equals(artifact.getScope()) &&
                    !Artifact.SCOPE_TEST.equals(artifact.getScope())) {
                String type = artifact.getType();
                if ("jar".equals(type)) {
                    File depFile = artifact.getFile();
                    File outDir = new File(outputDirectory);
                    try {
                        FileUtils.copyFileToDirectory(depFile, outDir);
                        if (bundleClasspath.length() == 0) bundleClasspath.append(".");
                        bundleClasspath.append("," + artifact.getFile().getName());
                    } catch (Exception e) {
                        String errmsg = "Error copying " + depFile.getAbsolutePath() + " to " + outDir.getAbsolutePath();
                        throw new MojoExecutionException(errmsg, e);
                    }
                }
            }
        }
        if (bundleClasspath.length() != 0) {
            archive.addManifestEntry("Bundle-ClassPath", bundleClasspath.toString());
        }
        bundleClasspath = null;

        // create the target bundle archive...
        try {
            File contentDirectory = new File(outputDirectory);
            if (!contentDirectory.exists()) {
                getLog().warn("Bundle archive JAR will be empty -- no content was marked for inclusion!");
            } else {
                archiver.getArchiver().addDirectory(contentDirectory, DEFAULT_INCLUDES, DEFAULT_EXCLUDES);
            }
            archiver.createArchive(project, archive);
        } catch (Exception e) {
            // TODO: improve error handling
            throw new MojoExecutionException("Error assembling Bundle archive JAR", e);
        }
    }
}
