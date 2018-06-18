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

import org.apache.karaf.docker.command.completers.ContainersNameCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "docker", name = "rename", description = "Rename a container")
@Service
public class RenameCommand extends DockerCommandSupport {

    @Argument(index = 0, name = "container", description = "Name or ID of the container to rename", required = true, multiValued = false)
    @Completion(ContainersNameCompleter.class)
    String container;

    @Argument(index = 1, name = "newName", description = "New name of the container", required = true, multiValued = false)
    String newName;

    @Override
    public Object execute() throws Exception {
        getDockerService().rename(container, newName, url);
        return null;
    }

}
