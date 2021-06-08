/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ConfigTest extends BaseTest {

    @Test
    public void listCommand() throws Exception {
        String configListOutput = executeCommand("config:list");
        System.out.println(configListOutput);
        assertFalse(configListOutput.isEmpty());
        configListOutput = executeCommand("config:list \"(service.pid=org.apache.karaf.features)\"");
        System.out.println(configListOutput);
        assertFalse(configListOutput.isEmpty());
    }

    @Test
    public void clAlias() throws Exception {
        String configListOutput = executeAlias("cl org.apache.karaf.features");
        System.out.println(configListOutput);
        assertFalse(configListOutput.isEmpty());
    }

    @Test
    public void listShortCommand() throws Exception {
        String configListOutput = executeCommand("config:list -s");
        System.out.println(configListOutput);
        assertFalse(configListOutput.isEmpty());
        assertContains("org.apache.karaf.jaas\norg.apache.karaf.kar\n", configListOutput);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void configsViaMBean() throws Exception {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("org.apache.karaf:type=config,name=root");
        List<String> configs = (List<String>) mbeanServer.getAttribute(name, "Configs");
        assertThat(configs, hasItem("org.apache.karaf.features"));
        Map<String, String> properties = (Map<String, String>) mbeanServer
            .invoke(name, "listProperties", new Object[]{"org.apache.karaf.features"}, new String[]{"java.lang.String"});
        assertThat(properties, hasKey("featuresRepositories"));
    }

}
