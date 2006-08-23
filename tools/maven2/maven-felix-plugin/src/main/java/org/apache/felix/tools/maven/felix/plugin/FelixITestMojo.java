/*
 *   Copyright 2006 The Apache Software Foundation
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
package org.apache.felix.tools.maven.felix.plugin;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.felix.framework.util.MutablePropertyResolver;
import org.apache.felix.framework.util.MutablePropertyResolverImpl;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import org.codehaus.plexus.util.FileUtils;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;


/**
 * 
 * @goal test
 * @phase integration-test
 */
public class FelixITestMojo extends AbstractMojo
{
    /**
     * The name of the test bundle activator.
     * 
     * @parameter expression=”${run.felix.test.activator}”
     * @required
     * @description the fqcn of the test activator
     */
    private String felixTestActivator;
    
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
    

    public void execute() throws MojoExecutionException, MojoFailureException
    {
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

        System.out.println();
        System.out.println( "-------------------------------------------------------" );
        System.out.println( "   F E L I X   I N T E G R A T I O N   T E S T I N G" );
        System.out.println( "-------------------------------------------------------" );
        System.out.println( "Running " + felixTestActivator );
        
        boolean keepRunning = true;
        int count = 0;
        List testRuns = new ArrayList();
        long startTime = System.currentTimeMillis();
        int errors = 0;
        int failures = 0; 
        while ( keepRunning )
        {
            FelixContainer felixContainer = new FelixContainer();
            
            try
            {
                Thread.sleep( 1000 );
            }
            catch ( InterruptedException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            
            TestRun testRun = new TestRun();
            testRuns.add( testRun );
            testRun.setStartTime( System.currentTimeMillis() );
            
            // ---------------------------------------------------------------
            // Clean out the old cache directory if it exists
            // ---------------------------------------------------------------
            
            if ( felixCacheDir.exists() )
            {
                try
                {
                    FileUtils.forceDelete( felixCacheDir );
                }
                catch ( IOException e )
                {
                    testRun.setThrowable( e );
                    testRun.setEndTime( System.currentTimeMillis() );
                    testRun.setError( true );
                    testRun.setFailure( false );
                    felixContainer.shutdown();
                    count++;
                    errors++;
                    e.printStackTrace();
                    continue;
                }
            }

            // ---------------------------------------------------------------
            // Startup the Felix container and access our bundle
            // ---------------------------------------------------------------
            
            MutablePropertyResolver resolver = new MutablePropertyResolverImpl(props);
            felixContainer.start( resolver, new ArrayList() );
            String artifactLocation = null;
            try
            {
                artifactLocation = project.getArtifact().getFile().toURL().toString();
            }
            catch ( MalformedURLException e )
            {
                testRun.setThrowable( e );
                testRun.setEndTime( System.currentTimeMillis() );
                testRun.setError( true );
                testRun.setFailure( false );
                felixContainer.shutdown();
                count++;
                errors++;
                e.printStackTrace();
                continue;
            }
            Bundle bundle = felixContainer.getBundle( artifactLocation );

            // ---------------------------------------------------------------
            // Analyze methods of our Bundle's BundleActivator looking for 
            // public nonstatic nonabstract methods that start with "test"
            // ---------------------------------------------------------------
            
            List testMethods = new ArrayList();
            try
            {
                Class clazz = bundle.loadClass( felixTestActivator );
                Method[] methods = clazz.getMethods();
                for ( int ii = 0; ii < methods.length; ii++ )
                {
                    int modifiers = methods[ii].getModifiers();
                    boolean isPublic = Modifier.isPublic( modifiers );
                    boolean isAbstract = Modifier.isAbstract( modifiers );
                    boolean isStatic = Modifier.isStatic( modifiers );
                    boolean startsWithTest = methods[ii].getName().startsWith( "test" );
                    
                    if ( isPublic && ! isAbstract && ! isStatic && startsWithTest )
                    {
                        testMethods.add( methods[ii] );
                    }
                }
            }
            catch ( ClassNotFoundException e )
            {
                testRun.setThrowable( e );
                testRun.setEndTime( System.currentTimeMillis() );
                testRun.setError( true );
                testRun.setFailure( false );
                felixContainer.shutdown();
                count++;
                errors++;
                e.printStackTrace();
                continue;
            }
            
            if ( count > testMethods.size() -1 )
            {
                keepRunning = false;
                continue;
            }

            // ---------------------------------------------------------------
            // No test methods found in this bundle so we just exist the loop
            // ---------------------------------------------------------------

            if ( testMethods.size() < 0 )
            {
                felixContainer.shutdown();
                count++;
                break;
            }

            // ---------------------------------------------------------------
            // Test methods found!  So let's get the method and invoke it
            // ---------------------------------------------------------------

            Method meth = ( Method ) testMethods.get( count );
            System.out.println( "Running integration test method: " + meth.getName() + "()" );
            try
            {
                BundleActivator activator = felixContainer.getBundleActivator( bundle );
                System.out.println( "ACTIVATOR = " + activator );
                meth.invoke( activator, new Object[] {} );
            }
            catch ( Exception e )
            {
                testRun.setThrowable( e );
                testRun.setEndTime( System.currentTimeMillis() );
                testRun.setError( true );
                testRun.setFailure( false );
                felixContainer.shutdown();
                count++;
                failures++;
                e.printStackTrace();
                continue;
            }

            felixContainer.shutdown();
            count++;
            if ( count > testMethods.size() -1 )
            {
                keepRunning = false;
            }
        }
        
        StringBuffer buf = new StringBuffer();
        buf.append( "Tests run: " ).append( testRuns.size() );
        buf.append( ", Failures: " ).append( failures );
        buf.append( ", Errors: " ).append( errors );
        buf.append( ", Skipped: 0, Time elapsed: ");
        double timeElapsed = ( System.currentTimeMillis() - startTime );
        timeElapsed = timeElapsed / 1000;
        buf.append( timeElapsed );
        buf.append( " sec" );
        
        System.out.println( buf.toString() );
        
        if ( errors + failures > 0 )
        {
            throw new MojoFailureException( "Errors and failures encountered during execution" );
        }
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
