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
package org.apache.karaf.features.command.completers;

import java.util.Arrays;
import java.util.List;

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.internal.FeatureFinder;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.apache.karaf.shell.inject.Reference;
import org.apache.karaf.shell.inject.Service;

/**
 * Shows the list of feature repos that can be installed with their short name
 */
@Service
public class AvailableRepoNameCompleter implements Completer {

    @Reference
    private FeaturesService featuresService;

    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }

    public int complete(final String buffer, final int cursor, @SuppressWarnings("rawtypes") final List candidates) {
        StringsCompleter delegate = new StringsCompleter(Arrays.asList(featuresService.getRepositoryNames()));
        return delegate.complete(buffer, cursor, candidates);
    }

}
