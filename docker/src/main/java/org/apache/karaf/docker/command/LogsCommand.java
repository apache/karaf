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

@Command(scope = "docker", name = "logs", description = "Fetch the logs of a container")
@Service
public class LogsCommand extends DockerCommandSupport {

    @Argument(index = 0, name = "container", description = "Name or ID of the container", required = true, multiValued = false)
    @Completion(ContainersNameCompleter.class)
    String container;

    @Option(name = "--stdout", description = "Display stdout", required = false, multiValued = false)
    boolean stdout = true;

    @Option(name = "--stderr", description = "Display stderr", required = false, multiValued = false)
    boolean stderr;

    @Option(name = "--timestamps", description = "Show timestamps", required = false, multiValued = false)
    boolean timestamps;

    @Option(name = "--details", description = "Show extra details provided to logs", required = false, multiValued = false)
    boolean details;

    @Override
    public Object execute() throws Exception {
        if (!stdout && !stderr) {
            System.err.println("You have at least to choose one stream: stdout or stderr using the corresponding command options");
        }
        System.out.println(getDockerService().logs(container, stdout, stderr, timestamps, details, url));
        return null;
    }

}
