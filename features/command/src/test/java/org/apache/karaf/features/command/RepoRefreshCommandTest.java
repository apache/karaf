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

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.easymock.EasyMock;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

public class RepoRefreshCommandTest extends RepositoryCommandTestBase {
    @Test
    public void testRefreshAllRepositories() throws Exception {
        FeaturesService service = EasyMock.createMock(FeaturesService.class);

        Repository repo1 = EasyMock.createMock(Repository.class);
        URI repo1Uri = URI.create("mvn:group:repo1:1.0.0");
        EasyMock.expect(repo1.getURI()).andReturn(repo1Uri);

        Repository repo2 = EasyMock.createMock(Repository.class);
        URI repo2Uri = URI.create("mvn:group2:repo2:2.0.0");
        EasyMock.expect(repo2.getURI()).andReturn(repo2Uri);

        EasyMock.expect(service.listRepositories()).andReturn(new Repository[]{repo1, repo2});

        service.refreshRepositories(new LinkedHashSet<>(Arrays.asList(repo1Uri, repo2Uri)));
        EasyMock.expectLastCall();

        EasyMock.replay(service, repo1, repo2);

        RepoRefreshCommand repoRefreshCommand = new RepoRefreshCommand();
        repoRefreshCommand.setFeaturesService(service);

        repoRefreshCommand.execute();

        EasyMock.verify(service, repo1, repo2);
    }

    @Test
    public void testRefreshRepositorySpecificVersion() throws Exception {
        FeaturesService service = EasyMock.createMock(FeaturesService.class);
        String repoName = "specificRepo";
        String repoVersion = "1.0.0";
        URI uri = URI.create("mvn:group:" + repoName + ":" + repoVersion);
        EasyMock.expect(service.getRepositoryUriFor(repoName, repoVersion)).andReturn(uri);

        service.refreshRepositories(new LinkedHashSet<>(Collections.singletonList(uri)));
        EasyMock.expectLastCall();

        EasyMock.replay(service);

        RepoRefreshCommand repoRefreshCommand = new RepoRefreshCommand();
        repoRefreshCommand.nameOrUrl = repoName;
        repoRefreshCommand.version = repoVersion;
        repoRefreshCommand.setFeaturesService(service);

        repoRefreshCommand.execute();

        EasyMock.verify(service);
    }

    @Test
    public void testRefreshRepositoryLatestVersion() throws Exception {
        FeaturesService service = EasyMock.createMock(FeaturesService.class);
        String repoName = "specificRepo";
        URI uri = URI.create("mvn:group:" + repoName + ":LATEST");
        EasyMock.expect(service.getRepositoryUriFor(repoName, "LATEST")).andReturn(uri);

        service.refreshRepositories(new LinkedHashSet<>(Collections.singletonList(uri)));
        EasyMock.expectLastCall();

        EasyMock.replay(service);

        RepoRefreshCommand repoRefreshCommand = new RepoRefreshCommand();
        repoRefreshCommand.nameOrUrl = repoName;
        repoRefreshCommand.version = null;
        repoRefreshCommand.setFeaturesService(service);

        repoRefreshCommand.execute();

        EasyMock.verify(service);
    }

    @Test
    public void testRefreshRepositoriesMatchingNamePattern() throws Exception {
        FeaturesService service = EasyMock.createMock(FeaturesService.class);
        String repoName = "specificRepo";
        String repoVersion = "1.0.0";
        EasyMock.expect(service.getRepositoryUriFor(repoName, repoVersion)).andReturn(null);

        Repository matchingRepo = mockRepository(repoName, repoVersion);
        Repository nonMatchingRepo = mockRepository("someRepo", "1.0.0");

        EasyMock.expect(service.listRepositories()).andReturn(new Repository[]{matchingRepo, nonMatchingRepo});

        service.refreshRepositories(new LinkedHashSet<>(Collections.singletonList(EasyMock.anyObject(URI.class))));
        EasyMock.expectLastCall();

        EasyMock.replay(service, matchingRepo, nonMatchingRepo);

        RepoRefreshCommand repoRefreshCommand = new RepoRefreshCommand();
        repoRefreshCommand.nameOrUrl = repoName;
        repoRefreshCommand.version = repoVersion;
        repoRefreshCommand.setFeaturesService(service);

        repoRefreshCommand.execute();

        EasyMock.verify(service, matchingRepo, nonMatchingRepo);
    }
}
