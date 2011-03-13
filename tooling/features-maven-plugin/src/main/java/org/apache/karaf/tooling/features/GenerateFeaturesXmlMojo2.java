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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import org.apache.karaf.deployer.kar.KarArtifactInstaller;
import org.apache.karaf.features.internal.model.Dependency;
import org.apache.karaf.features.internal.model.Feature;
import org.apache.karaf.features.internal.model.Bundle;
import org.apache.karaf.features.internal.model.Features;
import org.apache.karaf.features.internal.model.JaxbUtil;
import org.apache.karaf.features.internal.model.ObjectFactory;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeResolutionListener;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.xml.sax.SAXException;


/**
 * Generates the features XML file
 *
 * @version $Revision: 1.1 $
 * @goal generate-features-xml2
 * @phase compile
 * @requiresDependencyResolution runtime
 * @inheritByDefault true
 * @description Generates the features XML file
 */
@SuppressWarnings("unchecked")
public class GenerateFeaturesXmlMojo2 extends AbstractLogEnabled implements Mojo {

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

    /**
     * If false, feature dependencies are added to the assembled feature as dependencies.
     * If true, feature dependencies xml descriptors are read and their contents added to the features descriptor under assembly.
     *
     * @parameter default-value="${aggregateFeatures}"
     */
    private boolean aggregateFeatures = false;

    /**
     * If present, the bundles added to the feature constructed from the dependencies will be marked with this startlevel.
     *
     * @parameter
     */
    private Integer startLevel;

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


    //maven log
    private Log log;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            getDependencies(project, true);
            File dir = outputFile.getParentFile();
            if (dir.isDirectory() || dir.mkdirs()) {
                PrintStream out = new PrintStream(new FileOutputStream(outputFile));
                try {
                    writeFeatures(out);
                } finally {
                    out.close();
                }
                // now lets attach it
                projectHelper.attachArtifact(project, attachmentArtifactType, attachmentArtifactClassifier, outputFile);

            } else {
                throw new MojoExecutionException("Could not create directory for features file: " + dir);
            }
        } catch (Exception e) {
            getLogger().error(e.getMessage());
            throw new MojoExecutionException("Unable to create features.xml file: " + e, e);
        }
    }

    /*
     * Write all project dependencies as feature
     */
    private void writeFeatures(PrintStream out) throws ArtifactResolutionException, ArtifactNotFoundException,
            IOException, JAXBException, SAXException, ParserConfigurationException, XMLStreamException, MojoExecutionException {
        getLogger().info("Step 4 : Generating " + outputFile.getAbsolutePath());
        //read in an existing feature.xml
        ObjectFactory objectFactory = new ObjectFactory();
        Features features;
        if (inputFile.exists()) {
            features = readFeaturesFile(inputFile);
        } else {
            features = objectFactory.createFeaturesRoot();
        }
        if (features.getName() == null) {
            features.setName(project.getArtifactId());
        }

        Feature feature = null;
        for (Feature test : features.getFeature()) {
            if (test.getName().equals(project.getArtifactId())) {
                feature = test;
            }
        }
        if (feature == null) {
            feature = objectFactory.createFeature();
            feature.setName(project.getArtifactId());
        }
        feature.setVersion(project.getArtifact().getBaseVersion());
        feature.setResolver(resolver);
        for (Artifact artifact : localDependencies) {
            if (isFeature(artifact)) {
                if (aggregateFeatures && KarArtifactInstaller.FEATURE_CLASSIFIER.equals(artifact.getClassifier())) {
                    File featuresFile = artifact.getFile();
                    if (featuresFile == null || !featuresFile.exists()) {
                        throw new MojoExecutionException("Cannot locate file for feature: " + artifact + " at " + featuresFile);
                    }
                    Features includedFeatures = readFeaturesFile(featuresFile);
                    //TODO check for duplicates?
                    features.getFeature().addAll(includedFeatures.getFeature());
                } else {
                    Dependency dependency = objectFactory.createDependency();
                    dependency.setName(artifact.getArtifactId());
                    //TODO convert to osgi version?
                    dependency.setVersion(artifact.getVersion());
                    feature.getFeature().add(dependency);
                }
            } else {
                String bundleName;
                if (artifact.getType().equals("jar")) {
                    bundleName = String.format("mvn:%s/%s/%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion());
                } else {
                    bundleName = String.format("mvn:%s/%s/%s/%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion(), artifact.getType());
                }
                Bundle bundle = objectFactory.createBundle();
                bundle.setLocation(bundleName);
                if ("runtime".equals(artifact.getScope())) {
                    bundle.setDependency(true);
                }
                if (startLevel != null) {
                    bundle.setStartLevel(startLevel);
                }
                feature.getBundle().add(bundle);

            }
        }

        if ((!feature.getBundle().isEmpty() || !feature.getFeature().isEmpty()) && !features.getFeature().contains(feature)) {
            features.getFeature().add(feature);
        }

        JaxbUtil.marshal(Features.class, features, out);
        try {
            checkChanges(features, objectFactory);
        } catch (Exception e) {
            throw new MojoExecutionException("Features contents have changed", e);
        }
        getLogger().info("...done!");
    }

    private Features readFeaturesFile(File featuresFile) throws XMLStreamException, JAXBException, IOException {
        Features features;
        InputStream in = new FileInputStream(featuresFile);
        try {
            features = JaxbUtil.unmarshal(in, false);
        } finally {
            in.close();
        }
        return features;
    }


    //artifact search code adapted from geronimo car plugin

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
        } catch (ArtifactResolutionException exception) {
            throw new MojoExecutionException("Cannot build project dependency tree", exception);
        } catch (InvalidDependencyVersionException e) {
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

    private static boolean isFeature(Artifact artifact) {
        return artifact.getType().equals("kar") || KarArtifactInstaller.FEATURE_CLASSIFIER.equals(artifact.getClassifier());
    }

    //------------------------------------------------------------------------//
    // dependency change detection

    /**
     * Whether to look for changed dependencies at all
     *
     * @parameter
     */
    private boolean checkDependencyChange;

    /**
     * Whether to fail on changed dependencies
     *
     * @parameter
     */
    private boolean warnOnDependencyChange;

    /**
     * Whether to show changed dependencies in log
     *
     * @parameter
     */
    private boolean logDependencyChanges;

    /**
     * Whether to overwrite dependencies.xml if it has changed
     *
     * @parameter
     */
    private boolean overwriteChangedDependencies;

    /**
     * Location of existing dependency file.
     *
     * @parameter expression="${basedir}/src/main/history/dependencies.xml"
     * @required
     */
    private File dependencyFile;

    /**
     * Location of filtered dependency file.
     *
     * @parameter expression="${basedir}/target/history/dependencies.xml"
     * @required
     */
    private File filteredDependencyFile;

    //filtering support
    /**
     * The character encoding scheme to be applied when filtering resources.
     *
     * @parameter expression="${encoding}" default-value="${project.build.sourceEncoding}"
     */
    protected String encoding;

    /**
     * @component role="org.apache.maven.shared.filtering.MavenResourcesFiltering" role-hint="default"
     * @required
     */
    protected MavenResourcesFiltering mavenResourcesFiltering;

    /**
     * @parameter expression="${session}"
     * @readonly
     * @required
     */
    protected MavenSession session;

    /**
     * Expression preceded with the String won't be interpolated
     * \${foo} will be replaced with ${foo}
     *
     * @parameter expression="${maven.resources.escapeString}"
     * @since 2.3
     */
    protected String escapeString = "\\";

    /**
     * @plexus.requirement role-hint="default"
     * @component
     * @required
     */
    protected MavenFileFilter mavenFileFilter;
    /**
     * System properties.
     *
     * @parameter
     */
    protected Map<String, String> systemProperties;
    private Map<String, String> previousSystemProperties;

    private void checkChanges(Features newFeatures, ObjectFactory objectFactory) throws Exception, IOException, JAXBException, XMLStreamException {
        if (checkDependencyChange) {
            //combine all the dependencies to one feature and strip out versions
            Features features = objectFactory.createFeaturesRoot();
            features.setName(newFeatures.getName());
            Feature feature = objectFactory.createFeature();
            features.getFeature().add(feature);
            for (Feature f : newFeatures.getFeature()) {
                for (Bundle b : f.getBundle()) {
                    Bundle bundle = objectFactory.createBundle();
                    bundle.setLocation(b.getLocation());
                    feature.getBundle().add(bundle);
                }
                for (Dependency d : f.getFeature()) {
                    Dependency dependency = objectFactory.createDependency();
                    dependency.setName(d.getName());
                    feature.getFeature().add(dependency);
                }
            }

            Collections.sort(feature.getBundle(), new Comparator<Bundle>() {

                public int compare(Bundle bundle, Bundle bundle1) {
                    return bundle.getLocation().compareTo(bundle1.getLocation());
                }
            });
            Collections.sort(feature.getFeature(), new Comparator<Dependency>() {
                public int compare(Dependency dependency, Dependency dependency1) {
                    return dependency.getName().compareTo(dependency1.getName());
                }
            });

            if (dependencyFile.exists()) {
                //filter dependencies file
                filter(dependencyFile, filteredDependencyFile);
                //read dependency types, convert to dependencies, compare.
                Features oldfeatures = readFeaturesFile(filteredDependencyFile);
                Feature oldFeature = oldfeatures.getFeature().get(0);

                List<Bundle> addedBundles = new ArrayList<Bundle>(feature.getBundle());
                List<Bundle> removedBundles = new ArrayList<Bundle>();
                for (Bundle test : oldFeature.getBundle()) {
                    boolean t1 = addedBundles.contains(test);
                    int s1 = addedBundles.size();
                    boolean t2 = addedBundles.remove(test);
                    int s2 = addedBundles.size();
                    if (t1 != t2) {
                        getLogger().warn("dependencies.contains: " + t1 + ", dependencies.remove(test): " + t2);
                    }
                    if (t1 == (s1 == s2)) {
                        getLogger().warn("dependencies.contains: " + t1 + ", size before: " + s1 + ", size after: " + s2);
                    }
                    if (!t2) {
                        removedBundles.add(test);
                    }
                }

                List<Dependency> addedDependencys = new ArrayList<Dependency>(feature.getFeature());
                List<Dependency> removedDependencys = new ArrayList<Dependency>();
                for (Dependency test : oldFeature.getFeature()) {
                    boolean t1 = addedDependencys.contains(test);
                    int s1 = addedDependencys.size();
                    boolean t2 = addedDependencys.remove(test);
                    int s2 = addedDependencys.size();
                    if (t1 != t2) {
                        getLogger().warn("dependencies.contains: " + t1 + ", dependencies.remove(test): " + t2);
                    }
                    if (t1 == (s1 == s2)) {
                        getLogger().warn("dependencies.contains: " + t1 + ", size before: " + s1 + ", size after: " + s2);
                    }
                    if (!t2) {
                        removedDependencys.add(test);
                    }
                }
                if (!addedBundles.isEmpty() || !removedBundles.isEmpty() || !addedDependencys.isEmpty() || !removedDependencys.isEmpty()) {
                    saveDependencyChanges(addedBundles, removedBundles, addedDependencys, removedDependencys, objectFactory);
                    if (overwriteChangedDependencies) {
                        writeDependencies(features, dependencyFile);
                    }
                }

            } else {
                writeDependencies(features, dependencyFile);
            }

        }
    }

    protected void saveDependencyChanges(Collection<Bundle> addedBundles, Collection<Bundle> removedBundles, Collection<Dependency> addedDependencys, Collection<Dependency> removedDependencys, ObjectFactory objectFactory)
            throws Exception {
        File addedFile = new File(filteredDependencyFile.getParentFile(), "dependencies.added.xml");
        Features added = toFeatures(addedBundles, addedDependencys, objectFactory);
        writeDependencies(added,  addedFile);

        File removedFile = new File(filteredDependencyFile.getParentFile(), "dependencies.removed.xml");
        Features removed = toFeatures(removedBundles, removedDependencys, objectFactory);
        writeDependencies(removed,  removedFile);

        File treeListing = saveTreeListing();

        StringWriter out = new StringWriter();
        out.write("Dependencies have changed:\n");
        if (!addedBundles.isEmpty() || ! addedDependencys.isEmpty()) {
            out.write("\tAdded dependencies are saved here: " + addedFile.getAbsolutePath() + "\n");
            if (logDependencyChanges) {
                JaxbUtil.marshal(Features.class, added, out);
            }
        }
        if (!removedBundles.isEmpty() || !removedDependencys.isEmpty()) {
            out.write("\tRemoved dependencies are saved here: " + removedFile.getAbsolutePath() + "\n");
            if (logDependencyChanges) {
                JaxbUtil.marshal(Features.class, removed, out);
            }
        }
        out.write("\tTree listing is saved here: " + treeListing.getAbsolutePath() + "\n");
        out.write("Delete " + dependencyFile.getAbsolutePath()
                + " if you are happy with the dependency changes.");

        if (warnOnDependencyChange) {
            getLog().warn(out.toString());
        } else {
            throw new MojoFailureException(out.toString());
        }
    }

    private Features toFeatures(Collection<Bundle> addedBundles, Collection<Dependency> addedDependencys, ObjectFactory objectFactory) {
        Features features = objectFactory.createFeaturesRoot();
        Feature feature = objectFactory.createFeature();
        feature.getBundle().addAll(addedBundles);
        feature.getFeature().addAll(addedDependencys);
        features.getFeature().add(feature);
        return features;
    }


    private void writeDependencies(Features features, File file) throws JAXBException, IOException {
        file.getParentFile().mkdirs();
        if (!file.getParentFile().exists() || !file.getParentFile().isDirectory()) {
            throw new IOException("Cannot create directory at " + file.getParent());
        }
        FileOutputStream out = new FileOutputStream(file);
        try {
            JaxbUtil.marshal(Features.class, features, out);
        } finally {
            out.close();
        }
    }

    protected void filter(File sourceFile, File targetFile)
            throws MojoExecutionException {
        try {

            if (StringUtils.isEmpty(encoding)) {
                getLog().warn(
                        "File encoding has not been set, using platform encoding " + ReaderFactory.FILE_ENCODING
                                + ", i.e. build is platform dependent!");
            }
            targetFile.getParentFile().mkdirs();
            List filters = mavenFileFilter.getDefaultFilterWrappers(project, null, true, session, null);
            mavenFileFilter.copyFile(sourceFile, targetFile, true, filters, encoding, true);
        } catch (MavenFilteringException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }


    protected File saveTreeListing() throws IOException {
        File treeListFile = new File(filteredDependencyFile.getParentFile(), "treeListing.txt");
        OutputStream os = new FileOutputStream(treeListFile);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
        try {
            writer.write(treeListing);
        } finally {
            writer.close();
        }
        return treeListFile;
    }


}
