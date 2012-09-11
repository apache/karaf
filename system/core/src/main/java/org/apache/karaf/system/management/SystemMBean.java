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

/**
 * Describe the system MBean.
 */
public interface SystemMBean {

    void halt() throws Exception;
    void halt(String time) throws Exception;
    void reboot() throws Exception;
    void reboot(String time, boolean clean) throws Exception;

    void setStartLevel(int startLevel) throws Exception;
    int getStartLevel() throws Exception;

    /**
     * Get the current OSGi framework in use.
     *
     * @return the name of the OSGi framework in use.
     * @throws Exception
     */
    String getFramework();

    /**
     * change OSGi framework
     *
     * @param framework to use.
     */
    void setFramework(String framework);
    
    /**
     * Enable or diable debgging
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
    void setName(String name) throws Exception;

}
