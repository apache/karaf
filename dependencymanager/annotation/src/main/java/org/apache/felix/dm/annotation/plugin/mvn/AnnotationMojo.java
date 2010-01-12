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
package org.apache.felix.dm.annotation.plugin.mvn;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.apache.felix.dm.annotation.plugin.bnd.DescriptorGenerator;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Jar;
import aQute.lib.osgi.Resource;

/**
 * The <code>AnnotationMojo</code>
 * generates a Dependency Manager component descriptor file based on annotations found from java classes.
 *
 * @goal scan
 * @phase package
 * @description Build DependencyManager component descriptors from class annotations.
 * @requiresDependencyResolution compile
 */
public class AnnotationMojo extends AbstractMojo
{
    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject m_project;

    /**
     * The target extension
     *
     * @parameter default-value="jar"
     * @required
     */
    private String m_artifactExtension;

    /**
     * Executes this mojo. We'll use the bnd library in order to scan classes from our target bundle.
     */
    public void execute() throws MojoExecutionException
    {
        Analyzer analyzer = null;
        File output = null;
        Jar jar = null;

        try
        {
            // Get the name of our target bundle we are parsing for annotations.
            File target = getBundleName();
            getLog().info("Generating DM component descriptors for bundle " + target);

            // Create a bnd analyzer and analyze our target bundle classes.
            analyzer = new Analyzer();
            analyzer.setJar(target);
            analyzer.analyze();

            // This helper class will parse classes using the analyzer we just created.
            DescriptorGenerator generator = new DescriptorGenerator(analyzer);

            // Start scanning
            if (generator.execute())
            {
                // Some annotation has been parsed. 
                // Add the list of generated component descriptors in our special header.
                jar = analyzer.getJar();
                jar.getManifest().getMainAttributes().putValue("DependencyManager-Component",
                    generator.getDescriptorPaths());

                // Add generated descriptors into the target bundle (we'll use a temp file).
                Map<String, Resource> resources = generator.getDescriptors();
                for (Map.Entry<String, Resource> entry : resources.entrySet())
                {
                    jar.putResource(entry.getKey(), entry.getValue());
                }
                copy(jar, target);
            }

            // Check if some errors have to be logged.
            if (analyzer.getErrors().size() != 0)
            {
                for (Iterator<String> e = analyzer.getErrors().iterator(); e.hasNext();)
                {
                    getLog().error(e.next());
                }
                throw new MojoExecutionException("Errors while generating dm descriptors");
            }

            // Check if some warnings have to be logged.
            if (analyzer.getWarnings().size() != 0)
            {
                for (Iterator<String> e = analyzer.getWarnings().iterator(); e.hasNext();)
                {
                    getLog().info(e.next());
                }
            }
        }

        catch (MojoExecutionException e)
        {
            throw e;
        }

        catch (Throwable t)
        {
            getLog().warn("Exception while scanning annotation", t);
            throw new MojoExecutionException(t.getMessage(), t.getCause());
        }

        finally
        {
            if (output != null && output.exists())
            {
                //output.delete();
            }

            if (jar != null)
            {
                jar.close();
            }
        }
    }

    /**
     * Returns the target name of this maven project.
     * @return the target name of this maven project.
     */
    private File getBundleName()
    {
        Build build = m_project.getBuild();
        return new File(build.getDirectory() + File.separator + build.getFinalName() + "."
            + m_artifactExtension);
    }

    /**
     * Copy the generated jar into our target bundle.
     * @param jar the jar with the generated component descriptors
     * @param target our target bundle
     * @throws MojoExecutionException on any errors
     * @throws Exception on any error
     */
    private void copy(Jar jar, File target) throws MojoExecutionException, Exception
    {
        File tmp = new File(getBundleName() + ".tmp");
        try
        {
            if (tmp.exists())
            {
                if (! tmp.delete()) {
                    throw new MojoExecutionException("Could not remove " + tmp);
                }
            }
            jar.write(tmp);
            jar.close();
            
            if (!tmp.renameTo(target))
            {
                throw new MojoExecutionException("Could not rename " + tmp + " to " + target);
            }
        }
        finally
        {
            jar.close();
            tmp.delete();
        }
    }
}