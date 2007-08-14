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
package org.apache.felix.ipojo.plugin;

import java.io.File;

import org.apache.felix.ipojo.manipulator.Pojoization;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Package an OSGi jar "bundle" as an "iPOJO bundle".
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 * @version $Rev$, $Date$
 * @goal ipojo-bundle
 * @phase package
 * @requiresDependencyResolution runtime
 * @description manipulate an OSGi bundle jar to build an iPOJO bundle
 */
public class ManipulatorMojo extends AbstractMojo {

    /**
     * The directory for the generated JAR.
     * 
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private String m_buildDirectory;

    /**
     * The directory containing generated classes.
     * 
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    private File m_outputDirectory;

    /**
     * The name of the generated JAR file.
     * 
     * @parameter alias="jarName" expression="${project.build.finalName}"
     * @required
     */
    private String m_jarName;
    
    /**
     * Metadata file location.
     * @parameter expression="${metadata}" default-value="metadata.xml"
     */
    private String m_metadata;

    /**
     * Execute method : launch the pojoization.
     * @throws MojoExecutionException : an exception occurs.
     * @throws MojoFailureException : an failure occurs.
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Start bundle manipulation");
        // Get metadata file
        File meta = new File(m_outputDirectory + "/" + m_metadata);
        getLog().info("Metadata File : " + meta.getAbsolutePath());
        if (!meta.exists()) {
            getLog().info("No metadata file found - try to use only annotations");
            meta = null;
        }

        // Get input bundle
        File in = new File(m_buildDirectory + "/" + m_jarName + ".jar");
        getLog().info("Input Bundle File : " + in.getAbsolutePath());
        if (!in.exists()) {
            throw new MojoExecutionException("the specified bundle file does not exists");
        }
        
        File out = new File(m_buildDirectory + "/_out.jar");
        
        Pojoization pojo = new Pojoization();
        pojo.pojoization(in, out, meta);
        for (int i = 0; i < pojo.getWarnings().size(); i++) {
            getLog().warn((String) pojo.getWarnings().get(i));
        }
        if (pojo.getErrors().size() > 0) { throw new MojoExecutionException((String) pojo.getErrors().get(0)); }
        in.delete();
        out.renameTo(in);
        getLog().info("Bundle manipulation - SUCCESS");
    }

}
