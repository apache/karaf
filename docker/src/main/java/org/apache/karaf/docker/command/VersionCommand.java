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
package org.apache.karaf.docker.command;

import org.apache.karaf.docker.Version;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "docker", name = "version", description = "Show the Docker version information")
@Service
public class VersionCommand extends DockerCommandSupport {

    @Override
    public Object execute() throws Exception {
        Version version = getDockerService().version(url);
        System.out.println("Version: " + version.getVersion());
        System.out.println("Os: " + version.getOs());
        System.out.println("Kernel version: " + version.getKernelVersion());
        System.out.println("Go version: " + version.getGoVersion());
        System.out.println("Git commit: " + version.getGitCommit());
        System.out.println("Arch: "  + version.getArch());
        System.out.println("API version: " + version.getApiVersion());
        System.out.println("Build time: " + version.getBuildTime());
        System.out.println("Experimental: " + version.getExperimental());
        return null;
    }

}
