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
package org.apache.servicemix.tooling.features;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidArtifactRTException;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.osgi.impl.bundle.obr.resource.Manifest;

/**
 * Generates the features XML file
 * 
 * @version $Revision: 1.1 $
 * @goal generate-features-file
 * @phase compile
 * @execute phase="compile"
 * @requiresDependencyResolution runtime
 * @inheritByDefault true
 * @description Generates the features XML file
 */
@SuppressWarnings("unchecked")
public class GenerateFeaturesFileMojo extends MojoSupport {
    protected static final String SEPARATOR = "/";

    /**
     * The file to generate
     * 
     * @parameter default-value="${project.build.directory}/classes/feature.xml"
     */
    private File outputFile;

    /**
     * The name of the feature, which defaults to the artifact ID if its not
     * specified
     * 
     * @parameter default-value="${project.artifactId}"
     */
    private String featureName;

    /**
     * The artifact type for attaching the generated file to the project
     * 
     * @parameter default-value="features.xml"
     */
    private String attachmentArtifactType = "features.xml";

    /**
     * The artifact classifier for attaching the generated file to the project
     * 
     * @parameter
     */
    private String attachmentArtifactClassifier;

    /**
     * Should we generate a <feature> for the current project?
     * 
     * @parameter default-value="true"
     */
    private boolean includeProject = true;

    /**
     * Should we generate a <feature> for the current project's <dependency>s?
     * 
     * @parameter default-value="false"
     */
    private boolean includeDependencies = false;

    /*
     * These bundles are the features that will be built
     */
    private Set<Artifact> features = new HashSet<Artifact>();

    /*
     * These bundles are provided by SMX4 and will be excluded from <feature/>
     * generation
     */
    private Set<Artifact> provided = new HashSet<Artifact>();
    
    /*
     * List of bundles included in the current feature
     */
    private Set<Artifact> currentFeature = new HashSet<Artifact>();

    public void execute() throws MojoExecutionException, MojoFailureException {
        OutputStream out = null;
        try {
            outputFile.getParentFile().mkdirs();
            out = new FileOutputStream(outputFile);
            PrintStream printer = new PrintStream(out);
            populateProperties(printer);
            getLog().info("Created: " + outputFile);

            // now lets attach it
            projectHelper.attachArtifact(project, attachmentArtifactType, attachmentArtifactClassifier, outputFile);
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to create dependencies file: " + e, e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    getLog().info("Failed to close: " + outputFile + ". Reason: " + e, e);
                }
            }
        }
    }

    protected void populateProperties(PrintStream out) {
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<features>");
        if (includeProject) {
            writeCurrentProjectFeature(out);
        }
        if (includeDependencies) {
            prepare();
            writeProjectDependencyFeatures(out);
        }
        out.println("</features>");
    }

    private void prepare() {
        for (Artifact artifact : (Set<Artifact>)project.getDependencyArtifacts()) {
            if ("provided".equals(artifact.getScope())) {
                getLog().debug(String.format("Adding '%s' as provided bundle", artifact.toString()));
                provided.add(artifact);
            }
        }
    }

    private void writeProjectDependencyFeatures(PrintStream out) {
        Set<Artifact> dependencies = (Set<Artifact>)project.getDependencyArtifacts();
        dependencies.removeAll(provided);
        for (Artifact artifact : dependencies) {
            System.out.println("Adding feature " + artifact.getArtifactId() + " from " + artifact);
            out.println("  <feature name='" + artifact.getArtifactId() + "'>");
            currentFeature.clear();
            writeBundle(out, artifact);
            features.add(artifact);
            out.println("  </feature>");
        }

    }

    private void writeBundle(PrintStream out, Artifact artifact) {
        if (isProvided(artifact)) {
            getLog().debug(String.format("Skipping '%s' -- bundle will be provided at runtime", artifact));
            return;
        }
        if (features.contains(artifact)) {
            // if we already created a feature for this one, just add that instead of the bundle
            out.println(String.format("    <feature>%s</feature>", artifact.getArtifactId()));
            return;
        }
        // first write the dependencies
        for (Artifact dependency : getDependencies(artifact)) {
            if (dependency.isOptional() || "test".equals(dependency.getScope())) {
                // omit optional dependencies
                getLog().debug(String.format("Omitting optional and/or test scoped dependency '%s' for '%s'", 
                                             dependency, artifact));
                continue;
            }
            getLog().debug(String.format("Adding '%s' as a dependency for '%s'", dependency, artifact));
            writeBundle(out, dependency);
        }
        // skip the bundle if it was already added to this feature previously
        if (!currentFeature.add(artifact)) {
            getLog().debug(String.format("Artifact '%s' was already added to the current feature", artifact));
            return;
        }
        // and then write the bundle itself
        if (isBundle(artifact)) {
            writeBundle(out, artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion());
        } else {
            Artifact wrapper = findServicemixBundle(artifact);
            if (wrapper != null) {
                writeBundle(out, wrapper);
            } else {
                getLog().error(String.format("Unable to find suitable bundle for artifact '%s' -- resulting feature won't work", artifact));
            }
        }
    }

    private Artifact findServicemixBundle(Artifact artifact) {
        Artifact noVersionWrapper = factory.createArtifact("org.apache.servicemix.bundles", 
                                                  "org.apache.servicemix.bundles." + artifact.getArtifactId(), 
                                                  "", 
                                                  artifact.getScope(), artifact.getType());
        
        try {
            List versions = artifactMetadataSource.retrieveAvailableVersions(noVersionWrapper, localRepo, remoteRepos);
            Artifact wrapper = factory.createArtifact("org.apache.servicemix.bundles", 
                                                      "org.apache.servicemix.bundles." + artifact.getArtifactId(), 
                                                      getBestVersionForArtifact(artifact, versions), 
                                                      artifact.getScope(), artifact.getType());
            // let's check if the servicemix bundle for this artifact exists
            resolver.resolve(wrapper, remoteRepos, localRepo);
            for (Artifact dependency : getDependencies(wrapper)) {
                //some of these wrapper bundles provide for multiple JAR files, no need to include any of them after adding the wrapper
                getLog().debug(String.format("'%s' also provides '%s'", wrapper, dependency));
                currentFeature.add(dependency);
            }
            return wrapper;
        } catch (ArtifactResolutionException e) {
            getLog().debug("Couldn't find a ServiceMix bundle for " + artifact, e);
        } catch (ArtifactNotFoundException e) {
            getLog().debug("Couldn't find a ServiceMix bundle for " + artifact, e);
        } catch (ArtifactMetadataRetrievalException e) {
            getLog().debug("Couldn't find a ServiceMix bundle for " + artifact, e);
        }
        if (artifact.getArtifactId().contains("-")) {
            //let's try to see if we can't find a bundle wrapping multiple artifacts (e.g. mina -> mina-core, mina-codec, ...)
            return findServicemixBundle(factory.createArtifact(artifact.getGroupId(), artifact.getArtifactId().split("-")[0], 
                                                               artifact.getVersion(), artifact.getScope(), artifact.getType()));
        } else {
            return null;
        }
    }

    protected String getBestVersionForArtifact(Artifact artifact, List<ArtifactVersion> versions) throws ArtifactMetadataRetrievalException {
        if (versions.size() == 0) {
            throw new ArtifactMetadataRetrievalException("No wrapper bundle available for " + artifact);
        }
        Collections.sort(versions, Collections.reverseOrder());
        //check for same version
        for (ArtifactVersion version : versions) {
            if (version.toString().startsWith(artifact.getVersion())) {
                return version.toString();
            }
        }
        //check for same major/minor version
        for (ArtifactVersion version : versions) {
            String[] elements = version.toString().split("\\.");
            if (elements.length >= 2 && artifact.getVersion().startsWith(elements[0] + "." + elements[1])) {
                return version.toString();
            }
        }
        throw new ArtifactMetadataRetrievalException("No suitable version found for " + artifact + " wrapper bundle");
    }

    private boolean isProvided(Artifact bundle) {
        for (Artifact artifact : provided) {
            if (bundle.getArtifactId().equals(artifact.getArtifactId())
                && bundle.getGroupId().equals(artifact.getGroupId())) {
                return true;
            }
        }
        return false;
    }

    private boolean isBundle(Artifact artifact) {
        if (artifact.getArtifactHandler().getPackaging().equals("bundle")) {
            return true;
        } else {
            try {
                resolver.resolve(artifact, remoteRepos, localRepo);
                ZipFile file = new ZipFile(artifact.getFile());
                ZipEntry entry = file.getEntry("META-INF/MANIFEST.MF");
                Manifest manifest = new Manifest(file.getInputStream(entry));
                if (manifest.getBsn() != null) {
                    getLog().debug(String.format("MANIFEST.MF for '%s' contains Bundle-Name '%s'",
                                                 artifact, manifest.getBsn().getName()));
                    return true;
                }
            } catch (ZipException e) {
                getLog().warn("Unable to determine if " + artifact + " is a bundle; defaulting to false", e);
            } catch (IOException e) {
                getLog().warn("Unable to determine if " + artifact + " is a bundle; defaulting to false", e);
            } catch (Exception e) {
                getLog().warn("Unable to determine if " + artifact + " is a bundle; defaulting to false", e);
            }
        }
        return false;
    }

    private List<Artifact> getDependencies(Artifact artifact) {
        List<Artifact> list = new ArrayList<Artifact>();
        try {
            ResolutionGroup pom = artifactMetadataSource.retrieve(artifact, localRepo, remoteRepos);
            list.addAll(pom.getArtifacts());
        } catch (ArtifactMetadataRetrievalException e) {
            getLog().warn("Unable to retrieve metadata for " + artifact + ", not including dependencies for it");
        } catch (InvalidArtifactRTException e) {
            getLog().warn("Unable to retrieve metadata for " + artifact + ", not including dependencies for it");
        }
        return list;
    }


    private void writeCurrentProjectFeature(PrintStream out) {
        out.println("  <feature name='" + featureName + "'>");

        writeBundle(out, project.getGroupId(), project.getArtifactId(), project.getVersion());
        out.println();

        Iterator iterator = project.getDependencies().iterator();
        while (iterator.hasNext()) {
            Dependency dependency = (Dependency)iterator.next();

            if (isValidDependency(dependency)) {
                out.print("  ");
                writeBundle(out, dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
            }
        }

        out.println("  </feature>");
    }

    protected boolean isValidDependency(Dependency dependency) {
        // TODO filter out only compile time dependencies which are OSGi
        // bundles?
        return true;
    }

    protected void writeBundle(PrintStream out, String groupId, String artifactId, String version) {
        out.print("    <bundle>mvn:");
        out.print(groupId);
        out.print("/");
        out.print(artifactId);
        out.print("/");
        out.print(version);
        out.print("</bundle>");
        out.println();
    }
}
