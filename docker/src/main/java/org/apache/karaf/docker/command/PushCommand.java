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

import org.apache.karaf.docker.command.completers.ImagesRepoTagsCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "docker", name = "push", description = "Push an image or a repository to a registry")
@Service
public class PushCommand extends DockerCommandSupport {

    @Argument(index = 0, name = "image", description = "Name or ID of the image", required = true, multiValued = false)
    @Completion(ImagesRepoTagsCompleter.class)
    String image;

    @Option(name = "--tag", description = "Push tag",  required = false, multiValued = false)
    String tag = "latest";

    @Option(name = "-v", aliases = "--verbose", description = "Display push progress on console", required = false, multiValued = false)
    boolean verbose;

    @Override
    public Object execute() throws Exception {
        getDockerService().push(image, tag, verbose, url);
        return null;
    }

}
