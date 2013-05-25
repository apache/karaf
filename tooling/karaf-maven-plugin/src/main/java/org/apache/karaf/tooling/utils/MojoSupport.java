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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.tooling.features.Node;
import org.apache.karaf.tooling.features.ResolutionListenerImpl;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
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
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * Directory that resources are copied to during the build.
     *
     * @parameter expression="${project.build.directory}/${project.artifactId}-${project.version}-installer"
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
     * @parameter default-value="${project.remoteArtifactRepositories}"
     */
    protected List<ArtifactRepository> remoteRepos;

    /**
     * @component
     */
    protected ArtifactMetadataSource artifactMetadataSource;

    /**
     * @component
     */
    protected ArtifactResolver resolver;

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
     * @parameter expression="${session}"
     * @readonly
     * @required
     */
    protected MavenSession mavenSession;

    protected MavenProject getProject() {
        return project;
    }

    protected File getWorkDirectory() {
        return workDirectory;
    }

    public MavenProjectHelper getProjectHelper() {
        return projectHelper;
    }

    protected void removeBranch(ResolutionListenerImpl listener,
            Artifact artifact) {
        Node n = listener.getNode(artifact);
        if (n != null) {
            for (Iterator it = n.getParents().iterator(); it.hasNext();) {
                Node parent = (Node) it.next();
                parent.getChildren().remove(n);
            }
        }
    }

    protected void removeChildren(ResolutionListenerImpl listener,
            Artifact artifact) {
        Node n = listener.getNode(artifact);
        n.getChildren().clear();
    }

    protected Set getArtifacts(Node n, Set s) {
        if (!s.contains(n.getArtifact())) {
            s.add(n.getArtifact());
            for (Iterator iter = n.getChildren().iterator(); iter.hasNext();) {
                Node c = (Node) iter.next();
                getArtifacts(c, s);
            }
        }
        return s;
    }

    protected void excludeBranch(Node n, Set excludes) {
        excludes.add(n);
        for (Iterator iter = n.getChildren().iterator(); iter.hasNext();) {
            Node c = (Node) iter.next();
            excludeBranch(c, excludes);
        }
    }

    protected void print(Node rootNode) {
        for (Iterator iter = getArtifacts(rootNode, new HashSet()).iterator(); iter.hasNext();) {
            Artifact a = (Artifact) iter.next();
            getLog().info(" " + a);
        }
    }

    protected Set retainArtifacts(Set includes, ResolutionListenerImpl listener) {
        Set finalIncludes = new HashSet();
        Set filteredArtifacts = getArtifacts(listener.getRootNode(),
                new HashSet());
        for (Iterator iter = includes.iterator(); iter.hasNext();) {
            Artifact artifact = (Artifact) iter.next();
            for (Iterator iter2 = filteredArtifacts.iterator(); iter2.hasNext();) {
                Artifact filteredArtifact = (Artifact) iter2.next();
                if (filteredArtifact.getArtifactId().equals(
                        artifact.getArtifactId())
                        && filteredArtifact.getType()
                                .equals(artifact.getType())
                        && filteredArtifact.getGroupId().equals(
                                artifact.getGroupId())) {
                    if (!filteredArtifact.getVersion().equals(
                            artifact.getVersion())) {
                        getLog()
                                .warn(
                                        "Resolved artifact "
                                                + artifact
                                                + " has a different version from that in dependency management "
                                                + filteredArtifact
                                                + ", overriding dependency management");
                    }
                    finalIncludes.add(artifact);
                }
            }

        }

        return finalIncludes;
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

    /**
     * Set up a classloader for the execution of the main class.
     *
     * @return
     * @throws MojoExecutionException
     */
    protected URLClassLoader getClassLoader() throws MojoExecutionException {
        try {
            Set urls = new HashSet();

            URL mainClasses = new File(project.getBuild().getOutputDirectory())
                    .toURL();
            getLog().debug("Adding to classpath : " + mainClasses);
            urls.add(mainClasses);

            URL testClasses = new File(project.getBuild()
                    .getTestOutputDirectory()).toURL();
            getLog().debug("Adding to classpath : " + testClasses);
            urls.add(testClasses);

            Set dependencies = project.getArtifacts();
            Iterator iter = dependencies.iterator();
            while (iter.hasNext()) {
                Artifact classPathElement = (Artifact) iter.next();
                getLog().debug(
                        "Adding artifact: " + classPathElement.getFile()
                                + " to classpath");
                urls.add(classPathElement.getFile().toURL());
            }
            URLClassLoader appClassloader = new URLClassLoader((URL[]) urls
                    .toArray(new URL[urls.size()]), this.getClass().getClassLoader());
            return appClassloader;
        } catch (MalformedURLException e) {
            throw new MojoExecutionException(
                    "Error during setting up classpath", e);
        }
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
    private String extractProtocolFromLocalMavenRepo() {
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
}
