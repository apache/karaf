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

import org.apache.karaf.docker.Info;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "docker", name = "info", description = "Display system-wide information")
@Service
public class InfoCommand extends DockerCommandSupport {

    @Override
    public Object execute() throws Exception {
        Info info = getDockerService().info(url);
        System.out.println("Containers: " + info.getContainers());
        System.out.println("Debug: " + info.isDebug());
        System.out.println("Driver: " + info.getDriver());
        System.out.println("ExecutionDriver: " + info.getExecutionDriver());
        System.out.println("IPv4Forwarding: " + info.isIpv4Forwarding());
        System.out.println("Images: " + info.getImages());
        System.out.println("IndexServerAddress: " + info.getIndexServerAddress());
        System.out.println("InitPath: " + info.getInitPath());
        System.out.println("InitSha1: " + info.getInitSha1());
        System.out.println("KernelVersion: " + info.getKernelVersion());
        System.out.println("MemoryLimit: " + info.isMemoryLimit());
        System.out.println("NEventsListener: " + info.isnEventsListener());
        System.out.println("NFd: " + info.getNfd());
        System.out.println("NGoroutines: " + info.getNgoroutines());
        System.out.println("SwapLimit: " + info.isSwapLimit());
        return null;
    }

}
