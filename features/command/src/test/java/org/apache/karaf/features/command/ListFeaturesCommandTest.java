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
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.internal.FeatureImpl;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ListFeaturesCommandTest {

    private FeaturesService featuresService;

    @Before
    public void init() throws Exception {
        this.featuresService = EasyMock.createMock(FeaturesService.class);
        EasyMock.expect(this.featuresService.isInstalled(EasyMock.anyObject(Feature.class))).andReturn(true).anyTimes();

        Repository r1 = EasyMock.createMock(Repository.class);
        Repository r2 = EasyMock.createMock(Repository.class);
        EasyMock.expect(r1.getName()).andReturn("r1").anyTimes();
        EasyMock.expect(r2.getName()).andReturn("r2").anyTimes();
        EasyMock.expect(this.featuresService.listRepositories()).andReturn(new Repository[] { r1, r2 });

        EasyMock.expect(r1.getFeatures()).andReturn(new Feature[] {
            new FeatureImpl("f2", "v2")
        });
        EasyMock.expect(r2.getFeatures()).andReturn(new Feature[] {
            new FeatureImpl("f1", "v1")
        });

        EasyMock.replay(this.featuresService, r1, r2);
    }

    @Test
    public void testListOfFeatures() throws Exception {
        ListFeaturesCommand command = new ListFeaturesCommand();
        command.ordered = false;
        command.installed = false;

        PrintStream oldOut = System.out;
        ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(resultStream));
        command.doExecute(this.featuresService);
        System.setOut(oldOut);

        String result = new String(resultStream.toByteArray());
        assertThat(result.contains("[v1     ] f1   r2"), is(true));
    }

    @Test
    public void testSortingListOfFeatures() throws Exception {
        ListFeaturesCommand command = new ListFeaturesCommand();
        command.ordered = true;
        command.installed = false;

        PrintStream oldOut = System.out;
        ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(resultStream));
        command.doExecute(this.featuresService);
        System.setOut(oldOut);

        String result = new String(resultStream.toByteArray());
        assertThat(result.contains("[v1     ] f1   r2"), is(true));
    }

}
