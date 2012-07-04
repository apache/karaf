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
package org.apache.karaf.system.commands;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;

/**
 * Command to shut down Karaf container.
 */
@Command(scope = "system", name = "name", description = "Show or change Karaf instance name.")
public class Name extends AbstractSystemAction {

    @Argument(name = "name", index = 0, description = "New name for the instance", required = false, multiValued = false)
    String name;

    protected Object doExecute() throws Exception {
        if (name == null) {
            System.out.println(systemService.getName());
        } else {
            systemService.setName(name);
            System.out.println("Karaf instance name changed to " + name + ". Restart of karaf needed for this to take effect.");
        }
        return null;
    }

}
