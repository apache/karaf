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
package org.apache.karaf.management.mbeans.system;

/**
 * System MBean.
 */
public interface SystemMBean {

    /**
     * Shutdown the Karaf instance.
     *
     * @throws Exception
     */
    void halt() throws Exception;

    /**
     * Shutdown the Karaf instance at a certain time.
     *
     * @param time the time when to shutdown the Karaf instance.
     * @throws Exception
     */
    void halt(String time) throws Exception;

    /**
     * Reboot the Karaf instance.
     *
     * @throws Exception
     */
    void reboot() throws Exception;

    /**
     * Reboot the Karaf instance at a certain time and eventually cleanup the instance files.
     *
     * @param time the time when to reboot the Karaf instance.
     * @param clean true to clean the instance files, false else.
     * @throws Exception
     */
    void reboot(String time, boolean clean) throws Exception;

    /**
     * Get the name of the Karaf instance.
     *
     * @return the Karaf instance name.
     */
    String getName();

    /**
     * Set the name of the Karaf instance.
     *
     * @param name the new Karaf instance name. NB: the change require an instance reboot.
     */
    void setName(String name);

    /**
     * Get the Karaf version used by the Karaf instance.
     *
     * @return the Karaf version.
     */
    String getVersion();

    /**
     * Get the current OSGi framework in use.
     *
     * @return the name of the OSGi framework in use.
     */
    String getFramework();

    /**
     * Change the OSGi framework to run Karaf.
     *
     * @param framework the name of the framework to use (felix or equinox).
     */
    void setFramework(String framework) throws Exception;

    /**
     * Enable or disable the debug option on the OSGi framework.
     *
     * @param debug true to enable debug, false else.
     */
    void setFrameworkDebug(boolean debug) throws Exception;

    /**
     * Get the system start level.
     *
     * @return the current system start level.
     */
    int getStartLevel();

    /**
     * Set the system start level.
     *
     * @param startLevel the new system start level.
     */
    void setStartLevel(int startLevel);

    /* for backward compatibility */

    /**
     * @deprecated use halt() instead.
     */
    void shutdown() throws Exception;

}
