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
package org.apache.karaf.shell.osgi;

import java.util.Collection;
import java.util.List;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

@Command(scope = "osgi", name = "classes", description = "Displays list of classes contained in bundles")
public class Classes extends BundlesCommand {

    @Option(name = "-a", aliases = { "--display-all-files" }, description = "List all classes and files in bundles", required = false, multiValued = false)
    boolean displayAllFiles;

    protected void doExecute(List<Bundle> bundles) throws Exception {
        for (Bundle bundle : bundles) {
            printResources(bundle);
        }
    }

    protected void printResources(Bundle bundle) {
        BundleWiring wiring = (BundleWiring) bundle.adapt(BundleWiring.class);
        if (wiring != null) {
            Collection<String> resources = null;
            if (displayAllFiles) {
                resources = wiring.listResources("/", null, BundleWiring.LISTRESOURCES_RECURSE);
            } else {
                resources = wiring.listResources("/", "*class", BundleWiring.LISTRESOURCES_RECURSE);
            }
            if (resources.size() > 0) {
                System.out.println("\n" + Util.getBundleName(bundle));
            }
            for (String resource : resources) {
                System.out.println(resource);
            }
        } else {
            System.out.println("Bundle " + bundle.getBundleId() + " is not resolved");
        }
    }

}
