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
package org.apache.felix.karaf.shell.obr;

import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.karaf.shell.obr.util.RequirementImpl;
import org.apache.felix.karaf.shell.obr.util.ResourceImpl;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Requirement;
import org.osgi.service.obr.Resolver;
import org.osgi.service.obr.Resource;

@Command(scope = "obr", name = "resolve", description = "Show the resolution output for a given set of requirements")
public class ResolveCommand extends ObrCommandSupport {

    @Option(name = "-w", aliases = "--why", description = "Display the reason if the inclusion of the resource")
    boolean why;

    @Argument(index = 0, name = "requirements", description = "Requirements", required = true, multiValued = true)
    List<String> requirements;

    protected void doExecute(RepositoryAdmin admin) throws Exception {
        Resource resource = new ResourceImpl(null, null, null, null, null, null, getRequirements(), null, null, null);
        Resolver resolver = admin.resolver();
        resolver.add(resource);
        if (resolver.resolve()) {
            Resource[] resources;
            resources = resolver.getRequiredResources();
            if ((resources != null) && (resources.length > 0)) {
                System.out.println("Required resource(s):");
                printUnderline(System.out, 21);
                for (int resIdx = 0; resIdx < resources.length; resIdx++) {
                    System.out.println("   " + resources[resIdx].getPresentationName()
                        + " (" + resources[resIdx].getVersion() + ")");
                    if (why) {
                        Requirement[] r = resolver.getReason(resources[resIdx]);
                        for (int reqIdx = 0; r != null && reqIdx < r.length; reqIdx++) {
                            System.out.println("      - " + r[reqIdx].getName() + ":" + r[reqIdx].getFilter());
                        }
                    }
                }
            }
            resources = resolver.getOptionalResources();
            if ((resources != null) && (resources.length > 0)) {
                System.out.println();
                System.out.println("Optional resource(s):");
                printUnderline(System.out, 21);
                for (int resIdx = 0; resIdx < resources.length; resIdx++) {
                    System.out.println("   " + resources[resIdx].getPresentationName()
                        + " (" + resources[resIdx].getVersion() + ")");
                    if (why) {
                        Requirement[] r = resolver.getReason(resources[resIdx]);
                        for (int reqIdx = 0; r != null && reqIdx < r.length; reqIdx++) {
                            System.out.println("      - " + r[reqIdx].getName() + ":" + r[reqIdx].getFilter());
                        }
                    }
                }
            }
        } else {
            Requirement[] reqs = resolver.getUnsatisfiedRequirements();
            if ((reqs != null) && (reqs.length > 0)) {
                System.out.println("Unsatisfied requirement(s):");
                printUnderline(System.out, 27);
                for (int reqIdx = 0; reqIdx < reqs.length; reqIdx++) {
                    System.out.println("   " + reqs[reqIdx].getName() + ":" + reqs[reqIdx].getFilter());
                    Resource[] resources = resolver.getResources(reqs[reqIdx]);
                    for (int resIdx = 0; resIdx < resources.length; resIdx++) {
                        System.out.println("      " + resources[resIdx].getPresentationName());
                    }
                }
            } else {
                System.out.println("Could not resolve targets.");
            }
        }
    }

    private Requirement[] getRequirements() throws InvalidSyntaxException {
        Requirement[] reqs = new Requirement[requirements.size()];
        for (int i = 0; i < reqs.length; i++) {
            reqs[i] = parseRequirement(requirements.get(i));
        }
        return reqs;
    }

    private Requirement parseRequirement(String req) throws InvalidSyntaxException {
        int p = req.indexOf(':');
        String name;
        String filter;
        if (p > 0) {
            name = req.substring(0, p);
            filter = req.substring(p + 1);
        } else {
            if (req.contains("package")) {
                name = "package";
            } else {
                name = "bundle";
            }
            filter = req;
        }
        if (!filter.startsWith("(")) {
            filter = "(" + filter + ")";
        }
        Filter flt = FrameworkUtil.createFilter(filter);
        return new RequirementImpl(name, flt);
    }

}