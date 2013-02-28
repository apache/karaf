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
package org.apache.karaf.management.mbeans.log;

import java.util.List;

/**
 * Log MBean.
 */
public interface LogMBean {

    /**
     * Set the level of the root logger.
     *
     * @param level the new level value (INFO, ERROR, etc).
     * @throws Exception
     */
    void setLevel(String level) throws Exception;

    /**
     * Set the level of a given logger.
     *
     * @param level the new level value (INFO, ERROR, etc).
     * @param logger the target logger where to change the level.
     * @throws Exception
     */
    void setLevel(String level, String logger) throws Exception;

    /**
     * Get the level of the root logger.
     *
     * @return the value of the level (INFO, ERROR, etc).
     * @throws Exception
     */
    String getLevel() throws Exception;

    /**
     * Get the level of a given logger.
     *
     * @param logger the target logger.
     * @return the level of the logger.
     * @throws Exception
     */
    String getLevel(String logger) throws Exception;

    /**
     * @deprecated please, use setLevel() instead.
     */
    void set(String level) throws Exception;

    /**
     * @deprecated please, use setLevel() instead.
     */
    void set(String level, String logger) throws Exception;

    /**
     * @deprecated please, use getLevel() instead.
     */
    String get() throws Exception;

    /**
     * @deprecated please, use getLevel() instead.
     */
    String get(String logger) throws Exception;

}
