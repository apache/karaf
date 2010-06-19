/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.dm.shell;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Bundle activator for the dependency manager shell command.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Activator implements BundleActivator {
    public void start(BundleContext context) throws Exception {
        // Provide DependencyManager shell commands for the old Felix Shell.
        try {
            context.registerService("org.apache.felix.shell.Command", new FelixDMCommand(context), null);
        }
        catch (Throwable t) {
        }
        // Provide DependencyManager shell commands for the Equinox Shell.
        try {
            context.registerService("org.eclipse.osgi.framework.console.CommandProvider", new EquinoxDMCommand(context), null);
        }
        catch (Throwable t) {
        }
        // Provide DependencyManager shell commands for the Gogo Shell.
        try {
            Hashtable props = new Hashtable();
            props.put(org.apache.felix.service.command.CommandProcessor.COMMAND_SCOPE, "dependencymanager");
            props.put(org.apache.felix.service.command.CommandProcessor.COMMAND_FUNCTION, 
                      new String[] { "dmhelp", "dm", "dmnodeps", "dmnotavail", "dmcompact" });
            context.registerService(GogoDMCommand.class.getName(), new GogoDMCommand(context), props);
        }
        catch (Throwable t) {            
        }
    }

    public void stop(BundleContext context) throws Exception {
    }    
}
