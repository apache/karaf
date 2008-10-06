/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.ipojo.plugin;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.felix.ipojo.manipulator.Pojoization;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

/**
 * Packages an OSGi jar "bundle" as an "iPOJO bundle".
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 * @version $Rev$, $Date$
 * @goal ipojo-bundle
 * @phase package
 * @requiresDependencyResolution runtime
 * @description manipulate an OSGi bundle jar to build an iPOJO bundle
 */
public class ManipulatorMojo extends AbstractMojo {

    /**
     * The directory for the generated JAR.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private String m_buildDirectory;

    /**
     * The directory containing generated classes.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    private File m_outputDirectory;

    /**
     * The name of the generated JAR file.
     *
     * @parameter alias="jarName" expression="${project.build.finalName}"
     * @required
     */
    private String m_jarName;

    /**
     * Location of the metadata file.
     * @parameter alias="metadata" default-value="metadata.xml"
     */
    private String m_metadata;

    /**
     * If set, the manipulated jar will be attached to the project as a separate artifact.
     *
     * @parameter alias="classifier" expression="${ipojo.classifier}"
     */
    private String m_classifier;

    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject m_project;

    /**
     * Used for attaching new artifacts.
     * @component
     * @required
     */
    private MavenProjectHelper m_helper;

    /**
     * Project types which this plugin supports.
     * @parameter
     */
    private List m_supportedProjectTypes = Arrays.asList(new String[]{"bundle"});

    /**
     * Ignore annotations parameter.
     * @parameter alias="ignoreAnnotations" default-value="false"
     */
    private boolean m_ignoreAnnotations;

    protected MavenProject getProject() {
        return this.m_project;
    }

    /**
     * Execute method : this method launches the pojoization.
     * @throws MojoExecutionException : an exception occurs during the manipulation.
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute() throws MojoExecutionException {
        // ignore project types not supported, useful when the plugin is configured in the parent pom
        if (!this.m_supportedProjectTypes.contains(this.getProject().getArtifact().getType())) {
            this.getLog().debug("Ignoring project " + this.getProject().getArtifact() + " : type " + this.getProject().getArtifact().getType() + " is not supported by iPOJO plugin, supported types are " + this.m_supportedProjectTypes);
            return;
        }

        getLog().info("Start bundle manipulation");
        // Get metadata file
        
        // Look for the metadata file in the output directory
        File meta = new File(m_outputDirectory + File.separator + m_metadata);
        
        // If not found look inside the pom directory
        if (! meta.exists()) {
            meta = new File(m_project.getBasedir() + File.separator + m_metadata);
        }
        
        getLog().info("Metadata file : " + meta.getAbsolutePath());
        if (!meta.exists()) {
            // Verify if annotations are ignored
            if (m_ignoreAnnotations) {
                getLog().info("No metadata file found - ignoring annotations");
                return;
            } else {
                getLog().info("No metadata file found - trying to use only annotations");
                meta = null;
            }
        }

        // Get input bundle
        File in = new File(m_buildDirectory + File.separator + m_jarName + ".jar");
        getLog().info("Input Bundle File : " + in.getAbsolutePath());
        if (!in.exists()) {
            throw new MojoExecutionException("the specified bundle file does not exist");
        }

        File out = new File(m_buildDirectory + File.separator + "_out.jar");

        Pojoization pojo = new Pojoization();
        if (!m_ignoreAnnotations) { pojo.setAnnotationProcessing(); }
        pojo.pojoization(in, out, meta);
        for (int i = 0; i < pojo.getWarnings().size(); i++) {
            getLog().warn((String) pojo.getWarnings().get(i));
        }
        if (pojo.getErrors().size() > 0) { throw new MojoExecutionException((String) pojo.getErrors().get(0)); }

        if (m_classifier != null) {
            // The user want to attach the resulting jar
            // Do not delete in File
            m_helper.attachArtifact(m_project, "jar", m_classifier, out);
        } else {
            // Usual behavior
            in.delete();
            out.renameTo(in);
        }
        getLog().info("Bundle manipulation - SUCCESS");
    }

}
