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
package org.apache.karaf.management.mbeans.dev;

import java.util.Map;

/**
 * MBean providing dev actions.
 */
public interface DevMBean {

    /**
     * Get the current OSGi framework in use.
     *
     * @deprecated use SystemMBean#getFramework() instead.
     * @return the name of the OSGi framework in use.
     * @throws Exception
     */
    String framework() throws Exception;

    /**
     * OSGi framework options.
     *
     * @deprecated use SystemMBean#setFrameworkDebug() instead.
     * @param debug enable debug of the OSGi framework to use.
     * @param framework name of the OSGI framework to use.
     * @throws Exception
     */
    void frameworkOptions(boolean debug, String framework) throws Exception;

    /**
     * Restart Karaf, with eventually a cleanup.
     *
     * @deprecated use SystemMBean#reboot() instead.
     * @param clean if true, Karaf is cleanup, false else.
     * @throws Exception
     */
    void restart(boolean clean) throws Exception;

    /**
     * Get system (and eventually OSGi) properties.
     *
     * @param unset if true, show the OSGi properties even if unset.
     * @param dumpToFile if true, dump system properties into a file (in the data folder)
     * @return the system properties
     */
    Map getProperties(boolean unset, boolean dumpToFile) throws Exception;

    /**
     * Get the value of a property.
     * @param key the system property key.
     * @return the system property value.
     */
    String getProperty(String key);

    /**
     * Set the value of a system property.
     *
     * @param key the system property key.
     * @param value the system property value.
     * @param persistent if true, persist the new value to the etc/system.properties file.
     */
    void setProperty(String key, String value, boolean persistent) throws Exception;

}
