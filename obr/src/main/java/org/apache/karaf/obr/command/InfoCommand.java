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
package org.apache.karaf.obr.command;

import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resource;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "obr", name = "info", description = "Prints information about OBR bundles.")
@Service
public class InfoCommand extends ObrCommandSupport {

    @Argument(index = 0, name = "bundles", description = "Specify bundles to query for information (separated by whitespaces). The bundles are identified using the following syntax: symbolic_name,version where version is optional.", required = true, multiValued = true)
    List<String> bundles;

    protected void doExecute(RepositoryAdmin admin) throws Exception {
        for (String bundle : bundles) {
            String[] target = getTarget(bundle);
            Resource[] resources = searchRepository(admin, target[0], target[1]);
            if (resources == null)
            {
                System.err.println("Unknown bundle and/or version: "
                    + target[0]);
            }
            else
            {
                for (int resIdx = 0; resIdx < resources.length; resIdx++)
                {
                    if (resIdx > 0)
                    {
                        System.out.println();
                    }
                    printResource(System.out, resources[resIdx]);
                }
            }
        }
    }

    private void printResource(PrintStream out, Resource resource) {
        if (out != null && resource != null) {
            // OBR R5 per Spec has no presentation name
            String resourceId = getResourceId(resource);

            printUnderline(out, resourceId.length());
            out.println(resourceId);
            printUnderline(out, resourceId.length());

            Map map = resource.getProperties();
            for (Object o : map.entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                if (entry.getValue().getClass().isArray()) {
                    out.println(entry.getKey() + ":");
                    for (int j = 0; j < Array.getLength(entry.getValue()); j++) {
                        out.println("   " + Array.get(entry.getValue(), j));
                    }
                } else {
                    out.println(entry.getKey() + ": " + entry.getValue());
                }
            }

            Requirement[] reqs = resource.getRequirements();
            if ((reqs != null) && (reqs.length > 0)) {
                out.println("Requires:");
                for (Requirement req : reqs) {
                    out.println("   " + req.getName() + ":" + req.getFilter());
                }
            }

            Capability[] caps = resource.getCapabilities();
            if ((caps != null) && (caps.length > 0)) {
                out.println("Capabilities:");
                for (Capability cap : caps) {
                    out.println("   " + cap.getName() + ":" + cap.getPropertiesAsMap());
                }
            }
        }
    }

}
