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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;


/**
 * Base class for the command-line install-file and deploy-file goals.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class AbstractFileMojo extends AbstractMojo
{
    /**
     * GroupId of the bundle. Retrieved from POM file if specified.
     *
     * @parameter expression="${groupId}"
     */
    private String groupId;

    /**
     * ArtifactId of the bundle. Retrieved from POM file if specified.
     *
     * @parameter expression="${artifactId}"
     */
    private String artifactId;

    /**
     * Version of the bundle. Retrieved from POM file if specified.
     *
     * @parameter expression="${version}"
     */
    private String version;

    /**
     * Packaging type of the bundle. Retrieved from POM file if specified.
     *
     * @parameter expression="${packaging}"
     */
    private String packaging;

    /**
     * Classifier type of the bundle. Defaults to none.
     *
     * @parameter expression="${classifier}"
     */
    private String classifier;

    /**
     * Location of an existing POM file.
     *
     * @parameter expression="${pomFile}"
     */
    private File pomFile;

    /**
     * Bundle file, defaults to the artifact in the local Maven repository.
     *
     * @parameter expression="${file}"
     */
    protected File file;

    /**
     * Optional XML file describing additional requirements and capabilities.
     * 
     * @parameter expression="${obrXml}"
     */
    protected String obrXml;

    /**
     * Component factory for Maven artifacts
     * 
     * @component
     */
    private ArtifactFactory m_factory;


    /**
     * @return project based on command-line settings, with bundle attached
     * @throws MojoExecutionException
     */
    public MavenProject getProject() throws MojoExecutionException
    {
        final MavenProject project;
        if ( pomFile != null && pomFile.exists() )
        {
            project = PomHelper.readPom( pomFile );

            groupId = project.getGroupId();
            artifactId = project.getArtifactId();
            version = project.getVersion();
            packaging = project.getPackaging();
        }
        else
        {
            project = PomHelper.buildPom( groupId, artifactId, version, packaging );
        }

        if ( groupId == null || artifactId == null || version == null || packaging == null )
        {
            throw new MojoExecutionException( "Missing group, artifact, version, or packaging information" );
        }

        Artifact bundle = m_factory.createArtifactWithClassifier( groupId, artifactId, version, packaging, classifier );
        project.setArtifact( bundle );

        return project;
    }
}
