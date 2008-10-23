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
package org.apache.servicemix.kernel.gshell.osgi;

import org.apache.geronimo.gshell.clp.Argument;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

public class BundleLevel extends BundleCommand {

    @Argument(required = false, index = 1)
    Integer level;

    protected void doExecute(Bundle bundle) throws Exception {
        // Get package admin service.
        ServiceReference ref = getBundleContext().getServiceReference(org.osgi.service.startlevel.StartLevel.class.getName());
        if (ref == null) {
            io.out.println("StartLevel service is unavailable.");
            return;
        }
        try {
            org.osgi.service.startlevel.StartLevel sl = (org.osgi.service.startlevel.StartLevel) getBundleContext().getService(ref);
            if (sl == null) {
                io.out.println("StartLevel service is unavailable.");
                return;
            }

            if (level == null) {
                io.out.println("Level " + sl.getBundleStartLevel(bundle));
            }
            else {
                sl.setBundleStartLevel(bundle, level);
            }
        }
        finally {
            getBundleContext().ungetService(ref);
        }
    }

}