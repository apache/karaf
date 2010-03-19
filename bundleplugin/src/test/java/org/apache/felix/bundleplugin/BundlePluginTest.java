package org.apache.felix.bundleplugin;


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

import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import java.util.Properties;

import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.osgi.DefaultMaven2OsgiConverter;
import org.apache.maven.model.Organization;

import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Jar;


/**
 * Test for {@link BundlePlugin}.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class BundlePluginTest extends AbstractBundlePluginTest
{

    private BundlePlugin plugin;


    protected void setUp() throws Exception
    {
        super.setUp();
        plugin = new BundlePlugin();
        plugin.setMaven2OsgiConverter( new DefaultMaven2OsgiConverter() );
        plugin.setBasedir( new File( "." ) );
        plugin.setBuildDirectory( "." );
        plugin.setOutputDirectory( new File( "." ) );
    }


    public void testConvertVersionToOsgi()
    {
        String osgiVersion;

        osgiVersion = plugin.convertVersionToOsgi( "2.1.0-SNAPSHOT" );
        assertEquals( "2.1.0.SNAPSHOT", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "2.1-SNAPSHOT" );
        assertEquals( "2.1.0.SNAPSHOT", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "2-SNAPSHOT" );
        assertEquals( "2.0.0.SNAPSHOT", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "2" );
        assertEquals( "2.0.0", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "2.1" );
        assertEquals( "2.1.0", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "2.1.3" );
        assertEquals( "2.1.3", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "2.1.3.4" );
        assertEquals( "2.1.3.4", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "4aug2000r7-dev" );
        assertEquals( "0.0.0.4aug2000r7-dev", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "1.1-alpha-2" );
        assertEquals( "1.1.0.alpha-2", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "1.0-alpha-16-20070122.203121-13" );
        assertEquals( "1.0.0.alpha-16-20070122_203121-13", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "1.0-20070119.021432-1" );
        assertEquals( "1.0.0.20070119_021432-1", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "1-20070119.021432-1" );
        assertEquals( "1.0.0.20070119_021432-1", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "1.4.1-20070217.082013-7" );
        assertEquals( "1.4.1.20070217_082013-7", osgiVersion );
    }


    public void testReadExportedModules() throws Exception
    {
        File osgiBundleFile = getTestBundle();

        assertTrue( osgiBundleFile.exists() );

        MavenProject project = new MavenProjectStub();
        project.setGroupId( "group" );
        project.setArtifactId( "artifact" );
        project.setVersion( "1.1.0.0" );

        PackageVersionAnalyzer analyzer = new PackageVersionAnalyzer();
        Jar jar = new Jar( "name", osgiBundleFile );
        analyzer.setJar( jar );
        analyzer.setClasspath( new Jar[]
            { jar } );

        analyzer.setProperty( Analyzer.EXPORT_PACKAGE, "*" );
        analyzer.calcManifest();

        assertEquals( 3, analyzer.getExports().size() );

        analyzer.close();
    }


    public void testTransformDirectives() throws Exception
    {
        Map instructions = new TreeMap();

        instructions.put( "a", "1" );
        instructions.put( "-a", "2" );
        instructions.put( "_a", "3" );
        instructions.put( "A", "3" );
        instructions.put( "_A", "1" );
        instructions.put( "_b", "4" );
        instructions.put( "b", "6" );
        instructions.put( "_B", "6" );
        instructions.put( "-B", "5" );
        instructions.put( "B", "4" );

        instructions.put( "z", null );
        instructions.put( "_z", null );

        Map transformedInstructions = BundlePlugin.transformDirectives( instructions );

        assertEquals( "1", transformedInstructions.get( "a" ) );
        assertEquals( "3", transformedInstructions.get( "-a" ) );
        assertEquals( null, transformedInstructions.get( "_a" ) );
        assertEquals( "3", transformedInstructions.get( "A" ) );
        assertEquals( "1", transformedInstructions.get( "-A" ) );
        assertEquals( null, transformedInstructions.get( "_A" ) );
        assertEquals( null, transformedInstructions.get( "_b" ) );
        assertEquals( "4", transformedInstructions.get( "-b" ) );
        assertEquals( "6", transformedInstructions.get( "b" ) );
        assertEquals( null, transformedInstructions.get( "_B" ) );
        assertEquals( "6", transformedInstructions.get( "-B" ) );
        assertEquals( "4", transformedInstructions.get( "B" ) );

        assertEquals( "", transformedInstructions.get( "z" ) );
        assertEquals( "", transformedInstructions.get( "-z" ) );
    }

    public void testDefaultPropertiesIncludeOrganization()
    {
        final Organization organization = new Organization();
        organization.setName( "Example Organization" );
        organization.setUrl( "http://example.org" );

        // MavenProjectStub.setOrganization(Organization) doesn't do anything, so we have to make it work this way
        MavenProject project = new MavenProjectStub()
        {
            @Override public Organization getOrganization()
            {
                return organization;
            }
        };
        project.setGroupId( "group" );
        project.setArtifactId( "artifact" );
        project.setVersion( "1.1.0.0" );

        Properties properties = plugin.getDefaultProperties( project );
        assertEquals( organization.getName(), properties.getProperty( "project.organization.name" ) );
        assertEquals( organization.getName(), properties.getProperty( "pom.organization.name" ) );
        assertEquals( organization.getUrl(), properties.getProperty( "project.organization.url" ) );
        assertEquals( organization.getUrl(), properties.getProperty( "pom.organization.url" ) );
    }

    public void testVersion() throws Exception
    {
        String cleanupVersion = Builder.cleanupVersion( "0.0.0.4aug2000r7-dev" );
        assertEquals( "0.0.0.4aug2000r7-dev", cleanupVersion );
    }

}
