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
package org.apache.karaf.wrapper;

import java.io.File;

/**
 * Interface describing the Wrapper service.
 */
public interface WrapperService {

    /**
     * Install the Karaf container as a system service in the OS.
     *
     * @throws Exception If the wrapper install fails.
     */
    void install() throws Exception;

    /**
     * Install the Karaf container as a system service in the OS.
     *
     * @param name The service name that will be used when installing the service.
     * @param displayName The display name of the service.
     * @param description The description of the service.
     * @param startType Mode in which the service is installed. AUTO_START or DEMAND_START.
     * @return An array containing the wrapper configuration file (index 0) and the service file (index 1).
     * @throws Exception If the wrapper install fails.
     */
    File[] install(String name, String displayName, String description, String startType) throws Exception;
    
    /**
     * Install the Karaf container as a system service in the OS.
     *
     * @param name The service name that will be used when installing the service.
     * @param displayName The display name of the service.
     * @param description The description of the service.
     * @param startType Mode in which the service is installed. AUTO_START or DEMAND_START.
     * @param envs The environment variable and values
     * @param includes The include statement for JSW wrapper conf
     * @return An array containing the wrapper configuration file (index 0) and the service file (index 1).
     * @throws Exception If the wrapper install fails.
     */
    File[] install(String name, String displayName, String description, String startType, String[] envs, String[] includes) throws Exception;

}
