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
package org.apache.karaf.region.commands;

import java.io.PrintStream;

import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

public abstract class RegionCommandSupport extends OsgiCommandSupport {

    protected static final char VERSION_DELIM = ',';

    protected Object doExecute() throws Exception {
        // Get repository instance service.
        ServiceReference ref = getBundleContext().getServiceReference(RegionDigraph.class.getName());
        if (ref == null) {
            System.out.println("RegionDigraph service is unavailable.");
            return null;
        }
        try {
            RegionDigraph admin = (RegionDigraph) getBundleContext().getService(ref);
            if (admin == null) {
                System.out.println("RegionDigraph service is unavailable.");
                return null;
            }

            doExecute(admin);
        }
        finally {
            getBundleContext().ungetService(ref);
        }
        return null;
    }

    abstract void doExecute(RegionDigraph admin) throws Exception;

    protected void printUnderline(PrintStream out, int length)
    {
        for (int i = 0; i < length; i++)
        {
            out.print('-');
        }
        out.println("");
    }


    protected Region getRegion(RegionDigraph regionDigraph, String region) throws BundleException {
        Region r = regionDigraph.getRegion(region);
        if (r == null) {
            System.out.println("No region: " + region + ", creating it");
            r = regionDigraph.createRegion(region);
        }
        return r;
    }
}
