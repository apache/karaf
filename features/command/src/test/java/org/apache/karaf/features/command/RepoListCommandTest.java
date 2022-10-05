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
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Test;
import shaded.org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;

import static org.junit.Assert.*;

public class RepoListCommandTest extends RepositoryCommandTestBase {

    @Test
    public void testListReposAfterReload() throws Exception {
        FeaturesService service = EasyMock.createMock(FeaturesService.class);

        String repoName = "SomeRepository";
        Repository repo = mockRepository(repoName, "1.0.0");
        EasyMock.expect(repo.isBlacklisted()).andReturn(false);

        EasyMock.expect(service.listRepositories()).andReturn(new Repository[]{repo}).anyTimes();
        Capture<URI> uriCapture = Capture.newInstance(CaptureType.ALL);
        service.addRepository(EasyMock.capture(uriCapture));
        EasyMock.expectLastCall().times(1);

        EasyMock.replay(service, repo);

        RepoListCommand repoListCommand = new RepoListCommand();
        repoListCommand.setFeaturesService(service);
        repoListCommand.showBlacklisted = false;
        repoListCommand.noFormat = true;
        repoListCommand.reload = true;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);
        System.setOut(out);

        repoListCommand.execute();
        out.flush();

        assertEquals(repo.getURI(), uriCapture.getValue());

        String commandOutput = baos.toString();
        assertTrue(commandOutput.contains(repoName + "\t" + repo.getURI()));

        EasyMock.verify(service, repo);
    }

    @Test
    public void testBlacklistedRepositoriesNotShown() throws Exception {
        FeaturesService service = EasyMock.createMock(FeaturesService.class);
        Repository blacklistedRepo = EasyMock.createMock(Repository.class);
        EasyMock.expect(blacklistedRepo.isBlacklisted()).andReturn(true);

        String whitelistedRepoName = "SomeRepository";
        Repository whitelistedRepo = mockRepository(whitelistedRepoName, "1.0.0");
        EasyMock.expect(whitelistedRepo.isBlacklisted()).andReturn(false);

        EasyMock.expect(service.listRepositories()).andReturn(new Repository[]{blacklistedRepo, whitelistedRepo});

        EasyMock.replay(service, blacklistedRepo, whitelistedRepo);

        RepoListCommand repoListCommand = new RepoListCommand();
        repoListCommand.setFeaturesService(service);
        repoListCommand.showBlacklisted = false;
        repoListCommand.noFormat = true;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);
        System.setOut(out);

        repoListCommand.execute();
        out.flush();

        String commandOutput = baos.toString();
        assertTrue(commandOutput.contains(whitelistedRepoName + "\t" + whitelistedRepo.getURI()));

        EasyMock.verify(service, blacklistedRepo, whitelistedRepo);
    }

    @Test
    public void testBlacklistedRepositoriesShown() throws Exception {
        FeaturesService service = EasyMock.createMock(FeaturesService.class);
        String blacklistedRepoName = "BlacklistedRepo";
        Repository blacklistedRepo = mockRepository(blacklistedRepoName, "1.0.0");
        EasyMock.expect(blacklistedRepo.isBlacklisted()).andReturn(true);

        String whitelistedRepoName = "SomeRepository";
        Repository whitelistedRepo = mockRepository(whitelistedRepoName, "1.0.0");
        EasyMock.expect(whitelistedRepo.isBlacklisted()).andReturn(false);

        EasyMock.expect(service.listRepositories()).andReturn(new Repository[]{blacklistedRepo, whitelistedRepo});

        EasyMock.replay(service, blacklistedRepo, whitelistedRepo);

        RepoListCommand repoListCommand = new RepoListCommand();
        repoListCommand.setFeaturesService(service);
        repoListCommand.showBlacklisted = true;
        repoListCommand.noFormat = true;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);
        System.setOut(out);

        repoListCommand.execute();
        out.flush();

        String[] commandOutput = baos.toString().split("\\R");
        assertEquals(2, commandOutput.length);
        String blacklistedRepoLineNoSpaces = blacklistedRepoName + blacklistedRepo.getURI() + "yes";
        assertEquals(StringUtils.deleteWhitespace(commandOutput[0].trim()), blacklistedRepoLineNoSpaces);

        String whitelistedRepoLineNoSpaces = whitelistedRepoName + whitelistedRepo.getURI() + "no";
        assertEquals(StringUtils.deleteWhitespace(commandOutput[1]), whitelistedRepoLineNoSpaces);

        EasyMock.verify(service, blacklistedRepo, whitelistedRepo);
    }

    @Test
    public void testPasswordsHidden() throws Exception {
        FeaturesService service = EasyMock.createMock(FeaturesService.class);

        Repository repo = EasyMock.createMock(Repository.class);
        URI repoUri = URI.create("mvn:https://user:password@repo1.maven.org/maven2!org.apache.cxf.karaf/apache-cxf/3.5.3/xml/features");
        EasyMock.expect(repo.getURI()).andReturn(repoUri).anyTimes();
        EasyMock.expect(repo.getName()).andReturn("cxf-3.5.3").anyTimes();
        EasyMock.expect(repo.isBlacklisted()).andReturn(false);

        EasyMock.expect(service.listRepositories()).andReturn(new Repository[]{repo});

        EasyMock.replay(service, repo);

        RepoListCommand repoListCommand = new RepoListCommand();
        repoListCommand.setFeaturesService(service);
        repoListCommand.noFormat = true;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);
        System.setOut(out);

        repoListCommand.execute();
        out.flush();

        String commandOutput = baos.toString();
        assertTrue(commandOutput.contains("*****:*****"));
        assertFalse(commandOutput.contains("user:password"));

        EasyMock.verify(service, repo);
    }
}
