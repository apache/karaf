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
package org.apache.karaf.docker;

import java.util.List;

public interface DockerService {

    List<Container> ps(boolean showAll, String url) throws Exception;

    Container inspect(String name, String url) throws Exception;

    Info info(String url) throws Exception;

    Version version(String url) throws Exception;

    void create(String name, String url) throws Exception;

    void create(String name, String url, ContainerConfig config) throws Exception;

    void provision(String name, String sshPort, String jmxRmiPort, String jmxRmiRegistryPort, String httpPort, boolean copy, String url) throws Exception;

    void rm(String name, boolean removeVolumes, boolean force, String url) throws Exception;

    void start(String name, String url) throws Exception;

    void stop(String name, int timeToWait, String url) throws Exception;

    void restart(String name, int timeToWait, String url) throws Exception;

    void pause(String name, String url) throws Exception;

    void unpause(String name, String url) throws Exception;

    void kill(String name, String signal, String url) throws Exception;

    void rename(String name, String newName, String url) throws Exception;

    String logs(String name, boolean stdout, boolean stderr, boolean timestamps, boolean details, String url) throws Exception;

    Top top(String name, String url) throws Exception;

    void commit(String name, String repo, String tag, String message, String url) throws Exception;

    List<Image> images(String url) throws Exception;

    void pull(String image, String tag, boolean verbose, String url) throws Exception;

    void push(String image, String tag, boolean verbose, String url) throws Exception;

    List<ImageHistory> history(String image, String url) throws Exception;

    List<ImageSearch> search(String term, String url) throws Exception;

    void tag(String image, String repo, String tag, String url) throws Exception;

    void rmi(String image, boolean force, boolean noprune, String url) throws Exception;

}
