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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.service.cm.Configuration;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.Assert.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ExternalConfigTest extends KarafTestSupport {

    @Test
    public void externalConfigTest() throws Exception {
        Path dir = Paths.get(System.getProperty("karaf.base"), "system/org/foo/bar/1.0-SNAPSHOT");
        Files.createDirectories(dir);
        
        writeTo(dir.resolve("bar-1.0-SNAPSHOT.properties"), //
                "key=value");

        writeTo(dir.resolve("bar-1.0-SNAPSHOT-features.xml"), // 
                "<features name='org.foo'>\n" +
                "  <feature name='bar' version='1.0-SNAPSHOT'>\n" +
                "    <config name='org.foo' external='true'>\n" +
                "      mvn:org.foo/bar/1.0-SNAPSHOT/properties" +
                "    </config>\n" +
                "  </feature>\n" +
                "</features>\n");

        Configuration[] cfgs = configurationAdmin.listConfigurations("(service.pid=org.foo)");
        assertNull(cfgs);

        featureService.addRepository(URI.create("mvn:org.foo/bar/1.0-SNAPSHOT/xml/features"), true);

        cfgs = configurationAdmin.listConfigurations("(service.pid=org.foo)");
        assertNotNull(cfgs);
        assertEquals(1, cfgs.length);
        assertEquals("value", cfgs[0].getProperties().get("key"));
    }

    private void writeTo(Path file, String content) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file, CREATE, TRUNCATE_EXISTING, WRITE)) {
            w.write(content);
        }
    }

}
