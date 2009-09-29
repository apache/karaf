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

package org.apache.felix.sigil.ant;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import org.apache.felix.sigil.bnd.BundleBuilder;
import org.apache.felix.sigil.config.BldFactory;
import org.apache.felix.sigil.config.IBldProject;
import org.apache.felix.sigil.config.IBldProject.IBldBundle;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;

public class BundleTask extends Task
{
    private File[] classpath;
    private String destPattern;
    private boolean force;
    private String property;
    private String sigilFile;

    @Override
    public void execute() throws BuildException
    {
        if (classpath == null)
            throw new BuildException("missing: attribute: classpathref");
        if (destPattern == null)
            throw new BuildException("missing attribute: destpattern");

        @SuppressWarnings("unchecked")
        Hashtable<String, String> projectProperties = getProject().getProperties();
        Properties antProperties = new Properties();
        antProperties.putAll(projectProperties);

        IBldProject project;
        try
        {
            project = BldFactory.getProject(getSigilFileURI(), antProperties);
        }
        catch (IOException e)
        {
            throw new BuildException("failed to get project file: " + e);
        }

        Properties env = new Properties();
        for (String key : projectProperties.keySet())
        {
            if (key.matches("^[a-z].*"))
            { // avoid props starting with Uppercase - bnd adds them to manifest
                env.setProperty(key, projectProperties.get(key));
            }
        }

        BundleBuilder bb = new BundleBuilder(project, classpath, destPattern, env);
        boolean anyModified = false;

        for (IBldBundle bundle : project.getBundles())
        {
            String id = bundle.getId();
            log("creating bundle: " + id);
            int nWarn = 0;
            int nErr = 0;
            String msg = "";

            try
            {
                boolean modified = (bb.createBundle(bundle, force,
                    new BundleBuilder.Log()
                    {
                        public void warn(String msg)
                        {
                            log(msg, Project.MSG_WARN);
                        }

                        public void verbose(String msg)
                        {
                            log(msg, Project.MSG_VERBOSE);
                        }
                    }));
                nWarn = bb.warnings().size();
                if (modified)
                {
                    anyModified = true;
                }
                else
                {
                    msg = " (not modified)";
                }
            }
            catch (Exception e)
            {
                List<String> errors = bb.errors();
                if (errors != null)
                {
                    nErr = errors.size();
                    for (String err : errors)
                    {
                        log(err, Project.MSG_ERR);
                    }
                }
                throw new BuildException("failed to create: " + id + ": " + e, e);
            }
            finally
            {
                log(id + ": " + count(nErr, "error") + ", " + count(nWarn, "warning")
                    + msg);
            }
        }

        if (anyModified && property != null)
        {
            getProject().setProperty(property, "true");
        }
    }

    private URI getSigilFileURI()
    {
        File file = sigilFile == null ? new File(getProject().getBaseDir(),
            IBldProject.PROJECT_FILE) : new File(sigilFile);
        if (!file.isFile())
        {
            throw new BuildException("File not found " + file.getAbsolutePath());
        }
        return file.toURI();
    }

    private String count(int count, String msg)
    {
        return count + " " + msg + (count == 1 ? "" : "s");
    }

    public void setDestpattern(String pattern)
    {
        this.destPattern = pattern;
    }

    public void setForce(String force)
    {
        this.force = Boolean.parseBoolean(force);
    }

    public void setProperty(String property)
    {
        this.property = property;
    }

    public void setClasspathref(String value)
    {
        Path p = (Path) getProject().getReference(value);
        if (p == null)
        {
            throw new BuildException(value + "is not a path reference.");
        }

        String[] paths = p.list();
        classpath = new File[paths.length];
        for (int i = 0; i < paths.length; ++i)
        {
            classpath[i] = new File(paths[i]);
        }
    }

    public void setSigilFile(String sigilFile)
    {
        this.sigilFile = sigilFile;
    }
}
