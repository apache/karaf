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
package org.apache.felix.scrplugin.tags;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.*;

import org.apache.felix.scrplugin.Constants;
import org.apache.felix.scrplugin.om.Component;
import org.apache.felix.scrplugin.om.Components;
import org.apache.felix.scrplugin.tags.cl.ClassLoaderJavaClassDescription;
import org.apache.felix.scrplugin.tags.qdox.QDoxJavaClassDescription;
import org.apache.felix.scrplugin.xml.ComponentDescriptorIO;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.*;

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.JavaSource;

/**
 * <code>JavaClassDescriptorManager.java</code>...
 *
 */
public class JavaClassDescriptorManager {

    protected static final String SERVICE_COMPONENT = "Service-Component";

    /** The sources read by qdox. */
    protected final JavaSource[] sources;

    /** The maven log. */
    protected final Log log;

    /** The classloader used to compile the classes. */
    protected final ClassLoader classloader;

    /** A cache containing the java class descriptions hashed by classname. */
    protected final Map javaClassDescriptions = new HashMap();

    /** The component definitions from other bundles hashed by classname. */
    protected final Map componentDescriptions = new HashMap();

    /** The maven project. */
    protected final MavenProject project;

    /**
     * Construct a new manager.
     * @param log
     * @param project
     * @throws MojoFailureException
     * @throws MojoExecutionException
     */
    public JavaClassDescriptorManager(final Log          log,
                                      final MavenProject project,
                                      final String       excludeString)
    throws MojoFailureException, MojoExecutionException {
        this.log = log;
        this.project = project;
        this.classloader = this.getCompileClassLoader(project);

        // get all the class sources through qdox
        this.log.debug("Setting up QDox");
        JavaDocBuilder builder = new JavaDocBuilder();
        builder.getClassLibrary().addClassLoader(this.classloader);
        final Iterator i = project.getCompileSourceRoots().iterator();
        // FELIX-509: check for excludes
        if ( excludeString != null ) {
            final String[] excludes = StringUtils.split(excludeString, ",");
            final String[] includes = new String[] {"**/*.java"};

            while ( i.hasNext() ) {
                final String tree = (String)i.next();
                this.log.debug("Scanning source tree " + tree);
                final File directory = new File(tree);
                final DirectoryScanner scanner = new DirectoryScanner();
                scanner.setBasedir( directory );

                if ( excludes != null && excludes.length > 0 ) {
                    scanner.setExcludes( excludes );
                }
                scanner.addDefaultExcludes();
                scanner.setIncludes( includes );

                scanner.scan();

                final String[] files = scanner.getIncludedFiles();
                if ( files != null ) {
                    for(int m=0; m<files.length; m++) {
                        this.log.debug("Adding source file " + files[m]);
                        try {
                            builder.addSource(new File(directory, files[m]));
                        } catch (FileNotFoundException e) {
                            throw new MojoExecutionException("Unable to scan directory.", e);
                        } catch (IOException e) {
                            throw new MojoExecutionException("Unable to scan directory.", e);
                        }
                    }
                }
            }
        } else {
            while ( i.hasNext() ) {
                final String tree = (String)i.next();
                this.log.debug("Adding source tree " + tree);
                final File directory = new File(tree);
                builder.addSourceTree(directory);
            }
        }
        this.sources = builder.getSources();

        // and now scan artifacts
        final List components = new ArrayList();
        final Map resolved = project.getArtifactMap();
        final Set artifacts = project.getDependencyArtifacts();
        final Iterator it = artifacts.iterator();
        while ( it.hasNext() ) {
            final Artifact declared = (Artifact) it.next();
            this.log.debug("Checking artifact " + declared);
            if (Artifact.SCOPE_COMPILE.equals(declared.getScope())
                || Artifact.SCOPE_RUNTIME.equals(declared.getScope())) {
                this.log.debug("Resolving artifact " + declared);
                final Artifact artifact = (Artifact) resolved.get(ArtifactUtils.versionlessKey(declared));
                if (artifact != null) {
                    this.log.debug("Trying to get manifest from artifact " + artifact);
                    try {
                        final Manifest manifest = this.getManifest(artifact);
                        if ( manifest != null ) {
                            // read Service-Component entry
                            if ( manifest.getMainAttributes().getValue(JavaClassDescriptorManager.SERVICE_COMPONENT) != null ) {
                                final String serviceComponent = manifest.getMainAttributes().getValue(JavaClassDescriptorManager.SERVICE_COMPONENT);
                                this.log.debug("Found Service-Component: " + serviceComponent + " in artifact " + artifact);
                                final StringTokenizer st = new StringTokenizer(serviceComponent, ",");
                                while ( st.hasMoreTokens() ) {
                                    final String entry = st.nextToken().trim();
                                    if ( entry.length() > 0 ) {
                                        components.addAll(this.readServiceComponentDescriptor(artifact, entry).getComponents());
                                    }
                                }
                            } else {
                                this.log.debug("Artifact has no service component entry in manifest " + artifact);
                            }
                        } else {
                            this.log.debug("Unable to get manifest from artifact " + artifact);
                        }
                    } catch (IOException ioe) {
                        throw new MojoExecutionException("Unable to get manifest from artifact " + artifact, ioe);
                    }
                    this.log.debug("Trying to get scrinfo from artifact " + artifact);
                    // now read the scr private file - components stored there overwrite components already
                    // read from the service component section.
                    try {
                        final File scrInfoFile = this.getFile(artifact, Constants.ABSTRACT_DESCRIPTOR_ARCHIV_PATH);
                        if ( scrInfoFile != null ) {
                            components.addAll(this.parseServiceComponentDescriptor(artifact, scrInfoFile).getComponents());
                        } else {
                            this.log.debug("Artifact has no scrinfo file (it's optional): " + artifact);
                        }
                    } catch (IOException ioe) {
                        throw new MojoExecutionException("Unable to get scrinfo from artifact " + artifact, ioe);
                    }
                } else {
                    this.log.debug("Unable to resolve artifact " + declared);
                }
            } else {
                this.log.debug("Artifact " + declared + " has not scope compile or runtime, but " + declared.getScope());
            }
        }
        // now create map with component descriptions
        final Iterator cI = components.iterator();
        while ( cI.hasNext() ) {
            final Component component = (Component) cI.next();
            this.componentDescriptions.put(component.getImplementation().getClassame(), component);
        }
    }

    /**
     * Return the log.
     */
    public Log getLog() {
        return this.log;
    }

    /**
     * Return the class laoder.
     */
    public ClassLoader getClassLoader() {
        return this.classloader;
    }

    /**
     * Return the project.
     */
    public MavenProject getProject() {
        return this.project;
    }

    /**
     * Read the service component description.
     * @param artifact
     * @param entry
     * @throws IOException
     * @throws MojoExecutionException
     */
    protected Components readServiceComponentDescriptor(Artifact artifact, String entry)
    throws IOException, MojoExecutionException {
        this.log.debug("Reading " + entry + " from " + artifact);
        final File xml = this.getFile(artifact, entry);
        if ( xml == null ) {
            throw new MojoExecutionException("Artifact " + artifact + " does not contain declared service component descriptor " + entry);
        }
        return this.parseServiceComponentDescriptor(artifact, xml);
    }

    protected Components parseServiceComponentDescriptor(Artifact artifact, File file)
    throws IOException, MojoExecutionException {
        this.log.debug("Parsing " + file);
        final Components list = ComponentDescriptorIO.read(file);
        return list;
    }

    protected ClassLoader getCompileClassLoader(MavenProject project)
    throws MojoFailureException {
        List artifacts = project.getCompileArtifacts();
        URL[] path = new URL[artifacts.size() + 1];
        int i = 0;
        for (Iterator ai=artifacts.iterator(); ai.hasNext(); ) {
            Artifact a = (Artifact) ai.next();
            try {
                path[i++] = a.getFile().toURI().toURL();
            } catch (IOException ioe) {
                throw new MojoFailureException("Unable to get compile class loader.");
            }
        }
        final String targetDirectory = this.getProject().getBuild().getOutputDirectory();
        try {
            path[path.length - 1] = new File(targetDirectory).toURI().toURL();
        } catch (IOException ioe) {
            throw new MojoFailureException("Unable to add target directory to classloader.");
        }
        return new URLClassLoader(path, this.getClass().getClassLoader());
    }

    protected Manifest getManifest(Artifact artifact) throws IOException {
        JarFile file = null;
        try {
            file = new JarFile(artifact.getFile());
            return file.getManifest();
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    protected File getFile(Artifact artifact, String path) throws IOException {
        final int pos = path.lastIndexOf('.');
        final String suffix = path.substring(pos + 1);
        JarFile file = null;
        File tmpFile = null;
        try {
            file = new JarFile(artifact.getFile());
            final JarEntry entry = file.getJarEntry(path);
            if ( entry != null ) {
                tmpFile = File.createTempFile("scrjcdm" + artifact.getArtifactId(), suffix);
                tmpFile.deleteOnExit();
                final FileOutputStream fos = new FileOutputStream(tmpFile);
                IOUtil.copy(file.getInputStream(entry), fos);
                IOUtil.close(fos);
                return tmpFile;
            }
            return null;
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    /**
     * Return all source descriptions of this project.
     * @return All contained java class descriptions.
     */
    public JavaClassDescription[] getSourceDescriptions() throws MojoExecutionException {
        final JavaClassDescription[] descs = new JavaClassDescription[this.sources.length];
        for(int i=0; i<this.sources.length; i++) {
            final String className = this.sources[i].getClasses()[0].getFullyQualifiedName();
            try {
                descs[i] = new QDoxJavaClassDescription(this.classloader.loadClass(className), this.sources[i], this);
            } catch (ClassNotFoundException e) {
                throw new MojoExecutionException("Unable to load class " + className);
            }
        }
        return descs;
    }

    /**
     * Get a java class description for the class.
     * @param className
     * @return The java class description.
     * @throws MojoExecutionException
     */
    public JavaClassDescription getJavaClassDescription(String className)
    throws MojoExecutionException {
        JavaClassDescription result = (JavaClassDescription) this.javaClassDescriptions.get(className);
        if ( result == null ) {
            this.log.debug("Searching description for: " + className);
            int index = 0;
            while ( result == null && index < this.sources.length) {
                if ( this.sources[index].getClasses()[0].getFullyQualifiedName().equals(className) ) {
                    try {
                        this.log.debug("Found qdox description for: " + className);
                        result = new QDoxJavaClassDescription(this.classloader.loadClass(className), this.sources[index], this);
                    } catch (ClassNotFoundException e) {
                        throw new MojoExecutionException("Unable to load class " + className);
                    }
                } else {
                    index++;
                }
            }
            if ( result == null ) {
                try {
                    this.log.debug("Generating classloader description for: " + className);
                    result = new ClassLoaderJavaClassDescription(this.classloader.loadClass(className), (Component)this.componentDescriptions.get(className), this);
                } catch (ClassNotFoundException e) {
                    throw new MojoExecutionException("Unable to load class " + className);
                }
            }
            this.javaClassDescriptions.put(className, result);
        }
        return result;
    }
}
