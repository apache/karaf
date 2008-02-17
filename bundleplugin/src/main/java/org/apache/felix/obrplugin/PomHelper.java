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
package org.apache.felix.obrplugin;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;


/**
 * Maven POM helper methods.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public final class PomHelper
{
    public static MavenProject readPom( File pomFile ) throws MojoExecutionException
    {
        Reader reader = null;

        try
        {
            reader = new FileReader( pomFile );
            MavenXpp3Reader modelReader = new MavenXpp3Reader();
            return new MavenProject( modelReader.read( reader ) );
        }
        catch ( FileNotFoundException e )
        {
            throw new MojoExecutionException( "Error reading specified POM file: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error reading specified POM file: " + e.getMessage(), e );
        }
        catch ( XmlPullParserException e )
        {
            throw new MojoExecutionException( "Error reading specified POM file: " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }


    public static MavenProject buildPom( String groupId, String artifactId, String version, String packaging )
    {
        Model model = new Model();

        model.setModelVersion( "4.0.0" );
        model.setGroupId( groupId );
        model.setArtifactId( artifactId );
        model.setVersion( version );
        model.setPackaging( packaging );

        return new MavenProject( model );
    }
}
