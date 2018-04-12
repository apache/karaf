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
package org.apache.karaf.system;

/**
 * Describe a system service
 */
public interface SystemService {

    /**
     * Types defining what to remove on a restart of Karaf
     */
    enum Swipe {
        /** Delete nothing; simple restart */
        NONE,
        /** Delete only the cache; everything else remains */
        CACHE,
        /** Forces a clean restart by removing the working directory; this option is compatible to the former clean method. */
        ALL
    }

    /**
     * Halt the Karaf container.
     *
     * @throws Exception If the halt fails.
     */
    void halt() throws Exception;

    /**
     * Halt the Karaf container.
     *
     * @param time Shutdown delay. The time argument can have different formats.
     *  First, it can be an absolute time in the format hh:mm, in which hh is the hour (1 or 2 digits) and mm
     *  is the minute of the hour (in two digits). Second, it can be in the format +m, in which m is the number of minutes
     *  to wait. The word now is an alias for +0.
     * @throws Exception If the halt fails.
     */
    void halt(String time) throws Exception;

    /**
     * Reboot the Karaf container.
     *
     * @throws Exception If the reboot fails.
     */
    void reboot() throws Exception;

    /**
     * Reboot the Karaf container.
     *
     * @param time The reboot delay. The time argument can have different formats.
     *  First, it can be an absolute time in the format hh:mm, in which hh is the hour (1 or 2 digits) and mm
     *  is the minute of the hour (in two digits). Second, it can be in the format +m, in which m is the number of minutes
     *  to wait. The word now is an alias for +0.
     * @param clean Force a clean restart by deleting the working directory.
     * @throws Exception If the reboot fails.
     */
    void reboot(String time, Swipe clean) throws Exception;

    /**
     * Set the system start level.
     *
     * @param startLevel The new system start level.
     * @throws Exception If setting the start level fails.
     */
    void setStartLevel(int startLevel) throws Exception;

    /**
     * Get the system start level.
     *
     * @return The current system start level.
     * @throws Exception If an error occurs while retrieving the start level.
     */
    int getStartLevel() throws Exception;

    /**
     * Get the version of the current Karaf instance.
     *
     * @return The instance version.
     */
    String getVersion();

    /**
     * Get the name of the current Karaf instance.
     *
     * @return The instance name.
     */
    String getName();
    
    /**
     * Set the name of the Karaf instance.
     *
     * @param name The new instance name.
     */
    void setName(String name);
    
    /**
     * Get the current OSGi framework in use.
     *
     * @return The {@link FrameworkType} representing the OSGi framework in use.
     */
    FrameworkType getFramework();
    
    /**
     * Change OSGi framework to use.
     *
     * @param framework The new OSGi framework to use.
     */
    void setFramework(FrameworkType framework);
    
    /**
     * Enable or disable debugging.
     *
     * @param debug True to enable debugging, false else.
     */
    void setFrameworkDebug(boolean debug);

    /**
     * Set a system property and persist to etc/system.properties.
     *
     * @param key The system property key.
     * @param value The system property value.
     * @param persist True to persist the change in Karaf etc configuration file, false else.
     * @return The system property value as set.
     */
    String setSystemProperty(String key, String value, boolean persist);
}
