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
package org.apache.felix.karaf.features.command;

import java.util.EnumSet;

import org.apache.felix.gogo.commands.Option;
import org.apache.felix.karaf.features.FeaturesService;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;

@Command(scope = "features", name = "install", description = "Installs a feature with the specified name and version.")
public class InstallFeatureCommand extends FeaturesCommandSupport {

    private static String DEFAULT_VERSION = "0.0.0";

    @Argument(index = 0, name = "name", description = "The name of the feature", required = true, multiValued = false)
    String name;
    @Argument(index = 1, name = "version", description = "The version of the feature", required = false, multiValued = false)
    String version;
    @Option(name = "-c", aliases = "--no-clean", description = "Do not uninstall bundles on failure", required = false, multiValued = false)
    boolean noClean;
    @Option(name = "-r", aliases = "--no-auto-refresh", description = "Do not automatically refresh bundles", required = false, multiValued = false)
    boolean noRefresh;

    protected void doExecute(FeaturesService admin) throws Exception {
    	if (version == null || version.length() == 0) {
            version = DEFAULT_VERSION;
    	}
        EnumSet<FeaturesService.Option> options = EnumSet.of(FeaturesService.Option.PrintBundlesToRefresh);
        if (noRefresh) {
            options.add(FeaturesService.Option.NoAutoRefreshBundles);
        }
        if (noClean) {
            options.add(FeaturesService.Option.NoCleanIfFailure);
        }
        admin.installFeature(name, version, options);
    }
}
