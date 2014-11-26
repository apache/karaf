/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.tooling.utils;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.karaf.tooling.features.DependencyHelper;
import org.apache.karaf.util.StreamUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.settings.Proxy;
import org.codehaus.plexus.PlexusContainer;

@SuppressWarnings({"deprecation", "rawtypes", "unchecked"})
public abstract class MojoSupport extends AbstractMojo {

    /**
     * Maven ProjectHelper
     *
     * @component
     */
    protected MavenProjectHelper projectHelper;

    /**
     * The Maven project.
     *
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * Directory that resources are copied to during the build.
     *
     * @parameter default-value="${project.build.directory}/${project.artifactId}-${project.version}-installer"
     * @required
     */
    protected File workDirectory;

    /**
     * @component
     */
    protected MavenProjectBuilder projectBuilder;

    /**
     * @parameter default-value="${localRepository}"
     */
    protected ArtifactRepository localRepo;

    /**
     * @component
     */
    protected ArtifactMetadataSource artifactMetadataSource;

    /**
     * @component
     */
    protected ArtifactFactory factory;
    
    /**
     * The artifact type of a feature
     * 
     * @parameter default-value="xml"
     */
    private String featureArtifactType = "xml";
    
    /**
     * The Maven session.
     * 
     * @parameter default-value="${session}"
     * @readonly
     * @required
     */
    protected MavenSession mavenSession;

    /**
     * <p>We can't autowire strongly typed RepositorySystem from Aether because it may be Sonatype (Maven 3.0.x)
     * or Eclipse (Maven 3.1.x/3.2.x) version, so we switch to service locator by autowiring entire {@link PlexusContainer}</p>
     *
     * <p>It's a bit of a hack but we have not choice when we want to be usable both in Maven 3.0.x and 3.1.x/3.2.x</p>
     *
     * @component
     * @required
     * @readonly
     */
    protected PlexusContainer container;

    // an access layer for available Aether implementation
    protected DependencyHelper dependencyHelper;

    protected MavenProject getProject() {
        return project;
    }

    protected File getWorkDirectory() {
        return workDirectory;
    }

    public MavenProjectHelper getProjectHelper() {
        return projectHelper;
    }

    protected Map createManagedVersionMap(String projectId,
            DependencyManagement dependencyManagement) throws ProjectBuildingException {
        Map map;
        if (dependencyManagement != null
                && dependencyManagement.getDependencies() != null) {
            map = new HashMap();
            for (Iterator i = dependencyManagement.getDependencies().iterator(); i
                    .hasNext();) {
                Dependency d = (Dependency) i.next();

                try {
                    VersionRange versionRange = VersionRange
                            .createFromVersionSpec(d.getVersion());
                    Artifact artifact = factory.createDependencyArtifact(d
                            .getGroupId(), d.getArtifactId(), versionRange, d
                            .getType(), d.getClassifier(), d.getScope());
                    map.put(d.getManagementKey(), artifact);
                } catch (InvalidVersionSpecificationException e) {
                    throw new ProjectBuildingException(projectId,
                            "Unable to parse version '" + d.getVersion()
                                    + "' for dependency '"
                                    + d.getManagementKey() + "': "
                                    + e.getMessage(), e);
                }
            }
        } else {
            map = Collections.EMPTY_MAP;
        }
        return map;
    }
    
    protected String translateFromMaven(String uri) {
        if (uri.startsWith("mvn:")) {
            String[] parts = uri.substring("mvn:".length()).split("/");
            String groupId = parts[0];
            String artifactId = parts[1];
            String version = null;
            String classifier = null;
            String type = "jar";
            if (parts.length > 2) {
                version = parts[2];
                if (parts.length > 3) {
                    type = parts[3];
                    if (parts.length > 4) {
                        classifier = parts[4];
                    }
                }
            }
            String dir = groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/";
            String name = artifactId + "-" + version + (classifier != null ? "-" + classifier : "") + "." + type;

            return getLocalRepoUrl() + "/" + dir + name;
        }
        if (System.getProperty("os.name").startsWith("Windows") && uri.startsWith("file:")) {
                String baseDir = uri.substring(5).replace('\\', '/').replaceAll(" ", "%20");
                String result = baseDir;
                if (baseDir.indexOf(":") > 0) {
                        result = "file:///" + baseDir;
                }
                return result;
        }
        return uri;
    }

    protected String getLocalRepoUrl() {
         if (System.getProperty("os.name").startsWith("Windows")) {
             String baseDir = localRepo.getBasedir().replace('\\', '/').replaceAll(" ", "%20");
             return extractProtocolFromLocalMavenRepo()  + ":///" + baseDir;
         } else {
             return localRepo.getUrl();
         }
    }

    /**
     * Required because Maven 3 returns null in {@link ArtifactRepository#getProtocol()} (see KARAF-244)
     */
    protected String extractProtocolFromLocalMavenRepo() {
        try {
            return new URL(localRepo.getUrl()).getProtocol();
        } catch (MalformedURLException e) {
            // Basically this should not happen; if it does though cancel the process
            throw new RuntimeException("Repository URL is not valid", e);
        }
    }
    
    private Dependency findDependency(List<Dependency> dependencies, String artifactId, String groupId) {
        for(Dependency dep : dependencies) {
            if (artifactId.equals(dep.getArtifactId()) && groupId.equals(dep.getGroupId()) &&
                    featureArtifactType.equals(dep.getType())) {
                if (dep.getVersion() != null) 
                    return dep;
            }
        }
        return null;
    }

    /**
     * Convert a feature resourceLocation (bundle or configuration file) into an artifact.
     *
     * @param resourceLocation the feature resource location (bundle or configuration file).
     * @param skipNonMavenProtocols flag to skip protocol different than mvn:
     * @return the artifact corresponding to the resource.
     * @throws MojoExecutionException
     */
    protected Artifact resourceToArtifact(String resourceLocation, boolean skipNonMavenProtocols) throws MojoExecutionException {
        resourceLocation = resourceLocation.replace("\r\n", "").replace("\n", "").replace(" ", "").replace("\t", "");
        final int index = resourceLocation.indexOf("mvn:");
        if (index < 0) {
            if (skipNonMavenProtocols) {
                return null;
            }
            throw new MojoExecutionException("Resource URL is not a Maven URL: " + resourceLocation);
        } else {
            resourceLocation = resourceLocation.substring(index + "mvn:".length());
        }
        // Truncate the URL when a '#', a '?' or a '$' is encountered
        final int index1 = resourceLocation.indexOf('?');
        final int index2 = resourceLocation.indexOf('#');
        int endIndex = -1;
        if (index1 > 0) {
            if (index2 > 0) {
                endIndex = Math.min(index1, index2);
            } else {
                endIndex = index1;
            }
        } else if (index2 > 0) {
            endIndex = index2;
        }
        if (endIndex >= 0) {
            resourceLocation = resourceLocation.substring(0, endIndex);
        }
        final int index3 = resourceLocation.indexOf('$');
        if (index3 > 0) {
            resourceLocation = resourceLocation.substring(0, index3);
        }

        //check if the resourceLocation descriptor contains also remote repository information.
        ArtifactRepository repo = null;
        if (resourceLocation.startsWith("http://")) {
            final int repoDelimIntex = resourceLocation.indexOf('!');
            String repoUrl = resourceLocation.substring(0, repoDelimIntex);

            repo = new DefaultArtifactRepository(
                    repoUrl,
                    repoUrl,
                    new DefaultRepositoryLayout());
            org.apache.maven.repository.Proxy mavenProxy = configureProxyToInlineRepo();
            if (mavenProxy != null) {
                repo.setProxy(mavenProxy);
            }
            resourceLocation = resourceLocation.substring(repoDelimIntex + 1);

        }
        String[] parts = resourceLocation.split("/");
        String groupId = parts[0];
        String artifactId = parts[1];
        String version = null;
        String classifier = null;
        String type = "jar";
        if (parts.length > 2) {
            version = parts[2];
            if (parts.length > 3) {
                type = parts[3];
                if (parts.length > 4) {
                    classifier = parts[4];
                }
            }
        } else {
            Dependency dep = findDependency(project.getDependencies(), artifactId, groupId);
            if (dep == null && project.getDependencyManagement() != null) {
                dep = findDependency(project.getDependencyManagement().getDependencies(), artifactId, groupId);
            }
            if (dep != null) {
                version = dep.getVersion();
                classifier = dep.getClassifier();
                type = dep.getType();
            }
        }
        if (version == null || version.isEmpty()) {
            throw new MojoExecutionException("Cannot find version for: " + resourceLocation);
        }
        Artifact artifact = factory.createArtifactWithClassifier(groupId, artifactId, version, type, classifier);
        artifact.setRepository(repo);
        return artifact;
    }
    
    private org.apache.maven.repository.Proxy configureProxyToInlineRepo() {
        if (mavenSession != null && mavenSession.getSettings() != null) {
            Proxy proxy = mavenSession.getSettings().getActiveProxy();
            org.apache.maven.repository.Proxy mavenProxy = new org.apache.maven.repository.Proxy();
            if (proxy != null) {
                mavenProxy.setProtocol(proxy.getProtocol());
                mavenProxy.setHost(proxy.getHost());
                mavenProxy.setPort(proxy.getPort());
                mavenProxy.setNonProxyHosts(proxy.getNonProxyHosts());
                mavenProxy.setUserName(proxy.getUsername());
                mavenProxy.setPassword(proxy.getPassword());
                return mavenProxy;
            } else {
                return null;
            }
            
        } else {
            return null;
        }
    }

    protected void copy(File sourceFile, File destFile) {
        File targetDir = destFile.getParentFile();
        ensureDirExists(targetDir);

        try {
            try (
                FileInputStream is = new FileInputStream(sourceFile);
                FileOutputStream bos = new FileOutputStream(destFile)
            ) {
                StreamUtils.copy(is, bos);
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
    

    /**
     * Make sure the target directory exists and
     * that is actually a directory
     * @param targetDir
     * @throws IOException
     */
    private static void ensureDirExists(File targetDir) {
        if (!targetDir.exists()) {
            if (!targetDir.mkdirs()) {
                throw new RuntimeException("Unable to create target directory: " + targetDir);
            }
        } else if (!targetDir.isDirectory()) {
            throw new RuntimeException("Target is not a directory: " + targetDir);
        }
    }
}
