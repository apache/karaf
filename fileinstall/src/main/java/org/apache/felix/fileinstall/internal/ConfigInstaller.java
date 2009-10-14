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
package org.apache.felix.fileinstall.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Properties;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * ArtifactInstaller for configurations.
 * TODO: This service lifecycle should be bound to the ConfigurationAdmin service lifecycle.
 */
public class ConfigInstaller implements ArtifactInstaller
{
    private final BundleContext context;
    private final ConfigurationAdmin configAdmin;

    ConfigInstaller(BundleContext context, ConfigurationAdmin configAdmin)
    {
        this.context = context;
        this.configAdmin = configAdmin;
    }

    public boolean canHandle(File artifact)
    {
        return artifact.getName().endsWith(".cfg");
    }

    public void install(File artifact) throws Exception
    {
        setConfig(artifact);
    }

    public void update(File artifact) throws Exception
    {
        setConfig(artifact);
    }

    public void uninstall(File artifact) throws Exception
    {
        deleteConfig(artifact);
    }

    ConfigurationAdmin getConfigurationAdmin()
    {
        return configAdmin;
    }

    /**
     * Set the configuration based on the config file.
     *
     * @param f
     *            Configuration file
     * @return
     * @throws Exception
     */
    boolean setConfig(File f) throws Exception
    {
        Properties p = new Properties();
        InputStream in = new BufferedInputStream(new FileInputStream(f));
        try
        {
            in.mark(1);
            boolean isXml = in.read() == '<';
            in.reset();
            if (isXml) {
                p.loadFromXML(in);
            } else {
                p.load(in);
            }
        }
        finally
        {
            in.close();
        }
        Util.performSubstitution(p);
        String pid[] = parsePid(f.getName());
        Hashtable ht = new Hashtable();
        ht.putAll(p);
        ht.put(DirectoryWatcher.FILENAME, f.getName());
        Configuration config = getConfiguration(pid[0], pid[1]);
        if (config.getBundleLocation() != null)
        {
            config.setBundleLocation(null);
        }
        config.update(ht);
        return true;
    }

    /**
     * Remove the configuration.
     *
     * @param f
     *            File where the configuration in whas defined.
     * @return
     * @throws Exception
     */
    boolean deleteConfig(File f) throws Exception
    {
        String pid[] = parsePid(f.getName());
        Configuration config = getConfiguration(pid[0], pid[1]);
        config.delete();
        return true;
    }

    String[] parsePid(String path)
    {
        String pid = path.substring(0, path.length() - 4);
        int n = pid.indexOf('-');
        if (n > 0)
        {
            String factoryPid = pid.substring(n + 1);
            pid = pid.substring(0, n);
            return new String[]
                {
                    pid, factoryPid
                };
        }
        else
        {
            return new String[]
                {
                    pid, null
                };
        }
    }

    Configuration getConfiguration(String pid, String factoryPid)
        throws Exception
    {
        Configuration oldConfiguration = findExistingConfiguration(pid, factoryPid);
        if (oldConfiguration != null)
        {
            Util.log(context, 0, "Updating configuration from " + pid
                + (factoryPid == null ? "" : "-" + factoryPid) + ".cfg", null);
            return oldConfiguration;
        }
        else
        {
            Configuration newConfiguration;
            if (factoryPid != null)
            {
                newConfiguration = getConfigurationAdmin().createFactoryConfiguration(pid, null);
            }
            else
            {
                newConfiguration = getConfigurationAdmin().getConfiguration(pid, null);
            }
            return newConfiguration;
        }
    }

    Configuration findExistingConfiguration(String pid, String factoryPid) throws Exception
    {
        String suffix = factoryPid == null ? ".cfg" : "-" + factoryPid + ".cfg";

        String filter = "(" + DirectoryWatcher.FILENAME + "=" + pid + suffix + ")";
        Configuration[] configurations = getConfigurationAdmin().listConfigurations(filter);
        if (configurations != null && configurations.length > 0)
        {
            return configurations[0];
        }
        else
        {
            return null;
        }
    }

}
