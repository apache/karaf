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
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.karaf.util.tracker.Managed;
import org.apache.karaf.util.tracker.ProvideService;
import org.apache.karaf.util.tracker.RequireService;
import org.apache.karaf.util.tracker.Services;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.xbean.finder.ClassFinder;

/**
 * Generates service requirement and capabilities for activators
 */
@Mojo(name = "service-metadata-generate", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, inheritByDefault = false)
public class GenerateServiceMetadata extends AbstractMojo {

    /**
     * The maven project.
     */
    @Parameter(defaultValue = "${project}")
    protected MavenProject project;

    @Parameter(defaultValue="requirements")
    protected String requirementsProperty;

    @Parameter(defaultValue="capabilities")
    protected String capabilitiesProperty;

    @Parameter(defaultValue = "${project.build.directory}/generated/karaf-tracker")
    protected String outputDirectory;

    /**
     * The classloader to use for loading the commands.
     * Can be "project" or "plugin"
     */
    @Parameter(defaultValue = "project")
    protected String classLoader;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            boolean addSourceDirectory = false;

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

                Properties props = new Properties();
                Services services = clazz.getAnnotation(Services.class);
                if (services != null) {
                    for (RequireService req : services.requires()) {
                        String flt = req.filter();
                        if (flt == null) {
                            flt = "";
                        }
                        String fltWithClass;
                        if (!flt.isEmpty()) {
                            fltWithClass = "(&(objectClass=" + req.value().getName() + ")" + flt + ")";
                        } else {
                            fltWithClass = "(objectClass=" + req.value().getName() + ")";
                        }
                        if (requirements.length() > 0) {
                            requirements.append(",");
                        }
                        requirements.append("osgi.service;effective:=active;filter:=\"")
                                    .append(fltWithClass)
                                    .append("\"");
                        props.setProperty(req.value().getName(), flt);
                    }
                    for (ProvideService cap : services.provides()) {
                        if (capabilities.length() > 0) {
                            capabilities.append(",");
                        }
                        capabilities.append("osgi.service;effective:=active;objectClass=")
                                    .append(cap.value().getName());
                    }
                }
                Managed managed = clazz.getAnnotation(Managed.class);
                if (managed != null) {
                    props.setProperty("pid", managed.value());
                }

                File file = new File(outputDirectory, "OSGI-INF/karaf-tracker/" + clazz.getName());
                file.getParentFile().mkdirs();
                try (OutputStream os = new FileOutputStream(file)) {
                    props.store(os, null);
                }
                addSourceDirectory = true;
            }

            if (addSourceDirectory) {
                Resource resource = new Resource();
                resource.setDirectory(outputDirectory);
                project.addResource(resource);
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
