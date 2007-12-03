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
package org.apache.geronimo.gshell.obr;

import java.util.List;

import org.apache.geronimo.gshell.clp.Argument;
import org.apache.geronimo.gshell.command.annotation.CommandComponent;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Requirement;
import org.osgi.service.obr.Resolver;
import org.osgi.service.obr.Resource;

@CommandComponent(id="obr:deploy", description="Deploy")
public class DeployCommand extends ObrCommandSupport {

    @Argument(required = true, multiValued = true, description = "List of bundles")
    List<String> bundles;

    protected void doExecute(RepositoryAdmin admin) throws Exception {
        doDeploy(admin, false);
    }

    protected void doDeploy(RepositoryAdmin admin, boolean start) throws Exception {
        Resolver resolver = admin.resolver();
        for (String bundle : bundles) {
            String[] target = getTarget(bundle);
            Resource resource = selectNewestVersion(searchRepository(admin, target[0], target[1]));
            if (resource != null)
            {
                resolver.add(resource);
            }
            else
            {
                io.err.println("Unknown bundle - " + target[0]);
            }
        }
        if ((resolver.getAddedResources() != null) &&
            (resolver.getAddedResources().length > 0))
        {
            if (resolver.resolve())
            {
                io.out.println("Target resource(s):");
                printUnderline(io.out, 19);
                Resource[] resources = resolver.getAddedResources();
                for (int resIdx = 0; (resources != null) && (resIdx < resources.length); resIdx++)
                {
                    io.out.println("   " + resources[resIdx].getPresentationName()
                        + " (" + resources[resIdx].getVersion() + ")");
                }
                resources = resolver.getRequiredResources();
                if ((resources != null) && (resources.length > 0))
                {
                    io.out.println("\nRequired resource(s):");
                    printUnderline(io.out, 21);
                    for (int resIdx = 0; resIdx < resources.length; resIdx++)
                    {
                        io.out.println("   " + resources[resIdx].getPresentationName()
                            + " (" + resources[resIdx].getVersion() + ")");
                    }
                }
                resources = resolver.getOptionalResources();
                if ((resources != null) && (resources.length > 0))
                {
                    io.out.println("\nOptional resource(s):");
                    printUnderline(io.out, 21);
                    for (int resIdx = 0; resIdx < resources.length; resIdx++)
                    {
                        io.out.println("   " + resources[resIdx].getPresentationName()
                            + " (" + resources[resIdx].getVersion() + ")");
                    }
                }

                try
                {
                    io.out.print("\nDeploying...");
                    resolver.deploy(start);
                    io.out.println("done.");
                }
                catch (IllegalStateException ex)
                {
                    io.err.println(ex);
                }
            }
            else
            {
                Requirement[] reqs = resolver.getUnsatisfiedRequirements();
                if ((reqs != null) && (reqs.length > 0))
                {
                    io.out.println("Unsatisfied requirement(s):");
                    printUnderline(io.out, 27);
                    for (int reqIdx = 0; reqIdx < reqs.length; reqIdx++)
                    {
                        io.out.println("   " + reqs[reqIdx].getFilter());
                        Resource[] resources = resolver.getResources(reqs[reqIdx]);
                        for (int resIdx = 0; resIdx < resources.length; resIdx++)
                        {
                            io.out.println("      " + resources[resIdx].getPresentationName());
                        }
                    }
                }
                else
                {
                    io.out.println("Could not resolve targets.");
                }
            }
        }

    }

}