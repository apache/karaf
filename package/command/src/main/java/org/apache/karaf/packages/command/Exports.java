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
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.apache.karaf.shell.table.Col;
import org.apache.karaf.shell.table.ShellTable;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;

@Command(scope = "package", name = "exports", description = "Lists exported packages and the bundles that export them")
public class Exports extends OsgiCommandSupport {

    private PackageService packageService;
    
    @Option(name = "-d", description = "Only show packages that are exported by more than one bundle", required = false, multiValued = false)
    private boolean onlyDuplicates;

    @Option(name = "--no-format", description = "Disable table rendered output", required = false, multiValued = false)
    boolean noFormat;

    public Exports(PackageService packageService) {
        super();
        this.packageService = packageService;
    }

    protected Object doExecute() throws Exception {
    	if (onlyDuplicates) {
    		checkDuplicateExports();
    	} else {
    		showExports();
    	}
        return null;
    }

	private void showExports() {
		SortedMap<String, PackageVersion> exports = packageService.getExports();
        ShellTable table = new ShellTable();
        table.column(new Col("Package Name"));
        table.column(new Col("Version"));
        table.column(new Col("ID"));
        table.column(new Col("Bundle Name"));
        
        for (String key : exports.keySet()) {
            PackageVersion pVer = exports.get(key);
            for (Bundle bundle : pVer.getBundles()) {
                table.addRow().addContent(pVer.getPackageName(),pVer.getVersion().toString(), bundle.getBundleId(), bundle.getSymbolicName());
            }
        }
        table.print(System.out, !noFormat);
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

	private SortedMap<String, PackageVersion> getDuplicatePackages(
			Bundle[] bundles) {
		SortedMap<String, PackageVersion> packageVersionMap = new TreeMap<String, PackageVersion>();
        for (Bundle bundle : bundles) {
            BundleRevision rev = bundle.adapt(BundleRevision.class);
            if (rev!=null) {
                List<BundleCapability> caps = rev.getDeclaredCapabilities(BundleRevision.PACKAGE_NAMESPACE);
                for (BundleCapability cap : caps) {
                    Map<String, Object> attr = cap.getAttributes();
                    String packageName = (String)attr.get(BundleRevision.PACKAGE_NAMESPACE);
                    Version version = (Version)attr.get("version");
                    String key = packageName + ":" + version.toString();
                    PackageVersion pVer = packageVersionMap.get(key);
                    if (pVer == null) {
                        pVer = new PackageVersion(packageName, version);
                        packageVersionMap.put(key, pVer);
                    }
                    pVer.addBundle(bundle);
                }
            }
        }
		return packageVersionMap;
	}
}
