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

import java.util.List;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.Bundle;

@Command(scope = "region", name = "addBundle", description = "Adds a list of known bundles by id to a specified Region.")
public class AddBundleCommand extends RegionCommandSupport {

    @Argument(index = 0, name = "region", description = "Region to add the bundles to", required = true, multiValued = false)
    String region;

    @Argument(index = 1, name = "bundles", description = "Bundles by id to add to the region", required = true, multiValued = true)
    List<Long> ids;

    protected void doExecute(RegionDigraph regionDigraph) throws Exception {
        Region r = getRegion(regionDigraph, region);
        for (Long id : ids) {
            for (Region existing: regionDigraph.getRegions()) {
                if (existing.contains(id)) {
                    Bundle b = getBundleContext().getBundle(id);
                    System.out.println("Removing bundle " + id + " from region " + existing.getName());
                    existing.removeBundle(b);
                    break;
                }
            }
            r.addBundle(id);
        }
    }
}
