package org.apache.karaf.bundle.command;

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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.apache.karaf.shell.util.ShellUtil;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.Bundle;

import java.util.Collection;
import java.util.List;

@Command(scope = "bundle", name = "find-class", description = "Locates a specified class in any deployed bundle")
public class FindClass extends OsgiCommandSupport {

    @Argument(index = 0, name = "className", description = "Class name or partial class name to be found", required = true, multiValued = false)
    String className;

    protected Object doExecute() throws Exception {
        findResource();
        return null;
    }

    protected void findResource() {
        Bundle[] bundles = getBundleContext().getBundles();
        String filter = "*" + className + "*";
        for (Bundle bundle:bundles){
            BundleWiring wiring = bundle.adapt(BundleWiring.class);
            if (wiring != null){
                Collection<String> resources = wiring.listResources("/", filter, BundleWiring.LISTRESOURCES_RECURSE);
                if (resources.size() > 0){
                    String title = ShellUtil.getBundleName(bundle);
                    System.out.println("\n" + title);
                }
                for (String resource:resources){
                    System.out.println(resource);
                }
            } else {
                System.out.println("Bundle " + bundle.getBundleId() + " is not resolved.");
            }
        }
    }

}
