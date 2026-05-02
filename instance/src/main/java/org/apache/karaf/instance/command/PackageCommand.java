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
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "instance", name = "package", description = "Create a .zip archive of the instance.")
@Service
public class PackageCommand extends InstanceCommandSupport {

    @Argument(index = 0, name = "name", description = "The name of the container instance", required = true)
    @Completion(InstanceCompleter.class)
    private String name;
    @Argument(index = 1, name = "destination", description = "Destination path for the archive", required = true)
    private String destination;

    protected Object doExecute() throws Exception {
        getExistingInstance(name).packageInZip(destination);
        return "Archive available at " + destination;
    }
}
