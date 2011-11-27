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

package org.apache.karaf.tooling.exam.regression;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.editConfigurationFileExtend;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.OptionUtils.combine;

import java.io.File;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class DuplicatedPropertyEntryFromFileTest {

    @Inject
    private BundleContext ctx;

    @Configuration
    public Option[] config() {
        Option[] base =
            new Option[]{ karafDistributionConfiguration().frameworkUrl(
                maven().groupId("org.apache.karaf.assemblies").artifactId("apache-karaf").type("zip")
                    .versionAsInProject()) };
        base =
            combine(
                base,
                editConfigurationFileExtend("etc/tests.cfg", new File(
                    "src/test/resources/BaseKarafDefaultFrameworkDuplicatedPropertyEntryTestFirstKey")));
        base =
            combine(
                base,
                editConfigurationFileExtend("etc/tests.cfg", new File(
                    "src/test/resources/BaseKarafDefaultFrameworkDuplicatedPropertyEntryTestSecondKey")));
        return base;
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testConfiguration_shouldHaveWrittenTheLaterOne() throws Exception {
        ServiceReference[] allServiceReferences = ctx.getAllServiceReferences(ConfigurationAdmin.class.getName(), null);
        for (ServiceReference serviceReference : allServiceReferences) {
            ConfigurationAdmin service = (ConfigurationAdmin) ctx.getService(serviceReference);
            try {
                org.osgi.service.cm.Configuration configuration = service.getConfiguration("tests");
                assertEquals("myvalue2", configuration.getProperties().get("mykey"));
                return;
            } catch (Exception e) {
                // continue
            }
        }
        fail();
    }
}
