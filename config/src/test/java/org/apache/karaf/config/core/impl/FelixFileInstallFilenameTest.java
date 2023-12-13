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

import junit.framework.TestCase;
import org.apache.felix.utils.properties.TypedProperties;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import static org.easymock.EasyMock.*;

public class FelixFileInstallFilenameTest extends TestCase {

    private static final String PID = "my.test.persistent.id";

    private static final String CFG_TEST_FILE = "src/test/resources/test.cfg";

    private static final URI CFG_TEST_FILE_URI = Paths.get(CFG_TEST_FILE).toUri();

    private static final String FILE_INSTALL_FILENAME = "felix.fileinstall.filename";

    private ConfigRepositoryImpl configRepository;

    @Override
    protected void setUp() throws Exception {
        ConfigurationAdmin admin = createMock(ConfigurationAdmin.class);
        configRepository = new ConfigRepositoryImpl(admin);

        Configuration config = createMock(Configuration.class);
        expect(admin.getConfiguration(PID, null)).andReturn(config);
        replay(admin);

        Properties properties = new Properties();
        properties.load(Files.newInputStream(Paths.get(CFG_TEST_FILE)));

        Dictionary<String, Object> dictionary = new Hashtable<>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            dictionary.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        dictionary.put(FILE_INSTALL_FILENAME, CFG_TEST_FILE_URI);

        expect(config.getProcessedProperties(null))
                .andReturn(dictionary);
        replay(config);
    }

    public void testGetConfig() throws InvalidSyntaxException, IOException {

        TypedProperties tp = configRepository.getConfig(PID);

        assertNotNull("The felix.fileinstall.filename properties should be present",
                tp.get(FILE_INSTALL_FILENAME));

        assertEquals("The felix.fileinstall.filename properties should be set on PID configs",
                CFG_TEST_FILE_URI.toString(), tp.get(FILE_INSTALL_FILENAME));
    }

    public void testMBeanListProperties() throws Exception {

        ConfigMBeanImpl configMBean = new ConfigMBeanImpl();
        configMBean.setConfigRepo(configRepository);

        Map<String, String> pidProps = configMBean.listProperties(PID);

        assertNotNull("The felix.fileinstall.filename properties should be present",
                pidProps.get(FILE_INSTALL_FILENAME));
    }

}