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
import org.easymock.EasyMock;
import org.junit.Test;

import java.net.URI;

public class RepoAddCommandTest {
    @Test
    public void testAddBlacklistedRepository() throws Exception {
        String name = "repo";
        String version = "1.2.0-SNAPSHOT";
        URI uri = URI.create("mvn:group:" + name + ":" + version);
        FeaturesService service = EasyMock.createMock(FeaturesService.class);
        EasyMock.expect(service.getRepositoryUriFor(name, version)).andReturn(uri);
        EasyMock.expect(service.isRepositoryUriBlacklisted(uri)).andReturn(true);
        service.addRepository(uri);
        EasyMock.expectLastCall().andThrow(
                new AssertionFailedError("Repository added despite being blacklisted")).anyTimes();

        EasyMock.replay(service);

        RepoAddCommand repoAddCommand = new RepoAddCommand();
        repoAddCommand.setFeaturesService(service);
        repoAddCommand.install = true;
        repoAddCommand.nameOrUrl = name;
        repoAddCommand.version = version;

        repoAddCommand.execute();

        EasyMock.verify(service);
    }

    @Test
    public void testAddNonBlacklistedRepository() throws Exception {
        String name = "repo";
        String version = "1.2.0-SNAPSHOT";
        boolean install = true;

        URI uri = URI.create("mvn:group:" + name + ":" + version);
        FeaturesService service = EasyMock.createMock(FeaturesService.class);
        EasyMock.expect(service.getRepositoryUriFor(name, version)).andReturn(uri);
        EasyMock.expect(service.isRepositoryUriBlacklisted(uri)).andReturn(false);
        service.addRepository(uri, install);
        EasyMock.expectLastCall();

        EasyMock.replay(service);

        RepoAddCommand repoAddCommand = new RepoAddCommand();
        repoAddCommand.setFeaturesService(service);
        repoAddCommand.install = install;
        repoAddCommand.nameOrUrl = name;
        repoAddCommand.version = version;

        repoAddCommand.execute();

        EasyMock.verify(service);
    }
}
