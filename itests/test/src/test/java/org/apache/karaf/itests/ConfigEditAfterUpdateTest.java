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

import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.replaceConfigurationFile;

import java.util.Dictionary;

import javax.inject.Inject;

import org.apache.felix.utils.properties.TypedProperties;
import org.apache.karaf.config.core.ConfigRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

/**
 * Edits a configuration right after updating it, as {@code config:edit}, {@code config:property-set} and
 * {@code config:update} do. FileInstall writes the configuration back to its file after each update, and the edit
 * must not miss the properties while the file is being written.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ConfigEditAfterUpdateTest extends BaseTest {

    private static final int UPDATES = 1000;

    @Inject
    ConfigRepository configRepository;

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        return new Option[] {
                CoreOptions.composite(super.config()),
                replaceConfigurationFile("etc/myconfig-test1.cfg", getConfigFile("/etc/myconfig-test1.cfg")),
        };
    }

    @Test
    public void editRightAfterUpdateKeepsTheOtherProperties() throws Exception {
        String pid = configurationAdmin.listConfigurations("(service.factorypid=myconfig)")[0].getPid();
        for (int i = 0; i < UPDATES; i++) {
            TypedProperties props = configRepository.getConfig(pid);
            props.put("test1", "data" + i);
            configRepository.update(pid, props);
            Dictionary<String, Object> properties = configurationAdmin.getConfiguration(pid, null).getProperties();
            assertEquals("data" + i, properties.get("test1"));
            assertEquals("test2 lost after " + (i + 1) + " updates", "data2", properties.get("test2"));
        }
    }

}
