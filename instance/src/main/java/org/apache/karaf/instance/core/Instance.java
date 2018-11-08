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
package org.apache.karaf.instance.core;

public interface Instance {

    String STOPPED = "Stopped";
    String STARTING = "Starting";
    String STARTED = "Started";
    String ERROR = "Error";

    String getName();

    boolean isRoot();

    String getLocation();

    int getPid();

    int getSshPort();

    void changeSshPort(int port) throws Exception;

    String getSshHost();

    int getRmiRegistryPort();

    void changeRmiRegistryPort(int port) throws Exception;

    String getRmiRegistryHost();

    int getRmiServerPort();

    void changeRmiServerPort(int port) throws Exception;

    String getRmiServerHost();

    String getJavaOpts();

    void changeJavaOpts(String javaOpts) throws Exception;

    void restart(String javaOpts) throws Exception;

    void start(String javaOpts) throws Exception;

    void stop() throws Exception;

    void destroy() throws Exception;

    String getState() throws Exception;

    void changeSshHost(String host) throws Exception;
}
