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
package org.apache.karaf.wrapper.management;

import javax.management.MBeanException;
import java.io.File;

/**
 * Describe the WrapperMBean.
 */
public interface WrapperMBean {

    /**
     * Install the service wrapper.
     *
     * @throws MBeanException in case of installation failure.
     */
    void install() throws MBeanException;

    /**
     * Install the service wrapper.
     *
     * @param name the service name.
     * @param displayName the service display name.
     * @param description the service description.
     * @param startType the start type.
     * @return the wrapper configuration (index 0) and service files (index 1).
     * @throws MBeanException in case of installation failure.
     */
    File[] install(String name, String displayName, String description, String startType) throws MBeanException;
    
    /**
     * Install the service wrapper.
     *
     * @param name the service name.
     * @param displayName the service display name.
     * @param description the service description.
     * @param startType the start type.
     * @param envs The environment variable and values
     * @param includes The include statement for JSW wrapper conf
     * @return the wrapper configuration (index 0) and service files (index 1).
     * @throws MBeanException in case of installation failure.
     */
    File[] install(String name, String displayName, String description, String startType, String[] envs, String[] includes) throws MBeanException;

}
