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
package org.apache.karaf.itests;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.karaf.itests.monitoring.Activator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.replaceConfigurationFile;

/**
 * Test shows that without <code>org.ops4j.pax.url.mvn.requireConfigAdminConfig=true</code>,
 * there are two instances of MavenResolver service being published.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class MavenResolverRegisteredBeforeConfigAdminTest extends KarafMinimalMonitoredTestSupport {

    public static Logger LOG = LoggerFactory.getLogger(MavenResolverRegisteredBeforeConfigAdminTest.class);

    @Configuration
    public Option[] config() throws Exception {
        List<Option> result = new LinkedList<>();
        result.addAll(Arrays.asList(super.baseConfig()));
        result.addAll(Arrays.asList(editConfigurationFilePut("etc/org.apache.karaf.features.cfg", new File("target/test-classes/etc/org.apache.karaf.features.cfg"))));
        // etc/config.properties which doesn't have org.ops4j.pax.url.mvn.requireConfigAdminConfig=true
        result.add(replaceConfigurationFile("etc/config.properties", new File("target/test-classes/etc/config.properties")));

        return result.toArray(new Option[result.size()]);
    }

    @Test
    public void mavenResolverAvailable() throws Exception {
        assertNotNull(bundleContext);
        ServiceReference<List> sr = bundleContext.getBundle(0L).getBundleContext().getServiceReference(List.class);
        List<String> services = bundleContext.getService(sr);
        assertTrue("There should be two registrations/unregistrations of MavenResolver before final (ConfigAdmin based) registration", services.stream()
                .filter(v -> v.equals("org.ops4j.pax.url.mvn.MavenResolver")).count() == 5L);
    }

}
