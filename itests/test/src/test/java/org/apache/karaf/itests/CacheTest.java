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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import java.util.stream.Stream;

import static org.junit.Assert.fail;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CacheTest extends BaseTest
{

    @Configuration
    public Option[] config()
    {
        Option cacheConfigOption = KarafDistributionOption.replaceConfigurationFile("etc/test-cache-config.xml",
                getConfigFile("/etc/test-cache-config.xml"));
        return Stream.of(super.config(), new Option[] { cacheConfigOption })
                .flatMap(Stream::of)
                .toArray(Option[]::new);
    }

    @Before
    public void setUp() throws Exception
    {
        installAndAssertFeature("cache");
    }

    @Test
    public void testCommands() throws Exception
    {
        executeCommand("cache:create test-cache-config.xml");
        String list = executeCommand("cache:list");
        assertContains("TestCache", list);
        executeCommand("cache:put TestCache key value");
        String cachedValue = executeCommand("cache:get TestCache key");
        assertContains("value", cachedValue);
        executeCommand("cache:invalidate TestCache");

        try
        {
            executeCommand("cache:get TestCache key");
            fail("cache:get worked even though the cache was invalidated!");
        }
        catch (Exception e)
        {
            // expected
        }
    }
}
