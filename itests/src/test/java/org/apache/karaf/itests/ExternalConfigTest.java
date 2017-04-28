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

import org.apache.karaf.features.FeaturesService;
import org.apache.sshd.common.util.io.IoUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ExternalConfigTest extends KarafTestSupport {

    @Test
    public void externalConfigTest() throws Exception {
        Path dir = Paths.get(System.getProperty("karaf.base"), "system/org/foo/bar/1.0-SNAPSHOT");
        Files.createDirectories(dir);
        try (BufferedWriter w = Files.newBufferedWriter(dir.resolve("bar-1.0-SNAPSHOT.properties"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            w.write("key=value");
        }
        try (BufferedWriter w = Files.newBufferedWriter(dir.resolve("bar-1.0-SNAPSHOT-features.xml"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            w.write("<features name='org.foo'>\n" +
                    "  <feature name='bar' version='1.0-SNAPSHOT'>\n" +
                    "    <config name='org.foo' external='true'>\n" +
                    "      mvn:org.foo/bar/1.0-SNAPSHOT/properties" +
                    "    </config>\n" +
                    "  </feature>\n" +
                    "</features>\n");
        }

        ConfigurationAdmin ca = getOsgiService(ConfigurationAdmin.class);

        Configuration[] cfgs = ca.listConfigurations("(service.pid=org.foo)");
        assertNull(cfgs);

        getOsgiService(FeaturesService.class)
                .addRepository(URI.create("mvn:org.foo/bar/1.0-SNAPSHOT/xml/features"), true);

        cfgs = ca.listConfigurations("(service.pid=org.foo)");
        assertNotNull(cfgs);
        assertEquals(1, cfgs.length);
        assertEquals("value", cfgs[0].getProperties().get("key"));
    }

}
