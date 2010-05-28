/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.gogo.command;

import java.io.*;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.*;
import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Reason;

import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

public class OBR
{
    private static final String REPO_ADD = "add";
    private static final String REPO_REMOVE = "remove";
    private static final String REPO_LIST = "list";
    private static final String REPO_REFRESH = "refresh";

    private static final char VERSION_SEPARATOR = '@';

    private final BundleContext m_bc;
    private final ServiceTracker m_tracker;

    public OBR(BundleContext bc, ServiceTracker tracker)
    {
        m_bc = bc;
        m_tracker = tracker;
    }

    private RepositoryAdmin getRepositoryAdmin()
    {
        Object svcObj;
        try
        {
            svcObj = m_tracker.getService();
        }
        catch (Exception ex)
        {
            svcObj = null;
        }
        if (svcObj == null)
        {
            System.out.println("No repository admin service available");
        }
        return (RepositoryAdmin) svcObj;
    }

    @Descriptor("manage repositories")
    public void repos(
        @Descriptor("( add | list | refresh | remove )") String action,
        @Descriptor("space-delimited list of repository URLs") String[] args)
        throws IOException
    {
        Object svcObj = getRepositoryAdmin();
        if (svcObj == null)
        {
            return;
        }
        RepositoryAdmin ra = (RepositoryAdmin) svcObj;

        if (args.length > 0)
        {
            for (int i = 0; i < args.length; i++)
            {
                try
                {
                    if (action.equals(REPO_ADD))
                    {
                        ra.addRepository(args[i]);
                    }
                    else if (action.equals(REPO_REFRESH))
                    {
                        ra.removeRepository(args[i]);
                        ra.addRepository(args[i]);
                    }
                    else if (action.equals(REPO_REMOVE))
                    {
                        ra.removeRepository(args[i]);
                    }
                    else
                    {
                        System.out.println("Unknown repository operation: " + action);
                    }
                }
                catch (Exception ex)
                {
                    ex.printStackTrace(System.err);
                }
            }
        }
        else
        {
            org.apache.felix.bundlerepository.Repository[] repos =
                ra.listRepositories();
            if ((repos != null) && (repos.length > 0))
            {
                for (int i = 0; i < repos.length; i++)
                {
                    System.out.println(repos[i].getURI());
                }
            }
            else
            {
                System.out.println("No repository URLs are set.");
            }
        }
    }

    @Descriptor("list repository resources")
    public void list(
        @Descriptor("display all versions")
        @Parameter(names={ "-v", "--verbose" }, presentValue="true",
            absentValue="false") boolean verbose,
        @Descriptor("optional strings used for name matching") String[] args)
        throws IOException, InvalidSyntaxException
    {
        Object svcObj = getRepositoryAdmin();
        if (svcObj == null)
        {
            return;
        }
        RepositoryAdmin ra = (RepositoryAdmin) svcObj;

        // Create a filter that will match presentation name or symbolic name.
        StringBuffer sb = new StringBuffer();
        if ((args == null) || (args.length == 0))
        {
            sb.append("(|(presentationname=*)(symbolicname=*))");
        }
        else
        {
            StringBuffer value = new StringBuffer();
            for (int i = 0; i < args.length; i++)
            {
                if (i > 0)
                {
                    value.append(" ");
                }
                value.append(args[i]);
            }
            sb.append("(|(presentationname=*");
            sb.append(value);
            sb.append("*)(symbolicname=*");
            sb.append(value);
            sb.append("*))");
        }
        // Use filter to get matching resources.
        Resource[] resources = ra.discoverResources(sb.toString());

        // Group the resources by symbolic name in descending version order,
        // but keep them in overall sorted order by presentation name.
        Map revisionMap = new TreeMap(new Comparator() {
            public int compare(Object o1, Object o2)
            {
                Resource r1 = (Resource) o1;
                Resource r2 = (Resource) o2;
                // Assume if the symbolic name is equal, then the two are equal,
                // since we are trying to aggregate by symbolic name.
                int symCompare = r1.getSymbolicName().compareTo(r2.getSymbolicName());
                if (symCompare == 0)
                {
                    return 0;
                }
                // Otherwise, compare the presentation name to keep them sorted
                // by presentation name. If the presentation names are equal, then
                // use the symbolic name to differentiate.
                int compare = (r1.getPresentationName() == null)
                    ? -1
                    : (r2.getPresentationName() == null)
                        ? 1
                        : r1.getPresentationName().compareToIgnoreCase(
                            r2.getPresentationName());
                if (compare == 0)
                {
                    return symCompare;
                }
                return compare;
            }
        });
        for (int resIdx = 0; (resources != null) && (resIdx < resources.length); resIdx++)
        {
            Resource[] revisions = (Resource[]) revisionMap.get(resources[resIdx]);
            revisionMap.put(resources[resIdx], addResourceByVersion(revisions, resources[resIdx]));
        }

        // Print any matching resources.
        for (Iterator i = revisionMap.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) i.next();
            Resource[] revisions = (Resource[]) entry.getValue();
            String name = revisions[0].getPresentationName();
            name = (name == null) ? revisions[0].getSymbolicName() : name;
            System.out.print(name);

            if (verbose && revisions[0].getPresentationName() != null)
            {
                System.out.print(" [" + revisions[0].getSymbolicName() + "]");
            }

            System.out.print(" (");
            int revIdx = 0;
            do
            {
                if (revIdx > 0)
                {
                    System.out.print(", ");
                }
                System.out.print(revisions[revIdx].getVersion());
                revIdx++;
            }
            while (verbose && (revIdx < revisions.length));
            if (!verbose && (revisions.length > 1))
            {
                System.out.print(", ...");
            }
            System.out.println(")");
        }

        if ((resources == null) || (resources.length == 0))
        {
            System.out.println("No matching bundles.");
        }
    }

    @Descriptor("retrieve resource description from repository")
    public void info(
        @Descriptor("( <bundle-name> | <symbolic-name> | <bundle-id> )[@<version>] ...")
            String[] args)
        throws IOException, InvalidSyntaxException
    {
        Object svcObj = getRepositoryAdmin();
        if (svcObj == null)
        {
            return;
        }
        RepositoryAdmin ra = (RepositoryAdmin) svcObj;

        for (int argIdx = 0; (args != null) && (argIdx < args.length); argIdx++)
        {
            // Find the target's bundle resource.
            String targetName = args[argIdx];
            String targetVersion = null;
            int idx = args[argIdx].indexOf(VERSION_SEPARATOR);
            if (idx > 0)
            {
                targetName = args[argIdx].substring(0, idx);
                targetVersion = args[argIdx].substring(idx + 1);
            }
            Resource[] resources = searchRepository(ra, targetName, targetVersion);
            if (resources == null)
            {
                System.err.println("Unknown bundle and/or version: " + args[argIdx]);
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
    }

    @Descriptor("deploy resource from repository")
    public void deploy(
        @Descriptor("start deployed bundles")
        @Parameter(names={ "-s", "--start" }, presentValue="true",
            absentValue="false") boolean start,
        @Descriptor("( <bundle-name> | <symbolic-name> | <bundle-id> )[@<version>] ...")
            String[] args)
        throws IOException, InvalidSyntaxException
    {
        Object svcObj = getRepositoryAdmin();
        if (svcObj == null)
        {
            return;
        }
        RepositoryAdmin ra = (RepositoryAdmin) svcObj;

        Resolver resolver = ra.resolver();
        for (int argIdx = 0; (args != null) && (argIdx < args.length); argIdx++)
        {
            // Find the target's bundle resource.
            String targetName = args[argIdx];
            String targetVersion = null;
            int idx = args[argIdx].indexOf(VERSION_SEPARATOR);
            if (idx > 0)
            {
                targetName = args[argIdx].substring(0, idx);
                targetVersion = args[argIdx].substring(idx + 1);
            }
            Resource resource = selectNewestVersion(
                searchRepository(ra, targetName, targetVersion));
            if (resource != null)
            {
                resolver.add(resource);
            }
            else
            {
                System.err.println("Unknown bundle - " + args[argIdx]);
            }
        }

        if ((resolver.getAddedResources() != null) &&
            (resolver.getAddedResources().length > 0))
        {
            if (resolver.resolve())
            {
                System.out.println("Target resource(s):");
                System.out.println(Util.getUnderlineString(19));
                Resource[] resources = resolver.getAddedResources();
                for (int resIdx = 0; (resources != null) && (resIdx < resources.length); resIdx++)
                {
                    System.out.println("   " + resources[resIdx].getPresentationName()
                        + " (" + resources[resIdx].getVersion() + ")");
                }
                resources = resolver.getRequiredResources();
                if ((resources != null) && (resources.length > 0))
                {
                    System.out.println("\nRequired resource(s):");
                    System.out.println(Util.getUnderlineString(21));
                    for (int resIdx = 0; resIdx < resources.length; resIdx++)
                    {
                        System.out.println("   " + resources[resIdx].getPresentationName()
                            + " (" + resources[resIdx].getVersion() + ")");
                    }
                }
                resources = resolver.getOptionalResources();
                if ((resources != null) && (resources.length > 0))
                {
                    System.out.println("\nOptional resource(s):");
                    System.out.println(Util.getUnderlineString(21));
                    for (int resIdx = 0; resIdx < resources.length; resIdx++)
                    {
                        System.out.println("   " + resources[resIdx].getPresentationName()
                            + " (" + resources[resIdx].getVersion() + ")");
                    }
                }

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
            else
            {
                Reason[] reqs = resolver.getUnsatisfiedRequirements();
                if ((reqs != null) && (reqs.length > 0))
                {
                    System.out.println("Unsatisfied requirement(s):");
                    System.out.println(Util.getUnderlineString(27));
                    for (int reqIdx = 0; reqIdx < reqs.length; reqIdx++)
                    {
                        System.out.println("   " + reqs[reqIdx].getRequirement().getFilter());
                        System.out.println("      " + reqs[reqIdx].getResource().getPresentationName());
                    }
                }
                else
                {
                    System.out.println("Could not resolve targets.");
                }
            }
        }
    }

    @Descriptor("retrieve resource source code from repository")
    public void source(
        @Descriptor("extract source code")
        @Parameter(names={ "-x", "--extract" }, presentValue="true",
            absentValue="false") boolean extract,
        @Descriptor("local target directory") File localDir,
        @Descriptor("( <bundle-name> | <symbolic-name> | <bundle-id> )[@<version>] ...")
            String[] args)
        throws IOException, InvalidSyntaxException
    {
        Object svcObj = getRepositoryAdmin();
        if (svcObj == null)
        {
            return;
        }
        RepositoryAdmin ra = (RepositoryAdmin) svcObj;

        for (int argIdx = 0; argIdx < args.length; argIdx++)
        {
            // Find the target's bundle resource.
            String targetName = args[argIdx];
            String targetVersion = null;
            int idx = args[argIdx].indexOf(VERSION_SEPARATOR);
            if (idx > 0)
            {
                targetName = args[argIdx].substring(0, idx);
                targetVersion = args[argIdx].substring(idx + 1);
            }
            Resource resource = selectNewestVersion(
                searchRepository(ra, targetName, targetVersion));
            if (resource == null)
            {
                System.err.println("Unknown bundle and/or version: " + args[argIdx]);
            }
            else
            {
                String srcURI = (String) resource.getProperties().get(Resource.SOURCE_URI);
                if (srcURI != null)
                {
                    Util.downloadSource(
                        System.out, System.err, new URL(srcURI),
                        localDir, extract);
                }
                else
                {
                    System.err.println("Missing source URL: " + args[argIdx]);
                }
            }
        }
    }

    @Descriptor("retrieve resource JavaDoc from repository")
    public void javadoc(
        @Descriptor("extract documentation")
        @Parameter(names={"-x", "--extract" }, presentValue="true",
            absentValue="false") boolean extract,
        @Descriptor("local target directory") File localDir,
        @Descriptor("( <bundle-name> | <symbolic-name> | <bundle-id> )[@<version>] ...")
            String[] args)
        throws IOException, InvalidSyntaxException
    {
        Object svcObj = getRepositoryAdmin();
        if (svcObj == null)
        {
            return;
        }
        RepositoryAdmin ra = (RepositoryAdmin) svcObj;

        for (int argIdx = 0; argIdx < args.length; argIdx++)
        {
            // Find the target's bundle resource.
            String targetName = args[argIdx];
            String targetVersion = null;
            int idx = args[argIdx].indexOf(VERSION_SEPARATOR);
            if (idx > 0)
            {
                targetName = args[argIdx].substring(0, idx);
                targetVersion = args[argIdx].substring(idx + 1);
            }
            Resource resource = selectNewestVersion(
                searchRepository(ra, targetName, targetVersion));
            if (resource == null)
            {
                System.err.println("Unknown bundle and/or version: " + args[argIdx]);
            }
            else
            {
                URL docURL = (URL) resource.getProperties().get("javadoc");
                if (docURL != null)
                {
                    Util.downloadSource(
                        System.out, System.err, docURL, localDir, extract);
                }
                else
                {
                    System.err.println("Missing javadoc URL: " + args[argIdx]);
                }
            }
        }
    }

    private Resource[] searchRepository(
        RepositoryAdmin ra, String targetId, String targetVersion)
        throws InvalidSyntaxException
    {
        // Try to see if the targetId is a bundle ID.
        try
        {
            Bundle bundle = m_bc.getBundle(Long.parseLong(targetId));
            targetId = bundle.getSymbolicName();
        }
        catch (NumberFormatException ex)
        {
            // It was not a number, so ignore.
        }

        // The targetId may be a bundle name or a bundle symbolic name,
        // so create the appropriate LDAP query.
        StringBuffer sb = new StringBuffer("(|(presentationname=");
        sb.append(targetId);
        sb.append(")(symbolicname=");
        sb.append(targetId);
        sb.append("))");
        if (targetVersion != null)
        {
            sb.insert(0, "(&");
            sb.append("(version=");
            sb.append(targetVersion);
            sb.append("))");
        }
        return ra.discoverResources(sb.toString());
    }

    private Resource selectNewestVersion(Resource[] resources)
    {
        int idx = -1;
        Version v = null;
        for (int i = 0; (resources != null) && (i < resources.length); i++)
        {
            if (i == 0)
            {
                idx = 0;
                v = resources[i].getVersion();
            }
            else
            {
                Version vtmp = resources[i].getVersion();
                if (vtmp.compareTo(v) > 0)
                {
                    idx = i;
                    v = vtmp;
                }
            }
        }

        return (idx < 0) ? null : resources[idx];
    }

    private void printResource(PrintStream out, Resource resource)
    {
        System.out.println(Util.getUnderlineString(resource.getPresentationName().length()));
        out.println(resource.getPresentationName());
        System.out.println(Util.getUnderlineString(resource.getPresentationName().length()));

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
            out.println("Requires:");
            for (int i = 0; i < reqs.length; i++)
            {
                out.println("   " + reqs[i].getFilter());
            }
        }

        Capability[] caps = resource.getCapabilities();
        if ((caps != null) && (caps.length > 0))
        {
            out.println("Capabilities:");
            for (int i = 0; i < caps.length; i++)
            {
                out.println("   " + caps[i].getPropertiesAsMap());
            }
        }
    }

    private static Resource[] addResourceByVersion(Resource[] revisions, Resource resource)
    {
        // We want to add the resource into the array of revisions
        // in descending version sorted order (i.e., newest first)
        Resource[] sorted = null;
        if (revisions == null)
        {
            sorted = new Resource[] { resource };
        }
        else
        {
            Version version = resource.getVersion();
            Version middleVersion = null;
            int top = 0, bottom = revisions.length - 1, middle = 0;
            while (top <= bottom)
            {
                middle = (bottom - top) / 2 + top;
                middleVersion = revisions[middle].getVersion();
                // Sort in reverse version order.
                int cmp = middleVersion.compareTo(version);
                if (cmp < 0)
                {
                    bottom = middle - 1;
                }
                else
                {
                    top = middle + 1;
                }
            }

            // Ignore duplicates.
            if ((top >= revisions.length) || (revisions[top] != resource))
            {
                sorted = new Resource[revisions.length + 1];
                System.arraycopy(revisions, 0, sorted, 0, top);
                System.arraycopy(revisions, top, sorted, top + 1, revisions.length - top);
                sorted[top] = resource;
            }
        }
        return sorted;
    }
}