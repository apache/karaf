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
package org.apache.karaf.features;

public interface Library {

    String TYPE_ENDORSED = "endorsed";
    String TYPE_EXTENSION = "extension";
    String TYPE_BOOT = "boot";
    String TYPE_DEFAULT = "default";

    String getLocation();

    String getType();

    /**
     * Whether given library's exported packages should be added to <code>org.osgi.framework.system.packages.extra</code>
     * property in <code>${karaf.etc}/config.properties</code>.
     * @return
     */
    boolean isExport();

    /**
     * Whether given library's exported packages should be added to <code>org.osgi.framework.bootdelegation</code>
     * property in <code>${karaf.etc}/config.properties</code>
     * @return
     */
    boolean isDelegate();

}
