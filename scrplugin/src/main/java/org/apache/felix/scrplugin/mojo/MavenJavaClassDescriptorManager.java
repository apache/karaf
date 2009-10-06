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
package org.apache.felix.scrplugin.mojo;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.felix.scrplugin.Constants;
import org.apache.felix.scrplugin.JavaClassDescriptorManager;
import org.apache.felix.scrplugin.Log;
import org.apache.felix.scrplugin.SCRDescriptorException;
import org.apache.felix.scrplugin.SCRDescriptorFailureException;
import org.apache.felix.scrplugin.helper.StringUtils;
import org.apache.felix.scrplugin.om.Component;
import org.apache.felix.scrplugin.om.Components;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.JavaSource;


public class MavenJavaClassDescriptorManager extends JavaClassDescriptorManager
{

    private final MavenProject project;

    private final String excludeString;

    private JavaSource[] sources;

    /** The component definitions from other bundles hashed by classname. */
    private Map<String, Component> componentDescriptions;


    public MavenJavaClassDescriptorManager( MavenProject project, Log log, ClassLoader classLoader,
        String[] annotationTagProviders, String excludeString, boolean parseJavadocs, boolean processAnnotations )
        throws SCRDescriptorFailureException
    {
        super( log, classLoader, annotationTagProviders, parseJavadocs, processAnnotations );

        this.project = project;
        this.excludeString = excludeString;
    }


    public String getOutputDirectory()
    {
        return this.project.getBuild().getOutputDirectory();
    }


    protected JavaSource[] getSources() throws SCRDescriptorException
    {
        if ( this.sources == null )
        {

            this.log.debug( "Setting up QDox" );

            JavaDocBuilder builder = new JavaDocBuilder();
            builder.getClassLibrary().addClassLoader( this.getClassLoader() );

            @SuppressWarnings("unchecked")
            final Iterator<String> i = project.getCompileSourceRoots().iterator();
            // FELIX-509: check for excludes
            if ( excludeString != null )
            {
                final String[] excludes = StringUtils.split( excludeString, "," );
                final String[] includes = new String[]
                    { "**/*.java" };

                while ( i.hasNext() )
                {
                    final String tree = i.next();
                    this.log.debug( "Scanning source tree " + tree );
                    final File directory = new File( tree );
                    final DirectoryScanner scanner = new DirectoryScanner();
                    scanner.setBasedir( directory );

                    if ( excludes != null && excludes.length > 0 )
                    {
                        scanner.setExcludes( excludes );
                    }
                    scanner.addDefaultExcludes();
                    scanner.setIncludes( includes );

                    scanner.scan();

                    final String[] files = scanner.getIncludedFiles();
                    if ( files != null )
                    {
                        for ( int m = 0; m < files.length; m++ )
                        {
                            this.log.debug( "Adding source file " + files[m] );
                            try
                            {
                                builder.addSource( new File( directory, files[m] ) );
                            }
                            catch ( FileNotFoundException e )
                            {
                                throw new SCRDescriptorException( "Unable to scan directory.", files[m], 0, e );
                            }
                            catch ( IOException e )
                            {
                                throw new SCRDescriptorException( "Unable to scan directory.", files[m], 0, e );
                            }
                        }
                    }
                }
            }
            else
            {
                while ( i.hasNext() )
                {
                    final String tree = i.next();
                    this.log.debug( "Adding source tree " + tree );
                    final File directory = new File( tree );
                    builder.addSourceTree( directory );
                }
            }
            this.sources = builder.getSources();
        }

        return this.sources;
    }


    protected Map<String, Component> getComponentDescriptors() throws SCRDescriptorException
    {
        if ( this.componentDescriptions == null )
        {
            this.componentDescriptions = new HashMap<String, Component>();

            // and now scan artifacts
            final List<Component> components = new ArrayList<Component>();
            @SuppressWarnings("unchecked")
            final Map<String, Artifact> resolved = project.getArtifactMap();
            @SuppressWarnings("unchecked")
            final Set<Artifact> artifacts = project.getDependencyArtifacts();
            final Iterator<Artifact> it = artifacts.iterator();
            while ( it.hasNext() )
            {
                final Artifact declared = it.next();
                this.log.debug( "Checking artifact " + declared );
                if ( this.isJavaArtifact( declared ) )
                {
                    if ( Artifact.SCOPE_COMPILE.equals( declared.getScope() )
                        || Artifact.SCOPE_RUNTIME.equals( declared.getScope() )
                        || Artifact.SCOPE_PROVIDED.equals( declared.getScope() ) )
                    {
                        this.log.debug( "Resolving artifact " + declared );
                        final Artifact artifact = resolved.get( ArtifactUtils.versionlessKey( declared ) );
                        if ( artifact != null )
                        {
                            this.log.debug( "Trying to get manifest from artifact " + artifact );
                            try
                            {
                                final Manifest manifest = this.getManifest( artifact );
                                if ( manifest != null )
                                {
                                    // read Service-Component entry
                                    if ( manifest.getMainAttributes().getValue( Constants.SERVICE_COMPONENT ) != null )
                                    {
                                        final String serviceComponent = manifest.getMainAttributes().getValue(
                                            Constants.SERVICE_COMPONENT );
                                        this.log.debug( "Found Service-Component: " + serviceComponent
                                            + " in artifact " + artifact );
                                        final StringTokenizer st = new StringTokenizer( serviceComponent, "," );
                                        while ( st.hasMoreTokens() )
                                        {
                                            final String entry = st.nextToken().trim();
                                            if ( entry.length() > 0 )
                                            {
                                                final Components c = this.readServiceComponentDescriptor( artifact,
                                                    entry );
                                                if ( c != null )
                                                {
                                                    components.addAll( c.getComponents() );
                                                }
                                            }
                                        }
                                    }
                                    else
                                    {
                                        this.log.debug( "Artifact has no service component entry in manifest "
                                            + artifact );
                                    }
                                }
                                else
                                {
                                    this.log.debug( "Unable to get manifest from artifact " + artifact );
                                }
                            }
                            catch ( IOException ioe )
                            {
                                throw new SCRDescriptorException( "Unable to get manifest from artifact", artifact
                                    .toString(), 0, ioe );
                            }
                            this.log.debug( "Trying to get scrinfo from artifact " + artifact );
                            // now read the scr private file - components stored there overwrite components already
                            // read from the service component section.
                            InputStream scrInfoFile = null;
                            try
                            {
                                scrInfoFile = this.getFile( artifact, Constants.ABSTRACT_DESCRIPTOR_ARCHIV_PATH );
                                if ( scrInfoFile != null )
                                {
                                    components.addAll( this.parseServiceComponentDescriptor( scrInfoFile )
                                        .getComponents() );
                                }
                                else
                                {
                                    this.log.debug( "Artifact has no scrinfo file (it's optional): " + artifact );
                                }
                            }
                            catch ( IOException ioe )
                            {
                                throw new SCRDescriptorException( "Unable to get scrinfo from artifact", artifact
                                    .toString(), 0, ioe );
                            }
                            finally
                            {
                                if ( scrInfoFile != null )
                                {
                                    try
                                    {
                                        scrInfoFile.close();
                                    }
                                    catch ( IOException ignore )
                                    {
                                    }
                                }
                            }
                        }
                        else
                        {
                            this.log.debug( "Unable to resolve artifact " + declared );
                        }
                    }
                    else
                    {
                        this.log.debug( "Artifact " + declared + " has not scope compile or runtime, but "
                            + declared.getScope() );
                    }
                }
                else
                {
                    this.log.debug( "Artifact " + declared + " is not a java artifact, type is " + declared.getType() );
                }
            }
            // now create map with component descriptions
            for ( final Component component : components )
            {
                this.componentDescriptions.put( component.getImplementation().getClassame(), component );
            }
        }

        return this.componentDescriptions;
    }


    /**
     * Check if the artifact is a java artifact (jar or bundle)
     */
    private boolean isJavaArtifact( Artifact artifact )
    {
        if ( "jar".equals( artifact.getType() ) )
        {
            return true;
        }
        if ( "bundle".equals( artifact.getType() ) )
        {
            return true;
        }
        return false;
    }

    /**
     * Read the service component description.
     * @param artifact
     * @param entry
     * @throws IOException
     * @throws SCRDescriptorException
     */
    protected Components readServiceComponentDescriptor(Artifact artifact, String entry) {
        this.log.debug("Reading " + entry + " from " + artifact);
        InputStream xml = null;
        try {
            xml = this.getFile(artifact, entry);
            if ( xml == null ) {
                throw new SCRDescriptorException( "Entry " + entry + " not contained in artifact", artifact.toString(),
                    0 );
            }
            return this.parseServiceComponentDescriptor(xml);
        } catch (IOException mee) {
            this.log.warn("Unable to read SCR descriptor file from artifact " + artifact + " at " + entry);
            this.log.debug("Exception occurred during reading: " + mee.getMessage(), mee);
        } catch (SCRDescriptorException mee) {
            this.log.warn("Unable to read SCR descriptor file from artifact " + artifact + " at " + entry);
            this.log.debug("Exception occurred during reading: " + mee.getMessage(), mee);
        }
        finally
        {
            if ( xml != null )
            {
                try
                {
                    xml.close();
                }
                catch ( IOException ignore )
                {
                }
            }
        }
       return null;
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

    protected InputStream getFile(Artifact artifact, String path) throws IOException {
        JarFile file = null;
        try {
            file = new JarFile(artifact.getFile());
            final JarEntry entry = file.getJarEntry(path);
            if ( entry != null ) {
                final InputStream stream = new ArtifactFileInputStream( file, entry);
                file = null; // prevent file from being closed now
                return stream;
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

    private static class ArtifactFileInputStream extends FilterInputStream
    {
        final JarFile jarFile;


        ArtifactFileInputStream( JarFile jarFile, JarEntry jarEntry ) throws IOException
        {
            super( jarFile.getInputStream( jarEntry ) );
            this.jarFile = jarFile;
        }


        @Override
        public void close() throws IOException
        {
            try
            {
                super.close();
            }
            catch ( IOException ioe )
            {
            }
            jarFile.close();
        }


        @Override
        protected void finalize() throws Throwable
        {
            try
            {
                close();
            }
            finally
            {
                super.finalize();
            }
        }
    }
}
