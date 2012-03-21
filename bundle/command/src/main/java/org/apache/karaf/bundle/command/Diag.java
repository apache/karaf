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
package org.apache.karaf.bundle.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.karaf.bundle.core.BundleInfo;
import org.apache.karaf.bundle.core.BundleState;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.util.ShellUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;

@Command(scope = "bundle", name = "diag", description = "Displays diagnostic information why a bundle is not Active")
public class Diag extends BundlesCommand {

    public Diag() {
        super(true);
    }

    protected void doExecute(List<Bundle> bundles) throws Exception {
        checkDuplicateExports();
        for (Bundle bundle : bundles) {
            BundleInfo info = bundleService.getInfo(bundle);
            if (info.getState() == BundleState.Failure || info.getState() == BundleState.Waiting
                || info.getState() == BundleState.GracePeriod || info.getState() == BundleState.Installed) {
                String title = ShellUtil.getBundleName(bundle);
                System.out.println(title);
                System.out.println(ShellUtil.getUnderlineString(title));
                System.out.println("Status: " + info.getState().toString());
                System.out.println(this.bundleService.getDiag(bundle));
                System.out.println();
            }
        }
    }

    private void checkDuplicateExports() {
        Bundle[] bundles = bundleContext.getBundles();
        SortedMap<String, PackageVersion> packageVersionMap = new TreeMap<String, Diag.PackageVersion>();
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
                        pVer = new PackageVersion();
                        pVer.packageName = packageName;
                        pVer.version = version;
                        packageVersionMap.put(key, pVer);
                    }
                    pVer.bundles.add(bundle);
                }
            }
        }
        String title = "Same Package/Version exported by more than one bundle:";
        System.out.println(title);
        System.out.println(ShellUtil.getUnderlineString(title));
        for (String key : packageVersionMap.keySet()) {
            PackageVersion pVer = packageVersionMap.get(key);
            if (pVer.bundles.size() > 1) {
                System.out.print(pVer.packageName + ":" + pVer.version.toString() + " exported by ");
                for (Bundle bundle : pVer.bundles) {
                    System.out.print(bundle.getBundleId() + " ");
                }
                System.out.println("\n");
            }
        }
        System.out.println();
    }

    class PackageVersion {
        String packageName;
        Version version;
        List<Bundle> bundles = new ArrayList<Bundle>();
    }
}
