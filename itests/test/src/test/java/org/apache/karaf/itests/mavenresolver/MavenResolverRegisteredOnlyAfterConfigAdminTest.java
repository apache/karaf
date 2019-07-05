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
package org.apache.karaf.itests.mavenresolver;

import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test shows that without <code>org.ops4j.pax.url.mvn.requireConfigAdminConfig=true</code>,
 * there are two instances of MavenResolver service being published.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class MavenResolverRegisteredOnlyAfterConfigAdminTest extends KarafMinimalMonitoredTestSupport {

    public static Logger LOG = LoggerFactory.getLogger(MavenResolverRegisteredOnlyAfterConfigAdminTest.class);

    @Configuration
    public Option[] config() throws Exception {
        return new Option[] //
        {
         composite(super.baseConfig()),
         composite(editConfigurationFilePut("etc/org.apache.karaf.features.cfg",
                                            new File("target/test-classes/etc/org.apache.karaf.features.cfg"))),
         // etc/config.properties which have org.ops4j.pax.url.mvn.requireConfigAdminConfig=true
         editConfigurationFilePut("etc/config.properties", "org.ops4j.pax.url.mvn.requireConfigAdminConfig", "true")
        };
    }

    @Test
    public void mavenResolverAvailable() throws Exception {
        long count = numberOfServiceEventsFor("org.ops4j.pax.url.mvn.MavenResolver");
        assertEquals("There should be only one MavenResolver registration - after non-INITIAL ConfigAdmin update", 1l, count);
    }

}
