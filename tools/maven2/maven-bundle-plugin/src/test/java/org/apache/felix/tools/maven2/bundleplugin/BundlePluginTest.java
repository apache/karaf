package org.apache.felix.tools.maven2.bundleplugin;

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
import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;

import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Jar;

/**
 * Test for {@link BundlePlugin}.
 * 
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public class BundlePluginTest
    extends AbstractBundlePluginTest
{

    private BundlePlugin plugin;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        plugin = new BundlePlugin();
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
        assertEquals( "2", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "2.1" );
        assertEquals( "2.1", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "2.1.3" );
        assertEquals( "2.1.3", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "2.1.3.4" );
        assertEquals( "2.1.3.4", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "4aug2000r7-dev" );
        assertEquals( "0.0.0.4aug2000r7_dev", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "1.1-alpha-2" );
        assertEquals( "1.1.0.alpha_2", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "1.0-alpha-16-20070122.203121-13" );
        assertEquals( "1.0.0.alpha_16_20070122_203121_13", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "1.0-20070119.021432-1" );
        assertEquals( "1.0.0.20070119_021432_1", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "1-20070119.021432-1" );
        assertEquals( "1.0.0.20070119_021432_1", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "1.4.1-20070217.082013-7" );
        assertEquals( "1.4.1.20070217_082013_7", osgiVersion );
    }

    public void testReadExportedModules()
        throws Exception
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
        analyzer.setClasspath( new Jar[] { jar } );

        analyzer.setProperty( Analyzer.EXPORT_PACKAGE, "*" );
        analyzer.calcManifest();

        assertEquals( 3, analyzer.getExports().size() );
    }

    public void testGetPackages()
        throws Exception
    {
        File jarFile = getTestFile( "target/test-jar.jar" );

        createTestJar( jarFile );

        Jar jar = new Jar( "testJar", jarFile );
        List packages = plugin.getPackages( jar );

        assertEquals( 4, packages.size() );
        int i = 0;
        assertEquals( "META-INF", packages.get( i++ ) );
        assertEquals( "META-INF.maven.org.apache.maven.plugins.maven-bundle-plugin", packages.get( i++ ) );
        assertEquals( "org.apache.maven.test", packages.get( i++ ) );
        assertEquals( "org.apache.maven.test.resources", packages.get( i++ ) );
    }

    private void createTestJar( File jarFile )
        throws ArchiverException, IOException
    {
        JarArchiver archiver = new JarArchiver();
        archiver
            .addFile( getTestFile( "target/classes/" + BundlePlugin.class.getName().replace( '.', '/' ) + ".class" ),
                      "org/apache/maven/test/BundlePlugin.class" );
        archiver.addFile( getTestFile( "pom.xml" ),
                          "META-INF/maven/org.apache.maven.plugins/maven-bundle-plugin/pom.xml" );
        archiver.addFile( getTestFile( "pom.xml" ), "org/apache/maven/test/resources/someresource" );
        archiver.setDestFile( jarFile );
        archiver.createArchive();
    }
}
