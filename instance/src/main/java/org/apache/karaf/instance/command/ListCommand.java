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

import org.apache.karaf.instance.core.Instance;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

@Command(scope = "instance", name = "list", description = "Lists all existing container instances.")
@Service
public class ListCommand extends InstanceCommandSupport {

    @Option(name = "-l", aliases = { "--location" }, description = "Displays the location of the container instances", required = false, multiValued = false)
    boolean location;

    @Option(name = "-o", aliases = { "--java-opts" }, description = "Displays the Java options used to launch the JVM", required = false, multiValued = false)
    boolean javaOpts;

    @Option(name = "--no-color", description = "Disable table rendered output", required = false, multiValued = false)
    boolean noFormat;

    protected Object doExecute() throws Exception {
        Instance[] instances = getInstanceService().getInstances();
        ShellTable table = new ShellTable();
        table.column("SSH Port").alignRight();
        table.column("SSH Host").alignRight();
        table.column("RMI Registry").alignRight();
        table.column("RMI Registry Host").alignRight();
        table.column("RMI Server").alignRight();
        table.column("RMI Server Host").alignRight();
        table.column("State");
        table.column("PID");
        table.column(getRightColumnHeader());
        for (Instance instance : instances) {
            table.addRow().addContent(
                    instance.getSshPort(),
                    instance.getSshHost(),
                    instance.getRmiRegistryPort(),
                    instance.getRmiRegistryHost(),
                    instance.getRmiServerPort(),
                    instance.getRmiServerHost(),
                    instance.getState(),
                    instance.getPid(),
                    getRightColumnValue(instance));
        }
        table.print(System.out, !noFormat);
        return null;
    }

    private String getRightColumnHeader() {
        if (javaOpts) {
            return "JavaOpts";
        } else if (location) {
            return "Location";
        } else {
            return "Name";
        }
    }

    private String getRightColumnValue(Instance instance) {
        if (javaOpts) {
            return instance.getJavaOpts();
        } else if (location) {
            return instance.getLocation();
        } else {
            return instance.getName();
        }
    }

}
