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

@Command(scope = "docker", name = "rmi", description = "Remove an image")
@Service
public class RmiCommand extends DockerCommandSupport {

    @Argument(index = 0, name = "image", description = "The image to remove", required = true, multiValued = false)
    @Completion(ImagesRepoTagsCompleter.class)
    String image;

    @Option(name = "-f", aliases = "--force", description = "Force image remove", required = false, multiValued = false)
    boolean force;

    @Option(name = "-np", aliases = "--noprune", description = "Don't prune image", required = false, multiValued = false)
    boolean noprune;

    @Override
    public Object execute() throws Exception {
        getDockerService().rmi(image, force, noprune, url);
        return null;
    }

}
