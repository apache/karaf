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
package org.apache.karaf.tooling;

import org.apache.karaf.tooling.utils.MojoSupport;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileWriter;

@Mojo(name = "dockerfile", defaultPhase = LifecyclePhase.PACKAGE)
public class DockerfileMojo extends MojoSupport {

    @Parameter(defaultValue = "${project.build.directory}")
    private File destDir;

    @Parameter(defaultValue = "${project.build.directory}/assembly")
    private File assembly;

    @Parameter(defaultValue = "[\"karaf\", \"run\"]")
    private String command;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Creating Dockerfile");
        File dockerFile = new File(destDir, "Dockerfile");
        try {
            StringBuilder buffer = new StringBuilder();
            buffer.append("FROM adoptopenjdk:11-jre-hotspot").append("\n");
            buffer.append("ENV KARAF_INSTALL_PATH /opt").append("\n");
            buffer.append("ENV KARAF_HOME $KARAF_INSTALL_PATH/apache-karaf").append("\n");
            buffer.append("ENV KARAF_EXEC exec").append("\n");
            buffer.append("ENV PATH $PATH:$KARAF_HOME/bin").append("\n");
            buffer.append("COPY ").append(assembly.getName()).append(" $KARAF_HOME").append("\n");
            buffer.append("EXPOSE 8101 1099 44444 8181").append("\n");
            buffer.append("CMD ").append(command).append("\n");
            try (FileWriter writer = new FileWriter(dockerFile)) {
                writer.write(buffer.toString());
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Can't create Dockerfile: " + e.getMessage(), e);
        }
    }

}
