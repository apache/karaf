/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.runtime.filemonitor;

import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Inspired by <a href="http://www.aqute.biz/Code/FileInstall">FileInstall</a> by Peter Kriens.
 * 
 * @version $Revision: 1.1 $
 */
public class FileMonitorActivator implements BundleActivator, ManagedServiceFactory {
    private BundleContext context;
    private ServiceTracker packageAdminTracker;
    private ServiceTracker configurationAdminTracker;
    private Map<String, FileMonitor> fileMonitors = new HashMap<String, FileMonitor>();

    // BundleActivator interface
    //-------------------------------------------------------------------------
    public void start(BundleContext context) throws Exception {
        this.context = context;

        Hashtable properties = new Hashtable();
        properties.put(Constants.SERVICE_PID, getName());
        context.registerService(ManagedServiceFactory.class.getName(), this, properties);

        packageAdminTracker = new ServiceTracker(context, PackageAdmin.class.getName(), null);
        packageAdminTracker.open();

        configurationAdminTracker = new ServiceTracker(context, ConfigurationAdmin.class.getName(), null);
        configurationAdminTracker.open();

        Hashtable initialProperties = new Hashtable();
        setPropertiesFromContext(initialProperties,
                FileMonitor.CONFIG_DIR,
                FileMonitor.DEPLOY_DIR,
                FileMonitor.GENERATED_JAR_DIR,
                FileMonitor.SCAN_INTERVAL
        );
        updated("initialPid", initialProperties);
    }

    public void stop(BundleContext context) throws Exception {
        Collection<FileMonitor> fileMonitors = this.fileMonitors.values();
        for (FileMonitor monitor : fileMonitors) {
            try {
                monitor.stop();
            }
            catch (Exception e) {
                // Ignore
            }
        }
        this.fileMonitors.clear();

        configurationAdminTracker.close();
        packageAdminTracker.close();
    }

    // ManagedServiceFactory interface
    //-------------------------------------------------------------------------
    public String getName() {
        return "org.apache.servicemix.runtime.filemonitor.FileMonitor";
    }

    public void updated(String pid, Dictionary properties) throws ConfigurationException {
        deleted(pid);
        FileMonitor monitor = new FileMonitor(this, properties);
        fileMonitors.put(pid, monitor);
        monitor.start();
    }

    public void deleted(String pid) {
        FileMonitor monitor = fileMonitors.remove(pid);
        if (monitor != null) {
            monitor.stop();
        }
    }

    // Properties
    //-------------------------------------------------------------------------
    public BundleContext getContext() {
        return context;
    }

    public ServiceTracker getConfigurationAdminTracker() {
        return configurationAdminTracker;
    }

    public ServiceTracker getPackageAdminTracker() {
        return packageAdminTracker;
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return (ConfigurationAdmin) getConfigurationAdminTracker().getService();
    }

    // Implementation methods
    //-------------------------------------------------------------------------
    protected void setPropertiesFromContext(Hashtable properties, String... keys) {
        for (String key : keys) {
            Object value = context.getProperty(key);
            if (value != null) {
                properties.put(key, value);
            }
        }
    }
}
