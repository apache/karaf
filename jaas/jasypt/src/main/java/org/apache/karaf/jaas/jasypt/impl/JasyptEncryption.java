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

import java.util.Map;

import org.apache.karaf.jaas.modules.Encryption;
import org.apache.karaf.jaas.modules.EncryptionService;
import org.jasypt.digest.config.SimpleStringDigesterConfig;
import org.jasypt.exceptions.EncryptionInitializationException;
import org.jasypt.util.password.ConfigurablePasswordEncryptor;

/**
 * Jasypt implementation of the Encryption service.
 */
public class JasyptEncryption implements Encryption {

    private ConfigurablePasswordEncryptor passwordEncryptor;
    
    /**
     * <p>
     * Default constructor with the encryption algorithm.
     * </p>
     * 
     * @param params encryption parameters
     */
    public JasyptEncryption(Map<String,String> params) {
        SimpleStringDigesterConfig config = new SimpleStringDigesterConfig();
        for (String key : params.keySet()) {
            if (EncryptionService.ALGORITHM.equalsIgnoreCase(key)) {
                config.setAlgorithm(params.get(key));
            } else if (EncryptionService.ENCODING.equalsIgnoreCase(key)) {
                config.setStringOutputType(params.get(key));
            } else if ("providerName".equalsIgnoreCase(key)) {
                config.setProviderName(params.get(key));
            } else if ("saltSizeBytes".equalsIgnoreCase(key)) {
                config.setSaltSizeBytes(params.get(key));
            } else if ("iterations".equalsIgnoreCase(key)) {
                config.setIterations(params.get(key));
            } else if ("providerClassName".equalsIgnoreCase(key)) {
                config.setProviderClassName(params.get(key));
            } else if ("saltGeneratorClassName".equalsIgnoreCase(key)) {
                config.setSaltGeneratorClassName(params.get(key));
            } else {
                throw new IllegalArgumentException("Unsupported encryption parameter: " + key);
            }
        }
        this.passwordEncryptor = new ConfigurablePasswordEncryptor();
        this.passwordEncryptor.setConfig(config);
        try {
            this.passwordEncryptor.encryptPassword("test");
        } catch (EncryptionInitializationException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.karaf.jaas.modules.Encryption#encryptPassword(java.lang.String)
     */
    public String encryptPassword(String plain) {
        return this.passwordEncryptor.encryptPassword(plain);
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.karaf.jaas.modules.Encryption#checkPassword(java.lang.String, java.lang.String)
     */
    public boolean checkPassword(String input, String password) {
        return passwordEncryptor.checkPassword(input, password);
    }
    
}
