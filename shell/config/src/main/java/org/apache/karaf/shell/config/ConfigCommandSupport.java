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
package org.apache.karaf.shell.config;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;

import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Abstract class from which all commands related to the ConfigurationAdmin
 * service should derive.
 * This command retrieves a reference to the ConfigurationAdmin service before
 * calling another method to actually process the command.
 */
public abstract class ConfigCommandSupport extends OsgiCommandSupport {

    public static final String PROPERTY_CONFIG_PID = "ConfigCommand.PID";
    public static final String PROPERTY_CONFIG_PROPS = "ConfigCommand.Props";
    private static final String PID_FILTER="(service.pid=%s*)";
    private static final String FILE_PREFIX="file:";
    private static final String CONFIG_SUFFIX=".cfg";
    private static final String FACTORY_SEPARATOR = "-";
    private static final String FILEINSTALL_FILE_NAME="felix.fileinstall.filename";

    private File storage;

    protected Object doExecute() throws Exception {
        // Get config admin service.
        ServiceReference ref = getBundleContext().getServiceReference(ConfigurationAdmin.class.getName());
        if (ref == null) {
            System.out.println("ConfigurationAdmin service is unavailable.");
            return null;
        }
        try {
            ConfigurationAdmin admin = (ConfigurationAdmin) getBundleContext().getService(ref);
            if (admin == null) {
                System.out.println("ConfigAdmin service is unavailable.");
                return null;
            }

            doExecute(admin);
        }
        finally {
            getBundleContext().ungetService(ref);
        }
        return null;
    }

    protected Dictionary getEditedProps() throws Exception {
        return (Dictionary) this.session.get(PROPERTY_CONFIG_PROPS);
    }

    protected abstract void doExecute(ConfigurationAdmin admin) throws Exception;

    /**
     * <p>
     * Returns the Configuration object of the given (felix fileinstall) file name.
     * </p>
     * @param fileName
     * @return
     */
    public Configuration findConfigurationByFileName(ConfigurationAdmin admin, String fileName) throws IOException, InvalidSyntaxException {
        if (fileName != null && fileName.contains(FACTORY_SEPARATOR)) {
            String factoryPid = fileName.substring(0, fileName.lastIndexOf(FACTORY_SEPARATOR));
            String absoluteFileName = FILE_PREFIX +storage.getAbsolutePath() + File.separator + fileName + CONFIG_SUFFIX;
            Configuration[] configurations = admin.listConfigurations(String.format(PID_FILTER, factoryPid));
            if (configurations != null) {
                for (Configuration configuration : configurations) {
                    Dictionary dictionary = configuration.getProperties();
                    if (dictionary != null) {
                        String fileInstallFileName = (String) dictionary.get(FILEINSTALL_FILE_NAME);
                        if (absoluteFileName.equals(fileInstallFileName)) {
                            return configuration;
                        }
                    }
                }
            }
        }
        return null;
    }

    public File getStorage() {
        return storage;
    }

    public void setStorage(File storage) {
        this.storage = storage;
    }
}
