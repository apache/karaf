/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.apache.felix.tools.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.util.FileUtils;

/**
 * Package an OSGi jar "bundle."
 * 
 * @author <a href="mailto:felix-dev@incubator.apache.org">Apache Felix Project</a>
 * @version $Rev$, $Date$
 * @goal osgi-bundle
 * @phase package
 * @requiresDependencyResolution runtime
 * @description build an OSGi bundle jar
 */
public class OsgiJarMojo extends AbstractMojo
{
    private static final String[] EMPTY_STRING_ARRAY = {};

    /**
     * The Maven project.
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The directory for the generated JAR.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private String buildDirectory;

    /**
     * The directory containing generated classes.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    private File outputDirectory;

    /**
     * The name of the generated JAR file.
     * 
     * @parameter alias="jarName" expression="${project.build.finalName}"
     * @required
     */
    private String jarName;

    /**
     * The Jar archiver.
     *
     * @parameter expression="${component.org.codehaus.plexus.archiver.Archiver#jar}"
     * @required
     */
    private JarArchiver jarArchiver;

    /**
     * The maven archive configuration to use.
     */
    private MavenArchiveConfiguration archiveConfig = new MavenArchiveConfiguration();

    /**
     * The comma separated list of tokens to include in the JAR.
     * Default is '**'.
     *
     * @parameter alias="includes"
     */
    private String jarSourceIncludes = "**";

    /**
     * The comma separated list of tokens to exclude from the JAR.
     *
     * @parameter alias="excludes"
     */
    private String jarSourceExcludes;

    /**
     * @parameter
     */
    private String manifestFile;

    /**
     * @parameter expression="${org.apache.felix.tools.maven.plugin.OsgiManifest}"
     */
    private OsgiManifest osgiManifest;

    /**
     * Execute this Mojo
     * 
     * @throws MojoExecutionException
     */
    public void execute() throws MojoExecutionException
    {
        File jarFile = new File( buildDirectory, jarName + ".jar" );

        try
        {
            performPackaging( jarFile );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error assembling JAR bundle", e );
        }
    }

    /**
     * Generates the JAR bundle file.
     *
     * @param  jarFile the target JAR file
     * @throws IOException
     * @throws ArchiverException
     * @throws ManifestException
     * @throws DependencyResolutionRequiredException
     */
    private void performPackaging( File jarFile ) throws IOException, ArchiverException, ManifestException,
            DependencyResolutionRequiredException, MojoExecutionException
    {
        getLog().info( "Generating JAR bundle " + jarFile.getAbsolutePath() );

        MavenArchiver archiver = new MavenArchiver();

        archiver.setArchiver( jarArchiver );
        archiver.setOutputFile( jarFile );

        addManifestFile();
        addManifestEntries();

        addBundleClasspath();
        addBundleVersion();

        jarArchiver.addDirectory( outputDirectory, getIncludes(), getExcludes() );

        archiver.createArchive( project, archiveConfig );

        project.getArtifact().setFile( jarFile );
    }

    /**
     * TODO: Decide if we accept merging of entire manifest.mf files
     * Here's a big question to make a final decision at some point: Do accept
     * merging of manifest entries located in some file somewhere in the project
     * directory?  If so, do we allow both file and configuration based entries
     * to be specified simultaneously and how do we merge these?
     */
    private void addManifestFile()
    {
        if ( manifestFile != null )
        {
            File file = new File( project.getBasedir().getAbsolutePath(), manifestFile );
            getLog().info( "Manifest file: " + file.getAbsolutePath() + " will be used" );
            archiveConfig.setManifestFile( file );
        }
        else
        {
            getLog().info( "No manifest file specified. Default will be used." );
        }
    }

    /**
     * Look for any OSGi specified manifest entries in the maven-osgi-plugin configuration
     * section of the POM.  If we find some, then add them to the target artifact's manifest.
     */
    private void addManifestEntries()
    {
        if ( osgiManifest != null && osgiManifest.getEntries().size() > 0 )
        {
            Map entries = osgiManifest.getEntries();

            getLog().info( "Bundle manifest will be modified with the following entries: " + entries.toString() );
            archiveConfig.addManifestEntries( entries );
        }
        else
        {
            getLog().info( "No OSGi bundle manifest entries have been specified in the POM." );
        }
    }

    /**
     * We are going to iterate through the POM's specified JAR dependencies.  If a dependency
     * has a scope of either RUNTIME or COMPILE, then we'll JAR them up inside the
     * OSGi bundle artifact.  We will then add the Bundle-Classpath manifest entry.
     */
    private void addBundleClasspath() throws MojoExecutionException
    {
        StringBuffer bundleClasspath = new StringBuffer();
        Set artifacts = project.getArtifacts();

        for ( Iterator it = artifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();
            if ( !Artifact.SCOPE_PROVIDED.equals( artifact.getScope() )
                    && !Artifact.SCOPE_TEST.equals( artifact.getScope() ) )
            {
                String type = artifact.getType();

                if ( "jar".equals( type ) )
                {
                    File depFile = artifact.getFile();

                    try
                    {
                        FileUtils.copyFileToDirectory( depFile, outputDirectory );

                        if ( bundleClasspath.length() == 0 )
                        {
                            bundleClasspath.append( "." );
                        }

                        bundleClasspath.append( "," + artifact.getFile().getName() );
                    }
                    catch ( Exception e )
                    {
                        String errmsg = "Error copying " + depFile.getAbsolutePath() + " to "
                                + outputDirectory.getAbsolutePath();
                        throw new MojoExecutionException( errmsg, e );
                    }
                }
            }
        }

        String finalPath = bundleClasspath.toString();

        if ( finalPath.length() != 0 )
        {
            archiveConfig.addManifestEntry( "Bundle-Classpath", finalPath );
        }
    }

    /**
     * Auto-set the bundle version.
     */
    private void addBundleVersion()
    {
        // Maven uses a '-' to separate the version qualifier,
        // while OSGi uses a '.', so we need to convert to a '.'
        StringBuffer sb = new StringBuffer(project.getVersion());
        if (sb.indexOf("-") >= 0)
        {
            sb.setCharAt(sb.indexOf("-"), '.');
        }
        archiveConfig.addManifestEntry( "Bundle-Version", sb.toString() );
    }

    /**
     * Returns a string array of the includes to be used when assembling/copying the war.
     *
     * @return an array of tokens to include
     */
    private String[] getIncludes()
    {
        return new String[] { jarSourceIncludes };
    }

    /**
     * Returns a string array of the excludes to be used when assembling/copying the jar.
     *
     * @return an array of tokens to exclude
     */
    private String[] getExcludes()
    {
        List excludeList = new ArrayList( FileUtils.getDefaultExcludesAsList() );

        if ( jarSourceExcludes != null && !"".equals( jarSourceExcludes ) )
        {
            excludeList.add( jarSourceExcludes );
        }

        return (String[]) excludeList.toArray( EMPTY_STRING_ARRAY );
    }
}
