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
package org.apache.karaf.admin.management;

import javax.management.openmbean.TabularData;

public interface AdminServiceMBean {

    String INSTANCE_PID = "Pid";
    String INSTANCE_NAME = "Name";
    String INSTANCE_IS_ROOT = "Is Root";
    String INSTANCE_SSH_PORT = "SSH Port";
    String INSTANCE_RMI_REGISTRY_PORT = "RMI Registry Port";
    String INSTANCE_RMI_SERVER_PORT = "RMI Server Port";
    String INSTANCE_STATE = "State";
    String INSTANCE_LOCATION = "Location";
    String INSTANCE_JAVAOPTS = "JavaOpts";

    String[] INSTANCE = {INSTANCE_PID, INSTANCE_NAME, INSTANCE_IS_ROOT, INSTANCE_SSH_PORT, INSTANCE_RMI_REGISTRY_PORT,
            INSTANCE_RMI_SERVER_PORT, INSTANCE_STATE, INSTANCE_LOCATION, INSTANCE_JAVAOPTS };

    // Operations
    int createInstance(String name, int sshPort, int rmiRegistryPort, int rmiServerPort, String location, String javaOpts, String features, String featureURLs) throws Exception;
    int createInstance(String name, int sshPort, int rmiRegistryPort, int rmiServerPort, String location, String javaOpts, String features, String featureURLs, String address) throws Exception;
    void changeSshPort(String name, int port) throws Exception;
    void changeSshHost(String name, String host) throws Exception;
    void changeRmiRegistryPort(String name, int port) throws Exception;
    void changeRmiServerPort(String name, int port) throws Exception;
    void changeJavaOpts(String name, String javaopts) throws Exception;
    void destroyInstance(String name) throws Exception;
    void startInstance(String name) throws Exception;
    void startInstance(String name, String opts) throws Exception;
    void startInstance(String name, String opts, boolean wait) throws Exception;
    void stopInstance(String name) throws Exception;
    void renameInstance(String originalName, String newName) throws Exception;
    void cloneInstance(String name, String cloneName, int sshPort, int rmiRegistryPort, int rmiServerPort, String location, String javaOpts) throws Exception;

    // Attributes
    TabularData getInstances() throws Exception;

}
