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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

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
public class GenerateFeaturesFileMojo extends MojoSupport {
    protected static final String SEPARATOR = "/";
    
    /**
     * The file to generate
     *
     * @parameter default-value="${project.build.directory}/classes/feature.xml"
     */
    private File outputFile;
    
    /**
     * The name of the feature, which defaults to the artifact ID if its not specified
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
    
    private Set<Artifact> features = new HashSet<Artifact>();
        
    private static final List<String> bundles = new ArrayList<String>();
    static {
        bundles.add("org.apache.camel");
    }
    

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
        }
        catch (Exception e) {
            throw new MojoExecutionException(
                    "Unable to create dependencies file: " + e, e);
        }
        finally {
            if (out != null) {
                try {
                    out.close();
                }
                catch (IOException e) {
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
            writeProjectDependencyFeatures(out);
        }
        out.println("</features>");
    }

    @SuppressWarnings("unchecked")
    private void writeProjectDependencyFeatures(PrintStream out) {
        for (Artifact artifact : (Set<Artifact>) project.getDependencyArtifacts()) {
            System.out.println("Adding feature " + artifact.getArtifactId() + " from " + artifact);
            out.println("  <feature name='" + artifact.getArtifactId() + "'>");
            writeBundle(out, artifact);
            features.add(artifact);
            out.println("  </feature>");
        }
        
    }

    private void writeBundle(PrintStream out, Artifact artifact) {
        if (features.contains(artifact)) {
            //if we already created a feature for this one, just add that instead of the bundle
            out.println(String.format("    <feature>%s</feature>", artifact.getArtifactId()));
            return;
        }
        //first write the dependencies
        for (Artifact dependency : getDependencies(artifact)) {
            writeBundle(out, dependency);
        }
        //and then write the bundle itself
        if (isBundle(artifact)) {
            writeBundle(out, artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion());
        } else {
            writeServicemixWrapperBundle(out, artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion());
        }
    }

    private boolean isBundle(Artifact artifact) {
        return artifact.getArtifactHandler().getPackaging().equals("bundle") || bundles.contains(artifact.getGroupId());
    }

    @SuppressWarnings("unchecked")
    private List<Artifact> getDependencies(Artifact artifact) {
        List<Artifact> list = new ArrayList<Artifact>();
        try {
            ResolutionGroup pom = artifactMetadataSource.retrieve(artifact, localRepo, remoteRepos);
            list.addAll(pom.getArtifacts());
        } catch (ArtifactMetadataRetrievalException e) {
            getLog().warn("Unable to retrieve metadata for " + artifact + ", not including dependencies for it");
        }
        return list;
    }

    private void writeServicemixWrapperBundle(PrintStream out, String groupId, String artifactId,
                                              String version) {
        writeBundle(out, 
                    "org.apache.servicemix.bundles", 
                    "org.apache.servicemix.bundles." + artifactId, 
                    version + project.getProperties().getProperty("servicemix.bundle.suffix"));
        
    }


    private void writeCurrentProjectFeature(PrintStream out) {
        out.println("  <feature name='" + featureName + "'>");


        writeBundle(out, project.getGroupId(), project.getArtifactId(), project.getVersion());
        out.println();

        Iterator iterator = project.getDependencies().iterator();
        while (iterator.hasNext()) {
            Dependency dependency = (Dependency) iterator.next();

            if (isValidDependency(dependency)) {
                out.print("  ");
                writeBundle(out, dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
            }
        }

        out.println("  </feature>");
    }

    protected boolean isValidDependency(Dependency dependency) {
        // TODO filter out only compile time dependencies which are OSGi bundles?
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