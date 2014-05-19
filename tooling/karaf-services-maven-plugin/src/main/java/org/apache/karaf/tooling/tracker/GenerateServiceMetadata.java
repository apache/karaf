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
package org.apache.karaf.tooling.tracker;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.util.tracker.ProvideService;
import org.apache.karaf.util.tracker.RequireService;
import org.apache.karaf.util.tracker.Services;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.xbean.finder.ClassFinder;

/**
 * Generates service requirement and capabilities for activators
 *
 * @goal service-metadata-generate
 * @phase process-classes
 * @execute phase="generate-resources"
 * @requiresDependencyResolution compile+runtime
 * @inheritByDefault false
 * @description Generates service requirement and capabilities for activators
 */
public class GenerateServiceMetadata extends AbstractMojo {

    /**
     * The maven project.
     *
     * @parameter default-value="${project}"
     * @required
     */
    protected MavenProject project;

    /**
     * @parameter default-value="requirements"
     * @required
     */
    protected String requirementsProperty;

    /**
     * @parameter default-value="capabilities"
     * @required
     */
    protected String capabilitiesProperty;

    /**
     * The classloader to use for loading the commands.
     * Can be "project" or "plugin"
     *
     * @parameter default-value="project"
     */
    protected String classLoader;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            StringBuilder requirements = new StringBuilder();
            StringBuilder capabilities = new StringBuilder();

            ClassFinder finder = createFinder(classLoader);
            List<Class<?>> classes = finder.findAnnotatedClasses(Services.class);

            for (Class<?> clazz : classes) {
                URL classUrl = clazz.getClassLoader().getResource(clazz.getName().replace('.', '/') + ".class");
                if (classUrl == null || !classUrl.getPath().startsWith(project.getBuild().getOutputDirectory())) {
                    System.out.println("Ignoring " + classUrl);
                    continue;
                }
                Services services = clazz.getAnnotation(Services.class);
                if (services != null) {
                    for (RequireService req : services.requires()) {
                        String flt = req.filter();
                        if (flt != null && !flt.isEmpty()) {
                            flt = "(&(objectClass=" + req.value().getName() + ")" + flt + ")";
                        } else {
                            flt = "(objectClass=" + req.value().getName() + ")";
                        }
                        if (requirements.length() > 0) {
                            requirements.append(",");
                        }
                        requirements.append("osgi.service;effective:=active;filter:=\"")
                                    .append(flt)
                                    .append("\"");
                    }
                    for (ProvideService cap : services.provides()) {
                        if (capabilities.length() > 0) {
                            capabilities.append(",");
                        }
                        capabilities.append("osgi.service;effective:=active;objectClass=")
                                    .append(cap.value().getName());
                    }
                }
            }

            project.getProperties().setProperty(requirementsProperty, requirements.toString());
            project.getProperties().setProperty(capabilitiesProperty, capabilities.toString());

        } catch (Exception e) {
            throw new MojoExecutionException("Error building commands help", e);
        }
    }

    private ClassFinder createFinder(String classloaderType) throws Exception {
        ClassFinder finder;
        if ("project".equals(classloaderType)) {
            List<URL> urls = new ArrayList<>();

            urls.add( new File(project.getBuild().getOutputDirectory()).toURI().toURL() );
            for ( Artifact artifact : project.getArtifacts() ) {
                File file = artifact.getFile();
                if ( file != null ) {
                    urls.add( file.toURI().toURL() );
                    System.out.println("classpath: " + file);
                }
            }
            ClassLoader loader = new URLClassLoader(urls.toArray(new URL[urls.size()]), getClass().getClassLoader());
            finder = new ClassFinder(loader, urls);
        } else if ("plugin".equals(classLoader)) {
            finder = new ClassFinder(getClass().getClassLoader());
        } else {
            throw new MojoFailureException("classLoader attribute must be 'project' or 'plugin'");
        }
        return finder;
    }

}
