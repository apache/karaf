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

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.main.Main;
import org.apache.karaf.tooling.utils.MojoSupport;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Run a Karaf instance
 */
@Mojo(name = "run", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = false)
public class RunMojo extends MojoSupport {

    /**
     * Directory containing Karaf container base directory.
     */
    @Parameter(defaultValue = "${project.build.directory}/karaf")
    private File karafDirectory = null;

    /**
     * Location where to download the Karaf distribution
     */
    @Parameter(defaultValue = "mvn:org.apache.karaf/apache-karaf/LATEST/zip")
    private String karafDistribution = null;

    /**
     * Define if the project artifact should be deployed in the started container or not
     */
    @Parameter(defaultValue = "true")
    private boolean deployProjectArtifact = true;

    /**
     * A list of URLs referencing feature repositories that will be added
     * to the karaf instance started by this goal.
     */
    @Parameter
    private String[] featureRepositories = null;

    /**
     * Comma-separated list of features to install.
     */
    @Parameter(defaultValue = "")
    private String featuresToInstall = null;

    /**
     * Define if the Karaf container keep running or stop just after the goal execution
     */
    @Parameter(defaultValue = "true")
    private boolean keepRunning = true;

    /**
     * Define if the Karaf embedded sshd should be started or not
     */
    @Parameter(defaultValue = "false")
    private String startSsh = "false";

    private static final Pattern mvnPattern = Pattern.compile("mvn:([^/ ]+)/([^/ ]+)/([^/ ]*)(/([^/ ]+)(/([^/ ]+))?)?");

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (karafDirectory.exists()) {
            getLog().info("Using Karaf container located " + karafDirectory.getAbsolutePath());
        } else {
            getLog().info("Extracting Karaf container");
            try {
                File karafArchiveFile = resolveFile(karafDistribution);
                extract(karafArchiveFile, karafDirectory);
            } catch (Exception e) {
                throw new MojoFailureException("Can't extract Karaf container", e);
            }
        }

        getLog().info("Starting Karaf container");
        System.setProperty("karaf.home", karafDirectory.getAbsolutePath());
        System.setProperty("karaf.base", karafDirectory.getAbsolutePath());
        System.setProperty("karaf.data", karafDirectory.getAbsolutePath() + "/data");
        System.setProperty("karaf.etc", karafDirectory.getAbsolutePath() + "/etc");
        System.setProperty("karaf.log", karafDirectory.getAbsolutePath() + "/data/log");
        System.setProperty("karaf.instances", karafDirectory.getAbsolutePath() + "/instances");
        System.setProperty("karaf.startLocalConsole", "false");
        System.setProperty("karaf.startRemoteShell", startSsh);
        System.setProperty("karaf.lock", "false");
        Main main = new Main(new String[0]);
        try {
            main.launch();
            while (main.getFramework().getState() != Bundle.ACTIVE) {
                Thread.sleep(1000);
            }
            BundleContext bundleContext = main.getFramework().getBundleContext();
            Object bootFinished = null;
            while (bootFinished == null) {
                Thread.sleep(1000);
                ServiceReference ref = bundleContext.getServiceReference(BootFinished.class);
                if (ref != null) {
                    bootFinished = bundleContext.getService(ref);
                }
            }

            Object featureService = findFeatureService(bundleContext);
            addFeatureRepositories(featureService);
            deploy(bundleContext, featureService);
            addFeatures(featureService);
            if (keepRunning)
                main.awaitShutdown();
            main.destroy();
        } catch (Throwable e) {
            throw new MojoExecutionException("Can't start container", e);
        } finally {
            System.gc();
        }
    }

    void addFeatureRepositories(Object featureService) throws MojoExecutionException {
    	if (featureRepositories != null) {
            try {
            	Class<? extends Object> serviceClass = featureService.getClass();
                Method addRepositoryMethod = serviceClass.getMethod("addRepository", URI.class);

                for (String featureRepo : featureRepositories) {
                    addRepositoryMethod.invoke(featureService, URI.create(featureRepo));
                }
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to add feature repositories to karaf", e);
            }
    	}
    }

    void deploy(BundleContext bundleContext, Object featureService) throws MojoExecutionException {
        if (deployProjectArtifact) {
            File artifact = project.getArtifact().getFile();
            File attachedFeatureFile = getAttachedFeatureFile(project);
            boolean artifactExists = artifact != null && artifact.exists();
            boolean attachedFeatureFileExists = attachedFeatureFile != null && attachedFeatureFile.exists();
            if (artifactExists) {
                if (project.getPackaging().equals("bundle")) {
                    try {
                        Bundle bundle = bundleContext.installBundle(artifact.toURI().toURL().toString());
                        bundle.start();
                    } catch (Exception e) {
                        throw new MojoExecutionException("Can't deploy project artifact in container", e);
                    }
                } else {
                    throw new MojoExecutionException("Packaging " + project.getPackaging() + " is not supported");
                }
            } else if (attachedFeatureFileExists) {
                addFeaturesAttachmentAsFeatureRepository(featureService, attachedFeatureFile);
            } else {
                throw new MojoExecutionException("Project artifact doesn't exist");
            }
        }
    }

    void addFeatures(Object featureService) throws MojoExecutionException {
    	if (featuresToInstall != null) {
            try {
            	Class<? extends Object> serviceClass = featureService.getClass();
                Method installFeatureMethod = serviceClass.getMethod("installFeature", String.class);
                String[] features = featuresToInstall.split(" *, *");
                for (String feature : features) {
                    installFeatureMethod.invoke(featureService, feature);
                    Thread.sleep(1000L);
                }
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to add features to karaf", e);
            }
    	}
    }

    public static void extract(File sourceFile, File targetFolder) throws IOException {
        if (sourceFile.getAbsolutePath().indexOf(".zip") > 0) {
            extractZipDistribution(sourceFile, targetFolder);
        } else if (sourceFile.getAbsolutePath().indexOf(".tar.gz") > 0) {
            extractTarGzDistribution(sourceFile, targetFolder);
        } else {
            throw new IllegalStateException("Unknown packaging of distribution; only zip or tar.gz could be handled.");
        }
        return;
    }

    private static void extractTarGzDistribution(File sourceDistribution, File _targetFolder) throws IOException {
        File uncompressedFile = File.createTempFile("uncompressedTarGz-", ".tar");
        extractGzArchive(new FileInputStream(sourceDistribution), uncompressedFile);
        extract(new TarArchiveInputStream(new FileInputStream(uncompressedFile)), _targetFolder);
        FileUtils.forceDelete(uncompressedFile);
    }

    private static void extractZipDistribution(File sourceDistribution, File _targetFolder) throws IOException {
        extract(new ZipArchiveInputStream(new FileInputStream(sourceDistribution)), _targetFolder);
    }

    private static void extractGzArchive(InputStream tarGz, File tar) throws IOException {
        BufferedInputStream in = new BufferedInputStream(tarGz);
        FileOutputStream out = new FileOutputStream(tar);
        GzipCompressorInputStream gzIn = new GzipCompressorInputStream(in);
        final byte[] buffer = new byte[1000];
        int n = 0;
        while (-1 != (n = gzIn.read(buffer))) {
            out.write(buffer, 0, n);
        }
        out.close();
        gzIn.close();
    }

    private static void extract(ArchiveInputStream is, File targetDir) throws IOException {
        try {
            if (targetDir.exists()) {
                FileUtils.forceDelete(targetDir);
            }
            targetDir.mkdirs();
            ArchiveEntry entry = is.getNextEntry();
            while (entry != null) {
                String name = entry.getName();
                name = name.substring(name.indexOf("/") + 1);
                File file = new File(targetDir, name);
                if (entry.isDirectory()) {
                    file.mkdirs();
                }
                else {
                    file.getParentFile().mkdirs();
                    OutputStream os = new FileOutputStream(file);
                    try {
                        IOUtils.copy(is, os);
                    }
                    finally {
                        IOUtils.closeQuietly(os);
                    }
                }
                entry = is.getNextEntry();
            }
        }
        finally {
            is.close();
        }
    }

    protected static boolean isMavenUrl(String name) {
        Matcher m = mvnPattern.matcher(name);
        return m.matches();
    }

    private File resolveFile(String file) {
        File fileResolved = null;

        if (isMavenUrl(file)) {
            fileResolved = new File(fromMaven(file));
            try {
                Artifact artifactTemp = resourceToArtifact(file, false);
                if (!fileResolved.exists()) {
                    try {
                        artifactResolver.resolve(artifactTemp, remoteRepos, localRepo);
                        fileResolved = artifactTemp.getFile();
                    } catch (ArtifactResolutionException e) {
                        getLog().error("Artifact was not resolved", e);
                    } catch (ArtifactNotFoundException e) {
                        getLog().error("Artifact was not found", e);
                    }
                }
            } catch (MojoExecutionException e) {
                getLog().error(e);
            }
        } else {
            fileResolved = new File(file);
        }

        return fileResolved;
    }

    /**
     * Return a path for an artifact:
     * - if the input is already a path (doesn't contain ':'), the same path is returned.
     * - if the input is a Maven URL, the input is converted to a default repository location path, type and classifier
     *   are optional.
     *
     * @param name artifact data
     * @return path as supplied or a default Maven repository path
     */
    private static String fromMaven(String name) {
        Matcher m = mvnPattern.matcher(name);
        if (!m.matches()) {
            return name;
        }

        StringBuilder b = new StringBuilder();
        b.append(m.group(1));
        for (int i = 0; i < b.length(); i++) {
            if (b.charAt(i) == '.') {
                b.setCharAt(i, '/');
            }
        }
        b.append("/"); // groupId
        String artifactId = m.group(2);
        String version = m.group(3);
        String extension = m.group(5);
        String classifier = m.group(7);
        b.append(artifactId).append("/"); // artifactId
        b.append(version).append("/"); // version
        b.append(artifactId).append("-").append(version);
        if (present(classifier)) {
            b.append("-").append(classifier);
        }
        if (present(classifier)) {
            b.append(".").append(extension);
        } else {
            b.append(".jar");
        }
        return b.toString();
    }

    private static boolean present(String part) {
        return part != null && !part.isEmpty();
    }

    private File getAttachedFeatureFile(MavenProject project) {
        List<Artifact> attachedArtifacts = project.getAttachedArtifacts();
        for (Artifact artifact : attachedArtifacts) {
            if ("features".equals(artifact.getClassifier()) && "xml".equals(artifact.getType())) {
                return artifact.getFile();
            }
        }

        return null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" }) Object findFeatureService(BundleContext bundleContext) {
        // Use Object as the service type and use reflection when calling the service,
    	// because the returned services use the OSGi classloader
    	ServiceReference ref = bundleContext.getServiceReference(FeaturesService.class);
        if (ref != null) {
            Object featureService = bundleContext.getService(ref);
            return featureService;
        }

        return null;
    }

    private void addFeaturesAttachmentAsFeatureRepository(Object featureService, File attachedFeatureFile) throws MojoExecutionException {
        if (featureService != null) {
            try {
            	Class<? extends Object> serviceClass = featureService.getClass();
            	Method addRepositoryMethod = serviceClass.getMethod("addRepository", URI.class);
                addRepositoryMethod.invoke(featureService, attachedFeatureFile.toURI());
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to register attachment as feature repository", e);
            }
        } else {
            throw new MojoExecutionException("Failed to find the FeatureService when adding a feature repository");
        }
    }

}
