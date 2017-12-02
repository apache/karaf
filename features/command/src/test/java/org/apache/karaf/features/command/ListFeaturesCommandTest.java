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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureState;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.easymock.EasyMock;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class ListFeaturesCommandTest {

    @Test
    public void testHiddenFeatures() throws Exception {

        FeaturesService service = EasyMock.createMock(FeaturesService.class);
        Repository repo = EasyMock.createMock(Repository.class);
        Feature feature = EasyMock.createMock(Feature.class);

        EasyMock.expect(service.listRepositories()).andReturn(new Repository[] { repo });
        EasyMock.expect(repo.getFeatures()).andReturn(new Feature[] { feature });
        EasyMock.expect(feature.isHidden()).andReturn(true);
        EasyMock.expect(feature.isBlacklisted()).andReturn(false);

        EasyMock.replay(service, repo, feature);

        ListFeaturesCommand command = new ListFeaturesCommand();
        command.setFeaturesService(service);
        command.noFormat = true;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);
        System.setOut(out);

        command.execute();

        out.flush();
        assertFalse(baos.toString().contains("feature"));
        EasyMock.verify(service, repo, feature);
    }

    @Test
    public void testShowHiddenFeatures() throws Exception {

        FeaturesService service = EasyMock.createMock(FeaturesService.class);
        Repository repo = EasyMock.createMock(Repository.class);
        Feature feature = EasyMock.createMock(Feature.class);

        EasyMock.expect(service.listRepositories()).andReturn(new Repository[] { repo });
        EasyMock.expect(repo.getFeatures()).andReturn(new Feature[] { feature });
        EasyMock.expect(feature.isHidden()).andReturn(true).anyTimes();
        EasyMock.expect(feature.isBlacklisted()).andReturn(false).anyTimes();
        EasyMock.expect(feature.getName()).andReturn("feature");
        EasyMock.expect(feature.getId()).andReturn("feature/1.0.0");
        EasyMock.expect(service.getState(EasyMock.eq("feature/1.0.0"))).andReturn(FeatureState.Started);
        EasyMock.expect(feature.getDescription()).andReturn("description");
        EasyMock.expect(feature.getVersion()).andReturn("1.0.0");
        EasyMock.expect(service.isRequired(feature)).andReturn(true);
        EasyMock.expect(repo.getName()).andReturn("repository").anyTimes();

        EasyMock.replay(service, repo, feature);

        ListFeaturesCommand command = new ListFeaturesCommand();
        command.setFeaturesService(service);
        command.noFormat = true;
        command.showHidden = true;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);
        System.setOut(out);

        command.execute();

        out.flush();
        assertTrue(baos.toString().contains("feature"));
        EasyMock.verify(service, repo, feature);
    }

}
