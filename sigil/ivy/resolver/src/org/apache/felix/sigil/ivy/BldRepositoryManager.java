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

package org.apache.felix.sigil.ivy;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.sigil.config.IRepositoryConfig;
import org.apache.felix.sigil.repository.AbstractRepositoryManager;
import org.apache.felix.sigil.repository.IBundleRepository;
import org.apache.felix.sigil.repository.IRepositoryProvider;

public class BldRepositoryManager extends AbstractRepositoryManager
{
    private static Map<String, String> aliases = new HashMap<String, String>();

    static
    {
        aliases.put("filesystem",
            "org.apache.felix.sigil.core.repository.FileSystemRepositoryProvider");
        aliases.put("obr", "org.apache.felix.sigil.obr.OBRRepositoryProvider");
        aliases.put("project", "org.apache.felix.sigil.ivy.ProjectRepositoryProvider");
        aliases.put("system",
            "org.apache.felix.sigil.core.repository.SystemRepositoryProvider");
    };

    private Map<String, Properties> repos;

    public BldRepositoryManager(Map<String, Properties> repos)
    {
        this.repos = repos;
    }

    @Override
    protected void loadRepositories()
    {
        for (String name : repos.keySet())
        {
            Properties repo = repos.get(name);
            if (Boolean.parseBoolean(repo.getProperty("disabled", "false")))
            {
                continue;
            }
            
            String optStr = repo.getProperty("optional", "false");
            boolean optional = Boolean.parseBoolean(optStr.trim());

            String alias = repo.getProperty(IRepositoryConfig.REPOSITORY_PROVIDER);
            if (alias == null)
            {
                Log.error("provider not specified for repository: " + name);
                continue;
            }

            String provider = (aliases.containsKey(alias) ? aliases.get(alias) : alias);

            if (alias.equals("obr"))
            {
                // cache is directory where synchronized bundles are stored;
                // not needed in ivy.
                repo.setProperty("cache", "/no-cache");
                String index = repo.getProperty("index");

                if (index == null)
                {
                    // index is created to cache OBR url
                    File indexFile = new File(System.getProperty("java.io.tmpdir"),
                        "obr-index-" + name);
                    indexFile.deleteOnExit();
                    repo.setProperty("index", indexFile.getAbsolutePath());
                }
                else
                {
                    if (!new File(index).getParentFile().mkdirs())
                    {
                        // ignore - but keeps findbugs happy
                    }
                }
            }

            int level = Integer.parseInt(repo.getProperty(
                IRepositoryConfig.REPOSITORY_LEVEL, Integer.toString(IBundleRepository.NORMAL_PRIORITY)));

            try
            {
                IRepositoryProvider instance = (IRepositoryProvider) (Class.forName(provider).newInstance());
                IBundleRepository repository = instance.createRepository(name, repo);
                addRepository(repository, level);
                Log.verbose("added repository: " + repository + " : " + level);
            }
            catch (Exception e)
            {
                String msg = "failed to create repository: ";
                if (!optional)
                    throw new Error(msg + repo + " " + e, e);
                System.err.println("WARNING: " + msg + e);
            }
        }
    }
}