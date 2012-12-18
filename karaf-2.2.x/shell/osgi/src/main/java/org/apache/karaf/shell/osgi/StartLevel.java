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
package org.apache.karaf.shell.osgi;

import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.osgi.framework.ServiceReference;

@Command(scope = "osgi", name = "start-level", description = "Gets or sets the system start level.")
public class StartLevel extends OsgiCommandSupport {

    @Argument(index = 0, name = "level", description = "The new system start level to set", required = false, multiValued = false)
    Integer level;

    protected Object doExecute() throws Exception {
        // Get package admin service.
        ServiceReference ref = getBundleContext().getServiceReference(org.osgi.service.startlevel.StartLevel.class.getName());
        if (ref == null) {
            System.out.println("StartLevel service is unavailable.");
            return null;
        }
        try {
            org.osgi.service.startlevel.StartLevel sl = (org.osgi.service.startlevel.StartLevel) getBundleContext().getService(ref);
            if (sl == null) {
                System.out.println("StartLevel service is unavailable.");
                return null;
            }

            if (level == null) {
                System.out.println("Level " + sl.getStartLevel());
            }
            else {
                sl.setStartLevel(level);
            }
        }
        finally {
            getBundleContext().ungetService(ref);
        }
        return null;
    }

}
