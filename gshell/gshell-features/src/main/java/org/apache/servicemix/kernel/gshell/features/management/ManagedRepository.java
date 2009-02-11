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
package org.apache.servicemix.kernel.gshell.features.management;

import java.net.URI;

import org.apache.servicemix.kernel.gshell.features.Feature;
import org.apache.servicemix.kernel.gshell.features.FeaturesService;
import org.apache.servicemix.kernel.gshell.features.Repository;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.annotation.ManagedOperation;

@ManagedResource(description = "Features Repository")
public class ManagedRepository {
    private Repository repository;
    private FeaturesService featuresService;

    public ManagedRepository(Repository repository, FeaturesService featuresService) {
        this.repository = repository;
        this.featuresService = featuresService;
    }

    @ManagedAttribute
    public URI getUri() {
        return repository.getURI();
    }

    @ManagedAttribute
    public URI[] getRepositories() throws Exception {
        return repository.getRepositories();
    }

    @ManagedAttribute
    public Feature[] getFeatures() throws Exception {
        return repository.getFeatures();
    }

    @ManagedOperation
    public void removeRepository() throws Exception {
        featuresService.removeRepository(repository.getURI());
    }
}
