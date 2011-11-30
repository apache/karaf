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
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.apache.karaf.system.SystemService;

/**
 * Get/set the system start level.
 */
@Command(scope = "system", name = "start-level", description = "Gets or sets the system start level.")
public class StartLevel extends OsgiCommandSupport {

    @Argument(index = 0, name = "level", description = "The new system start level to set", required = false, multiValued = false)
    Integer level;

    private SystemService systemService;

    public void setSystemService(SystemService systemService) {
        this.systemService = systemService;
    }

    protected Object doExecute() throws Exception {
        if (level == null) {
            System.out.println("Level " + systemService.getStartLevel());
        } else {
            systemService.setStartLevel(level);
        }
        return null;
    }

}
