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
import java.util.Iterator;

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

    public void execute() throws MojoExecutionException, MojoFailureException {
        OutputStream out = null;
        try {
            outputFile.getParentFile().mkdirs();
            out = new FileOutputStream(outputFile);
            PrintStream printer = new PrintStream(out);
            populateProperties(printer);
            getLog().info("Created: " + outputFile);

            // now lets attach it
            projectHelper.attachArtifact(project, "features.xml", null, outputFile);
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
        out.println("</features>");
    }

    protected boolean isValidDependency(Dependency dependency) {
        // TODO filter out only compile time dependencies which are OSGi bundles?
        return true;
    }

    protected void writeBundle(PrintStream out, String groupId, String artifactId, String version) {
        out.print("  <bundle>mvn:");
        out.print(groupId);
        out.print("/");
        out.print(artifactId);
        out.print("/");
        out.print(version);
        out.print("</bundle>");
        out.println();
    }
}