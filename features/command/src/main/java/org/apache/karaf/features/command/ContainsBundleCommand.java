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

import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.command.completers.InstalledRepoNameCompleter;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.CommandException;
import org.osgi.framework.Bundle;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Command(scope = "feature", name = "contains", description = "Finds the features that contain a specified bundle.")
@Service
public class ContainsBundleCommand extends FeaturesCommandSupport {

    @Option(name = "-b", description = "Show features containing the given bundle id", required = false, multiValued = false)
    Integer bundleId;

    @Option(name = "-s", description = "Show features containing a bundle with the given symbolic name", required = false, multiValued = false)
    String bundleName;

    @Option(name = "-u", description = "Show features containing a bundle with the given URL", required = false, multiValued = false)
    String bundleUrl;

    @Option(name = "-i", aliases = {"--installed"}, description = "Display a list of all installed features only", required = false, multiValued = false)
    boolean onlyInstalled;

    @Option(name = "--repository", description = "Only list features from that repository", required = false, multiValued = false)
    @Completion(InstalledRepoNameCompleter.class)
    String repository;

    @Reference
    BundleService bundleService;

    @Override
    protected void doExecute(FeaturesService featuresService) throws Exception {
        String bundleLocation;
        if (bundleId != null) {
            Bundle bundle = bundleService.getBundle(bundleId.toString());
            if (bundle == null)
                throw new CommandException("No bundles found with id " + bundleId);
            bundleLocation = bundleService.getInfo(bundle).getUpdateLocation();
        } else if (bundleName != null) {
            Bundle bundle = bundleService.selectBundles(List.of(), true).stream()
                    .filter(b -> bundleName.equals(b.getSymbolicName()))
                    .findAny()
                    .orElseThrow(() -> new CommandException("No bundles found with name " + bundleName));
            bundleLocation = bundleService.getInfo(bundle).getUpdateLocation();
        } else if (bundleUrl != null) {
             bundleLocation = bundleUrl;
        } else {
            throw new CommandException("Either bundle id, name or URL must be specified");
        }

        if (bundleLocation == null)
            throw new CommandException("Could not determine URL of the specified bundle");

        Feature[] features;
        if (repository != null && !onlyInstalled)
            features = featuresService.getFeatures(repository);
        else
            features = featuresService.listFeatures();

        Set<Feature> containingFeatures = Arrays.stream(features)
                .filter(f -> !onlyInstalled || featuresService.isInstalled(f))
                .filter(f -> f.getBundles().stream().anyMatch(byBundleLocation(bundleLocation)))
                .collect(Collectors.toUnmodifiableSet());

        containingFeatures.stream()
                .map(f -> String.format("%s %s", f.getName(), f.getVersion()))
                .sorted()
                .forEach(System.out::println);
    }

    private Predicate<BundleInfo> byBundleLocation(String bundleLocation) {
        return bundleInfo ->
        {
            String location = bundleInfo.getLocation();
            String originalLocation = bundleInfo.getOriginalLocation();
            return (location != null && location.startsWith(bundleLocation)) ||
                    (originalLocation != null && originalLocation.startsWith(bundleLocation));
        };
    }

}
