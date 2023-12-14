/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.config.core;

import javax.management.MBeanException;
import java.util.List;
import java.util.Map;

/**
 * MBean to manipulate the Config layer.
 */
public interface ConfigMBean {

    /**
     * Get the list of all configuration PIDs.
     *
     * @return the list of all configuration PIDs.
     * @throws MBeanException in case of MBean failure.
     */
    List<String> getConfigs() throws MBeanException;

    /**
     * Create a new configuration for the given PID.
     *
     * @param pid the configuration PID.
     * @throws MBeanException in case of MBean failure.
     */
    void create(String pid) throws MBeanException;

    /**
     * Install a cfg file.
     *
     * @param url The location of the cfg file.
     * @param finalname The final name of the cfg file in the etc folder.
     * @param override True to override the cfg file if it already exists, false else.
     * @throws MBeanException in case of MBean failure.
     */
    void install(String url, String finalname, boolean override) throws MBeanException;

    /**
     * Delete a configuration identified by the given PID.
     *
     * @param pid the configuration PID to delete.
     * @throws MBeanException in case of MBean failure.
     */
    void delete(String pid) throws MBeanException;

    /**
     * Check if a configuration identified by the given PID exists.
     *
     * @param pid The configuration PID to check.
     * @return true if the configuration exists, false else.
     * @throws MBeanException in case of MBean failure.
     */
    boolean exists(String pid) throws MBeanException;

    /**
     * Get the list of properties for a configuration PID.
     *
     * @param pid the configuration PID.
     * @return the list of properties.
     * @throws MBeanException in case of MBean failure.
     */
    Map<String, String> listProperties(String pid) throws MBeanException;

    /**
     * Remove the configuration property identified by the given key.
     *
     * @param pid the configuration PID.
     * @param key the property key.
     * @throws MBeanException in case of MBean failure.
     */
    void deleteProperty(String pid, String key) throws MBeanException;

    /**
     * Append (or add) a value for the given configuration key.
     *
     * @param pid the configuration PID.
     * @param key the property key.
     * @param value the value to append to the current property value.
     * @throws MBeanException in case of MBean failure.
     */
    void appendProperty(String pid, String key, String value) throws MBeanException;

    /**
     * Set a configuration property.
     *
     * @param pid the configuration PID.
     * @param key the property key.
     * @param value the property value.
     * @throws MBeanException in case of MBean failure.
     */
    void setProperty(String pid, String key, String value) throws MBeanException;

    /**
     * Get a configuration property.
     *
     * @param pid the configuration PID.
     * @param key the property key.
     * @throws MBeanException in case of MBean failure.
     */
    String getProperty(String pid, String key) throws MBeanException;

    /**
     * Update a complete configuration.
     *
     * @param pid the configuration PID.
     * @param properties the new properties to set in the configuration.
     * @throws MBeanException in case of MBean failure.
     */
    void update(String pid, Map<String, String> properties) throws MBeanException;

    /**
     * Add new properties or update existing ones (without removing others) in a given configuration.
     *
     * @param pid the configuration PID.
     * @param properties the properties to add/update.
     * @throws MBeanException in case of MBean failure.
     */
    void append(String pid, Map<String, String> properties) throws MBeanException;

    /**
     * Delete properties from a configuration.
     *
     * @param pid the configuration PID.
     * @param properties the properties to delete from the configuration.
     * @throws MBeanException in case of MBean failure.
     */
    void delete(String pid, List<String> properties) throws MBeanException;

    String createFactoryConfiguration(String factoryPid) throws MBeanException;

    String createFactoryConfiguration(String factoryPid, String alias) throws MBeanException;

    String createFactoryConfiguration(String factoryPid, String alias, Map<String, String> properties) throws MBeanException;
    
    /**
     * Create a factory based configuration.
     *
     * @param factoryPid the configuration factory PID.
     * @param properties the new properties to set in the configuration.
     * @return the created PID.
     * @throws MBeanException in case of MBean failure.
     */
    String createFactoryConfiguration(String factoryPid, Map<String, String> properties) throws MBeanException;

}
