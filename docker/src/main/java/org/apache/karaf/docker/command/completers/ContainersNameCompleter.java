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
package org.apache.karaf.docker.command.completers;

import java.util.List;
import org.apache.karaf.docker.Container;
import org.apache.karaf.docker.DockerService;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;

@Service
public class ContainersNameCompleter implements Completer {

    @Reference private DockerService dockerService;

    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        try {
            for (Container container : dockerService.ps(true, null)) {
                for (String name : container.getNames()) {
                    delegate.getStrings().add(name);
                }
            }
        } catch (Exception e) {
            // nothing to do
        }
        return delegate.complete(session, commandLine, candidates);
    }

    public void setDockerService(DockerService dockerService) {
        this.dockerService = dockerService;
    }

    public DockerService getDockerService() {
        return this.dockerService;
    }
}
