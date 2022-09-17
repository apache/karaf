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

import junit.framework.AssertionFailedError;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.easymock.EasyMock;
import org.junit.Test;

import java.net.URI;

public class RepoRemoveCommandTest extends RepositoryCommandTestBase {
    @Test
    public void testRemoveRepo() throws Exception {
        FeaturesService service = EasyMock.createMock(FeaturesService.class);
        String repoName = "specificRepo";
        String repoVersion = "1.0.0";
        boolean uninstall = true;

        URI uri = URI.create("mvn:group:" + repoName + ":" + repoVersion);
        EasyMock.expect(service.getRepositoryUriFor(repoName, repoVersion)).andReturn(uri);

        service.removeRepository(uri, uninstall);
        EasyMock.expectLastCall();

        EasyMock.replay(service);

        RepoRemoveCommand repoRemoveCommand = new RepoRemoveCommand();
        repoRemoveCommand.nameOrUrl = repoName;
        repoRemoveCommand.version = repoVersion;
        repoRemoveCommand.uninstall = uninstall;
        repoRemoveCommand.setFeaturesService(service);

        repoRemoveCommand.execute();

        EasyMock.verify(service);
    }

    @Test
    public void testNoMatchingRepo() throws Exception {
        FeaturesService service = EasyMock.createMock(FeaturesService.class);
        String repoName = "specificRepo";
        String repoVersion = "1.0.0";
        boolean uninstall = true;

        URI uri = URI.create("mvn:group:" + repoName + ":" + repoVersion);
        EasyMock.expect(service.getRepositoryUriFor(repoName, repoVersion)).andReturn(null);
        EasyMock.expect(service.listRepositories()).andReturn(new Repository[]{});

        service.removeRepository(uri, uninstall);
        EasyMock.expectLastCall().andThrow(new AssertionFailedError()).anyTimes();

        EasyMock.replay(service);

        RepoRemoveCommand repoRemoveCommand = new RepoRemoveCommand();
        repoRemoveCommand.nameOrUrl = repoName;
        repoRemoveCommand.version = repoVersion;
        repoRemoveCommand.uninstall = uninstall;
        repoRemoveCommand.setFeaturesService(service);

        repoRemoveCommand.execute();

        EasyMock.verify(service);
    }

    @Test
    public void testMultipleReposMatching() throws Exception {
        String duplicatedRepoName = "repo";
        String repoVersion = "1.0.0";
        boolean uninstall = true;

        FeaturesService service = EasyMock.createMock(FeaturesService.class);

        EasyMock.expect(service.getRepositoryUriFor(duplicatedRepoName, repoVersion)).andReturn(null);

        Repository repo1 = mockRepository(duplicatedRepoName, "2.0.0");
        Repository repo2 = mockRepository(duplicatedRepoName, "3.0.0");

        EasyMock.expect(service.listRepositories()).andReturn(new Repository[]{repo1, repo2});

        service.removeRepository(EasyMock.anyObject(URI.class), EasyMock.anyBoolean());
        EasyMock.expectLastCall().andThrow(new AssertionFailedError(
                "Removed a repo even though it shouldn't have because multiple repos matched the criteria")).anyTimes();

        EasyMock.replay(service, repo1, repo2);

        RepoRemoveCommand repoRemoveCommand = new RepoRemoveCommand();
        repoRemoveCommand.uninstall = uninstall;
        repoRemoveCommand.nameOrUrl = duplicatedRepoName;
        repoRemoveCommand.version = repoVersion;
        repoRemoveCommand.setFeaturesService(service);

        repoRemoveCommand.execute();

        EasyMock.verify(service, repo1, repo2);
    }
}
