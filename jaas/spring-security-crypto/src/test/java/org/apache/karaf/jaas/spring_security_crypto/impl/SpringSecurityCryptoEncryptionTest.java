/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.apache.karaf.jaas.spring_security_crypto.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.karaf.jaas.modules.EncryptionService;

/**
 * Test <code>SpringSecurityCryptoEncryption</code>.
 */
public class SpringSecurityCryptoEncryptionTest {

    @org.junit.Test
    public void testCheckPasswordPBKDF12() throws Exception {
        performTest("pbkdf2", EncryptionService.ENCODING_BASE64);
        performTest("pbkdf2", EncryptionService.ENCODING_HEXADECIMAL);
        performTest("pbkdf2", null);
    }

    @org.junit.Test
    public void testCheckPasswordBCrypt() throws Exception {
        performTest("bcrypt", EncryptionService.ENCODING_BASE64);
        performTest("bcrypt", EncryptionService.ENCODING_HEXADECIMAL);
        performTest("bcrypt", null);
    }

    @org.junit.Test
    public void testCheckPasswordSCrypt() throws Exception {
        performTest("scrypt", EncryptionService.ENCODING_BASE64);
        performTest("scrypt", EncryptionService.ENCODING_HEXADECIMAL);
        performTest("scrypt", null);
    }

    @org.junit.Test
    public void testCheckPasswordArgon2() throws Exception {
        performTest("argon2", EncryptionService.ENCODING_BASE64);
        performTest("argon2", EncryptionService.ENCODING_HEXADECIMAL);
    }

    private void performTest(String algorithm, String encoding) throws Exception {
        Map<String,String> props = new HashMap<>();
        props.put(EncryptionService.ALGORITHM, algorithm);
        props.put(EncryptionService.ENCODING, encoding);
        SpringSecurityCryptoEncryption encryption = new SpringSecurityCryptoEncryption(props);
        String password = encryption.encryptPassword("test");
        org.junit.Assert.assertEquals(true, encryption.checkPassword("test", password));
    }

}
