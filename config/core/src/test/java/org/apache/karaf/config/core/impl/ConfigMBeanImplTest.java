/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.config.core.impl;

import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class ConfigMBeanImplTest {

    @Test(expected = IllegalArgumentException.class)
    public void testInstallWithNonAuthorizePath() throws Exception {
        System.setProperty("karaf.etc", ".");

        ConfigMBeanImpl configMBean = new ConfigMBeanImpl();

        configMBean.install("file:foo.cfg", "../test.cfg", false);
    }

    @Test
    public void testInstall() throws Exception {
        System.setProperty("karaf.etc", "./target/test-classes");

        ConfigMBeanImpl configMBean = new ConfigMBeanImpl();

        configMBean.install("file:./target/test-classes/test.cfg", "foo.cfg", true);

        File output = new File("target/test-classes/foo.cfg");

        Assert.assertTrue(output.exists());

        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(output));
        String line = null;
        while ((line = reader.readLine()) != null) {
            builder.append(line).append("\n");
        }
        Assert.assertTrue(builder.toString().contains("foo=bar"));
    }

}
