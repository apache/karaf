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

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.osgi.framework.Bundle;

/**
 * Command for enabling/disabling debug logging on a bundle and calculating the difference in
 * wired imports.
 */
@Command(scope = "bundle", name = "dynamic-import", description = "Enables/disables dynamic-import for a given bundle.")
@Service
public class DynamicImport extends BundleCommand {

    @Override
    protected Object doExecute(Bundle bundle) throws Exception {
        if (bundleService.isDynamicImport(bundle)) {
            System.out.printf("Disabling dynamic imports on bundle %s%n", bundle);
            bundleService.disableDynamicImports(bundle);
        } else {
            System.out.printf("Enabling dynamic imports on bundle %s%n", bundle);
            bundleService.enableDynamicImports(bundle);
        }
        return null;
    }

}
