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
package org.apache.karaf.features.internal;

import static java.util.Arrays.asList;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.EnumSet;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService.Option;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

public class BootFeaturesInstallerTest extends TestBase {

    @Test
    @SuppressWarnings("unchecked")
    public void testParser() {
        BootFeaturesInstaller installer = new BootFeaturesInstaller(null, null, "", false);
        Assert.assertEquals(asList(setOf("test1", "test2"),setOf("test3")), installer.parseBootFeatures("(test1, test2), test3"));
        Assert.assertEquals(asList(setOf("test1", "test2", "test3")), installer.parseBootFeatures("test1, test2, test3"));
    }
    
    @Test
    public void testDefaultBootFeatures() throws Exception  {
        FeaturesServiceImpl impl = EasyMock.createMock(FeaturesServiceImpl.class);
        Feature configFeature = feature("config", "1.0.0");
        Feature standardFeature = feature("standard", "1.0.0");
        Feature regionFeature = feature("region", "1.0.0");
        expect(impl.listInstalledFeatures()).andStubReturn(new Feature[]{});
        expect(impl.getFeature("config", "0.0.0")).andReturn(configFeature);
        expect(impl.getFeature("standard", "0.0.0")).andReturn(standardFeature);
        expect(impl.getFeature("region", "0.0.0")).andReturn(regionFeature);

        impl.installFeatures(setOf(configFeature, standardFeature, regionFeature), EnumSet.of(Option.NoCleanIfFailure, Option.ContinueBatchOnFailure));
        EasyMock.expectLastCall();
        
        replay(impl);
        BootFeaturesInstaller bootFeatures = new BootFeaturesInstaller(null, impl, "config,standard,region", false);
        bootFeatures.installBootFeatures();
        EasyMock.verify(impl);        
    }

    /**
     * This test checks KARAF-388 which allows you to specify version of boot feature.
     * @throws Exception 
     */
    @Test
    public void testStartDoesNotFailWithNonExistentVersion() throws Exception  {
        FeaturesServiceImpl impl = EasyMock.createMock(FeaturesServiceImpl.class);
        expect(impl.listInstalledFeatures()).andReturn(new Feature[]{});
        EasyMock.expectLastCall();
        Feature sshFeature = feature("ssh", "1.0.0");
        expect(impl.getFeature("ssh", "1.0.0")).andReturn(sshFeature);
        expect(impl.getFeature("transaction", "1.2")).andReturn(null);
        
        // Only the ssh feature should get installed
        impl.installFeatures(setOf(sshFeature), EnumSet.of(Option.NoCleanIfFailure, Option.ContinueBatchOnFailure));
        EasyMock.expectLastCall();
        
        replay(impl);
        BootFeaturesInstaller bootFeatures = new BootFeaturesInstaller(null, impl , "transaction;version=1.2,ssh;version=1.0.0", false);
        bootFeatures.installBootFeatures();
        EasyMock.verify(impl);        
    }
    
    @Test
    public void testStagedBoot() throws Exception  {
        FeaturesServiceImpl impl = EasyMock.createStrictMock(FeaturesServiceImpl.class);
        Feature sshFeature = feature("ssh", "1.0.0");
        Feature transactionFeature = feature("transaction", "2.0.0");
        expect(impl.listInstalledFeatures()).andStubReturn(new Feature[]{});
        expect(impl.getFeature("transaction", "0.0.0")).andStubReturn(transactionFeature);
        expect(impl.getFeature("ssh", "0.0.0")).andStubReturn(sshFeature);

        impl.installFeatures(setOf(transactionFeature), EnumSet.of(Option.NoCleanIfFailure, Option.ContinueBatchOnFailure));
        EasyMock.expectLastCall();
        impl.installFeatures(setOf(sshFeature), EnumSet.of(Option.NoCleanIfFailure, Option.ContinueBatchOnFailure));
        EasyMock.expectLastCall();
        
        replay(impl);
        BootFeaturesInstaller bootFeatures = new BootFeaturesInstaller(null, impl , "(transaction), ssh", false);
        bootFeatures.installBootFeatures();
        EasyMock.verify(impl);        
    }
}
