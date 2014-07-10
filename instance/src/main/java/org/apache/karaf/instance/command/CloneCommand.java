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
package org.apache.karaf.instance.command;

import org.apache.karaf.instance.command.completers.InstanceCompleter;
import org.apache.karaf.instance.core.InstanceSettings;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Clone an existing instance.
 */
@Command(scope = "instance", name = "clone", description = "Clones an existing container instance.")
@Service
public class CloneCommand extends InstanceCommandSupport {

    @Option(name = "-s", aliases = {"--ssh-port"}, description = "Port number for remote secure shell connection", required = false, multiValued = false)
    int sshPort = 0;

    @Option(name = "-r", aliases = {"-rr", "--rmi-port", "--rmi-registry-port"}, description = "Port number for RMI registry connection", required = false, multiValued = false)
    int rmiRegistryPort = 0;

    @Option(name = "-rs", aliases = {"--rmi-server-port"}, description = "Port number for RMI server connection", required = false, multiValued = false)
    int rmiServerPort = 0;

    @Option(name = "-l", aliases = {"--location"}, description = "Location of the cloned container instance in the file system", required = false, multiValued = false)
    String location;

    @Option(name = "-o", aliases = {"--java-opts"}, description = "JVM options to use when launching the cloned instance", required = false, multiValued = false)
    String javaOpts;

    @Option(name = "-v", aliases = {"--verbose"}, description = "Display actions performed by the command (disabled by default)", required = false, multiValued = false)
    boolean verbose = false;

    @Option(name = "-tr", aliases = {"--text-resource"},
            description = "Add a text resource to the instance", required = false, multiValued = true)
    List<String> textResourceLocation;

    @Option(name = "-br", aliases = {"--binary-resource"},
            description = "Add a text resource to the instance", required = false, multiValued = true)
    List<String> binaryResourceLocations;

    @Argument(index = 0, name = "name", description = "The name of the source container instance", required = true, multiValued = false)
    @Completion(InstanceCompleter.class)
    String name;

    @Argument(index = 1, name = "cloneName", description = "The name of the cloned container instance", required = true, multiValued = false)
    String cloneName;

    protected Object doExecute() throws Exception {
        Map<String, URL> textResources = getResources(textResourceLocation);
        Map<String, URL> binaryResources = getResources(binaryResourceLocations);
        InstanceSettings settings = new InstanceSettings(sshPort, rmiRegistryPort, rmiServerPort, location, javaOpts, null, null, null, textResources, binaryResources);
        getInstanceService().cloneInstance(name, cloneName, settings, verbose);
        return null;
    }

}
