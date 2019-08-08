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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.ansi.SimpleAnsi;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;

@Command(scope = "bundle", name = "classes", description = "Displays a list of classes/resources contained in the bundle")
@Service
public class Classes extends BundlesCommand {

    @Option(name = "-a", aliases={"--display-all-files"}, description="List all classes and files in the bundle", required = false, multiValued = false)
    boolean displayAllFiles;

    @Override
    protected void executeOnBundle(Bundle bundle) throws Exception {
        BundleWiring wiring = bundle.adapt(BundleWiring.class);
        if (wiring != null){
            Collection<String> resources;
            List<String> exports = getExports(bundle);
            if (displayAllFiles){
                resources = wiring.listResources("/", null, BundleWiring.LISTRESOURCES_RECURSE);
            } else {
                resources = wiring.listResources("/", "*class", BundleWiring.LISTRESOURCES_RECURSE);
            }
            Collection<String> localResources;
            if (displayAllFiles) {
                localResources = wiring.listResources("/", null, BundleWiring.LISTRESOURCES_RECURSE | BundleWiring.LISTRESOURCES_LOCAL);
            } else {
                localResources = wiring.listResources("/", "/*.class", BundleWiring.LISTRESOURCES_RECURSE | BundleWiring.LISTRESOURCES_LOCAL);
            }
            for (String resource:resources){
                StringBuilder stringBuilder = new StringBuilder();
                boolean localResource = localResources.contains(resource);
                if(localResource) {
                    stringBuilder.append(SimpleAnsi.INTENSITY_BOLD);
                }
                if(ids == null || ids.size() != 1) {
                    stringBuilder.append(bundle.getBundleId()).append(" | ");
                }
                stringBuilder.append(resource).append(" | ");
                stringBuilder.append("exported: ").append(isExported(resource, exports));
                if(localResource) {
                    stringBuilder.append(SimpleAnsi.INTENSITY_NORMAL);
                }

                System.out.println(stringBuilder.toString());
            }
        } else {
            System.out.println("Bundle " + bundle.getBundleId() + " is not resolved.");
        }
    }

    private boolean isExported(String className, List<String> exports) {
        boolean exported = false;
        int lastSlashIdx = className.lastIndexOf("/");
        if (lastSlashIdx > -1) {
            String packageName = className.substring(0, lastSlashIdx).replaceAll("/", ".");
            if (exports.contains(packageName)) {
                exported = true;
            }
        }
        return exported;
    }

    private List<String> getExports(Bundle bundle) {
        List<String> exports = new ArrayList<>();
        BundleRevision rev = bundle.adapt(BundleRevision.class);
        List<BundleCapability> caps = rev.getDeclaredCapabilities(BundleRevision.PACKAGE_NAMESPACE);
        for (BundleCapability cap : caps) {
            Map<String, Object> attr = cap.getAttributes();
            String packageName = (String)attr.get(BundleRevision.PACKAGE_NAMESPACE);
            exports.add(packageName);
        }
        return exports;
    }


}
