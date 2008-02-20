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


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipException;

import org.apache.maven.archiver.ManifestSection;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.osgi.Maven2OsgiConverter;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringInputStream;

import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.EmbeddedResource;
import aQute.lib.osgi.FileResource;
import aQute.lib.osgi.Jar;


/**
 * Create an OSGi bundle from Maven project
 *
 * @goal bundle
 * @phase package
 * @requiresDependencyResolution runtime
 * @description build an OSGi bundle jar
 */
public class BundlePlugin extends AbstractMojo
{
    /**
     * Directory where the manifest will be written
     *
     * @parameter expression="${manifestLocation}" default-value="${project.build.outputDirectory}/META-INF"
     */
    protected File manifestLocation;

    /**
     * When true, unpack the bundle contents to the outputDirectory
     *
     * @parameter expression="${unpackBundle}"
     */
    protected boolean unpackBundle;

    /**
     * Comma separated list of artifactIds to exclude from the dependency classpath passed to BND (use "true" to exclude everything)
     *
     * @parameter expression="${excludeDependencies}"
     */
    protected String excludeDependencies;

    /**
     * Classifier type of the bundle to be installed.  For example, "jdk14".
     * Defaults to none which means this is the project's main bundle.
     *
     * @parameter expression="${classifier}"
     */
    protected String classifier;

    /**
     * @component
     */
    private MavenProjectHelper m_projectHelper;

    /**
     * @component
     */
    private ArchiverManager m_archiverManager;

    /**
     * @component
     */
    private ArtifactHandlerManager m_artifactHandlerManager;

    /**
     * Project types which this plugin supports.
     *
     * @parameter
     */
    private List supportedProjectTypes = Arrays.asList( new String[]
        { "jar", "bundle" } );

    /**
     * The directory for the generated bundles.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private File outputDirectory;

    /**
     * The directory for the pom
     *
     * @parameter expression="${basedir}"
     * @required
     */
    private File baseDir;

    /**
     * The directory for the generated JAR.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private String buildDirectory;

    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The BND instructions for the bundle.
     *
     * @parameter
     */
    private Map instructions = new HashMap();

    /**
     * @component
     */
    private Maven2OsgiConverter m_maven2OsgiConverter;

    private static final String MAVEN_RESOURCES = "{maven-resources}";
    private static final String MAVEN_RESOURCES_REGEX = "\\{maven-resources\\}";
    private static final String[] EMPTY_STRING_ARRAY =
        {};
    private static final String[] DEFAULT_INCLUDES =
        { "**/**" };


    protected Maven2OsgiConverter getMaven2OsgiConverter()
    {
        return m_maven2OsgiConverter;
    }


    protected void setMaven2OsgiConverter( Maven2OsgiConverter maven2OsgiConverter )
    {
        m_maven2OsgiConverter = maven2OsgiConverter;
    }


    protected MavenProject getProject()
    {
        return project;
    }


    /**
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute() throws MojoExecutionException
    {
        Properties properties = new Properties();

        // ignore project types not supported, useful when the plugin is configured in the parent pom
        if ( !supportedProjectTypes.contains( getProject().getArtifact().getType() ) )
        {
            getLog().debug(
                "Ignoring project " + getProject().getArtifact() + " : type " + getProject().getArtifact().getType()
                    + " is not supported by bundle plugin, supported types are " + supportedProjectTypes );
            return;
        }

        execute( getProject(), instructions, properties );
    }


    protected void execute( MavenProject currentProject, Map originalInstructions, Properties properties )
        throws MojoExecutionException
    {
        try
        {
            execute( currentProject, originalInstructions, properties, getClasspath( currentProject ) );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error calculating classpath for project " + currentProject, e );
        }
    }


    /* transform directives from their XML form to the expected BND syntax (eg. _include becomes -include) */
    protected static Map transformDirectives( Map originalInstructions )
    {
        Map transformedInstructions = new HashMap();
        for ( Iterator i = originalInstructions.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry e = ( Map.Entry ) i.next();

            String key = ( String ) e.getKey();
            if ( key.startsWith( "_" ) )
            {
                key = "-" + key.substring( 1 );
            }

            String value = ( String ) e.getValue();
            if ( null == value )
            {
                value = "";
            }
            else
            {
                value = value.replaceAll( "[\r\n]", "" );
            }

            transformedInstructions.put( key, value );
        }
        return transformedInstructions;
    }


    protected void execute( MavenProject currentProject, Map originalInstructions, Properties properties,
        Jar[] classpath ) throws MojoExecutionException
    {
        try
        {
            File jarFile = new File( getBuildDirectory(), getBundleName( currentProject ) );

            Builder builder = buildOSGiBundle( currentProject, originalInstructions, properties, classpath );

            List errors = builder.getErrors();
            List warnings = builder.getWarnings();

            for ( Iterator w = warnings.iterator(); w.hasNext(); )
            {
                String msg = ( String ) w.next();
                getLog().warn( "Warning building bundle " + currentProject.getArtifact() + " : " + msg );
            }
            for ( Iterator e = errors.iterator(); e.hasNext(); )
            {
                String msg = ( String ) e.next();
                getLog().error( "Error building bundle " + currentProject.getArtifact() + " : " + msg );
            }

            if ( errors.size() > 0 )
            {
                String failok = properties.getProperty( "-failok" );
                if ( null == failok || "false".equalsIgnoreCase( failok ) )
                {
                    jarFile.delete();

                    throw new MojoFailureException( "Error(s) found in bundle configuration" );
                }
            }

            // attach bundle to maven project
            jarFile.getParentFile().mkdirs();
            builder.getJar().write( jarFile );

            Artifact mainArtifact = currentProject.getArtifact();

            // workaround for MNG-1682: force maven to install artifact using the "jar" handler
            mainArtifact.setArtifactHandler( m_artifactHandlerManager.getArtifactHandler( "jar" ) );

            if ( null == classifier || classifier.trim().length() == 0 )
            {
                mainArtifact.setFile( jarFile );
            }
            else
            {
                m_projectHelper.attachArtifact( currentProject, jarFile, classifier );
            }

            if ( unpackBundle )
            {
                unpackBundle( jarFile );
            }

            if ( manifestLocation != null )
            {
                File outputFile = new File( manifestLocation, "MANIFEST.MF" );

                try
                {
                    Manifest manifest = builder.getJar().getManifest();
                    ManifestPlugin.writeManifest( manifest, outputFile );
                }
                catch ( IOException e )
                {
                    getLog().error( "Error trying to write Manifest to file " + outputFile, e );
                }
            }
        }
        catch ( MojoFailureException e )
        {
            getLog().error( e.getLocalizedMessage() );
            throw new MojoExecutionException( "Error(s) found in bundle configuration", e );
        }
        catch ( Exception e )
        {
            getLog().error( "An internal error occurred", e );
            throw new MojoExecutionException( "Internal error in maven-bundle-plugin", e );
        }
    }


    protected Builder buildOSGiBundle( MavenProject currentProject, Map originalInstructions, Properties properties,
        Jar[] classpath ) throws Exception
    {
        properties.putAll( getDefaultProperties( currentProject ) );
        properties.putAll( transformDirectives( originalInstructions ) );

        // update BND instructions to add Maven resources
        includeMavenResources( currentProject, properties, getLog() );

        Builder builder = new Builder();
        builder.setBase( currentProject.getBasedir() );
        builder.setProperties( properties );
        builder.setClasspath( classpath );

        if ( null != classifier && classifier.trim().length() > 0 )
        {
            String bundleVersion = properties.getProperty( Analyzer.BUNDLE_VERSION );
            properties.put( Analyzer.BUNDLE_VERSION, bundleVersion + '-' + classifier );
        }

        if ( !properties.containsKey( Analyzer.EXPORT_PACKAGE ) && !properties.containsKey( Analyzer.PRIVATE_PACKAGE ) )
        {
            String bsn = properties.getProperty( Analyzer.BUNDLE_SYMBOLICNAME );
            String namespace = bsn.replaceAll( "\\W", "." );

            properties.put( Analyzer.EXPORT_PACKAGE, namespace + ".*" );
        }

        // update BND instructions to embed selected Maven dependencies
        Collection embeddableArtifacts = getEmbeddableArtifacts( currentProject, properties );
        new DependencyEmbedder( embeddableArtifacts ).processHeaders( properties );

        builder.build();
        Jar jar = builder.getJar();
        doMavenMetadata( currentProject, jar );
        builder.setJar( jar );

        String[] removeHeaders = properties.getProperty( Analyzer.REMOVE_HEADERS, "" ).split( "," );
        mergeMavenManifest( currentProject, jar, removeHeaders, getLog() );

        return builder;
    }


    protected static void includeMavenResources( MavenProject currentProject, Properties properties, Log log )
    {
        // pass maven resource paths onto BND analyzer
        final String mavenResourcePaths = getMavenResourcePaths( currentProject );
        final String includeResource = ( String ) properties.get( Analyzer.INCLUDE_RESOURCE );
        if ( includeResource != null )
        {
            if ( includeResource.indexOf( MAVEN_RESOURCES ) >= 0 )
            {
                // if there is no maven resource path, we do a special treatment and replace
                // every occurance of MAVEN_RESOURCES and a following comma with an empty string
                if ( mavenResourcePaths.length() == 0 )
                {
                    String cleanedResource = removeTagFromInstruction( includeResource, MAVEN_RESOURCES );
                    if ( cleanedResource.length() > 0 )
                    {
                        properties.put( Analyzer.INCLUDE_RESOURCE, cleanedResource );
                    }
                    else
                    {
                        properties.remove( Analyzer.INCLUDE_RESOURCE );
                    }
                }
                else
                {
                    String combinedResource = includeResource.replaceAll( MAVEN_RESOURCES_REGEX, mavenResourcePaths );
                    properties.put( Analyzer.INCLUDE_RESOURCE, combinedResource );
                }
            }
            else if ( mavenResourcePaths.length() > 0 )
            {
                log.warn( Analyzer.INCLUDE_RESOURCE + ": overriding " + mavenResourcePaths + " with " + includeResource
                    + " (add " + MAVEN_RESOURCES + " if you want to include the maven resources)" );
            }
        }
        else if ( mavenResourcePaths.length() > 0 )
        {
            properties.put( Analyzer.INCLUDE_RESOURCE, mavenResourcePaths );
        }
    }


    protected static void mergeMavenManifest( MavenProject currentProject, Jar jar, String[] removeHeaders, Log log )
    {
        try
        {
            /*
             * Grab customized manifest entries from the maven-jar-plugin configuration
             */
            MavenArchiveConfiguration archiveConfig = JarPluginConfiguration.getArchiveConfiguration( currentProject );
            String mavenManifestText = new MavenArchiver().getManifest( currentProject, archiveConfig ).toString();
            Manifest mavenManifest = new Manifest();

            // First grab the external manifest file (if specified)
            File externalManifestFile = archiveConfig.getManifestFile();
            if ( null != externalManifestFile && externalManifestFile.exists() )
            {
                InputStream mis = new FileInputStream( externalManifestFile );
                mavenManifest.read( mis );
                mis.close();
            }

            // Then apply the customized entries from the jar plugin
            mavenManifest.read( new StringInputStream( mavenManifestText ) );

            if ( !archiveConfig.isManifestSectionsEmpty() )
            {
                /*
                 * Add customized manifest sections (for some reason MavenArchiver doesn't do this for us)
                 */
                List sections = archiveConfig.getManifestSections();
                for ( Iterator i = sections.iterator(); i.hasNext(); )
                {
                    ManifestSection section = ( ManifestSection ) i.next();
                    Attributes attributes = new Attributes();

                    if ( !section.isManifestEntriesEmpty() )
                    {
                        Map entries = section.getManifestEntries();
                        for ( Iterator j = entries.entrySet().iterator(); j.hasNext(); )
                        {
                            Map.Entry entry = ( Map.Entry ) j.next();
                            attributes.putValue( ( String ) entry.getKey(), ( String ) entry.getValue() );
                        }
                    }

                    mavenManifest.getEntries().put( section.getName(), attributes );
                }
            }

            Attributes mainMavenAttributes = mavenManifest.getMainAttributes();
            mainMavenAttributes.putValue( "Created-By", "Apache Maven Bundle Plugin" );

            // apply -removeheaders to the custom manifest
            for ( int i = 0; i < removeHeaders.length; i++ )
            {
                for ( Iterator j = mainMavenAttributes.keySet().iterator(); j.hasNext(); )
                {
                    if ( j.next().toString().matches( removeHeaders[i].trim() ) )
                    {
                        j.remove();
                    }
                }
            }

            /*
             * Overlay generated bundle manifest with customized entries
             */
            Manifest bundleManifest = jar.getManifest();
            bundleManifest.getMainAttributes().putAll( mainMavenAttributes );
            bundleManifest.getEntries().putAll( mavenManifest.getEntries() );
            jar.setManifest( bundleManifest );
        }
        catch ( Exception e )
        {
            log.warn( "Unable to merge Maven manifest: " + e.getLocalizedMessage() );
        }
    }


    private void unpackBundle( File jarFile )
    {
        File outputDir = getOutputDirectory();
        if ( null == outputDir )
        {
            outputDir = new File( getBuildDirectory(), "classes" );
        }

        try
        {
            /*
             * this directory must exist before unpacking, otherwise the plexus
             * unarchiver decides to use the current working directory instead!
             */
            if ( !outputDir.exists() )
            {
                outputDir.mkdirs();
            }

            UnArchiver unArchiver = m_archiverManager.getUnArchiver( "jar" );
            unArchiver.setDestDirectory( outputDir );
            unArchiver.setSourceFile( jarFile );
            unArchiver.extract();
        }
        catch ( Exception e )
        {
            getLog().error( "Problem unpacking " + jarFile + " to " + outputDir, e );
        }
    }


    protected static String removeTagFromInstruction( String instruction, String tag )
    {
        StringBuffer buf = new StringBuffer();

        String[] clauses = instruction.split( "," );
        for ( int i = 0; i < clauses.length; i++ )
        {
            String clause = clauses[i].trim();
            if ( !tag.equals( clause ) )
            {
                if ( buf.length() > 0 )
                {
                    buf.append( ',' );
                }
                buf.append( clause );
            }
        }

        return buf.toString();
    }


    private static Map getProperties( Model projectModel, String prefix )
    {
        Map properties = new HashMap();
        Method methods[] = Model.class.getDeclaredMethods();
        for ( int i = 0; i < methods.length; i++ )
        {
            String name = methods[i].getName();
            if ( name.startsWith( "get" ) )
            {
                try
                {
                    Object v = methods[i].invoke( projectModel, null );
                    if ( v != null )
                    {
                        name = prefix + Character.toLowerCase( name.charAt( 3 ) ) + name.substring( 4 );
                        if ( v.getClass().isArray() )
                            properties.put( name, Arrays.asList( ( Object[] ) v ).toString() );
                        else
                            properties.put( name, v );

                    }
                }
                catch ( Exception e )
                {
                    // too bad
                }
            }
        }
        return properties;
    }


    private static StringBuffer printLicenses( List licenses )
    {
        if ( licenses == null || licenses.size() == 0 )
            return null;
        StringBuffer sb = new StringBuffer();
        String del = "";
        for ( Iterator i = licenses.iterator(); i.hasNext(); )
        {
            License l = ( License ) i.next();
            String url = l.getUrl();
            if ( url == null )
                continue;
            sb.append( del );
            sb.append( url );
            del = ", ";
        }
        if ( sb.length() == 0 )
            return null;
        return sb;
    }


    /**
     * @param jar
     * @throws IOException
     */
    private void doMavenMetadata( MavenProject currentProject, Jar jar ) throws IOException
    {
        String path = "META-INF/maven/" + currentProject.getGroupId() + "/" + currentProject.getArtifactId();
        File pomFile = new File( baseDir, "pom.xml" );
        jar.putResource( path + "/pom.xml", new FileResource( pomFile ) );

        Properties p = new Properties();
        p.put( "version", currentProject.getVersion() );
        p.put( "groupId", currentProject.getGroupId() );
        p.put( "artifactId", currentProject.getArtifactId() );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        p.store( out, "Generated by org.apache.felix.bundleplugin" );
        jar
            .putResource( path + "/pom.properties",
                new EmbeddedResource( out.toByteArray(), System.currentTimeMillis() ) );
    }


    /**
     * @return
     * @throws ZipException
     * @throws IOException
     */
    protected Jar[] getClasspath( MavenProject currentProject ) throws ZipException, IOException
    {
        List list = new ArrayList();

        if ( getOutputDirectory() != null && getOutputDirectory().exists() )
        {
            list.add( new Jar( ".", getOutputDirectory() ) );
        }

        final Collection artifacts = getSelectedDependencies( currentProject.getArtifacts() );
        for ( Iterator it = artifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = ( Artifact ) it.next();
            if ( artifact.getArtifactHandler().isAddedToClasspath() )
            {
                if ( Artifact.SCOPE_COMPILE.equals( artifact.getScope() )
                    || Artifact.SCOPE_SYSTEM.equals( artifact.getScope() )
                    || Artifact.SCOPE_PROVIDED.equals( artifact.getScope() ) )
                {
                    File file = getFile( artifact );
                    if ( file == null )
                    {
                        getLog().warn(
                            "File is not available for artifact " + artifact + " in project "
                                + currentProject.getArtifact() );
                        continue;
                    }
                    Jar jar = new Jar( artifact.getArtifactId(), file );
                    list.add( jar );
                }
            }
        }
        Jar[] cp = new Jar[list.size()];
        list.toArray( cp );
        return cp;
    }


    private Collection getSelectedDependencies( Set artifacts )
    {
        if ( null == excludeDependencies || excludeDependencies.length() == 0 )
        {
            return artifacts;
        }
        else if ( "true".equalsIgnoreCase( excludeDependencies ) )
        {
            return Collections.EMPTY_LIST;
        }

        List excludes = Arrays.asList( excludeDependencies.trim().split( "\\s*,\\s*" ) );

        Collection classpath = new ArrayList();
        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = ( Artifact ) i.next();
            if ( !excludes.contains( artifact.getArtifactId() ) )
            {
                classpath.add( artifact );
            }
        }

        return classpath;
    }


    /**
     * Get the file for an Artifact
     *
     * @param artifact
     */
    protected File getFile( Artifact artifact )
    {
        return artifact.getFile();
    }


    private static void header( Properties properties, String key, Object value )
    {
        if ( value == null )
            return;

        if ( value instanceof Collection && ( ( Collection ) value ).isEmpty() )
            return;

        properties.put( key, value.toString().replaceAll( "[\r\n]", "" ) );
    }


    /**
     * Convert a Maven version into an OSGi compliant version
     *
     * @param version Maven version
     * @return the OSGi version
     */
    protected String convertVersionToOsgi( String version )
    {
        return getMaven2OsgiConverter().getVersion( version );
    }


    /**
     * TODO this should return getMaven2Osgi().getBundleFileName( project.getArtifact() )
     */
    protected String getBundleName( MavenProject currentProject )
    {
        String finalName = currentProject.getBuild().getFinalName();
        if ( null != classifier && classifier.trim().length() > 0 )
        {
            return finalName + '-' + classifier + ".jar";
        }

        return finalName + ".jar";
    }


    protected String getBuildDirectory()
    {
        return buildDirectory;
    }


    protected void setBuildDirectory( String _buildirectory )
    {
        buildDirectory = _buildirectory;
    }


    protected Properties getDefaultProperties( MavenProject currentProject )
    {
        Properties properties = new Properties();

        String bsn;
        try
        {
            bsn = getMaven2OsgiConverter().getBundleSymbolicName( currentProject.getArtifact() );
        }
        catch ( Exception e )
        {
            bsn = currentProject.getGroupId() + "." + currentProject.getArtifactId();
        }

        // Setup defaults
        properties.put( Analyzer.BUNDLE_SYMBOLICNAME, bsn );
        properties.put( Analyzer.IMPORT_PACKAGE, "*" );
        properties.put( Analyzer.BUNDLE_VERSION, currentProject.getVersion() );

        // remove the verbose Include-Resource entry from generated manifest
        properties.put( Analyzer.REMOVE_HEADERS, Analyzer.INCLUDE_RESOURCE );

        header( properties, Analyzer.BUNDLE_DESCRIPTION, currentProject.getDescription() );
        StringBuffer licenseText = printLicenses( currentProject.getLicenses() );
        if ( licenseText != null )
        {
            header( properties, Analyzer.BUNDLE_LICENSE, licenseText );
        }
        header( properties, Analyzer.BUNDLE_NAME, currentProject.getName() );

        if ( currentProject.getOrganization() != null )
        {
            header( properties, Analyzer.BUNDLE_VENDOR, currentProject.getOrganization().getName() );
            if ( currentProject.getOrganization().getUrl() != null )
            {
                header( properties, Analyzer.BUNDLE_DOCURL, currentProject.getOrganization().getUrl() );
            }
        }

        properties.putAll( currentProject.getProperties() );
        properties.putAll( currentProject.getModel().getProperties() );
        properties.putAll( getProperties( currentProject.getModel(), "project.build." ) );
        properties.putAll( getProperties( currentProject.getModel(), "pom." ) );
        properties.putAll( getProperties( currentProject.getModel(), "project." ) );
        properties.put( "project.baseDir", baseDir );
        properties.put( "project.build.directory", getBuildDirectory() );
        properties.put( "project.build.outputdirectory", getOutputDirectory() );

        return properties;
    }


    protected void setBasedir( File _basedir )
    {
        baseDir = _basedir;
    }


    protected File getOutputDirectory()
    {
        return outputDirectory;
    }


    protected void setOutputDirectory( File _outputDirectory )
    {
        outputDirectory = _outputDirectory;
    }


    private static String getMavenResourcePaths( MavenProject project )
    {
        final String basePath = project.getBasedir().getAbsolutePath();

        StringBuffer resourcePaths = new StringBuffer();
        for ( Iterator i = project.getResources().iterator(); i.hasNext(); )
        {
            org.apache.maven.model.Resource resource = ( org.apache.maven.model.Resource ) i.next();

            final String sourcePath = resource.getDirectory();
            final String targetPath = resource.getTargetPath();

            // ignore empty or non-local resources
            if ( new File( sourcePath ).exists() && ( ( targetPath == null ) || ( targetPath.indexOf( ".." ) < 0 ) ) )
            {
                DirectoryScanner scanner = new DirectoryScanner();

                scanner.setBasedir( resource.getDirectory() );
                if ( resource.getIncludes() != null && !resource.getIncludes().isEmpty() )
                {
                    scanner.setIncludes( ( String[] ) resource.getIncludes().toArray( EMPTY_STRING_ARRAY ) );
                }
                else
                {
                    scanner.setIncludes( DEFAULT_INCLUDES );
                }

                if ( resource.getExcludes() != null && !resource.getExcludes().isEmpty() )
                {
                    scanner.setExcludes( ( String[] ) resource.getExcludes().toArray( EMPTY_STRING_ARRAY ) );
                }

                scanner.addDefaultExcludes();
                scanner.scan();

                List includedFiles = Arrays.asList( scanner.getIncludedFiles() );

                for ( Iterator j = includedFiles.iterator(); j.hasNext(); )
                {
                    String name = ( String ) j.next();
                    String path = sourcePath + '/' + name;

                    // make relative to project
                    if ( path.startsWith( basePath ) )
                    {
                        if ( path.length() == basePath.length() )
                        {
                            path = ".";
                        }
                        else
                        {
                            path = path.substring( basePath.length() + 1 );
                        }
                    }

                    // replace windows backslash with a slash
                    // this is a workaround for a problem with bnd 0.0.189
                    if ( File.separatorChar != '/' )
                    {
                        name = name.replace( File.separatorChar, '/' );
                        path = path.replace( File.separatorChar, '/' );
                    }

                    // copy to correct place
                    path = name + '=' + path;
                    if ( targetPath != null )
                    {
                        path = targetPath + '/' + path;
                    }

                    if ( resourcePaths.length() > 0 )
                    {
                        resourcePaths.append( ',' );
                    }

                    if ( resource.isFiltering() )
                    {
                        resourcePaths.append( '{' );
                        resourcePaths.append( path );
                        resourcePaths.append( '}' );
                    }
                    else
                    {
                        resourcePaths.append( path );
                    }
                }
            }
        }

        return resourcePaths.toString();
    }


    protected static Collection getEmbeddableArtifacts( MavenProject project, Properties properties )
    {
        String embedTransitive = properties.getProperty( DependencyEmbedder.EMBED_TRANSITIVE );
        if ( Boolean.valueOf( embedTransitive ).booleanValue() )
        {
            // includes transitive dependencies
            return project.getArtifacts();
        }

        // only includes direct dependencies
        return project.getDependencyArtifacts();
    }
}
