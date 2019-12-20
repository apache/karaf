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
package org.apache.karaf.instance.core.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.karaf.instance.core.Instance;
import org.apache.karaf.instance.core.InstanceSettings;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class InstanceServiceImplTest {

    @Rule
    public TestName name = new TestName();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUpClass() throws Exception {
        String buildDirectory = ClassLoader.getSystemResource("etc/startup.properties").getFile()
                .replace("startup.properties", "");
        System.setProperty("karaf.etc", buildDirectory);
    }

    @Test
    public void testHandleFeatures() throws Exception {
        InstanceServiceImpl as = new InstanceServiceImpl();

        File f = tempFolder.newFile(getName() + ".test");
        Properties p = new Properties();
        p.put("featuresBoot", "abc,def ");
        p.put("featuresRepositories", "somescheme://xyz");
        try (OutputStream os = new FileOutputStream(f)) {
            p.store(os, "Test comment");
        }

        InstanceSettings s = new InstanceSettings(8122, 1122, 44444, null, null, null,
            Collections.singletonList("test"));
        as.addFeaturesFromSettings(f, s);

        Properties p2 = new Properties();
        try (InputStream is = new FileInputStream(f)) {
            p2.load(is);
        }
        assertEquals(2, p2.size());
        assertEquals("abc,def,test", p2.get("featuresBoot"));
        assertEquals("somescheme://xyz", p2.get("featuresRepositories"));
    }

    @Test
    public void testConfigurationFiles() throws Exception {
        InstanceServiceImpl service = new InstanceServiceImpl();
        service.setStorageLocation(tempFolder.newFolder("instances"));

        InstanceSettings settings = new InstanceSettings(8122, 1122, 44444, getName(), null, null, null);
        Instance instance = service.createInstance(getName(), settings, true);

        assertFileExists(instance.getLocation(), "etc/config.properties");
        assertFileExists(instance.getLocation(), "etc/users.properties");
        assertFileExists(instance.getLocation(), "etc/startup.properties");

        assertFileExists(instance.getLocation(), "etc/java.util.logging.properties");
        assertFileExists(instance.getLocation(), "etc/org.apache.karaf.features.cfg");
        assertFileExists(instance.getLocation(), "etc/org.apache.karaf.shell.cfg");
        assertFileExists(instance.getLocation(), "etc/org.apache.karaf.management.cfg");
        assertFileExists(instance.getLocation(), "etc/org.ops4j.pax.logging.cfg");
        assertFileExists(instance.getLocation(), "etc/org.ops4j.pax.url.mvn.cfg");
    }

    public void testTextResources() throws Exception {
        InstanceServiceImpl service = new InstanceServiceImpl();
        service.setStorageLocation(new File("target/instances/" + System.currentTimeMillis()));
        Map<String, URL> textResources = new HashMap<>();
        textResources.put("etc/myresource", getClass().getClassLoader().getResource("myresource"));

        InstanceSettings settings = new InstanceSettings(8122, 1122, 44444, getName(), null, null, null, null, textResources, new HashMap<>());
        Instance instance = service.createInstance(getName(), settings, false);

        assertFileExists(instance.getLocation(), "etc/myresource");
    }

    /**
     * <p>
     * Test the renaming of an existing instance.
     * </p>
     */
    @Test
    public void testRenameInstance() throws Exception {
        InstanceServiceImpl service = new InstanceServiceImpl();
        service.setStorageLocation(tempFolder.newFolder("instances"));

        InstanceSettings settings = new InstanceSettings(8122, 1122, 44444, getName(), null, null, null);
        service.createInstance(getName(), settings, true);

        service.renameInstance(getName(), getName() + "b", true);
        assertNotNull(service.getInstance(getName() + "b"));
    }

    /**
     * <p>
     * Test the renaming of an existing instance.
     * </p>
     */
    @Test
    public void testToSimulateRenameInstanceByExternalProcess() throws Exception {
        InstanceServiceImpl service = new InstanceServiceImpl();
        File storageLocation = tempFolder.newFolder("instances");
        service.setStorageLocation(storageLocation);

        InstanceSettings settings = new InstanceSettings(8122, 1122, 44444, getName(), null, null, null);
        service.createInstance(getName(), settings, true);
        
        //to simulate the scenario that the instance name get changed by 
        //external process, likely the admin command CLI tool, which cause
        //the instance storage file get updated, the AdminService should be 
        //able to reload the storage file before check any status for the 
        //instance
        
        File storageFile = new File(storageLocation, InstanceServiceImpl.STORAGE_FILE);
        assertTrue(storageFile.isFile());
        Properties storage = loadStorage(storageFile);
        storage.setProperty("item.0.name", getName() + "b");
        saveStorage(storage, storageFile, "testToSimulateRenameInstanceByExternalProcess");
        
        assertNotNull(service.getInstance(getName() + "b"));
    }

    @Test
    public void testSshPortChange() throws Exception {
        InstanceServiceImpl service = new InstanceServiceImpl();
        File storageLocation = tempFolder.newFolder("instances");
        service.setStorageLocation(storageLocation);

        InstanceSettings settings = new InstanceSettings(8122, 1122, 44444, getName(), null, null, null);
        service.createInstance(getName(), settings, true);

        service.changeInstanceSshPort(getName(), 9999);

        File shellCfg = new File(new File(new File(storageLocation, getName()), "etc"), "org.apache.karaf.shell.cfg");
        Properties props = new Properties();
        props.load(new FileInputStream(shellCfg));
        assertEquals("9999", props.get("sshPort"));
    }

    private String getName() {
        return name.getMethodName();
    }

    private void saveStorage(Properties props, File location, String comment) throws IOException {
        try (OutputStream os = new FileOutputStream(location)) {
            props.store(os, comment);
        }
    }
    
    private Properties loadStorage(File location) throws IOException {
        try (InputStream is = new FileInputStream(location)) {
            Properties props = new Properties();
            props.load(is);
            return props;
        }
    }

    private void assertFileExists(String path, String name) throws IOException {
        File file = new File(path, name);
        assertTrue("Expected " + file.getCanonicalPath() + " to exist",
                   file.exists());
    }   
}
