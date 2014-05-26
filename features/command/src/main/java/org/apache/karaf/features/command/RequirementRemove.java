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

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "feature", name = "requirement-remove", description = "Remove provisioning requirements.")
@Service
public class RequirementRemove implements Action {

    @Reference
    private FeaturesService featuresService;

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

    @Option(name = "-g", aliases = "--region", description = "Region to install to")
    String region;

    @Override
    public Object execute() throws Exception {
        EnumSet<FeaturesService.Option> options = EnumSet.noneOf(FeaturesService.Option.class);
        if (simulate) {
            options.add(FeaturesService.Option.Simulate);
        }
        if (noStart) {
            options.add(FeaturesService.Option.NoAutoStartBundles);
        }
        if (noRefresh) {
            options.add(FeaturesService.Option.NoAutoRefreshBundles);
        }
        if (noManage) {
            options.add(FeaturesService.Option.NoAutoManageBundles);
        }
        if (verbose) {
            options.add(FeaturesService.Option.Verbose);
        }
        Map<String, Set<String>> reqs = new HashMap<>();
        reqs.put(region == null ? FeaturesService.ROOT_REGION : region, new HashSet<>(requirements));
        featuresService.removeRequirements(reqs, options);
        return null;
    }
}
