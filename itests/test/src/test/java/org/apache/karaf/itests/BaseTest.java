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

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;

import java.util.stream.Stream;

public class BaseTest extends KarafTestSupport {

    @Configuration
    public Option[] config() {
        Option[] options = new Option[]{
                KarafDistributionOption.replaceConfigurationFile("etc/org.ops4j.pax.logging.cfg", getConfigFile("/etc/org.ops4j.pax.logging.cfg")),
                KarafDistributionOption.editConfigurationFilePut("etc/system.properties", "spring31.version", System.getProperty("spring31.version")),
                KarafDistributionOption.editConfigurationFilePut("etc/system.properties", "spring43.version", System.getProperty("spring43.version")),
                KarafDistributionOption.editConfigurationFilePut("etc/system.properties", "spring52.version", System.getProperty("spring52.version")),
                KarafDistributionOption.editConfigurationFilePut("etc/system.properties", "spring53.version", System.getProperty("spring53.version")),
                KarafDistributionOption.editConfigurationFilePut("etc/system.properties", "spring.security31.version", System.getProperty("spring.security31.version")),
                KarafDistributionOption.editConfigurationFilePut("etc/system.properties", "spring.security42.version", System.getProperty("spring.security42.version")),
                KarafDistributionOption.editConfigurationFilePut("etc/system.properties", "spring.security53.version", System.getProperty("spring.security53.version")),
                KarafDistributionOption.editConfigurationFilePut("etc/system.properties", "activemq.version", System.getProperty("activemq.version"))
        };
        return Stream.of(super.config(), options).flatMap(Stream::of).toArray(Option[]::new);
    }

}
