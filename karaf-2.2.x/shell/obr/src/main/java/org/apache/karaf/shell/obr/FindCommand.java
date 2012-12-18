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

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;

import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Command(scope = "obr", name = "find", description = "Find OBR bundles for a given filter.")
public class FindCommand extends ObrCommandSupport {

    @Argument(index = 0, name = "requirements", description = "Requirement", required = true, multiValued = true)
    List<String> requirements;

    protected void doExecute(RepositoryAdmin admin) throws Exception {
        Resource[] resources = admin.discoverResources(parseRequirements(admin, requirements));
        if (resources == null)
        {
            System.err.println("No matching resources.");
        }
        else
        {
            for (int resIdx = 0; resIdx < resources.length; resIdx++)
            {
                if (resIdx > 0)
                {
                    System.out.println("");
                }
                printResource(System.out, resources[resIdx]);
            }
        }
    }

    private void printResource(PrintStream out, Resource resource)
    {
        String name = resource.getPresentationName();
        if (name == null) {
            name = resource.getSymbolicName();
        }

        printUnderline(out, name.length());
        out.println(name);
        printUnderline(out, name    .length());

        Map map = resource.getProperties();
        for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) iter.next();
            if (entry.getValue().getClass().isArray())
            {
                out.println(entry.getKey() + ":");
                for (int j = 0; j < Array.getLength(entry.getValue()); j++)
                {
                    out.println("   " + Array.get(entry.getValue(), j));
                }
            }
            else
            {
                out.println(entry.getKey() + ": " + entry.getValue());
            }
        }

        Requirement[] reqs = resource.getRequirements();
        if ((reqs != null) && (reqs.length > 0))
        {
            boolean hdr = false;
            for (int i = 0; i < reqs.length; i++)
            {
                if (!reqs[i].isOptional())
                {
                    if (!hdr)
                    {
                        hdr = true;
                        out.println("Requirements:");
                    }
                    out.println("   " + reqs[i].getName() + ":" + reqs[i].getFilter());
                }
            }
            hdr = false;
            for (int i = 0; i < reqs.length; i++)
            {
                if (reqs[i].isOptional())
                {
                    if (!hdr)
                    {
                        hdr = true;
                        out.println("Optional Requirements:");
                    }
                    out.println("   " + reqs[i].getName() + ":" + reqs[i].getFilter());
                }
            }
        }

        Capability[] caps = resource.getCapabilities();
        if ((caps != null) && (caps.length > 0))
        {
            out.println("Capabilities:");
            for (int i = 0; i < caps.length; i++)
            {
                out.println("   " + caps[i].getName() + ":" + caps[i].getPropertiesAsMap());
            }
        }
    }

}
