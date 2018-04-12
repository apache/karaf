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
package org.apache.karaf.packages.command;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.karaf.packages.core.PackageService;
import org.apache.karaf.packages.core.PackageVersion;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;

@Command(scope = "package", name = "exports", description = "Lists exported packages and the bundles that export them")
@Service
public class Exports implements Action {

    @Option(name = "-d", description = "Only show packages that are exported by more than one bundle", required = false, multiValued = false)
    private boolean onlyDuplicates;

    @Option(name = "--no-format", description = "Disable table rendered output", required = false, multiValued = false)
    boolean noFormat;
    
    @Option(name = "--show-name-only", description = "Show only package name", required = false, multiValued = false)
    boolean showOnlyName = false;
    
    @Option(name = "-b", description = "Only show packages exported by given bundle id", required = false, multiValued = false)
    private Integer bundleId;
    
    @Option(name = "-p", description = "Only show package starting with given name", required = false, multiValued = false)
    private String packageFilter;

    @Reference
    private PackageService packageService;

    @Reference
    private BundleContext bundleContext;

    @Override
    public Object execute() throws Exception {
        if (onlyDuplicates) {
            checkDuplicateExports();
        } else {
            showExports();
        }
        return null;
    }

    private void showExports() {
        List<PackageVersion> exports = packageService.getExports();
        ShellTable table = new ShellTable();
        table.column("Package Name");
        if (!showOnlyName) {
            table.column("Version");
            table.column("ID");
            table.column("Bundle Name");
        }
        for (PackageVersion pVer : exports) {
            for (Bundle bundle : pVer.getBundles()) {
                if (matchesFilter(pVer, bundle)) {
                    if (!showOnlyName) {
                        table.addRow().addContent(pVer.getPackageName(),
                                              pVer.getVersion().toString(),
                                              bundle.getBundleId(),
                                              bundle.getSymbolicName());
                    } else {
                        table.addRow().addContent(pVer.getPackageName());
                    }
                }
            }
        }
        table.print(System.out, !noFormat);
    }
    
    private boolean matchesFilter(PackageVersion pVer, Bundle bundle) {
        return (bundleId == null || bundle.getBundleId() == bundleId)
            && (packageFilter == null || pVer.getPackageName().startsWith(packageFilter));
    }

    private void checkDuplicateExports() {
        Bundle[] bundles = bundleContext.getBundles();
        SortedMap<String, PackageVersion> packageVersionMap = getDuplicatePackages(bundles);
        ShellTable table = new ShellTable();
        table.column(new Col("Package Name"));
        table.column(new Col("Version"));
        table.column(new Col("Exporting bundles (ID)"));
       
        for (String key : packageVersionMap.keySet()) {
            PackageVersion pVer = packageVersionMap.get(key);
            if (pVer.getBundles().size() > 1) {
            	String pBundles = getBundlesSt(pVer.getBundles());
            	table.addRow().addContent(pVer.getPackageName(), pVer.getVersion().toString(), pBundles); 
            }
        }
        table.print(System.out, !noFormat);
    }

    private String getBundlesSt(Set<Bundle> bundles) {
        StringBuilder st = new StringBuilder();
        for (Bundle bundle : bundles) {
            st.append(bundle.getBundleId() + " ");
        }
        return st.toString();
    }

    private SortedMap<String, PackageVersion> getDuplicatePackages(Bundle[] bundles) {
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
        return packageVersionMap;
    }
}
