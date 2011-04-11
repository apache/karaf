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
package org.apache.karaf.features.obr.internal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resource;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Resolver;
import org.osgi.framework.InvalidSyntaxException;

public class ObrResolver implements Resolver {

    private RepositoryAdmin repositoryAdmin;
    private boolean resolveOptionalImports;

    public RepositoryAdmin getRepositoryAdmin() {
        return repositoryAdmin;
    }

    public void setRepositoryAdmin(RepositoryAdmin repositoryAdmin) {
        this.repositoryAdmin = repositoryAdmin;
    }

    public boolean isResolveOptionalImports() {
        return resolveOptionalImports;
    }

    /**
     * When set to <code>true</code>, the OBR resolver will try to resolve optional imports as well.
     * Defaults to <code>false</code>
     *
     * @param resolveOptionalImports
     */
    public void setResolveOptionalImports(boolean resolveOptionalImports) {
        this.resolveOptionalImports = resolveOptionalImports;
    }

    public List<BundleInfo> resolve(Feature feature) throws Exception {
        List<Requirement> reqs = new ArrayList<Requirement>();
        List<Resource> ress = new ArrayList<Resource>();
        List<Resource> deploy = new ArrayList<Resource>();
        Map<Object, BundleInfo> infos = new HashMap<Object, BundleInfo>();
        for (BundleInfo bundleInfo : feature.getBundles()) {
            try {
                URL url = new URL(bundleInfo.getLocation());
                Resource res = repositoryAdmin.getHelper().createResource(url);
                ress.add(res);
                infos.put(res, bundleInfo);
            } catch (MalformedURLException e) {
                Requirement req = parseRequirement(bundleInfo.getLocation());
                reqs.add(req);
                infos.put(req, bundleInfo);
            }
        }

        Repository repository = repositoryAdmin.getHelper().repository(ress.toArray(new Resource[ress.size()]));
        List<Repository> repos = new ArrayList<Repository>();
        repos.add(repositoryAdmin.getSystemRepository());
        repos.add(repositoryAdmin.getLocalRepository());
        repos.add(repository);
        repos.addAll(Arrays.asList(repositoryAdmin.listRepositories()));
        org.apache.felix.bundlerepository.Resolver resolver = repositoryAdmin.resolver(repos.toArray(new Repository[repos.size()]));

        for (Resource res : ress) {
            if (!infos.get(res).isDependency()) {
                resolver.add(res);
            }
        }
        for (Requirement req : reqs) {
            resolver.add(req);
        }

        if (!doResolve(resolver)) {
            StringWriter w = new StringWriter();
            PrintWriter out = new PrintWriter(w);
            Reason[] failedReqs = resolver.getUnsatisfiedRequirements();
            if ((failedReqs != null) && (failedReqs.length > 0)) {
                out.println("Unsatisfied requirement(s):");
                printUnderline(out, 27);
                for (Reason r : failedReqs) {
                    out.println("   " + r.getRequirement().getName() + ":" + r.getRequirement().getFilter());
                    out.println("      " + r.getResource().getPresentationName());
                }
            } else {
                out.println("Could not resolve targets.");
            }
            out.flush();
            throw new Exception("Can not resolve feature:\n" + w.toString());
        }

        List<BundleInfo> bundles = new ArrayList<BundleInfo>();
        Collections.addAll(deploy, resolver.getAddedResources());
        Collections.addAll(deploy, resolver.getRequiredResources());
        if (resolveOptionalImports) {
            Collections.addAll(deploy, resolver.getOptionalResources());
        }
        for (Resource res : deploy) {
            BundleInfo info = infos.get(res);
            if (info == null) {
                Reason[] reasons = resolver.getReason(res);
                if (reasons != null) {
                    for (Reason r : reasons) {
                        info = infos.get(r);
                        if (info != null) {
                            break;
                        }
                    }
                }
            }
            if (info == null) {
                info = new BundleInfoImpl(res.getURI());
            }
            bundles.add(info);
        }
        return bundles;
    }

    private boolean doResolve(org.apache.felix.bundlerepository.Resolver resolver) {
        if (resolveOptionalImports) {
            return resolver.resolve();
        } else {
            return resolver.resolve(org.apache.felix.bundlerepository.Resolver.NO_OPTIONAL_RESOURCES);
        }
    }

    protected void printUnderline(PrintWriter out, int length) {
        for (int i = 0; i < length; i++) {
            out.print('-');
        }
        out.println("");
    }

    protected Requirement parseRequirement(String req) throws InvalidSyntaxException {
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
            } else {
                name = "bundle";
            }
            filter = req;
        }
        if (!filter.startsWith("(")) {
            filter = "(" + filter + ")";
        }
        return repositoryAdmin.getHelper().requirement(name, filter);
    }
}
