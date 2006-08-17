package org.apache.felix.tools.maven.felix.plugin;


import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.MutablePropertyResolver;
import org.apache.felix.framework.util.MutablePropertyResolverImpl;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;


/**
 * Starts up Felix, installs this module's bundle artifact with dependent 
 * bundles, and starts them.
 * 
 * @goal run
 * @phase integration-test
 */
public class FelixRunMojo extends AbstractMojo
{
    /**
     * The name of the felix cache profile.
     * 
     * @parameter expression=”${run.felix.cache.profile}” default-value="test"
     * @description the felix.cache.profile property value
     */
    private String felixCacheProfile;
    
    /**
     * The location of the felix bundle cache directory.
     * 
     * @parameter expression=”${run.felix.cache.dir}” default-value="${basedir}/target/.felix"
     * @description the felix.cache.dir property value
     */
    private File felixCacheDir;
    
    /**
     * The location of the test bundle.
     * 
     * @parameter expression="${run.felix.test.bundle}" 
     *            default-value="${basedir}/target/${project.artifactId}-${project.version}.jar"
     */
    private File felixTestBundle;

    /**
     * @parameter default-value="${project}"
     */
    private MavenProject project;

    /**
     * @parameter
     */
    private List exclusions;
    
    /**
     * The felix container used to run the integration tests.
     */
    private Felix felixContainer = new Felix();


    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // -------------------------------------------------------------------
        // Clean out the old cache directory if it exists
        // -------------------------------------------------------------------
        
        if ( felixCacheDir.exists() )
        {
            try
            {
                FileUtils.forceDelete( felixCacheDir );
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "failed to delete old Felix cache directory: " 
                    + felixCacheDir.getAbsolutePath() );
            }
        }

        // -------------------------------------------------------------------
        // Build the properties we will stuff into the resolver
        // -------------------------------------------------------------------

        Properties props = new Properties();
        props.put( "felix.cache.dir", felixCacheDir.getAbsolutePath() );
        props.put( "felix.cache.profile", felixCacheProfile );
        props.put( "felix.embedded.execution", "true" );
        props.put( "org.osgi.framework.system.packages", 
            "org.osgi.framework; version=1.3.0, " +
            "org.osgi.service.packageadmin; version=1.2.0, " +
            "org.osgi.service.startlevel; version=1.0.0, " +
            "org.osgi.service.url; version=1.0.0, " +
            "${jre-${java.specification.version}}" );
        try
        {
            props.put( "felix.auto.start.1", getAutoStart() );
        }
        catch ( MalformedURLException e )
        {
            e.printStackTrace();
        }
        
        // -------------------------------------------------------------------
        // Start up Felix with resolver and shut it down
        // -------------------------------------------------------------------

        MutablePropertyResolver resolver = new MutablePropertyResolverImpl(props);
        felixContainer.start( resolver, new ArrayList() );
        getLog().info( "-=============================-" );
        getLog().info( "| Felix: successfully started |" );
        getLog().info( "-=============================-" );
        felixContainer.shutdown();
        getLog().info( "-==============================-" );
        getLog().info( "| Felix: successfully shutdown |" );
        getLog().info( "-==============================-" );
    }
    

    public String getAutoStart() throws MalformedURLException
    {
        List included = new ArrayList();
        List excluded = new ArrayList();
        if ( exclusions == null )
        {
            exclusions = Collections.EMPTY_LIST;
        }
        
        StringBuffer buf = new StringBuffer();
        StringBuffer keyBuf = new StringBuffer();
        for ( Iterator ii = project.getDependencyArtifacts().iterator(); ii.hasNext(); /**/) 
        {
            Artifact dep = ( Artifact ) ii.next();
            keyBuf.setLength( 0 );
            keyBuf.append( dep.getGroupId() ).append( ":" ).append( dep.getArtifactId() );
            String depKey = keyBuf.toString();
            
            // -------------------------------------------------------------------
            // Add only provided dependent artifacts that have not been excluded
            // -------------------------------------------------------------------
            
            if ( dep.getScope().equalsIgnoreCase( "provided" ) )
            {
                if ( dep.getArtifactId().equalsIgnoreCase( "org.osgi.core" ) ||
                     exclusions.contains( depKey ) )
                {
                    excluded.add( depKey );
                    continue;
                }
                
                included.add( depKey );
                buf.append( dep.getFile().toURL() );
                buf.append( " " );
            }
        }
        
        keyBuf.setLength( 0 );
        keyBuf.append( project.getGroupId() ).append( ":" ).append( project.getArtifactId() );
        String depKey = keyBuf.toString();
        included.add( depKey );
        buf.append( felixTestBundle.toURL() );
        
        // -------------------------------------------------------------------
        // Report what was included and what was excluded
        // -------------------------------------------------------------------
        
        getLog().info( "" );
        getLog().info( "\t\tJars/Bundles Included for Autostart" ); 
        getLog().info( "\t\t-----------------------------------" );
        getLog().info( "" );
        for ( Iterator ii = included.iterator(); ii.hasNext(); /**/ )
        {
            getLog().info( "\t\t" + ( String ) ii.next() );
        }
        getLog().info( "" );
        getLog().info( "" );
        getLog().info( "\t\tJars/Bundles Excluded from Autostart" );
        getLog().info( "\t\t------------------------------------" );
        getLog().info( "" );
        for ( Iterator ii = excluded.iterator(); ii.hasNext(); /**/ )
        {
            getLog().info( "\t\t" + ( String ) ii.next() );
        }
        getLog().info( "" );
        getLog().info( "" );
        return buf.toString();
    }
}
