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
package org.apache.karaf.features.internal.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.FeaturesService.Option;
import org.apache.karaf.features.TestBase;
import org.apache.karaf.features.internal.util.ExitManager;
import org.easymock.Capture;
import org.junit.Assert;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertTrue;

public class BootFeaturesInstallerTest extends TestBase {

    private static final String INEXISTANT_REPO = "mvn:inexistent/features/1.0/xml/features";

    @Test
    public void testParser() {
        BootFeaturesInstaller installer = new BootFeaturesInstaller(null, null, null, new String[0], "", false);
        Assert.assertEquals(asList(setOf("test1", "test2"), setOf("test3")), installer.parseBootFeatures(" ( test1 , test2 ) , test3 "));
        Assert.assertEquals(Collections.singletonList(setOf("test1", "test2", "test3")), installer.parseBootFeatures(" test1 , test2, test3"));
        Assert.assertEquals(asList(setOf("test1"), setOf("test2"), setOf("test3")), installer.parseBootFeatures("(test1), (test2), test3"));
    }
    
    @Test
    public void testDefaultBootFeatures() throws Exception  {
        FeaturesServiceImpl impl = createMock(FeaturesServiceImpl.class);

        Capture<Set<String>> featuresCapture = newCapture();
        impl.installFeatures(capture(featuresCapture), eq(EnumSet.of(Option.NoFailOnFeatureNotFound)));
        expectLastCall();

        impl.bootDone();
        expectLastCall();

        replay(impl);
        BootFeaturesInstaller bootFeatures = new BootFeaturesInstaller(null, impl, null, new String[0], "config,standard,region", false);
        bootFeatures.installBootFeatures(false);
        verify(impl);

        List<String> features = new ArrayList<>(featuresCapture.getValue());
        Assert.assertEquals("config", features.get(0));
        Assert.assertEquals("standard", features.get(1));
        Assert.assertEquals("region", features.get(2));
    }

    @Test
    public void testParseBootFeaturesQuitsWhenFailed() throws Exception  {
        FeaturesServiceImpl impl = createStrictMock(FeaturesServiceImpl.class);
        MockedExitManager mockedExitManager = new MockedExitManager();
        BootFeaturesInstaller bootFeatures = new BootFeaturesInstaller(null, impl, mockedExitManager, new String[0], "config,standard,region,fail(())-me", false);
        bootFeatures.installBootFeatures(true);
        assertTrue(mockedExitManager.exitCalled);
    }

    @Test
    public void testInstallBootFeatuesQuitsWhenAddingRepositoriesFails() throws Exception  {
        FeaturesServiceImpl impl = createMock(FeaturesServiceImpl.class);
        impl.addRepository(anyObject());
        expectLastCall().andThrow(new Exception());

        replay(impl);

        MockedExitManager mockedExitManager = new MockedExitManager();
        BootFeaturesInstaller bootFeatures = new BootFeaturesInstaller(null, impl, mockedExitManager, new String[] {"fail-me" },"config,standard,region", false);
        bootFeatures.installBootFeatures(true);

        assertTrue(mockedExitManager.exitCalled);
    }

    @Test
    public void testInstallBootFeatuesQuitsWhenInstallingFeaturesFails() throws Exception  {
        FeaturesServiceImpl impl = createMock(FeaturesServiceImpl.class);
        impl.installFeatures(anyObject(), eq(EnumSet.noneOf(FeaturesService.Option.class)));
        expectLastCall().andThrow(new Exception());

        replay(impl);

        MockedExitManager mockedExitManager = new MockedExitManager();

        BootFeaturesInstaller bootFeatures = new BootFeaturesInstaller(null, impl, mockedExitManager, new String[0], "config,standard,region", false);
        bootFeatures.installBootFeatures(true);
        verify(impl);

        assertTrue(mockedExitManager.exitCalled);
    }

    @Test
    public void testStagedBoot() throws Exception  {
        FeaturesServiceImpl impl = createStrictMock(FeaturesServiceImpl.class);

        impl.installFeatures(setOf("transaction"), EnumSet.of(Option.NoFailOnFeatureNotFound));
        expectLastCall();
        impl.installFeatures(setOf("ssh"), EnumSet.of(Option.NoFailOnFeatureNotFound));
        expectLastCall();

        impl.bootDone();
        expectLastCall();

        replay(impl);
        BootFeaturesInstaller bootFeatures = new BootFeaturesInstaller(null, impl, null, new String[0], "(transaction), ssh", false);
        bootFeatures.installBootFeatures(false);
        verify(impl);
    }

    @Test
    public void testStartDoesNotFailWithOneInvalidUri() throws Exception {
        FeaturesServiceImpl impl = createStrictMock(FeaturesServiceImpl.class);
        impl.addRepository(URI.create(INEXISTANT_REPO));
        expectLastCall().andThrow(new IllegalArgumentException("Part of the test. Can be ignored."));

        impl.bootDone();
        expectLastCall();

        replay(impl);
        String[] repositories = new String[] { INEXISTANT_REPO };
        BootFeaturesInstaller bootFeatures = new BootFeaturesInstaller(null, impl, null, repositories, "", false);
        Logger logger = Logger.getLogger(BootFeaturesInstaller.class.getName());
        logger.setLevel(Level.OFF); // Switch off to suppress logging of IllegalArgumentException
        bootFeatures.installBootFeatures(false);
        logger.setLevel(Level.INFO);
        verify(impl);
    }

    @Test
    public void testParseBootFeatures() throws Exception {
        String features = "foo, jim, (ssh, shell, jaas, feature, framework), (system, bundle, management, service), (instance, package, log, deployer, diagnostic, config, kar), bar, zad";
        BootFeaturesInstaller bootFeatures = new BootFeaturesInstaller(null, null, null, null, null, false);
        List<Set<String>> stages = bootFeatures.parseBootFeatures(features);
        Assert.assertEquals(5, stages.size());
        for (String f : Arrays.asList("foo", "jim")) {
            assertTrue("Should contain '" + f + "'", stages.get(0).contains(f));
        }
        for (String f : Arrays.asList("ssh", "shell", "jaas", "feature", "framework")) {
            assertTrue("Should contain '" + f + "'", stages.get(1).contains(f));
        }
        for (String f : Arrays.asList("system", "bundle", "management", "service")) {
            assertTrue("Should contain '" + f + "'", stages.get(2).contains(f));
        }
        for (String f : Arrays.asList("instance", "package", "log", "deployer", "diagnostic", "config", "kar")) {
            assertTrue("Should contain '" + f + "'", stages.get(3).contains(f));
        }
        for (String f : Arrays.asList("bar", "zad")) {
            assertTrue("Should contain '" + f + "'", stages.get(4).contains(f));
        }
    }

    private static class MockedExitManager implements ExitManager {

        public boolean exitCalled;

        @Override
        public void exit() {
            exitCalled = true;
        }
    }
}
