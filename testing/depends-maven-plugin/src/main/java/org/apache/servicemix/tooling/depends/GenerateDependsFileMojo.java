/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.tooling.depends;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.DefaultArtifactCollector;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * Generates the dependencies properties file
 *
 * @version $Id: $
 * @goal generate-depends-file
 * @phase generate-resources
 * @requiresDependencyResolution test
 * @description Generates the dependencies properties file
 */
public class GenerateDependsFileMojo extends AbstractMojo {

    protected static final String SEPARATOR = "/";

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     *
     * @parameter expression='true'
     * @required
     */
    protected boolean recursive;

    /**
     * The file to generate
     *
     * @parameter default-value="${project.build.directory}/classes/META-INF/maven/dependencies.properties"
     */
    
    private File outputFile;

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

    protected ArtifactCollector collector = new DefaultArtifactCollector();

    /**
     * @component
     */
    protected ArtifactFactory factory;

    public void execute() throws MojoExecutionException, MojoFailureException {
        OutputStream out = null;
        try {
            outputFile.getParentFile().mkdirs();
            out = new FileOutputStream(outputFile);
            PrintStream printer = new PrintStream(out);

            List<Dependency> dependencies;
            if (!recursive) {
                dependencies = project.getDependencies();
            } else {
                Set<Artifact> artifacts = project.getArtifacts();
                dependencies = new ArrayList<Dependency>();
                for (Artifact a : artifacts) {
                    Dependency dep = new Dependency();
                    dep.setGroupId(a.getGroupId());
                    dep.setArtifactId(a.getArtifactId());
                    dep.setVersion(a.getVersion());
                    dep.setClassifier(a.getClassifier());
                    dep.setType(a.getType());
                    dep.setScope(a.getScope());
                    dependencies.add(dep);
                }
            }
            Collections.sort(dependencies, new Comparator<Dependency>() {
                public int compare(Dependency o1, Dependency o2) {
                    int result = o1.getGroupId().compareTo( o2.getGroupId() );
                    if ( result == 0 ) {
                        result = o1.getArtifactId().compareTo( o2.getArtifactId() );
                        if ( result == 0 ) {
                            result = o1.getType().compareTo( o2.getType() );
                            if ( result == 0 ) {
                                if ( o1.getClassifier() == null ) {
                                    if ( o2.getClassifier() != null ) {
                                        result = 1;
                                    }
                                } else {
                                    if ( o2.getClassifier() != null ) {
                                        result = o1.getClassifier().compareTo( o2.getClassifier() );
                                    } else {
                                        result = -1;
                                    }
                                }
                                if ( result == 0 ) {
                                    // We don't consider the version range in the comparison, just the resolved version
                                    result = o1.getVersion().compareTo( o2.getVersion() );
                                }
                            }
                        }
                    }
                    return result;
                }
            });
            populateProperties(printer, dependencies);
            getLog().info("Created: " + outputFile);
        } catch (Exception e) {
            throw new MojoExecutionException(
                    "Unable to create dependencies file: " + e, e);
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

    protected void populateProperties(PrintStream out, List<Dependency> dependencies) {
        out.println("# Project dependencies generated by the Apache ServiceMix Maven Plugin");
        out.println("# Generated at: " + new Date());
        out.println();

        out.println("groupId = " + project.getGroupId());
        out.println("artifactId = " + project.getArtifactId());
        out.println("version = " + project.getVersion());
        out.println(project.getGroupId() + SEPARATOR + project.getArtifactId() + SEPARATOR + "version = " + project.getVersion());
        out.println();
        out.println("# dependencies");
        out.println();

        Iterator iterator = dependencies.iterator();

        while (iterator.hasNext()) {
            Dependency dependency = (Dependency) iterator.next();
            String prefix = dependency.getGroupId() + SEPARATOR + dependency.getArtifactId() + SEPARATOR;
            out.println(prefix + "version = " + dependency.getVersion());
            String classifier = dependency.getClassifier();
            if (classifier != null) {
                out.println(prefix + "classifier = " + classifier);
            }
            out.println(prefix + "type = " + dependency.getType());
            out.println(prefix + "scope = " + dependency.getScope());
            out.println();

            getLog().debug("Dependency: " + dependency + " classifier: " + classifier + " type: " + dependency.getType());
        }
    }

}