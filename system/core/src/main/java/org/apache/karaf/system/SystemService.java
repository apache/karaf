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
    public enum Swipe {
        /** Delete nothing; simple restart */
        NONE,
        /** Delete only the cache; everything else remains */
        CACHE,
        /** Forces a clean restart by removing the working directory; this option is compatible to the former clean method. */
        ALL
    }

    /**
     * Halt the Karaf container.
     */
    void halt() throws Exception;

    /**
     * Halt the Karaf container.
     *
     * @param time shutdown delay. The time argument can have different formats.
     *  First, it can be an absolute time in the format hh:mm, in which hh is the hour (1 or 2 digits) and mm
     *  is the minute of the hour (in two digits). Second, it can be in the format +m, in which m is the number of minutes
     *  to wait. The word now is an alias for +0.
     */
    void halt(String time) throws Exception;

    /**
     * Reboot the Karaf container.
     *
     * @throws Exception
     */
    void reboot() throws Exception;

    /**
     * Reboot the Karaf container.
     *
     * @param time reboot delay. The time argument can have different formats.
     *  First, it can be an absolute time in the format hh:mm, in which hh is the hour (1 or 2 digits) and mm
     *  is the minute of the hour (in two digits). Second, it can be in the format +m, in which m is the number of minutes
     *  to wait. The word now is an alias for +0.
     *  @param clean Force a clean restart by deleting the working directory.
     */
    void reboot(String time, Swipe clean) throws Exception;

    /**
     * Set the system start level.
     *
     * @param startLevel the new system start level.
     */
    void setStartLevel(int startLevel) throws Exception;

    /**
     * Get the system start level.
     *
     * @return the current system start level.
     */
    int getStartLevel() throws Exception;

    /**
     * Get the version of the current Karaf instance
     *
     * @return instance version
     */
    String getVersion();

    /**
     * Get the name of the current Karaf instance
     *
     * @return instance name
     */
    String getName();
    
    /**
     * Set the name of the Karaf instance
     *
     * @param name new instance name
     */
    void setName(String name);
    
    /**
     * Get the current OSGi framework in use.
     *
     * @return the name of the OSGi framework in use.
     * @throws Exception
     */
    FrameworkType getFramework();
    
    /**
     * change OSGi framework
     *
     * @param framework to use.
     */
    void setFramework(FrameworkType framework);
    
    /**
     * Enable or diable debgging
     * @param debug enable if true
     */
    void setFrameworkDebug(boolean debug);

    /**
     * Set a system property and persist to etc/system.properties
     * @param key
     */
    String setSystemProperty(String key, String value, boolean persist);
}
