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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidArtifactRTException;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.DefaultArtifactCollector;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;
import org.osgi.impl.bundle.obr.resource.Manifest;
import org.osgi.impl.bundle.obr.resource.ManifestEntry;
import org.osgi.impl.bundle.obr.resource.VersionRange;

/**
 * Generates the features XML file
 * 
 * @version $Revision: 1.1 $
 * @goal generate-features-xml
 * @phase compile
 * @execute phase="compile"
 * @requiresDependencyResolution runtime
 * @inheritByDefault true
 * @description Generates the features XML file
 */
@SuppressWarnings("unchecked")
public class GenerateFeaturesXmlMojo extends MojoSupport {
    protected static final String SEPARATOR = "/";
    
    /**
     * The dependency tree builder to use.
     * 
     * @component
     * @required
     * @readonly
     */
    private DependencyTreeBuilder dependencyTreeBuilder;

    /**
     * The file to generate
     * 
     * @parameter default-value="${project.build.directory}/classes/feature.xml"
     */
    private File outputFile;

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
     * The kernel version for which to generate the bundle
     * 
     * @parameter
     */
    private String kernelVersion;
    
    /*
     * A list of packages exported by the kernel
     */
    private Map<String, VersionRange> kernelExports = new HashMap<String, VersionRange>();

    /**
     * A file containing the list of bundles
     * 
     * @parameter
     */
    private File bundles;

    /*
     * A set of known bundles
     */
    private Set<String> knownBundles = new HashSet<String>();
    
    /*
     * A list of exports by the bundles
     */
    private Map<String, Map<VersionRange, Artifact>> bundleExports = new HashMap<String, Map<VersionRange, Artifact>>();

    /*
     * The set of system exports
     */
    private List<String> systemExports = new LinkedList<String>();
    
    /*
     * These bundles are the features that will be built
     */
    private Map<Artifact, Feature> features = new HashMap<Artifact, Feature>();

    public void execute() throws MojoExecutionException, MojoFailureException {
        PrintStream out = null;
        try {
            out = new PrintStream(new FileOutputStream(outputFile));
            readSystemPackages();
            readKernelBundles();
            readBundles();
            discoverBundles();
            writeFeatures(out);
            // now lets attach it
            projectHelper.attachArtifact(project, attachmentArtifactType, attachmentArtifactClassifier, outputFile);
        } catch (Exception e) {
            getLog().error(e);
            throw new MojoExecutionException("Unable to create features.xml file: " + e, e);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
    
	/*
     * Read all the system provided packages from the <code>config.properties</code> file 
     */
    private void readSystemPackages() throws IOException {
        Properties properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
        readSystemPackages(properties, "jre-1.5");
        readSystemPackages(properties, "osgi");
    }

    
    private void readSystemPackages(Properties properties, String key) {
        String packages = (String) properties.get(key);
        for (String pkg : packages.split(";")) {
            systemExports.add(pkg.trim());
        }
    }

    /*
     * Download a Kernel distro and check the list of bundles provided by the Kernel
     */
    private void readKernelBundles() throws ArtifactResolutionException, ArtifactNotFoundException, MojoExecutionException,
        ZipException, IOException, DependencyTreeBuilderException {
        final Collection<Artifact> kernelArtifacts;
        if (kernelVersion == null) {
           getLog().info("Step 1: Building list of provided bundle exports");
           kernelArtifacts = new HashSet<Artifact>();
           DependencyNode tree = dependencyTreeBuilder.buildDependencyTree(project, localRepo, factory, artifactMetadataSource, new ArtifactFilter() {

            public boolean include(Artifact artifact) {
                return true;
            }
               
           }, new DefaultArtifactCollector());
           tree.accept(new DependencyNodeVisitor() {
                public boolean endVisit(DependencyNode node) {
                    // we want the next sibling too
                    return true;
                }
                public boolean visit(DependencyNode node) {
                    if (node.getState() != DependencyNode.OMITTED_FOR_CONFLICT) {
                        Artifact artifact = node.getArtifact();
                        if (Artifact.SCOPE_PROVIDED.equals(artifact.getScope()) && !artifact.getType().equals("pom")) {
                            kernelArtifacts.add(artifact);
                        }
                    }
                    // we want the children too
                    return true;
                }
            });
        } else {
            getLog().info("Step 1 : Building list of kernel exports");
            getLog().warn("Use of 'kernelVersion' is deprecated -- use a dependency with scope 'provided' instead");
            Artifact kernel = factory.createArtifact("org.apache.felix.karaf", "apache-felix-karaf", kernelVersion, Artifact.SCOPE_PROVIDED, "pom");
            resolver.resolve(kernel, remoteRepos, localRepo);
            kernelArtifacts = getDependencies(kernel);
        }
        for (Artifact artifact : kernelArtifacts) {
            registerKernelBundle(artifact);
        }
        getLog().info("...done!");
    }

    private void registerKernelBundle(Artifact artifact) throws ArtifactResolutionException, ArtifactNotFoundException, ZipException,
            IOException {
        Manifest manifest = getManifest(artifact);
        if (manifest.getExports() != null) {
            for (ManifestEntry entry : (List<ManifestEntry>)manifest.getExports()) {
                kernelExports.put(entry.getName(), entry.getVersion());
                getLog().debug(" adding kernel export " + entry.getName() + " (" + entry.getVersion() + ")");
            }
        }
        registerBundle(artifact);
    }

    /*
     * Read the list of bundles we can use to satisfy links
     */
    private void readBundles() throws IOException, ArtifactResolutionException, ArtifactNotFoundException {        
        BufferedReader reader = null;
        try {
            if (bundles != null) {
                getLog().info("Step 2 : Building a list of exports for bundles in " + bundles.getAbsolutePath());
                reader = new BufferedReader(new FileReader(bundles));
                String line = reader.readLine();
                while (line != null) {
                    if (line.contains("/") && !line.startsWith("#")) {
                        String[] elements = line.split("/");
                        Artifact artifact = factory.createArtifact(elements[0], elements[1], elements[2], Artifact.SCOPE_PROVIDED,
                                                                   elements[3]);
                        registerBundle(artifact);
                    }
                    line = reader.readLine();
                }                
            } else {
                getLog().info("Step 2 : No Bundle file supplied for building list of exports");
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        getLog().info("...done!");
    }
    
    /*
     * Auto-discover bundles currently in the dependencies
     */
    private void discoverBundles() throws ArtifactResolutionException, ArtifactNotFoundException, ZipException, IOException {
    	getLog().info("Step 3 : Discovering bundles in Maven dependencies");
		for (Artifact dependency : (Set<Artifact>) project.getArtifacts()) {
			// we will generate a feature for this afterwards
			if (project.getDependencyArtifacts().contains(dependency)) {
				continue;
			}
			// this is a provided bundle, has been handled in step 1
			if (dependency.getScope().equals(Artifact.SCOPE_PROVIDED)) {
			    continue;
			}
			if (isDiscoverableBundle(dependency)) {
				getLog().info("  Discovered " + dependency);
				registerBundle(dependency);
			}
		}
		getLog().info("...done!");
	}

    /*
     * Write all project dependencies as feature
     */
    private void writeFeatures(PrintStream out) throws ArtifactResolutionException, ArtifactNotFoundException,
        ZipException, IOException {
        getLog().info("Step 4 : Generating " + outputFile.getAbsolutePath());
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<features>");
        Set<Artifact> dependencies = (Set<Artifact>)project.getDependencyArtifacts();
        for (Artifact artifact : dependencies) {
            if (!artifact.getScope().equals(Artifact.SCOPE_PROVIDED) && !artifact.getType().equals("pom")) {
                getLog().info(" Generating feature " + artifact.getArtifactId() + " from " + artifact);
                Feature feature = getFeature(artifact);
                feature.write(out);
                registerFeature(artifact, feature);
            }
        }
        out.println("</features>");
        getLog().info("...done!");
    }

    /*
     * Get the feature for an artifact 
     */
    private Feature getFeature(Artifact artifact) throws ArtifactResolutionException, ArtifactNotFoundException, ZipException, IOException {
        Feature feature = new Feature(artifact);
        addRequirements(artifact, feature);
        return feature;
    }

    /*
     * Only auto-discover an OSGi bundle
     * - if it is not already known as a feature itself
     * - if it is not another version of an already known bundle
     */
    private boolean isDiscoverableBundle(Artifact artifact) {
        if (isBundle(artifact) && !isFeature(artifact) && !artifact.getScope().equals(Artifact.SCOPE_PROVIDED)) {
            for (String known : knownBundles) {
                String[] elements = known.split("/");
                if (artifact.getGroupId().equals(elements[0]) &&
                    artifact.getArtifactId().equals(elements[1])) {
                    getLog().debug(String.format("  Avoid auto-discovery for %s because of existing bundle %s", 
                                                 toString(artifact), known));
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /*
     * Check if the given artifact is a bundle
     */
    private boolean isBundle(Artifact artifact) {
        if (knownBundles.contains(toString(artifact)) || artifact.getArtifactHandler().getPackaging().equals("bundle")) {
            return true;
        } else {
            try {
                Manifest manifest = getManifest(artifact);
                if (manifest.getBsn() != null) {
                    getLog().debug(String.format("MANIFEST.MF for '%s' contains Bundle-Name '%s'",
                                                 artifact, manifest.getBsn().getName()));
                    return true;
                }
            } catch (ZipException e) {
                getLog().debug("Unable to determine if " + artifact + " is a bundle; defaulting to false", e);
            } catch (IOException e) {
                getLog().debug("Unable to determine if " + artifact + " is a bundle; defaulting to false", e);
            } catch (Exception e) {
                getLog().debug("Unable to determine if " + artifact + " is a bundle; defaulting to false", e);
            }
        }
        return false;
     }

    /*
     * Add requirements for an artifact to a feature
     */
    private void addRequirements(Artifact artifact, Feature feature) throws ArtifactResolutionException, ArtifactNotFoundException, ZipException, IOException {
        Manifest manifest = getManifest(artifact);
        Collection<ManifestEntry> remaining = getRemainingImports(manifest);
        Artifact previous = null;
        for (ManifestEntry entry : remaining) {
            Artifact add = null;
            Map<VersionRange, Artifact> versions = bundleExports.get(entry.getName());
            if (versions != null) {
                for (VersionRange range : versions.keySet()) {
                    add = versions.get(range);
                    if (range.compareTo(entry.getVersion()) == 0) {
                        add = versions.get(range);
                    }
                }
            }
            if (add == null) {
                if (isOptional(entry)) {
                    // debug logging for optional dependency...
                    getLog().debug(String.format("  Unable to find suitable bundle for optional dependency %s (%s)", 
                                                 entry.getName(), entry.getVersion()));
                } else {
                    // ...but a warning for a mandatory dependency
                    getLog().warn(
                                  String.format("  Unable to find suitable bundle for dependency %s (%s) (required by %s)", 
                                                entry.getName(), entry.getVersion(), artifact.getArtifactId()));
                }
            } else {
                if (!add.equals(previous) && feature.push(add) && !isFeature(add)) {
                    //and get requirements for the bundle we just added
                    getLog().debug("  Getting requirements for " + add);
                    addRequirements(add, feature);
                }
            }
            previous = add;
        }
    }

    /*
     * Check if a given bundle is itself being generated as a feature
     */
    private boolean isFeature(Artifact artifact) {
        return features.containsKey(artifact);
    }

    /*
     * Check a manifest entry and check if the resolution for the import has been marked as optional 
     */
    private boolean isOptional(ManifestEntry entry) {
        return entry.getAttributes() != null && entry.getAttributes().get("resolution:") != null
               && entry.getAttributes().get("resolution:").equals("optional");
    }

    /*
     * Register a bundle, enlisting all packages it provides
     */
    private void registerBundle(Artifact artifact) throws ArtifactResolutionException, ArtifactNotFoundException, ZipException,
        IOException {
        getLog().debug("Registering bundle " + artifact);
        knownBundles.add(toString(artifact));
        Manifest manifest = getManifest(artifact);
        for (ManifestEntry entry : getManifestEntries(manifest.getExports())) {
            Map<VersionRange, Artifact> versions = bundleExports.get(entry.getName());
            if (versions == null) {
                versions = new HashMap<VersionRange, Artifact>();
            }
            versions.put(entry.getVersion(), artifact);
            getLog().debug(String.format(" %s exported by bundle %s", entry.getName(), artifact));
            bundleExports.put(entry.getName(), versions);
        }
    }

    /*
     * Register a feature and also register the bundle for the feature
     */
    private void registerFeature(Artifact artifact, Feature feature) throws ArtifactResolutionException, ArtifactNotFoundException, ZipException,
        IOException {
        features.put(artifact, feature);
        registerBundle(artifact);
    }

    /*
     * Determine the list of imports to be resolved
     */
    private Collection<ManifestEntry> getRemainingImports(Manifest manifest) {
        // take all imports
        Collection<ManifestEntry> input = getManifestEntries(manifest.getImports());
        Collection<ManifestEntry> output = new LinkedList<ManifestEntry>(input);
        // remove imports satisfied by exports in the same bundle
        for (ManifestEntry entry : input) {
            for (ManifestEntry export : getManifestEntries(manifest.getExports())) {
                if (entry.getName().equals(export.getName())) {
                    output.remove(entry);
                }
            }
        }
        // remove imports for packages exported by the kernel
        for (ManifestEntry entry : input) {
            for (String export : kernelExports.keySet()) {
                if (entry.getName().equals(export)) {
                    output.remove(entry);
                }
            }
        }
        // remove imports for packages exported by the system bundle
        for (ManifestEntry entry : input) {
            if (systemExports.contains(entry.getName())) {
                output.remove(entry);
            }
        }
        return output;
    }

    private Collection<ManifestEntry> getManifestEntries(List imports) {
        if (imports == null) {
            return new LinkedList<ManifestEntry>();
        } else {
            return (Collection<ManifestEntry>)imports;
        }
    }

    private Manifest getManifest(Artifact artifact) throws ArtifactResolutionException, ArtifactNotFoundException, ZipException,
        IOException {
        File localFile = new File(localRepo.pathOf(artifact));
        ZipFile file;
        if (localFile.exists()) {
            //avoid going over to the repository if the file is already on the disk
            file = new ZipFile(localFile);
        } else {
            resolver.resolve(artifact, remoteRepos, localRepo);
            file = new ZipFile(artifact.getFile());
        }
        return new Manifest(file.getInputStream(file.getEntry("META-INF/MANIFEST.MF")));
    }

    private List<Artifact> getDependencies(Artifact artifact) {
        List<Artifact> list = new ArrayList<Artifact>();
        try {
            ResolutionGroup pom = artifactMetadataSource.retrieve(artifact, localRepo, remoteRepos);
            if (pom != null) {
                list.addAll(pom.getArtifacts());
            }
        } catch (ArtifactMetadataRetrievalException e) {
            getLog().warn("Unable to retrieve metadata for " + artifact + ", not including dependencies for it");
        } catch (InvalidArtifactRTException e) {
            getLog().warn("Unable to retrieve metadata for " + artifact + ", not including dependencies for it");
        }
        return list;
    }
    
    public static String toString(Artifact artifact) {
        return String.format("%s/%s/%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
    }

    private class Feature {

        private Stack<Artifact> artifacts = new Stack<Artifact>();
        private final Artifact artifact;
        
        private Feature(Artifact artifact) {
            super();
            this.artifact = artifact;
            artifacts.push(artifact);
        }

        public boolean push(Artifact item) {
            if (artifacts.contains(item)) {
                artifacts.remove(item);
                artifacts.push(item);
                return false;
            }
            if (!artifacts.contains(item)) {
                artifacts.push(item);
                return true;
            }
            return false;
        }

        public void write(PrintStream out) {
            out.println("  <feature name='" + artifact.getArtifactId() + "' version='"
            		+ artifact.getBaseVersion() + "'>");
            
            Stack<Artifact> resulting = new Stack<Artifact>();
            resulting.addAll(artifacts);

            // remove dependencies for included features
            for (Artifact next : artifacts) {
                if (isFeature(next)) {
                    resulting.removeAll(features.get(next).getDependencies());
                }
            }
            
            while (!resulting.isEmpty()) {
            	Artifact next = resulting.pop();
                if (isFeature(next)) {
                    out.println("    <feature version='"
            		+ next.getBaseVersion() + "'>" + String.format("%s</feature>", next.getArtifactId()));
                } else {
            		out.println(String.format("    <bundle>mvn:%s/%s/%s</bundle>", 
                            next.getGroupId(), next.getArtifactId(), next.getBaseVersion()));
                }
            }
            out.println("  </feature>");
        }
        
        public List<Artifact> getDependencies() {
            List<Artifact> dependencies = new LinkedList<Artifact>(artifacts);
            dependencies.remove(artifact);
            return dependencies;
        }
    }
}
