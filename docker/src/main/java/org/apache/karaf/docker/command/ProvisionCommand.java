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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "docker", name = "provision", description = "Create a Docker container using the current running Karaf instance")
@Service
public class ProvisionCommand extends DockerCommandSupport {

    @Argument(index = 0, name = "name", description = "Name of the Docker container", required = true, multiValued = false)
    String name;

    @Option(name = "-c", aliases = "--copy", description = "Use directly the current Karaf instance working dir or make a copy", required = false, multiValued = false)
    boolean copy;

    @Option(name = "--sshPort", description = "Port number used by the Karaf SSH server", required = false, multiValued = false)
    String sshPort = "8101";

    @Option(name = "--jmxRmiPort", description = "Port number used by the Karaf JMX RMI MBeanServer", required = false, multiValued = false)
    String jmxRmiPort = "1099";

    @Option(name = "--jmxRmiRegistryPort", description = "Port number used by the Karaf JMX RMI Registry MBeanServer", required = false, multiValued = false)
    String jmxRmiRegistryPort = "44444";

    @Option(name = "--httpPort", description = "Port number used by the Karaf HTTP service", required = false, multiValued = false)
    String httpPort = "8181";

    @Override
    public Object execute() throws Exception {
        getDockerService().provision(name, sshPort, jmxRmiPort, jmxRmiRegistryPort, httpPort, copy, url);
        return null;
    }

}
