/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.config.core.impl;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.karaf.config.core.ConfigRepository;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class ConfigRepositoryImpl implements ConfigRepository {
    private ConfigurationAdmin configAdmin;
    
    private File storage;

    public ConfigRepositoryImpl(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    /* (non-Javadoc)
     * @see org.apache.karaf.shell.config.impl.ConfigRepository#update(java.lang.String, java.util.Dictionary, boolean)
     */
    @Override
    @SuppressWarnings("rawtypes")
    public void update(String pid, Dictionary props) throws IOException {
        Configuration cfg = this.configAdmin.getConfiguration(pid, null);
        if (cfg.getProperties() == null) {
            PidParts pidParts = parsePid(pid);
            if (pidParts.factoryPid != null) {
                cfg = this.configAdmin.createFactoryConfiguration(pidParts.pid, null);
            }
        }
        if (cfg.getBundleLocation() != null) {
            cfg.setBundleLocation(null);
        }
        cfg.update(props);
    }

    private PidParts parsePid(String sourcePid) {
        PidParts pidParts = new PidParts();
        int n = sourcePid.indexOf('-');
        if (n > 0) {
            pidParts.factoryPid = sourcePid.substring(n + 1);
            pidParts.pid = sourcePid.substring(0, n);
        } else {
            pidParts.pid = sourcePid;
        }
        return pidParts;
    }
    
    private class PidParts {
        String pid;
        String factoryPid;
    }
    
    /* (non-Javadoc)
     * @see org.apache.karaf.shell.config.impl.ConfigRepository#delete(java.lang.String)
     */
    @Override
    public void delete(String pid) throws Exception {
        Configuration configuration = this.configAdmin.getConfiguration(pid);
        configuration.delete();
        deleteStorage(pid);
    }
    
    protected void deleteStorage(String pid) throws Exception {
        if (storage != null) {
            File cfgFile = new File(storage, pid + ".cfg");
            cfgFile.delete();
        }
    }

    /* (non-Javadoc)
     * @see org.apache.karaf.shell.config.impl.ConfigRepository#getConfigProperties(java.lang.String)
     */
    @Override
    @SuppressWarnings("rawtypes")
    public Dictionary getConfigProperties(String pid) throws IOException, InvalidSyntaxException {
        if(pid != null && configAdmin != null) {
            Configuration configuration = this.configAdmin.getConfiguration(pid);
            if(configuration != null) {
                Dictionary props = configuration.getProperties();
                return (props != null) ? props : new Hashtable<String, String>();
            }
        }
        return null;
    }

    public ConfigurationAdmin getConfigAdmin() {
        return this.configAdmin;
    }

}
