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

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.system.FrameworkType;
import org.apache.karaf.system.SystemService;

/**
 * Command for enabling/disabling debug logging on the OSGi framework
 */
@Command(scope = "system", name = "framework", description = "OSGi Framework options.")
@Service
public class FrameworkOptions implements Action {

    @Option(name = "-debug", aliases={"--enable-debug"}, description="Enable debug for the OSGi framework", required = false, multiValued = false)
    boolean debug;

    @Option(name = "-nodebug", aliases={"--disable-debug"}, description="Disable debug for the OSGi framework", required = false, multiValued = false)
    boolean nodebug;

    @Argument(name = "framework", required = false, description = "Name of the OSGi framework to use")
    String framework;

    @Reference
    SystemService systemService;

    @Override
    public Object execute() throws Exception {

        if (!debug^nodebug && framework == null) {
            System.out.printf("Current OSGi framework is %s%n", systemService.getFramework().name());
            return null;
        }
        if (framework != null) {
            FrameworkType frameworkType = FrameworkType.valueOf(framework);
            systemService.setFramework(frameworkType);
            System.out.println("Changed OSGi framework to " + frameworkType.toString().toLowerCase() + ". Karaf needs to be restarted to make the change effective");
        }
        if (debug) {
            FrameworkType frameworkType = systemService.getFramework();
            System.out.printf("Enabling debug for OSGi framework (%s)%n", frameworkType.name());
            systemService.setFrameworkDebug(true);
        }
        if (nodebug) {
            FrameworkType frameworkType = systemService.getFramework();
            System.out.printf("Disabling debug for OSGi framework (%s)%n", frameworkType.name());
            systemService.setFrameworkDebug(false);
        }

        return null;
    }

}
