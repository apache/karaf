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
package org.apache.felix.karaf.shell.packages;

import org.apache.felix.karaf.shell.console.OsgiCommandSupport;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Abstract class from which all commands related to the PackageAdmin
 * service should derive.
 * This command retrieves a reference to the PackageAdmin service before
 * calling another method to actually process the command.
 */
public abstract class PackageCommandSupport extends OsgiCommandSupport {

    protected Object doExecute() throws Exception {
        // Get package admin service.
        ServiceReference ref = getBundleContext().getServiceReference(PackageAdmin.class.getName());
        if (ref == null) {
            System.out.println("PackageAdmin service is unavailable.");
            return null;
        }
        try {
            PackageAdmin admin = (PackageAdmin) getBundleContext().getService(ref);
            if (admin == null) {
                System.out.println("PackageAdmin service is unavailable.");
                return null;
            }

            doExecute(admin);
        }
        finally {
            getBundleContext().ungetService(ref);
        }
        return null;
    }

    protected abstract void doExecute(PackageAdmin admin) throws Exception;

}
