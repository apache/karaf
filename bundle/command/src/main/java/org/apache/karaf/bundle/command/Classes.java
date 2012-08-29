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

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

import java.util.Collection;
import java.util.List;

@Command(scope = "bundle", name = "classes", description = "Displays a list of classes contained in the bundle")
public class Classes extends BundlesCommand {

    @Option(name = "-a", aliases={"--display-all-files"}, description="List all classes and files in the bundle", required = false, multiValued = false)
    boolean displayAllFiles;

    public Classes() {
        super(true);
    }

    protected void doExecute(List<Bundle> bundles) throws Exception {
        for (Bundle bundle : bundles) {
            printResources(bundle);
        }
    }

    protected void printResources(Bundle bundle) {
        BundleWiring wiring = bundle.adapt(BundleWiring.class);
        if (wiring != null){
            Collection<String> resources;
            if (displayAllFiles){
                resources = wiring.listResources("/", null, BundleWiring.LISTRESOURCES_RECURSE);
            }else{
                resources = wiring.listResources("/", "*class", BundleWiring.LISTRESOURCES_RECURSE);
            }
            for (String resource:resources){
                System.out.println(resource);
            }
        } else {
            System.out.println("Bundle " + bundle.getBundleId() + " is not resolved.");
        }
    }


}
