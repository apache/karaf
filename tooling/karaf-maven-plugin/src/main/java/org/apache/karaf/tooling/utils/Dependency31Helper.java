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
package org.apache.karaf.tooling.utils;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.*;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.transformer.*;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static java.lang.String.*;
import static org.apache.commons.lang3.reflect.MethodUtils.invokeMethod;
import static org.apache.karaf.deployer.kar.KarArtifactInstaller.FEATURE_CLASSIFIER;

/**
 * <p>{@link DependencyHelper} for accessing Eclipse Aether system used in Maven 3.1+. It uses reflection to access
 * these methods of {@code maven-core} APIs which directly references Eclipse Aether classes.</p>
 */
public class Dependency31Helper implements DependencyHelper {

    /**
     * The entry point to Aether, i.e. the component doing all the work.
     */
    private final RepositorySystem repositorySystem;

    /**
     * The current repository/network configuration of Maven.
     */
    private final RepositorySystemSession repositorySystemSession;

    /**
     * The project's remote repositories to use for the resolution of project dependencies.
     */
    private final List<RemoteRepository> projectRepositories;

    private final SimpleLRUCache<Artifact, ArtifactResult> artifactCache;

    // dependencies we are interested in
    protected Map<Artifact, LocalDependency> localDependencies;
    // log of what happened during search
    protected String treeListing;

    @SuppressWarnings("unchecked")
    public Dependency31Helper(List<?> repositories, Object session, RepositorySystem repositorySystem, int cacheSize) {
        this.projectRepositories = (List<RemoteRepository>) repositories;
        this.repositorySystemSession = (RepositorySystemSession) session;
        this.repositorySystem = repositorySystem;
        this.artifactCache = new SimpleLRUCache<>(cacheSize);
    }

    public Dependency31Helper(List<?> repositories, Object session, RepositorySystem repositorySystem) {
        this(repositories, session, repositorySystem, 32);
    }

    public void setRepositorySession(final ProjectBuildingRequest request) throws MojoExecutionException {
        try {
            invokeMethod(request, "setRepositorySession", repositorySystemSession);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new MojoExecutionException("Cannot set repository session on project building request", e);
        }
    }

    @Override
    public Collection<LocalDependency> getLocalDependencies() {
        return localDependencies.values();
    }

    @Override
    public String getTreeListing() {
        return treeListing;
    }

    @Override
    public void getDependencies(MavenProject project, boolean useTransitiveDependencies) throws MojoExecutionException {
        DependencyNode rootNode = getDependencyTree(toArtifact(project.getArtifact()));

        Scanner scanner = new Scanner();
        scanner.scan(rootNode, useTransitiveDependencies);
        localDependencies = scanner.localDependencies;
        treeListing = scanner.getLog();
    }

    private DependencyNode getDependencyTree(Artifact artifact) throws MojoExecutionException {
        try {
            CollectRequest collectRequest = new CollectRequest(new Dependency(artifact, "compile"), null, projectRepositories);
            DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(repositorySystemSession);
            session.setDependencySelector(new AndDependencySelector(new OptionalDependencySelector(),
                    new ScopeDependencySelector1(),
                    new ExclusionDependencySelector()));
            // between aether-util 0.9.0.M1 and M2, JavaEffectiveScopeCalculator was removed
            // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=397241
            DependencyGraphTransformer transformer = new ChainedDependencyGraphTransformer(new ConflictMarker(),
                    new ConflictResolver(new NearestVersionSelector(), new JavaScopeSelector(), new SimpleOptionalitySelector(), new JavaScopeDeriver()),
                    new JavaDependencyContextRefiner());
            session.setDependencyGraphTransformer(transformer);
            CollectResult result = repositorySystem.collectDependencies(session, collectRequest);
            return result.getRoot();
        } catch (DependencyCollectionException e) {
            throw new MojoExecutionException("Cannot build project dependency tree", e);
        }
    }

    /**
     * Aether's ScopeDependencySelector appears to always exclude the configured scopes (test and provided) and there is no way to configure it to
     * accept the top level provided scope dependencies. We need this 3 layers cake since Aether never actually uses the top level selector you give it,
     * it always starts by getting the child to apply to the project's dependencies.
     */
    private static class ScopeDependencySelector1 implements DependencySelector {

        private DependencySelector child = new ScopeDependencySelector2();

        public boolean selectDependency(Dependency dependency) {
            throw new IllegalStateException("This does not appear to be called");
        }

        public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
            return child;
        }

    }

    public static class ScopeDependencySelector2 implements DependencySelector {

        private DependencySelector child = new ScopeDependencySelector3();

        public boolean selectDependency(Dependency dependency) {
            String scope = dependency.getScope();
            return !"test".equals(scope) && !"runtime".equals(scope);
        }

        public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
            return child;
        }

    }

    private static class ScopeDependencySelector3 implements DependencySelector {

        public boolean selectDependency(Dependency dependency) {
            String scope = dependency.getScope();
            return !"test".equals(scope) && !"provided".equals(scope) && !"runtime".equals(scope);
        }

        public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
            return this;
        }

    }

    private static class Scanner {

        private enum Accept {
            ACCEPT(true, true),
            PROVIDED(true, false),
            STOP(false, false);

            private final boolean more;
            private final boolean local;

            Accept(boolean more, boolean local) {
                this.more = more;
                this.local = local;
            }

            public boolean isContinue() {
                return more;
            }

            public boolean isLocal() {
                return local;
            }
        }

        // all the dependencies needed, with provided dependencies removed
        private final Map<Artifact, LocalDependency> localDependencies = new LinkedHashMap<>();

        // dependencies from ancestor, to be removed from localDependencies
        private final Set<Artifact> dependencies = new LinkedHashSet<>();

        private final StringBuilder log = new StringBuilder();

        public void scan(DependencyNode rootNode, boolean useTransitiveDependencies) throws MojoExecutionException {
            for (DependencyNode child : rootNode.getChildren()) {
                scan(rootNode, child, Accept.ACCEPT, useTransitiveDependencies, false, 0);
            }
            if (useTransitiveDependencies) {
                localDependencies.keySet().removeAll(dependencies);
            }
        }

        private void scan(DependencyNode parentNode, DependencyNode dependencyNode, Accept parentAccept, boolean useTransitiveDependencies, boolean isFromFeature, int transitiveLevel) throws MojoExecutionException {
            String indent = StringUtils.repeat(" ", transitiveLevel);

            Accept accept = accept(dependencyNode, parentAccept);
            if (accept.isLocal()) {
                if (isFromFeature) {
                    if (!isFeature(dependencyNode)) {
                        log.append(indent).append("from feature:").append(dependencyNode).append("\n");
                        dependencies.add(dependencyNode.getDependency().getArtifact());
                    } else {
                        log.append(indent).append("is feature:").append(dependencyNode).append("\n");
                    }
                } else {
                    log.append(indent).append("local:").append(dependencyNode).append("\n");
                    if (localDependencies.keySet().contains(dependencyNode.getDependency().getArtifact())) {
                        log.append(indent).append("already in feature, returning:").append(dependencyNode).append("\n");
                        return;
                    }
                    // TODO resolve scope conflicts
                    localDependencies.put(dependencyNode.getDependency().getArtifact(),
                            new LocalDependency(dependencyNode.getDependency().getScope(), transitiveLevel > 0, dependencyNode.getDependency().getArtifact(), parentNode.getDependency().getArtifact()));
                    if (isFeature(dependencyNode) || !useTransitiveDependencies) {
                        isFromFeature = true;
                    }
                }
                if (useTransitiveDependencies && accept.isContinue()) {
                    List<DependencyNode> children = dependencyNode.getChildren();
                    for (DependencyNode child : children) {
                        scan(dependencyNode, child, accept, useTransitiveDependencies, isFromFeature, transitiveLevel + 1);
                    }
                }
            }
        }

        public String getLog() {
            return log.toString();
        }

        private Accept accept(DependencyNode dependency, Accept previous) {
            String scope = dependency.getDependency().getScope();
            if (scope == null || "runtime".equalsIgnoreCase(scope) || "compile".equalsIgnoreCase(scope)) {
                return previous;
            }
            if ("provided".equalsIgnoreCase(scope)) {
                return Accept.PROVIDED;
            }
            return Accept.STOP;
        }


    }

    public static boolean isFeature(DependencyNode dependencyNode) {
        return isFeature(dependencyNode.getDependency().getArtifact());
    }

    public static boolean isFeature(Artifact artifact) {
        return artifact.getExtension().equals("kar") || FEATURE_CLASSIFIER.equals(artifact.getClassifier());
    }

    @Override
    public boolean isArtifactAFeature(Object artifact) {
        return Dependency31Helper.isFeature((Artifact) artifact);
    }

	@Override
	public String getBaseVersion(Object artifact) {
		return ((Artifact) artifact).getBaseVersion();
	}

	@Override
	public String getGroupId(Object artifact) {
		return ((Artifact) artifact).getGroupId();
	}

    @Override
    public String getArtifactId(Object artifact) {
        return ((Artifact) artifact).getArtifactId();
    }

    @Override
    public String getClassifier(Object artifact) {
        return ((Artifact) artifact).getClassifier();
    }

    private ArtifactResult resolveArtifact(Artifact artifact) throws ArtifactResolutionException {
        ArtifactResult result = artifactCache.get(artifact);
        if (result != null) {
            return result;
        }

        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(artifact);
        request.setRepositories(projectRepositories);

        result = repositorySystem.resolveArtifact(repositorySystemSession, request);
        if (result != null) {
            artifactCache.put(artifact, result);
        }
        return result;
    }

    @Override
    public File resolve(Object artifact, Log log) {
        if (log.isDebugEnabled()) {
            log.debug("Resolving artifact " + artifact + " from " + projectRepositories);
        }

        ArtifactResult result;
        try {
            result = resolveArtifact((Artifact) artifact);
        } catch (ArtifactResolutionException e) {
            log.warn("Cound not resolve " + artifact, e);
            return null;
        }

        if (log.isDebugEnabled()) {
            log.debug("Resolved artifact " + artifact + " to " + result.getArtifact().getFile() + " from "
                    + result.getRepository());
        }

        return result.getArtifact().getFile();
    }

    @Override
    public File resolveById(String id, Log log) throws MojoFailureException {
        if (id.startsWith("mvn:")) {
            if (id.contains("!")) {
                id = id.substring(0, "mvn:".length()) + id.substring(id.indexOf("!") + 1);
            }
            if (id.endsWith("/")) {
                id = id.substring(0, id.length() - 1);
            }
        }
        id = MavenUtil.mvnToAether(id);

        if (log.isDebugEnabled()) {
            log.debug("Resolving artifact " + id + " from " + projectRepositories);
        }

        ArtifactResult result;
        try {
            result = resolveArtifact(MavenUtil.aetherToArtifact(id));
        } catch (ArtifactResolutionException e) {
            log.warn("Could not resolve " + id, e);
            throw new MojoFailureException(format("Couldn't resolve artifact %s", id), e);
        }

        if (log.isDebugEnabled()) {
            log.debug("Resolved artifact " + id + " to " + result.getArtifact().getFile() + " from "
                    + result.getRepository());
        }

        return result.getArtifact().getFile();
    }

    @Override
    public String artifactToMvn(org.apache.maven.artifact.Artifact artifact, String versionOrRange) throws MojoExecutionException {
        return this.artifactToMvn(toArtifact(artifact), versionOrRange);
    }

    @Override
    public String artifactToMvn(Object _artifact, String versionOrRange) {
        Artifact artifact = (Artifact) _artifact;
        String bundleName;
        if (artifact.getExtension().equals("jar") && MavenUtil.isEmpty(artifact.getClassifier())) {
            bundleName = String.format("mvn:%s/%s/%s", artifact.getGroupId(), artifact.getArtifactId(), versionOrRange);
        } else {
            if (MavenUtil.isEmpty(artifact.getClassifier())) {
                bundleName = String.format("mvn:%s/%s/%s/%s", artifact.getGroupId(), artifact.getArtifactId(), versionOrRange, artifact.getExtension());
            } else {
                bundleName = String.format("mvn:%s/%s/%s/%s/%s", artifact.getGroupId(), artifact.getArtifactId(), versionOrRange, artifact.getExtension(), artifact.getClassifier());
            }
        }
        return bundleName;
    }

    private static Artifact toArtifact(org.apache.maven.artifact.Artifact artifact) throws MojoExecutionException {
        try {
            Method toArtifact = RepositoryUtils.class.getMethod("toArtifact", org.apache.maven.artifact.Artifact.class);
            return (Artifact) toArtifact.invoke(null, artifact);
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private static org.apache.maven.artifact.Artifact toArtifact(Artifact artifact) throws MojoExecutionException {
        try {
            Method toArtifact = RepositoryUtils.class.getMethod("toArtifact", Artifact.class);
            return (org.apache.maven.artifact.Artifact) toArtifact.invoke(null, artifact);
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    @Override
    public org.apache.maven.artifact.Artifact mvnToArtifact(String name) throws MojoExecutionException {
        DefaultArtifact artifact = MavenUtil.mvnToArtifact(name);
        org.apache.maven.artifact.Artifact mavenArtifact = toArtifact(artifact);
        return mavenArtifact;
    }

    @Override
    public String pathFromMaven(String name) throws MojoExecutionException {
        if (name.indexOf(':') == -1) {
            return name;
        }
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }
        name = MavenUtil.mvnToAether(name);
        return pathFromAether(name);
    }

    @Override
    public String pathFromAether(String name) throws MojoExecutionException {
        DefaultArtifact artifact = MavenUtil.aetherToArtifact(name);
        org.apache.maven.artifact.Artifact mavenArtifact = toArtifact(artifact);
        return MavenUtil.layout.pathOf(mavenArtifact);
    }

}
