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
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.eclipse.aether.util.graph.transformer.ConflictMarker;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.transformer.JavaDependencyContextRefiner;
import org.eclipse.aether.util.graph.transformer.JavaScopeDeriver;
import org.eclipse.aether.util.graph.transformer.JavaScopeSelector;
import org.eclipse.aether.util.graph.transformer.NearestVersionSelector;
import org.eclipse.aether.util.graph.transformer.SimpleOptionalitySelector;

import static java.lang.String.*;
import static org.apache.karaf.deployer.kar.KarArtifactInstaller.FEATURE_CLASSIFIER;

/**
 * <p>{@link DependencyHelper} for accessing Aether system when used with Maven 3.1.0+. It uses reflection to access
 * these methods of {@code maven-core} APIs which directly references Aether classes.</p>
 * 
 * <p>When {@code karaf-maven-plugin} switches to {@code maven-core:3.1.0+}, reflection should be use for Sonatype
 * variant of Aether in {@link Dependency30Helper} and this class will use Maven API directly..</p>
 *
 * @author Grzegorz Grzybek
 */
public class Dependency31Helper implements DependencyHelper {

    /**
      * The entry point to Aether, i.e. the component doing all the work.
      */
     private final RepositorySystem repoSystem;

     /**
      * The current repository/network configuration of Maven.
      */
     private final RepositorySystemSession repoSession;

     /**
      * The project's remote repositories to use for the resolution of project dependencies.
      *
      * @parameter default-value="${project.remoteProjectRepositories}"
      * @readonly
      */
     private final List<RemoteRepository> projectRepos;

    //dependencies we are interested in
    protected Map<Artifact, String> localDependencies;
    //log of what happened during search
    protected String treeListing;

    @SuppressWarnings("unchecked")
    public Dependency31Helper(List<?> repositories, Object session, RepositorySystem repoSystem) {
        this.projectRepos = (List<RemoteRepository>)repositories;
        this.repoSession = (RepositorySystemSession)session;
        this.repoSystem = repoSystem;
    }

    @Override
    public Map<?, String> getLocalDependencies() {
        return localDependencies;
    }

    @Override
    public String getTreeListing() {
        return treeListing;
    }

    //artifact search code adapted from geronimo car plugin

    @Override
    public void getDependencies(MavenProject project, boolean useTransitiveDependencies) throws MojoExecutionException {

        DependencyNode rootNode = getDependencyTree(toArtifact(project.getArtifact()));;

        Scanner scanner = new Scanner();
        scanner.scan(rootNode, useTransitiveDependencies);
        localDependencies = scanner.localDependencies;
        treeListing = scanner.getLog();
    }

    private DependencyNode getDependencyTree(Artifact artifact) throws MojoExecutionException {
        try {
            CollectRequest collectRequest = new CollectRequest(new Dependency(artifact, "compile"), null, projectRepos);
            DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(repoSession);
            session.setDependencySelector(new AndDependencySelector(new OptionalDependencySelector(),
                    new ScopeDependencySelector1(),
                    new ExclusionDependencySelector()));
            // between aether-util-0.9.0.M1 and M2 JavaEffectiveScopeCalculator was removed
            // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=397241
            DependencyGraphTransformer transformer = new ChainedDependencyGraphTransformer(new ConflictMarker(),
                    new ConflictResolver(new NearestVersionSelector(), new JavaScopeSelector(), new SimpleOptionalitySelector(), new JavaScopeDeriver()),
                    new JavaDependencyContextRefiner());
            session.setDependencyGraphTransformer(transformer);
            CollectResult result = repoSystem.collectDependencies(session, collectRequest);
            return result.getRoot();
        } catch (DependencyCollectionException e) {
            throw new MojoExecutionException("Cannot build project dependency tree", e);
        }
    }

    //aether's ScopeDependencySelector appears to always exclude the configured scopes (test and provided) and there is no way to configure it to
    //accept the top level provided scope dependencies.  We need this 3 layer cake since aether never actually uses the top level selector you give it,
    //it always starts by getting the child to apply to the project's dependencies.
    private static class ScopeDependencySelector1 implements DependencySelector {

        private DependencySelector child = new ScopeDependencySelector2();

        public boolean selectDependency(Dependency dependency) {
            throw new IllegalStateException("this does not appear to be called");
        }

        public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
            return child;
        }
    }

    private static class ScopeDependencySelector2 implements DependencySelector {

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

        private static enum Accept {
            ACCEPT(true, true),
            PROVIDED(true, false),
            STOP(false, false);

            private final boolean more;
            private final boolean local;

            private Accept(boolean more, boolean local) {
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

        //all the dependencies needed for this car, with provided dependencies removed. artifact to scope map
        private final Map<Artifact, String> localDependencies = new LinkedHashMap<Artifact, String>();
        //dependencies from ancestor cars, to be removed from localDependencies.
        private final Set<Artifact> carDependencies = new LinkedHashSet<Artifact>();

        private final StringBuilder log = new StringBuilder();

        public void scan(DependencyNode rootNode, boolean useTransitiveDependencies) throws MojoExecutionException {
            for (DependencyNode child : rootNode.getChildren()) {
                scan(child, Accept.ACCEPT, useTransitiveDependencies, false, "");
            }
            if (useTransitiveDependencies) {
                localDependencies.keySet().removeAll(carDependencies);
            }
        }

        private void scan(DependencyNode dependencyNode, Accept parentAccept, boolean useTransitiveDependencies, boolean isFromFeature, String indent) throws MojoExecutionException {
//            Artifact artifact = getArtifact(rootNode);

            Accept accept = accept(dependencyNode, parentAccept);
            if (accept.isLocal()) {
                if (isFromFeature) {
                    if (!isFeature(dependencyNode)) {
                        log.append(indent).append("from feature:").append(dependencyNode).append("\n");
                        carDependencies.add(dependencyNode.getDependency().getArtifact());
                    } else {
                        log.append(indent).append("is feature:").append(dependencyNode).append("\n");
                    }
                } else {
                    log.append(indent).append("local:").append(dependencyNode).append("\n");
                    if (localDependencies.containsKey(dependencyNode.getDependency().getArtifact())) {
                        log.append(indent).append("already in feature, returning:").append(dependencyNode).append("\n");
                        return;
                    }
                    //TODO resolve scope conflicts
                    localDependencies.put(dependencyNode.getDependency().getArtifact(), dependencyNode.getDependency().getScope());
                    if (isFeature(dependencyNode) || !useTransitiveDependencies) {
                        isFromFeature = true;
                    }
                }
                if (useTransitiveDependencies && accept.isContinue()) {
                    List<DependencyNode> children = dependencyNode.getChildren();
                    for (DependencyNode child : children) {
                        scan(child, accept, useTransitiveDependencies, isFromFeature, indent + "  ");
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
        return Dependency31Helper.isFeature((Artifact)artifact);
    }

    @Override
    public String getArtifactId(Object artifact) {
        return ((Artifact)artifact).getArtifactId();
    }

    @Override
    public String getClassifier(Object artifact) {
        return ((Artifact)artifact).getClassifier();
    }

    @Override
    public File resolve(Object artifact, Log log) {
        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact((Artifact)artifact);
        request.setRepositories(projectRepos);

        log.debug("Resolving artifact " + artifact +
                " from " + projectRepos);

        ArtifactResult result;
        try {
            result = repoSystem.resolveArtifact(repoSession, request);
        } catch (org.eclipse.aether.resolution.ArtifactResolutionException e) {
            log.warn("could not resolve " + artifact, e);
            return null;
        }

        log.debug("Resolved artifact " + artifact + " to " +
                result.getArtifact().getFile() + " from "
                + result.getRepository());
        return result.getArtifact().getFile();
    }

    @Override
    public File resolveById(String id, Log log) throws MojoFailureException {
        id = MavenUtil.mvnToAether(id);
        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(new DefaultArtifact(id));
        request.setRepositories((List<RemoteRepository>)projectRepos);

        log.debug("Resolving artifact " + id +
                " from " + projectRepos);

        ArtifactResult result;
        try {
            result = repoSystem.resolveArtifact(repoSession, request);
        } catch (ArtifactResolutionException e) {
            log.warn("could not resolve " + id, e);
            throw new MojoFailureException(format("Couldn't resolve artifact %s", id), e);
        }

        log.debug("Resolved artifact " + id + " to " +
                result.getArtifact().getFile() + " from "
                + result.getRepository());
        return result.getArtifact().getFile();
    }

    @Override
    public String artifactToMvn(org.apache.maven.artifact.Artifact artifact) throws MojoExecutionException {
        return this.artifactToMvn(toArtifact(artifact));
    }

    @Override
    public String artifactToMvn(Object _artifact) {
        Artifact artifact = (Artifact)_artifact;
        String bundleName;
        if (artifact.getExtension().equals("jar") && MavenUtil.isEmpty(artifact.getClassifier())) {
            bundleName = String.format("mvn:%s/%s/%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion());
        } else {
            if (MavenUtil.isEmpty(artifact.getClassifier())) {
                bundleName = String.format("mvn:%s/%s/%s/%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion(), artifact.getExtension());
            } else {
                bundleName = String.format("mvn:%s/%s/%s/%s/%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion(), artifact.getExtension(), artifact.getClassifier());
            }
        }
        return bundleName;
    }

    private static Artifact toArtifact(org.apache.maven.artifact.Artifact artifact) throws MojoExecutionException {
        try {
            Method toArtifact = RepositoryUtils.class.getMethod("toArtifact", org.apache.maven.artifact.Artifact.class);
            return (Artifact)toArtifact.invoke(null, artifact);
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
    
    private static org.apache.maven.artifact.Artifact toArtifact(Artifact artifact) throws MojoExecutionException {
        try {
            Method toArtifact = RepositoryUtils.class.getMethod("toArtifact", Artifact.class);
            return (org.apache.maven.artifact.Artifact)toArtifact.invoke(null, artifact);
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    @Override
    public org.apache.maven.artifact.Artifact mvnToArtifact(String name) throws MojoExecutionException {
        name = MavenUtil.mvnToAether(name);
        DefaultArtifact artifact = new DefaultArtifact(name);
        org.apache.maven.artifact.Artifact mavenArtifact = toArtifact(artifact);
        return mavenArtifact;
    }

    @Override
    public String pathFromMaven(String name) throws MojoExecutionException {
        if (name.indexOf(':') == -1) {
            return name;
        }
        name = MavenUtil.mvnToAether(name);
        return pathFromAether(name);
    }

    @Override
    public String pathFromAether(String name) throws MojoExecutionException {
        DefaultArtifact artifact = new DefaultArtifact(name);
        org.apache.maven.artifact.Artifact mavenArtifact = toArtifact(artifact);
        return MavenUtil.layout.pathOf(mavenArtifact);
    }

}
