/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.karaf.deployer.spring;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.JarInputStream;

import javax.xml.transform.dom.DOMSource;

import junit.framework.TestCase;
import org.apache.felix.karaf.deployer.spring.SpringDeploymentListener;
import org.apache.felix.karaf.deployer.spring.SpringTransformer;

public class SpringDeploymentListenerTest extends TestCase {

    public void testPackagesExtraction() throws Exception {
        SpringDeploymentListener l = new SpringDeploymentListener();
        File f = new File(getClass().getClassLoader().getResource("test.xml").toURI());
        Set<String> pkgs = SpringTransformer.analyze(new DOMSource(SpringTransformer.parse(f.toURL())));
        assertNotNull(pkgs);
        assertEquals(2, pkgs.size());
        Iterator<String> it = pkgs.iterator();
        assertEquals("org.apache.felix.karaf.deployer.spring", it.next());
        assertEquals("org.osgi.service.url", it.next());
    }

    public void testCustomManifest() throws Exception {
        File f = File.createTempFile("smx", ".jar");
        try {
            OutputStream os = new FileOutputStream(f);
            SpringTransformer.transform(getClass().getClassLoader().getResource("test.xml"), os);
            os.close();
            InputStream is = new FileInputStream(f);
            JarInputStream jar = new JarInputStream(is);
            jar.getManifest().write(System.err);
            is.close();
        } finally {
            f.delete();
        }
    }

    public void testVersions() {
        assertVersion("org.apache.servicemix.bundles.ant-1.7.0-1.0-m3-SNAPSHOT.jar",
                      "org.apache.servicemix.bundles.ant-1.7.0", "1.0.0.m3-SNAPSHOT", "jar");
        assertVersion("org.apache.activemq.core-1.0-SNAPSHOT.xml",
                      "org.apache.activemq.core", "1.0.0.SNAPSHOT", "xml");
        assertVersion("org.apache.activemq.core-1.0.0-SNAPSHOT.xml",
                      "org.apache.activemq.core", "1.0.0.SNAPSHOT", "xml");
        assertVersion("org.apache.activemq.core-1.0.0.xml",
                      "org.apache.activemq.core", "1.0.0", "xml");
        assertVersion("geronimo-servlet_2.5_spec-1.1.2.jar",
                      "geronimo-servlet_2.5_spec", "1.1.2", "jar");
        assertVersion("spring-aop-2.5.1.jar",
                      "spring-aop", "2.5.1", "jar");
    }

    private void assertVersion(String s, String... expectedParts) {
        String[] parts = SpringTransformer.extractNameVersionType(s);
        assertEquals(expectedParts.length, parts.length);
        for (int i = 0; i < expectedParts.length; i++) {
            assertEquals(expectedParts[i], parts[i]);
        }
    }

}
