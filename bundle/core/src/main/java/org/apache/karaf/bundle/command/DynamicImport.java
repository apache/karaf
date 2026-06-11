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

import java.util.List;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.osgi.framework.Bundle;

/**
 * Command for enabling/disabling dynamic imports on a bundle and calculating the difference in
 * wired imports.
 */
@Command(scope = "bundle", name = "dynamic-import", description = "Enables/disables dynamic-import for a given bundle.")
@Service
public class DynamicImport extends BundleCommand {

    @Argument(index = 1, name = "packages", description = "Bundle URLs separated by whitespaces", required = false, multiValued = true)
    List<String> packages;

    @Option(name = "--enable", aliases = {"-e"}, description = "Forces the command to execute", required = false, multiValued = false)
    boolean enable;

    @Option(name = "--disable", aliases = {"-d"}, description = "Forces the command to execute", required = false, multiValued = false)
    boolean disable;

    @Override
    protected Object doExecute(Bundle bundle) throws Exception {
        if (enable && disable) {
            throw new IllegalArgumentException("Cannot 'enable' and 'disable' at the same time");
        }
        if ((enable || disable) && (packages != null && !packages.isEmpty())) {
            throw new IllegalArgumentException("Options are incompatible with providing package list");
        }
        if (enable) {
            System.out.printf("Enabling dynamic imports on bundle %s%n", bundle);
            bundleService.setDynamicImports(bundle, List.of("*"));
        } else if (disable) {
            System.out.printf("Disabling dynamic imports on bundle %s%n", bundle);
            bundleService.setDynamicImports(bundle, List.of());
        } else if (packages != null && !packages.isEmpty()) {
            System.out.printf("Enabling dynamic imports for [%s] on bundle %s%n",
                String.join(", ",packages), bundle);
            bundleService.setDynamicImports(bundle, packages);
        } else if (bundleService.isDynamicImport(bundle)) {
            System.out.printf("Disabling dynamic imports on bundle %s%n", bundle);
            bundleService.setDynamicImports(bundle, List.of());
        } else {
            System.out.printf("Enabling dynamic imports on bundle %s%n", bundle);
            bundleService.setDynamicImports(bundle, List.of("*"));
        }
        return null;
    }

}
