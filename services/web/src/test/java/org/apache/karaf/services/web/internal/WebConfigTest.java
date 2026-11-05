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
package org.apache.karaf.services.web.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Hashtable;

import org.junit.Test;

public class WebConfigTest {

    @Test
    public void defaults() {
        WebConfig config = WebConfig.from(null);
        assertEquals("0.0.0.0", config.host);
        assertTrue(config.httpEnabled);
        assertEquals(8181, config.httpPort);
        assertFalse(config.httpsEnabled);
        assertEquals(30, config.sessionTimeout);
        assertEquals("karaf", config.getRealm());
        assertEquals(config, WebConfig.from(new Hashtable<>()));
    }

    @Test
    public void properties() {
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put("http.port", "8282");
        properties.put("https.enabled", "true");
        properties.put("https.key.alias", " ");
        properties.put("session.timeout", 10);
        WebConfig config = WebConfig.from(properties);
        assertEquals(8282, config.httpPort);
        assertTrue(config.httpsEnabled);
        // blank values are ignored
        assertNull(config.keyAlias);
        assertEquals(10, config.sessionTimeout);
        assertNotEquals(WebConfig.from(null), config);
        assertEquals(WebConfig.from(properties), config);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidPort() {
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put("http.port", "http");
        WebConfig.from(properties);
    }

    @Test
    public void keystorePath() {
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put("https.keystore", "etc/keystore");
        String path = WebConfig.from(properties).getKeystorePath();
        assertTrue(new File(path).isAbsolute());
        assertTrue(path.endsWith("etc" + File.separator + "keystore"));
    }

}
