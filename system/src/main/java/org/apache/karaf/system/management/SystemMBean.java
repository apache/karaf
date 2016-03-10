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
package org.apache.karaf.system.management;

import javax.management.MBeanException;
import java.util.Map;

/**
 * Describe the system MBean.
 */
public interface SystemMBean {

    /**
     * Stop the Karaf instance.
     *
     * @throws MBeanException If a failure occurs.
     */
    void halt() throws MBeanException;

    /**
     * Stop the Karaf instance at a given time.
     *
     * @param time the time when to stop the Karaf instance.
     * @throws MBeanException If a failure occurs.
     */
    void halt(String time) throws MBeanException;

    /**
     * Reboot the Karaf instance.
     *
     * @throws MBeanException If a failure occurs.
     */
    void reboot() throws MBeanException;

    /**
     * Reboot the Karaf instance at a given time.
     *
     * @param time the time when to reboot the Karaf instance.
     * @throws MBeanException If a failure occurs.
     */
    void reboot(String time) throws MBeanException;

    /**
     * Reboot the Karaf instance at a given time and clean the cache.
     *
     * @param time the time when to reboot the Karaf instance.
     * @throws MBeanException If a failure occurs.
     */
    void rebootCleanCache(String time) throws MBeanException;

    /**
     * Reboot the Karaf instance at a given time and clean all working files.
     *
     * @param time the time when to reboot the Karaf instance.
     * @throws MBeanException If a failure occurs.
     */
    void rebootCleanAll(String time) throws MBeanException;

    /**
     * Set the system bundle start level.
     *
     * @param startLevel the new system bundle start level.
     * @throws MBeanException If a failure occurs.
     */
    void setStartLevel(int startLevel) throws MBeanException;

    /**
     * Get the current system bundle start level.
     *
     * @return the current system bundle start level.
     * @throws MBeanException If a failure occurs.
     */
    int getStartLevel() throws MBeanException;

    /**
     * Get the current OSGi framework in use.
     *
     * @return the name of the OSGi framework in use.
     */
    String getFramework();

    /**
     * Change OSGi framework
     *
     * @param framework The framework to use.
     */
    void setFramework(String framework);
    
    /**
     * Enable or disable debugging
     *
     * @param debug enable if true
     */
    void setFrameworkDebug(boolean debug);

    /**
     * Get the current Karaf instance name.
     *
     * @return the current Karaf instance name.
     */
    String getName();

    /**
     * Change Karaf instance name.
     *
     * @param name the new Karaf instance name.
     */
    void setName(String name);

    /**
     * Get the version of the current Karaf instance.
     *
     * @return the current Karaf instance version.
     */
    String getVersion();

    /**
     * Get all system properties.
     *
     * @param unset if true, display the OSGi properties even if they are not defined (with "undef" value).
     * @param dumpToFile if true, dump the properties into a file in the data folder.
     * @return the list of system properties.
     * @throws MBeanException If a failure occurs.
     */
    Map<String, String> getProperties(boolean unset, boolean dumpToFile) throws MBeanException;

    /**
     * Get the value of a given system property.
     *
     * @param key the system property key.
     * @return the system property value.
     */
    String getProperty(String key);

    /**
     * Set the value of a system property.
     *
     * @param key the system property key.
     * @param value the new system property value.
     * @param persistent if true, persist the new value to the etc/system.properties file.
     */
    void setProperty(String key, String value, boolean persistent);

}
