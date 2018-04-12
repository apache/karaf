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
package org.apache.karaf.packages.core.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.karaf.packages.core.PackageRequirement;
import org.apache.karaf.packages.core.PackageService;
import org.apache.karaf.packages.core.PackageVersion;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

public class PackageServiceImpl implements PackageService {

    private final BundleContext bundleContext;

    public PackageServiceImpl(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public List<PackageVersion> getExports() {
        Bundle[] bundles = bundleContext.getBundles();
        SortedMap<String, PackageVersion> packageVersionMap = new TreeMap<>();
        for (Bundle bundle : bundles) {
            BundleRevision rev = bundle.adapt(BundleRevision.class);
            if (rev != null) {
                List<BundleCapability> caps = rev.getDeclaredCapabilities(BundleRevision.PACKAGE_NAMESPACE);
                for (BundleCapability cap : caps) {
                    Map<String, Object> attr = cap.getAttributes();
                    String packageName = (String)attr.get(BundleRevision.PACKAGE_NAMESPACE);
                    Version version = (Version)attr.get("version");
                    String key = packageName + ":" + version.toString();
                    PackageVersion pVer =
                            packageVersionMap.computeIfAbsent(key, k -> new PackageVersion(packageName, version));
                    pVer.addBundle(bundle);
                }
            }
        }
        return new ArrayList<>(packageVersionMap.values());
    }

    @Override
    public List<PackageRequirement> getImports() {
        Bundle[] bundles = bundleContext.getBundles();
        SortedMap<String, PackageRequirement> requirements = new TreeMap<>();
        for (Bundle bundle : bundles) {
            BundleRevision rev = bundle.adapt(BundleRevision.class);
            if (rev != null) {
                List<BundleRequirement> reqs = rev.getDeclaredRequirements(BundleRevision.PACKAGE_NAMESPACE);
                for (BundleRequirement req : reqs) {
                    PackageRequirement preq = create(req, bundle);
                    requirements.put(preq.getPackageName() + "|" + preq.getFilter() + "|" + preq.getBundle().getBundleId(), preq);
                }
            }
        }
        return new ArrayList<>(requirements.values());
    }

    private boolean checkResolveAble(BundleRequirement req) {
        Bundle[] bundles = bundleContext.getBundles();
        for (Bundle bundle : bundles) {
            BundleRevision rev = bundle.adapt(BundleRevision.class);
            if (rev != null) {
                List<BundleCapability> caps = rev.getDeclaredCapabilities(BundleRevision.PACKAGE_NAMESPACE);
                for (BundleCapability cap : caps) {
                    if (req.matches(cap)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public List<String> getExports(long bundleId) {
        Bundle bundle = bundleContext.getBundle(bundleId);
        BundleRevision rev = bundle.adapt(BundleRevision.class);
        List<BundleCapability> caps = rev.getDeclaredCapabilities(BundleRevision.PACKAGE_NAMESPACE);
        List<String> exports = new ArrayList<>();
        for (BundleCapability cap : caps) {
            Map<String, Object> attr = cap.getAttributes();
            String packageName = (String)attr.get(BundleRevision.PACKAGE_NAMESPACE);
            exports.add(packageName);
        }
        return exports;
    }

    @Override
    public List<String> getImports(long bundleId) {
        Bundle bundle = bundleContext.getBundle(bundleId);
        BundleRevision rev = bundle.adapt(BundleRevision.class);
        List<BundleRequirement> reqs = rev.getDeclaredRequirements(BundleRevision.PACKAGE_NAMESPACE);
        List<String> imports = new ArrayList<>();
        for (BundleRequirement req : reqs) {
            PackageRequirement packageReq = create(req, bundle);
            imports.add(packageReq.getPackageName());
        }
        return imports;
    }
    
    PackageRequirement create(BundleRequirement req, Bundle bundle) {
        Map<String, String> attr = req.getDirectives();
        String filter = attr.get("filter");
        String resolution = attr.get("resolution");
        boolean optional = "optional".equals(resolution);
        boolean resolveable = checkResolveAble(req);
        ImportDetails details = new ImportDetails(filter);
        return new PackageRequirement(filter, optional, bundle, resolveable, 
                                      details.name, details.minVersion, details.maxVersion);
    }
    

}
