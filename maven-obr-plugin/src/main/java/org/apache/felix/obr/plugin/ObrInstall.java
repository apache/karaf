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
package org.apache.felix.obr.plugin;

import java.io.File;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

/**
 * construct the repository.xml with a project Maven compiled
 *
 * @goal repository
 * @phase install
 * @requiresDependencyResolution compile
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ObrInstall extends AbstractMojo {
    /**
     * The local Maven repository.
     * 
     * @parameter expression="${localRepository}"
     * @required
     */
    private ArtifactRepository m_localRepo;

    /**
     * path to the repository.xml.
     * 
     * @parameter expression="${repository-path}"
     * @require
     */
    private String m_repositoryPath;

    /**
     * Project in use.
     * 
     * @parameter expression="${project}"
     * @require
     */

    private MavenProject m_project;

    /**
     * setting of maven.
     * 
     * @parameter expression="${settings}"
     * @require
     */

    private Settings m_settings;
    
    /**
     * Enable/Disable this goal
     * @description If true evrything the goal do nothing, the goal just skip over 
     * @parameter expression="${maven.obr.installToLocalOBR}" default-value="true"
     */
    private boolean installToLocalOBR;    
    

    /**
     * path to file in the maven local repository.
     */
    private String m_fileInLocalRepo;

    /**
     * main method for this goal.
     * @implements org.apache.maven.plugin.Mojo.execute 
     * @throws MojoExecutionException if the plugin failed
     */
    public void execute() throws MojoExecutionException {
        getLog().info("Obr Plugin starts:");
        if(!installToLocalOBR)
        {
        	getLog().info("maven-obr-plugin:repository goal is disable due to one of the following reason:");
        	getLog().info(" - 'installToLocalOBR' configuration set to false");
        	getLog().info(" - JVM property maven.obr.installToLocalOBR set to false");
        	return;
        }
        
        if (m_repositoryPath == null) {
            m_repositoryPath = "file:/" + m_localRepo.getBasedir() + File.separator + "repository.xml";
            getLog().warn("-DpathRepo is not define, use default repository: " + m_repositoryPath);
        }

        PathFile file = new PathFile(m_repositoryPath);
        if (file.isExists()) {
            if (!m_repositoryPath.startsWith("file:/")) { m_repositoryPath = "file:/" + m_repositoryPath; }
        }

        // locate the obr.xml file
        String obrXmlFile = null;
        List l = m_project.getResources();
        for (int i = 0; i < l.size(); i++) {
            File f = new File(((Resource) l.get(i)).getDirectory() + File.separator + "obr.xml");
            if (f.exists()) {
                obrXmlFile = ((Resource) l.get(i)).getDirectory() + File.separator + "obr.xml";
                break;
            }
        }
        // the obr.xml file is not present
        if (obrXmlFile == null) {
            getLog().warn("obr.xml is not present, use default");
        }

        // get the path to local maven repository
        file = new PathFile(PathFile.uniformSeparator(m_settings.getLocalRepository()) + 
        		File.separator + PathFile.uniformSeparator(m_localRepo.pathOf(m_project.getArtifact())));
	
        if (file.isExists()) {
            m_fileInLocalRepo = file.	getOnlyAbsoluteFilename();
        } else {
            getLog().error("file not found in local repository: " + m_settings.getLocalRepository() + File.separator + m_localRepo.pathOf(m_project.getArtifact()));
		getLog().error("file not found in local repository: " 
				+ m_localRepo.getBasedir() + File.separator + m_localRepo.pathOf(m_project.getArtifact()));
            return;
        }

        // verify the repository.xml
        PathFile fileRepo = new PathFile(m_repositoryPath);
        if (fileRepo.isRelative()) { fileRepo.setBaseDir(m_settings.getLocalRepository()); }

        // create the folder to the repository
        PathFile repoExist = new PathFile(fileRepo.getAbsolutePath());
        if (!repoExist.isExists()) { fileRepo.createPath(); }

        // build the user configuration (use default)
        Config user = new Config();

    	getLog().debug("Maven2 Local File repository = "+fileRepo.getAbsoluteFilename());
    	getLog().debug("OBR repository = "+obrXmlFile);
	
        ObrUpdate obrUpdate = new ObrUpdate(fileRepo, obrXmlFile, m_project, m_fileInLocalRepo, PathFile.uniformSeparator(m_settings.getLocalRepository()), user, getLog());
        try {
            obrUpdate.updateRepository();
        } catch (MojoExecutionException e) {
            e.printStackTrace();
            throw new MojoExecutionException("MojoFailureException");
        }
    }

}
