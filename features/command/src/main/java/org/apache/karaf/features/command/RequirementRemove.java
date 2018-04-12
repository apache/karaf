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
package org.apache.karaf.features.command;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "feature", name = "requirement-remove", description = "Remove provisioning requirements.")
@Service
public class RequirementRemove extends FeaturesCommandSupport {

    @Argument(required = true, multiValued = true)
    List<String> requirements;

    @Option(name = "-r", aliases = "--no-auto-refresh", description = "Do not automatically refresh bundles", required = false, multiValued = false)
    boolean noRefresh;

    @Option(name = "-s", aliases = "--no-auto-start", description = "Do not start the bundles", required = false, multiValued = false)
    boolean noStart;

    @Option(name = "-m", aliases = "--no-auto-manage", description = "Do not automatically manage bundles", required = false, multiValued = false)
    boolean noManage;

    @Option(name = "-v", aliases = "--verbose", description = "Explain what is being done")
    boolean verbose;

    @Option(name = "-t", aliases = "--simulate", description = "Perform a simulation only")
    boolean simulate;

    @Option(name = "--store", description = "Store the resolution into the given file and result for offline analysis")
    String outputFile;

    @Option(name = "--features-wiring", description = "Print the wiring between features")
    boolean featuresWiring;

    @Option(name = "--all-wiring", description = "Print the full wiring")
    boolean allWiring;

    @Option(name = "-g", aliases = "--region", description = "Region to apply to")
    String region = FeaturesService.ROOT_REGION;

    @Override
    protected void doExecute(FeaturesService featuresService) throws Exception {
        addOption(FeaturesService.Option.Simulate, simulate);
        addOption(FeaturesService.Option.NoAutoStartBundles, noStart);
        addOption(FeaturesService.Option.NoAutoRefreshBundles, noRefresh);
        addOption(FeaturesService.Option.NoAutoManageBundles, noManage);
        addOption(FeaturesService.Option.Verbose, verbose);
        addOption(FeaturesService.Option.DisplayFeaturesWiring, featuresWiring);
        addOption(FeaturesService.Option.DisplayAllWiring, allWiring);
        Map<String, Set<String>> reqs = new HashMap<>();
        reqs.put(region, new HashSet<>(requirements));
        featuresService.setResolutionOutputFile(outputFile);
        featuresService.removeRequirements(reqs, options);
    }

}
