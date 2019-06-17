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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.List;

import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;

public abstract class ObrCommandSupport implements Action {

    protected static final char VERSION_DELIM = ',';

    @Reference
    private RepositoryAdmin repositoryAdmin;

    @Reference
    BundleContext bundleContext;

    public void setRepositoryAdmin(RepositoryAdmin repositoryAdmin) {
        this.repositoryAdmin = repositoryAdmin;
    }

    @Override
    public Object execute() throws Exception {
        doExecute(repositoryAdmin);
        return null;
    }

    abstract void doExecute(RepositoryAdmin admin) throws Exception;

    protected Resource[] searchRepository(RepositoryAdmin admin, String targetId, String targetVersion) throws InvalidSyntaxException {
        // Try to see if the targetId is a bundle ID.
        try {
            Bundle bundle = bundleContext.getBundle(Long.parseLong(targetId));
            targetId = bundle.getSymbolicName();
        } catch (NumberFormatException ex) {
            // It was not a number, so ignore.
        }

        // The targetId may be a bundle name or a bundle symbolic name,
        // so create the appropriate LDAP query.
        StringBuilder sb = new StringBuilder("(|(presentationname=");
        sb.append(targetId);
        sb.append(")(symbolicname=");
        sb.append(targetId);
        sb.append("))");
        if (targetVersion != null) {
            sb.insert(0, "(&");
            sb.append("(version=");
            sb.append(targetVersion);
            sb.append("))");
        }
        return admin.discoverResources(sb.toString());
    }

    public Resource selectNewestVersion(Resource[] resources) {
        int idx = -1;
        Version v = null;
        for (int i = 0; (resources != null) && (i < resources.length); i++) {
            if (i == 0) {
                idx = 0;
                v = resources[i].getVersion();
            } else {
                Version vtmp = resources[i].getVersion();
                if (vtmp.compareTo(v) > 0) {
                    idx = i;
                    v = vtmp;
                }
            }
        }
        return (idx < 0) ? null : resources[idx];
    }

    protected String[] getTarget(String bundle) {
        String[] target;
        int idx = bundle.indexOf(VERSION_DELIM);
        if (idx > 0) {
            target = new String[]{bundle.substring(0, idx), bundle.substring(idx + 1)};
        } else {
            target = new String[]{bundle, null};
        }
        return target;
    }

    protected void printUnderline(PrintStream out, int length) {
        for (int i = 0; i < length; i++) {
            out.print('-');
        }
        out.println();
    }

    protected void doDeploy(RepositoryAdmin admin, List<String> bundles, boolean start, boolean deployOptional) throws Exception {
        Resolver resolver = admin.resolver();
        for (String bundle : bundles) {
            String[] target = getTarget(bundle);
            Resource resource = selectNewestVersion(searchRepository(admin, target[0], target[1]));
            if (resource != null) {
                resolver.add(resource);
            } else {
                System.err.println("Unknown bundle - " + target[0]);
            }
        }
        if ((resolver.getAddedResources() != null) &&
                (resolver.getAddedResources().length > 0)) {
            if (resolver.resolve(deployOptional ? 0 : Resolver.NO_OPTIONAL_RESOURCES)) {
                System.out.println("Target resource(s):");
                printUnderline(System.out, 19);
                Resource[] resources = resolver.getAddedResources();
                for (int resIdx = 0; (resources != null) && (resIdx < resources.length); resIdx++) {
                    System.out.println("   " + getResourceId(resources[resIdx])
                            + " (" + resources[resIdx].getVersion() + ")");
                }
                resources = resolver.getRequiredResources();
                if ((resources != null) && (resources.length > 0)) {
                    System.out.println("\nRequired resource(s):");
                    printUnderline(System.out, 21);
                    for (Resource resource : resources) {
                        System.out.println("   " + getResourceId(resource)
                                + " (" + resource.getVersion() + ")");
                    }
                }
                if (deployOptional) {
                    resources = resolver.getOptionalResources();
                    if ((resources != null) && (resources.length > 0)) {
                        System.out.println("\nOptional resource(s):");
                        printUnderline(System.out, 21);
                        for (Resource resource : resources) {
                            System.out.println("   " + getResourceId(resource) + " (" + resource.getVersion() + ")");
                        }
                    }
                }

                try {
                    System.out.print("\nDeploying...");
                    resolver.deploy(start ? Resolver.START : 0);
                    System.out.println("done.");
                } catch (IllegalStateException ex) {
                    System.err.println(ex);
                }
            } else {
                Reason[] reqs = resolver.getUnsatisfiedRequirements();
                if ((reqs != null) && (reqs.length > 0)) {
                    System.out.println("Unsatisfied requirement(s):");
                    printUnderline(System.out, 27);
                    for (Reason req : reqs) {
                        System.out.println("   " + req.getRequirement().getFilter());
                        System.out.println("      " + getResourceId(req.getResource()));
                    }
                } else {
                    System.out.println("Could not resolve targets.");
                }
            }
        }

    }

    protected String getResourceId(Resource resource) {
        return resource.getPresentationName() != null ? resource.getPresentationName() : resource.getSymbolicName();
    }

    protected Requirement parseRequirement(RepositoryAdmin admin, String req) throws InvalidSyntaxException {
        int p = req.indexOf(':');
        String name;
        String filter;
        if (p > 0) {
            name = req.substring(0, p);
            filter = req.substring(p + 1);
        } else {
            if (req.contains("package")) {
                name = "package";
            } else if (req.contains("service")) {
                name = "service";
            } else if (req.contains("osgi.extender")) {
                name = "osgi.extender";
            } else {
                name = "bundle";
            }
            filter = req;
        }
        if (!filter.startsWith("(")) {
            filter = "(" + filter + ")";
        }
        return admin.getHelper().requirement(name, filter);
    }

    protected Requirement[] parseRequirements(RepositoryAdmin admin, List<String> requirements) throws InvalidSyntaxException {
        Requirement[] reqs = new Requirement[requirements.size()];
        for (int i = 0; i < reqs.length; i++) {
            reqs[i] = parseRequirement(admin, requirements.get(i));
        }
        return reqs;
    }

    public static final String REPOSITORY_URL_PROP = "obr.repository.url";

    protected void persistRepositoryList(RepositoryAdmin admin) {
        try {
            StringBuilder sb = new StringBuilder();
            for (Repository repo : admin.listRepositories()) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(repo.getURI());
            }
            File etc = new File(System.getProperty("karaf.etc"));
            File sys = new File(etc, "config.properties");
            File sysTmp = new File(etc, "config.properties.tmp");

            boolean modified = false;
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sysTmp)))) {
                if (sys.exists()) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(sys)))) {
                        String line = reader.readLine();
                        while (line != null) {
                            if (line.matches("obr\\.repository\\.url[:= ].*")) {
                                modified = true;
                                line = "obr.repository.url = " + sb.toString();
                            }
                            writer.write(line);
                            writer.newLine();
                            line = reader.readLine();
                        }
                    }
                }
                if (!modified) {
                    writer.newLine();
                    writer.write("# ");
                    writer.newLine();
                    writer.write("# OBR Repository list");
                    writer.newLine();
                    writer.write("# ");
                    writer.newLine();
                    writer.write("obr.repository.url = " + sb.toString());
                    writer.newLine();
                    writer.newLine();
                }
            }

            sys.delete();
            sysTmp.renameTo(sys);

        } catch (Exception e) {
            System.err.println("Error while persisting repository list");
        }
    }

}
