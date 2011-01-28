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
package org.apache.karaf.tooling.features;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import org.apache.karaf.features.internal.Bundle;
import org.apache.karaf.features.internal.Feature;
import org.apache.karaf.features.internal.Features;
import org.apache.karaf.features.internal.ObjectFactory;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeResolutionListener;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.xml.sax.SAXException;


/**
 * Generates the features XML file
 *
 * @version $Revision: 1.1 $
 * @goal generate-features-xml2
 * @phase compile
 * @execute phase="compile"
 * @requiresDependencyResolution runtime
 * @inheritByDefault true
 * @description Generates the features XML file
 */
@SuppressWarnings("unchecked")
public class GenerateFeaturesXmlMojo2 extends AbstractLogEnabled implements Mojo {

    /**
     * The dependency tree builder to use.
     *
     * @component
     * @required
     * @readonly
     */
//    private DependencyTreeBuilder dependencyTreeBuilder;

    /**
     * The (optional) input feature.file to extend
     *
     * @parameter default-value="${project.build.directory}/src/main/feature/feature.xml"
     */
    private File inputFile;

    /**
     * The file to generate
     *
     * @parameter default-value="${project.build.directory}/feature/feature.xml"
     */
    private File outputFile;

    /**
     * The resolver to use for the feature.  Normally null or "OBR" or "(OBR)"
     *
     * @parameter default-value="${resolver}"
     */
    private String resolver;

    /**
     * The artifact type for attaching the generated file to the project
     *
     * @parameter default-value="xml"
     */
    private String attachmentArtifactType = "xml";

    /**
     * The artifact classifier for attaching the generated file to the project
     *
     * @parameter default-value="features"
     */
    private String attachmentArtifactClassifier = "features";

    //new

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * The maven project's helper.
     *
     * @component
     * @required
     * @readonly
     */
    protected MavenProjectHelper projectHelper;

    //maven log
    private Log log;

    public void execute() throws MojoExecutionException, MojoFailureException {
        PrintStream out = null;
        try {
            File dir = outputFile.getParentFile();
            dir.mkdirs();
            out = new PrintStream(new FileOutputStream(outputFile));
            getDependencies(project, true);
            writeFeatures(out);
            // now lets attach it
            projectHelper.attachArtifact(project, attachmentArtifactType, attachmentArtifactClassifier, outputFile);
        } catch (Exception e) {
            getLogger().error(e.getMessage());
            throw new MojoExecutionException("Unable to create features.xml file: " + e, e);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /*
     * Write all project dependencies as feature
     */
    private void writeFeatures(PrintStream out) throws ArtifactResolutionException, ArtifactNotFoundException,
            IOException, JAXBException, SAXException, ParserConfigurationException, XMLStreamException {
        getLogger().info("Step 4 : Generating " + outputFile.getAbsolutePath());
        //read in an existing feature.xml
        ObjectFactory objectFactory = new ObjectFactory();
        Features featuresRoot;
        if (inputFile.exists()) {
            InputStream in = new FileInputStream(inputFile);
            try {
                featuresRoot = JaxbUtil.unmarshal(in, false);
            } finally {
                in.close();
            }
        } else {
            featuresRoot = objectFactory.createFeaturesRoot();
        }

        Feature feature = null;
        for (Feature test: featuresRoot.getFeature()) {
            if (test.getName().equals(project.getArtifactId())) {
                feature = test;
            }
        }
        if (feature == null) {
            feature = objectFactory.createFeature();
        }
        featuresRoot.getFeature().add(feature);
        feature.setName(project.getArtifactId());
        feature.setVersion(project.getArtifact().getBaseVersion());
        feature.setResolver(resolver);
        for (Artifact artifact : localDependencies) {
            String bundleName;
            if (artifact.getType().equals("jar")) {
                bundleName = String.format("mvn:%s/%s/%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion());
            } else {
                bundleName = String.format("mvn:%s/%s/%s/%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion(), artifact.getType());
            }
            Bundle bundle = objectFactory.createBundle();
            bundle.setValue(bundleName);
            if ("runtime".equals(artifact.getScope())) {
                bundle.setDependency(true);
            }
            feature.getBundle().add(bundle);
        }
        JaxbUtil.marshal(Features.class, featuresRoot, out);
        getLogger().info("...done!");
    }


    //artifact search code adapted from geronimo car plugin

    /**
     * The artifact factory to use.
     *
     * @component
     * @required
     * @readonly
     */
    protected ArtifactFactory artifactFactory;
    /**
     * The artifact repository to use.
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * The artifact metadata source to use.
     *
     * @component
     * @required
     * @readonly
     */
    private ArtifactMetadataSource artifactMetadataSource;

    /**
     * The artifact collector to use.
     *
     * @component
     * @required
     * @readonly
     */
    private ArtifactCollector artifactCollector;

    //all dependencies
    protected Set<Artifact> dependencyArtifacts;
    //dependencies we are interested in
    protected Set<Artifact> localDependencies;
    //log of what happened during search
    protected String treeListing;


    protected void getDependencies(MavenProject project, boolean useTransitiveDependencies) throws MojoExecutionException {

        DependencyTreeResolutionListener listener = new DependencyTreeResolutionListener(getLogger());

        DependencyNode rootNode;
        try {
            Map managedVersions = project.getManagedVersionMap();

            Set dependencyArtifacts = project.getDependencyArtifacts();

            if (dependencyArtifacts == null) {
                dependencyArtifacts = project.createArtifacts(artifactFactory, null, null);
            }
            ArtifactResolutionResult result = artifactCollector.collect(dependencyArtifacts, project.getArtifact(), managedVersions, localRepository,
                    project.getRemoteArtifactRepositories(), artifactMetadataSource, null,
                    Collections.singletonList(listener));

            this.dependencyArtifacts = result.getArtifacts();
            rootNode = listener.getRootNode();
        }
        catch (ArtifactResolutionException exception) {
            throw new MojoExecutionException("Cannot build project dependency tree", exception);
        }
        catch (InvalidDependencyVersionException e) {
            throw new MojoExecutionException("Invalid dependency version for artifact "
                    + project.getArtifact());
        }

        Scanner scanner = new Scanner();
        scanner.scan(rootNode, useTransitiveDependencies);
        localDependencies = scanner.localDependencies.keySet();
        treeListing = scanner.getLog();
    }

    public void setLog(Log log) {
        this.log = log;
    }

    public Log getLog() {
        if (log == null) {
            setLog(new SystemStreamLog());
        }
        return log;
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

        //all the dependencies needed for this car, with provided dependencies removed
        private final Map<Artifact, Set<Artifact>> localDependencies = new LinkedHashMap<Artifact, Set<Artifact>>();
        //dependencies from ancestor cars, to be removed from localDependencies.
        private final Set<Artifact> carDependencies = new LinkedHashSet<Artifact>();

        private final StringBuilder log = new StringBuilder();

        public void scan(DependencyNode rootNode, boolean useTransitiveDependencies) {
            Set<Artifact> children = new LinkedHashSet<Artifact>();
            for (DependencyNode child : (List<DependencyNode>) rootNode.getChildren()) {
                scan(child, Accept.ACCEPT, useTransitiveDependencies, false, "", children);
            }
            if (useTransitiveDependencies) {
                localDependencies.keySet().removeAll(carDependencies);
            }
        }

        private void scan(DependencyNode rootNode, Accept parentAccept, boolean useTransitiveDependencies, boolean isFromCar, String indent, Set<Artifact> parentsChildren) {
            Artifact artifact = getArtifact(rootNode);

            Accept accept = accept(artifact, parentAccept);
            if (accept.isContinue()) {
                Set<Artifact> children = localDependencies.get(artifact);
                if (isFromCar) {
                    if (!isFeature(artifact)) {
                        log.append(indent).append("from feature:").append(artifact).append("\n");
                        carDependencies.add(artifact);
                    } else {
                        log.append(indent).append("is feature:").append(artifact).append("\n");
                    }
                } else {
                    log.append(indent).append("local:").append(artifact).append("\n");
                    if (carDependencies.contains(artifact)) {
                        log.append(indent).append("already in feature, returning:").append(artifact).append("\n");
                        parentsChildren.add(artifact);
                        return;
                    }
                    parentsChildren.add(artifact);
                    if (children == null) {
                        children = new LinkedHashSet<Artifact>();
                        localDependencies.put(artifact, children);
                    }
                    if (isFeature(artifact) || !useTransitiveDependencies) {
                        isFromCar = true;
                    }
                }
                for (DependencyNode child : (List<DependencyNode>) rootNode.getChildren()) {
                    scan(child, accept, useTransitiveDependencies, isFromCar, indent + "  ", children);
                }
            }
        }

        private boolean isFeature(Artifact artifact) {
            return artifact.getType().equals("kar") || "feature".equals(artifact.getClassifier());
        }

        public String getLog() {
            return log.toString();
        }

        private Artifact getArtifact(DependencyNode rootNode) {
            Artifact artifact = rootNode.getArtifact();
            if (rootNode.getRelatedArtifact() != null) {
                artifact = rootNode.getRelatedArtifact();
            }
            return artifact;
        }

        private Accept accept(Artifact dependency, Accept previous) {
//            if (dependency.getGroupId().startsWith("org.apache.geronimo.genesis")) {
//                return Accept.STOP;
//            }
            String scope = dependency.getScope();
            if (scope == null || "runtime".equalsIgnoreCase(scope) || "compile".equalsIgnoreCase(scope)) {
                return previous;
            }
            return Accept.STOP;
        }

    }

}
