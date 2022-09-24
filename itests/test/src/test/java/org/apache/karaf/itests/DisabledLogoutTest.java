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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class DisabledLogoutTest extends BaseTest {

    @Configuration
    public Option[] config() {
        List<Option> options = new ArrayList<>(Arrays.asList(super.config()));
        options.add(KarafDistributionOption.editConfigurationFilePut("etc/org.apache.karaf.shell.cfg",
                        "disableLogout", "true"));
        return options.toArray(new Option[options.size()]);
    }

    @Test
    public void testShellLogoutDisabled() {
        executeCommand("shell:logout");
        // Execute anything at all to verify that we didn't exit from Karaf. If we did, we'd get a runtime exception
        assertNotNull(executeAlias("ld"));
    }

}
