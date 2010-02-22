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

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.service.obr.Capability;
import org.osgi.service.obr.Repository;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Requirement;
import org.osgi.service.obr.Resource;

@Command(scope = "obr", name = "find", description = "Find OBR bundles for a given filter")
public class FindCommand extends ObrCommandSupport {

    @Argument(index = 0, name = "requirement", description = "Requirement", required = true, multiValued = false)
    String requirement;

    protected void doExecute(RepositoryAdmin admin) throws Exception {
        List<Resource> matching = new ArrayList<Resource>();
        Repository[] repos = admin.listRepositories();
        Requirement req = parseRequirement(admin, requirement);
        for (int repoIdx = 0; (repos != null) && (repoIdx < repos.length); repoIdx++) {
            Resource[] resources = repos[repoIdx].getResources();
            for (int resIdx = 0; (resources != null) && (resIdx < resources.length); resIdx++) {
                Capability[] caps = resources[resIdx].getCapabilities();
                for (int capIdx = 0; (caps != null) && (capIdx < caps.length); capIdx++) {
                    if (req.isSatisfied(caps[capIdx])) {
                        matching.add(resources[resIdx]);
                        break;
                    }
                }
            }
        }
        if (matching.isEmpty()) {
            System.out.println("No matching resources.");
        } else {
            for (Resource resource : matching) {
                String name = resource.getPresentationName();
                Version version = resource.getVersion();
                System.out.println(version != null ? name + " (" + version + ")" : name);
            }
        }
    }

    private Requirement parseRequirement(RepositoryAdmin admin, String req) throws InvalidSyntaxException {
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
        return admin.requirement(name, filter);
    }

}