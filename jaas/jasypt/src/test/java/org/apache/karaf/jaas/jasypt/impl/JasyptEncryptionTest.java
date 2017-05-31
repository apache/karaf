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
package org.apache.karaf.jaas.jasypt.impl;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import org.apache.karaf.jaas.modules.EncryptionService;

/**
 * Test <code>JasyptEncryption</code>.
 */
public class JasyptEncryptionTest extends TestCase {
    
    private JasyptEncryption encryption;
    
    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    public void setUp() {
        Map<String,String> props = new HashMap<>();
        props.put(EncryptionService.ALGORITHM, "MD5");
        this.encryption = new JasyptEncryption(props);
    }
    
    /**
     * <p>
     * Test <code>checkPassword()</p> method.
     * </p>
     * 
     * @throws Exception in case of test error.
     */
    public void testCheckPassword() throws Exception {
        String password = this.encryption.encryptPassword("test");
        
        assertEquals(true, this.encryption.checkPassword("test", password));
    }
    
}
