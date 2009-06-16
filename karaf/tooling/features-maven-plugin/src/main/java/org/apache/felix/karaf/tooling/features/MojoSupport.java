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
package org.apache.felix.karaf.tooling.features;


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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectBuildingException;

/**
 * @version $Revision: 1.1 $
 */
public abstract class MojoSupport extends AbstractMojo {

    /**
     * Maven ProjectHelper
     *
     * @component
     */
    protected MavenProjectHelper projectHelper;

    /**
     * The maven project.
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
    protected List remoteRepos;

    /**
     * @component
     */
    protected ArtifactMetadataSource artifactMetadataSource;

    /**
     * @component
     */
    protected ArtifactResolver resolver;

    protected ArtifactCollector collector = new GraphArtifactCollector();

    /**
     * @component
     */
    protected ArtifactFactory factory;

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

    protected ResolutionListenerImpl resolveProject() {
        Map managedVersions = null;
        try {
            managedVersions = createManagedVersionMap(project.getId(), project
                    .getDependencyManagement());
        } catch (ProjectBuildingException e) {
            getLog().error(
                    "An error occurred while resolving project dependencies.",
                    e);
        }
        ResolutionListenerImpl listener = new ResolutionListenerImpl();
        listener.setLog(getLog());
        try {
            collector.collect(project.getDependencyArtifacts(), project
                    .getArtifact(), managedVersions, localRepo, remoteRepos,
                    artifactMetadataSource, null, Collections
                            .singletonList(listener));
        } catch (ArtifactResolutionException e) {
            getLog().error(
                    "An error occurred while resolving project dependencies.",
                    e);
        }
        if (getLog().isDebugEnabled()) {
            getLog().debug("Dependency graph");
            getLog().debug("================");
            print(listener.getRootNode());
            getLog().debug("================");
        }
        return listener;
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
}
