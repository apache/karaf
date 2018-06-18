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

import javax.management.MBeanException;
import javax.management.openmbean.TabularData;
import java.util.Map;

public interface DockerMBean {

    TabularData ps(boolean showAll, String url) throws MBeanException;

    Map<String, String> info(String url) throws MBeanException;

    Map<String, String> version(String url) throws MBeanException;

    void provision(String name, String sshPort, String jmxRmiPort, String jmxRmiRegistryPort, String httpPort, boolean copy, String url) throws MBeanException;

    void rm(String container, boolean removeVolumes, boolean force, String url) throws MBeanException;

    void rename(String container, String newName, String url) throws MBeanException;

    void start(String container, String url) throws MBeanException;

    void stop(String container, int timeToWait, String url) throws MBeanException;

    void restart(String container, int timeToWait, String url) throws MBeanException;

    void kill(String container, String signal, String url) throws MBeanException;

    void pause(String container, String url) throws MBeanException;

    void unpause(String container, String url) throws MBeanException;

    String logs(String container, boolean stdout, boolean stderr, boolean timestamps, boolean details, String url) throws MBeanException;

    void commit(String container, String repo, String tag, String message, String url) throws MBeanException;

    TabularData images(String url) throws MBeanException;

    TabularData search(String term, String url) throws MBeanException;

    void tag(String image, String tag, String repo, String url) throws MBeanException;

    void rmi(String image, boolean force, boolean noprune, String url) throws MBeanException;

    void pull(String image, String tag, String url) throws MBeanException;

    void push(String image, String tag, String url) throws MBeanException;

}
