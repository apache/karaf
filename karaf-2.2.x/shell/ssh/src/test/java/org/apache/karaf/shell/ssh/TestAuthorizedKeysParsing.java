/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.ssh;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;

/**
 * Test parsing an authorized_keys file.
 */
public class TestAuthorizedKeysParsing extends TestCase {

    public void testAuthorizedKeysParsing() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        InputStream is = TestAuthorizedKeysParsing.class.getClassLoader().getResourceAsStream("org/apache/karaf/shell/ssh/authorized_keys");
        Map<PublicKey, KarafPublickeyAuthenticator.AuthorizedKey> keys = KarafPublickeyAuthenticator.parseAuthorizedKeys(is);
        assertEquals(2, keys.size());
        for (Map.Entry<PublicKey, KarafPublickeyAuthenticator.AuthorizedKey> e : keys.entrySet()) {
            assertSame(e.getKey(), e.getValue().getPublicKey());
            assertTrue("ssh-dss".equals(e.getValue().getFormat()) || "ssh-rsa".equals(e.getValue().getFormat()));

            if ("ssh-dss".equals(e.getValue().getFormat())) {
                assertTrue(e.getKey() instanceof DSAPublicKey);
                assertEquals("dsa-test", e.getValue().getAlias());
            }
            if ("ssh-rsa".equals(e.getValue().getFormat())) {
                assertTrue(e.getKey() instanceof RSAPublicKey);
                assertEquals("rsa-test", e.getValue().getAlias());
            }
        }
    }

}
