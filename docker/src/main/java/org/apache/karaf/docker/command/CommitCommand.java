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
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "docker", name = "commit", description = "Create a new image from a container's changes")
@Service
public class CommitCommand extends DockerCommandSupport {

    @Argument(index = 0, name = "container", description = "Name or ID of the container", required = true, multiValued = false)
    @Completion(ContainersNameCompleter.class)
    String container;

    @Argument(index = 1, name = "repository", description = "Repository", required = true, multiValued = false)
    String repo;

    @Argument(index = 2, name = "tag", description = "Tag", required = true, multiValued = false)
    String tag;

    @Option(name = "--message", description = "Commit message",  required = false, multiValued = false)
    String message = "";

    @Override
    public Object execute() throws Exception {
        getDockerService().commit(container, repo, tag, message, url);
        return null;
    }
}
