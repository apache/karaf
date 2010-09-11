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
package org.apache.karaf.features.encryption.internal;

import org.apache.karaf.jaas.modules.Encryption;
import org.jasypt.util.password.ConfigurablePasswordEncryptor;

public class JasyptEncryption implements Encryption {
    
    private ConfigurablePasswordEncryptor passwordEncryptor;
    
    /**
     * <p>
     * Default constructor.
     * </p>
     */
    public JasyptEncryption() {
        passwordEncryptor = new ConfigurablePasswordEncryptor();
    }
    
    /**
     * <p>
     * Constructor with encryption algorithm.
     * </p>
     * 
     * @param algorithm the encryption algorithm to use.
     */
    public JasyptEncryption(String algorithm) {
        passwordEncryptor = new ConfigurablePasswordEncryptor();
        passwordEncryptor.setAlgorithm(algorithm);
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.karaf.jaas.modules.Encryption#setAlgorithm(java.lang.String)
     */
    public void setAlgorithm(String algorithm) {
        passwordEncryptor.setAlgorithm(algorithm);
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.karaf.jaas.modules.Encryption#encryptPassword(java.lang.String)
     */
    public String encryptPassword(String plain) {
        return passwordEncryptor.encryptPassword(plain);
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.karaf.jaas.modules.Encryption#checkPassword(java.lang.String, java.lang.String)
     */
    public boolean checkPassword(String input, String password) {
        return passwordEncryptor.checkPassword(input, password);
    }

}
