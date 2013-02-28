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
package org.apache.karaf.shell.obr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

@Command(scope = "obr", name = "resolve", description = "Shows the resolution output for a given set of requirements.")
public class ResolveCommand extends ObrCommandSupport {

    @Option(name = "-w", aliases = "--why", description = "Display the reason of the inclusion of the resource")
    boolean why;

    @Option(name = "-l", aliases = "--no-local", description = "Ignore local resources during resolution")
    boolean noLocal;

    @Option(name = "--no-remote", description = "Ignore remote resources during resolution")
    boolean noRemote;

    @Option(name = "--deploy", description = "Deploy the selected bundles")
    boolean deploy;

    @Option(name = "--start", description = "Deploy and start the selected bundles")
    boolean start;

    @Option(name = "--optional", description = "Resolve optional dependencies")
    boolean optional;

    @Argument(index = 0, name = "requirements", description = "Requirements", required = true, multiValued = true)
    List<String> requirements;

    protected void doExecute(RepositoryAdmin admin) throws Exception {
        List<Repository> repositories = new ArrayList<Repository>();
        repositories.add(admin.getSystemRepository());
        if (!noLocal) {
            repositories.add(admin.getLocalRepository());
        }
        if (!noRemote) {
            repositories.addAll(Arrays.asList(admin.listRepositories()));
        }
        Resolver resolver = admin.resolver(repositories.toArray(new Repository[repositories.size()]));
        for (Requirement requirement : parseRequirements(admin, requirements)) {
            resolver.add(requirement);
        }
        if (resolver.resolve(optional ? 0 : Resolver.NO_OPTIONAL_RESOURCES)) {
            Resource[] resources;
            resources = resolver.getRequiredResources();
            if ((resources != null) && (resources.length > 0)) {
                System.out.println("Required resource(s):");
                printUnderline(System.out, 21);
                for (int resIdx = 0; resIdx < resources.length; resIdx++) {
                    System.out.println("   " + resources[resIdx].getPresentationName() + " (" + resources[resIdx].getVersion() + ")");
                    if (why) {
                        Reason[] req = resolver.getReason(resources[resIdx]);
                        for (int reqIdx = 0; req != null && reqIdx < req.length; reqIdx++) {
                            if (!req[reqIdx].getRequirement().isOptional()) {
                                Resource r = req[reqIdx].getResource();
                                if (r != null) {
                                    System.out.println("      - " + r.getPresentationName() + " / " + req[reqIdx].getRequirement().getName() + ":" + req[reqIdx].getRequirement().getFilter());
                                } else {
                                    System.out.println("      - " + req[reqIdx].getRequirement().getName() + ":" + req[reqIdx].getRequirement().getFilter());
                                }
                            }
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
                        Reason[] req = resolver.getReason(resources[resIdx]);
                        for (int reqIdx = 0; req != null && reqIdx < req.length; reqIdx++) {
                            if (!req[reqIdx].getRequirement().isOptional()) {
                                Resource r = req[reqIdx].getResource();
                                if (r != null) {
                                    System.out.println("      - " + r.getPresentationName() + " / " + req[reqIdx].getRequirement().getName() + ":" + req[reqIdx].getRequirement().getFilter());
                                } else {
                                    System.out.println("      - " + req[reqIdx].getRequirement().getName() + ":" + req[reqIdx].getRequirement().getFilter());
                                }
                            }
                        }
                    }
                }
            }
            if (deploy || start) {
                try
                {
                    System.out.print("\nDeploying...");
                    resolver.deploy(start ? Resolver.START : 0);
                    System.out.println("done.");
                }
                catch (IllegalStateException ex)
                {
                    System.err.println(ex);
                }
            }
        } else {
            Reason[] reqs = resolver.getUnsatisfiedRequirements();
            if ((reqs != null) && (reqs.length > 0)) {
                System.out.println("Unsatisfied requirement(s):");
                printUnderline(System.out, 27);
                for (int reqIdx = 0; reqIdx < reqs.length; reqIdx++) {
                    System.out.println("   " + reqs[reqIdx].getRequirement().getName() + ":" + reqs[reqIdx].getRequirement().getFilter());
                    System.out.println("      " +reqs[reqIdx].getResource().getPresentationName());
                }
            } else {
                System.out.println("Could not resolve targets.");
            }
        }
    }

}
