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
package org.apache.felix.bundleplugin;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Manifest;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Jar;


/**
 * Generate an OSGi manifest for this project
 * 
 * @goal manifest
 * @phase process-classes
 * @requiresDependencyResolution runtime
 */
public class ManifestPlugin extends BundlePlugin
{
    protected void execute( MavenProject project, Map instructions, Properties properties, Jar[] classpath )
        throws MojoExecutionException
    {
        Manifest manifest;
        try
        {
            if ( "bundle".equals( project.getPackaging() ) )
            {
                manifest = buildOSGiBundle( project, instructions, properties, classpath ).getJar().getManifest();
            }
            else
            {
                manifest = getManifest( project, instructions, properties, classpath );
            }
        }
        catch ( FileNotFoundException e )
        {
            throw new MojoExecutionException( "Cannot find " + e.getMessage()
                + " (manifest goal must be run after compile phase)", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error trying to generate Manifest", e );
        }
        catch ( Exception e )
        {
            getLog().error( "An internal error occurred", e );
            throw new MojoExecutionException( "Internal error in maven-bundle-plugin", e );
        }

        File outputFile = new File( manifestLocation, "MANIFEST.MF" );

        try
        {
            writeManifest( manifest, outputFile );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error trying to write Manifest to file " + outputFile, e );
        }
    }


    public Manifest getManifest( MavenProject project, Jar[] classpath ) throws IOException
    {
        return getManifest( project, new Properties(), new Properties(), classpath );
    }


    public Manifest getManifest( MavenProject project, Map instructions, Properties properties, Jar[] classpath )
        throws IOException
    {
        return getAnalyzer( project, instructions, properties, classpath ).getJar().getManifest();
    }


    protected Analyzer getAnalyzer( MavenProject project, Jar[] classpath ) throws IOException
    {
        return getAnalyzer( project, new HashMap(), new Properties(), classpath );
    }


    protected Analyzer getAnalyzer( MavenProject project, Map instructions, Properties properties, Jar[] classpath )
        throws IOException
    {
        PackageVersionAnalyzer analyzer = new PackageVersionAnalyzer();

        Properties props = getDefaultProperties( project );
        props.putAll( properties );

        if ( !instructions.containsKey( Analyzer.IMPORT_PACKAGE ) )
        {
            props.put( Analyzer.IMPORT_PACKAGE, "*" );
        }

        props.putAll( transformDirectives( instructions ) );

        analyzer.setProperties( props );

        File file = project.getArtifact().getFile();
        if ( file == null )
        {
            file = getOutputDirectory();
        }

        if ( !file.exists() )
        {
            throw new FileNotFoundException( file.getPath() );
        }

        analyzer.setJar( file );

        if ( classpath != null )
            analyzer.setClasspath( classpath );

        if ( !instructions.containsKey( Analyzer.PRIVATE_PACKAGE )
            && !instructions.containsKey( Analyzer.EXPORT_PACKAGE ) )
        {
            String export = analyzer.calculateExportsFromContents( analyzer.getJar() );
            analyzer.setProperty( Analyzer.EXPORT_PACKAGE, export );
        }

        analyzer.mergeManifest( analyzer.getJar().getManifest() );

        analyzer.calcManifest();

        return analyzer;
    }


    public static void writeManifest( Manifest manifest, File outputFile ) throws IOException
    {
        outputFile.getParentFile().mkdirs();

        FileOutputStream os;
        os = new FileOutputStream( outputFile );
        try
        {
            manifest.write( os );
        }
        finally
        {
            try
            {
                os.close();
            }
            catch ( IOException e )
            {
                // nothing we can do here
            }
        }
    }
}
