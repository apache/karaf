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

package org.apache.felix.sigil.config;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class BldFactory
{
    private static Map<URI, BldProject> projects = new HashMap<URI, BldProject>();

    public static IBldProject getProject(URI uri) throws IOException
    {
        return load(uri, false, null);
    }

    public static IBldProject getProject(URI uri, Properties overrides) throws IOException
    {
        return load(uri, false, overrides);
    }

    public static IBldProject getProject(URI uri, boolean ignoreCache) throws IOException
    {
        return load(uri, ignoreCache, null);
    }

    public static IRepositoryConfig getConfig(URI uri) throws IOException
    {
        return load(uri, false, null);
    }

    /**
     * creates a new project file, initialised with defaults.
     * @param uri where the file will be saved - used to resolve relative paths.
     * @param defaults relative path to defaults file - default ../sigil.properties.
     * @return
     * @throws IOException
     */
    public static IBldProject newProject(URI uri, String defaults) throws IOException
    {
        BldProject project = new BldProject(uri, null);
        Properties p = new Properties();
        if (defaults != null)
            p.setProperty(BldConfig.S_DEFAULTS, defaults);
        project.loadDefaults(p);
        return project;
    }

    private static BldProject load(URI uri, boolean ignoreCache, Properties overrides) throws IOException
    {
        BldProject p = null;
        if (!ignoreCache)
        {
            p = projects.get(uri);
        }

        if (p == null)
        {
            p = new BldProject(uri, overrides);
            p.load();
            projects.put(uri, p);

            if (Boolean.getBoolean("org.apache.felix.sigil.config.test"))
            {
                File path = new File(uri.getPath() + ".tmp");
                System.out.println("XXX: config.test writing: " + path);
                p.saveAs(path);
            }
        }
        return p;
    }

}
