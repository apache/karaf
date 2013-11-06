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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.CollectResult;
import org.sonatype.aether.collection.DependencyCollectionContext;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.collection.DependencyGraphTransformer;
import org.sonatype.aether.collection.DependencySelector;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.aether.util.graph.selector.AndDependencySelector;
import org.sonatype.aether.util.graph.selector.ExclusionDependencySelector;
import org.sonatype.aether.util.graph.selector.OptionalDependencySelector;
import org.sonatype.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.sonatype.aether.util.graph.transformer.ConflictMarker;
import org.sonatype.aether.util.graph.transformer.JavaDependencyContextRefiner;
import org.sonatype.aether.util.graph.transformer.JavaEffectiveScopeCalculator;

import static org.apache.karaf.deployer.kar.KarArtifactInstaller.FEATURE_CLASSIFIER;

public class DependencyHelper {

    /**
      * The entry point to Aether, i.e. the component doing all the work.
      *
      * @component
      * @required
      * @readonly
      */
     private final RepositorySystem repoSystem;

     /**
      * The current repository/network configuration of Maven.
      *
      * @parameter default-value="${repositorySystemSession}"
      * @required
      * @readonly
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

    public DependencyHelper(List<RemoteRepository> projectRepos, RepositorySystemSession repoSession, RepositorySystem repoSystem) {
        this.projectRepos = projectRepos;
        this.repoSession = repoSession;
        this.repoSystem = repoSystem;
    }

    public Map<Artifact, String> getLocalDependencies() {
        return localDependencies;
    }

    public String getTreeListing() {
        return treeListing;
    }

    //artifact search code adapted from geronimo car plugin

    public void getDependencies(MavenProject project, boolean useTransitiveDependencies) throws MojoExecutionException {

        DependencyNode rootNode = getDependencyTree(RepositoryUtils.toArtifact(project.getArtifact()));

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
            DependencyGraphTransformer transformer = new ChainedDependencyGraphTransformer(new ConflictMarker(),
                    new JavaEffectiveScopeCalculator(),
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
            String scope = dependency.getPremanagedScope();
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


}
