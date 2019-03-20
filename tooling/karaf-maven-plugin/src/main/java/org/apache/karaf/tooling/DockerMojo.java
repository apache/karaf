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

import org.apache.karaf.jpm.Process;
import org.apache.karaf.jpm.impl.ProcessBuilderFactoryImpl;
import org.apache.karaf.tooling.utils.MojoSupport;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

@Mojo(name = "docker", defaultPhase = LifecyclePhase.PACKAGE)
public class DockerMojo extends MojoSupport {

    @Parameter(defaultValue = "${project.build.directory}")
    private File location;

    @Parameter(defaultValue = "karaf")
    private String imageName;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Creating Docker image");
        try {
            Process process = new ProcessBuilderFactoryImpl().newBuilder()
                    .command("docker build -t " + imageName + " .")
                    .directory(location)
                    .start();

            getLog().info("Docker PID " + process.getPid() + " running");

            while (process.isRunning()) {
                Thread.sleep(100);
            }

            getLog().info("Docker image " + imageName + " created");
        } catch (Exception e) {
            throw new MojoExecutionException("Can't create Docker image: " + e.getMessage(), e);
        }
    }



}
