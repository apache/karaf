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
import java.util.Collections;

import org.apache.maven.plugin.testing.stubs.ArtifactStub;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

/**
 * Test for {@link BundleAllPlugin}
 * 
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public class BundleAllPluginTest
    extends AbstractBundlePluginTest
{

    private BundleAllPlugin plugin;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        init();
    }

    private void init()
    {
        plugin = new BundleAllPlugin();
        File basedir = new File( getBasedir() );
        plugin.setBasedir( basedir );
        File buildDirectory = new File( basedir, "target" );
        plugin.setBuildDirectory( buildDirectory.getPath() );
        File outputDirectory = new File( buildDirectory, "classes" );
        plugin.setOutputDirectory( outputDirectory );
    }

    public void testSnapshotMatch()
    {
        ArtifactStub artifact = getArtifactStub();
        String bundleName;

        artifact.setVersion( "2.1-SNAPSHOT" );
        bundleName = "group.artifact_2.1.0.20070207_193904_2.jar";

        assertTrue( plugin.snapshotMatch( artifact, bundleName ) );

        artifact.setVersion( "2-SNAPSHOT" );
        assertFalse( plugin.snapshotMatch( artifact, bundleName ) );

        artifact.setArtifactId( "artifactx" );
        artifact.setVersion( "2.1-SNAPSHOT" );
        assertFalse( plugin.snapshotMatch( artifact, bundleName ) );
    }

//    public void testRewriting()
//        throws Exception
//    {
//
//        MavenProjectStub project = new MavenProjectStub();
//        project.setArtifact( getArtifactStub() );
//        project.getArtifact().setFile( getTestBundle() );
//        project.setDependencyArtifacts( Collections.EMPTY_SET );
//        project.setVersion( project.getArtifact().getVersion() );
//
//        File output = new File( plugin.getBuildDirectory(), plugin.getBundleName( project ) );
//        boolean delete = output.delete();
//
//        plugin.bundle( project );
//
//        init();
//        try
//        {
//            plugin.bundle( project );
//            fail();
//        }
//        catch ( RuntimeException e )
//        {
//            // expected
//        }
//    }
}
