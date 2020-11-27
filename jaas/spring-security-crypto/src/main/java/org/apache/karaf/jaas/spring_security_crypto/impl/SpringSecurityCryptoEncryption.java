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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.apache.karaf.jaas.modules.Encryption;
import org.apache.karaf.jaas.modules.EncryptionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;

/**
 * Spring Security Crypto implementation of the Encryption service.
 */
public class SpringSecurityCryptoEncryption implements Encryption {

    private static final Logger log = LoggerFactory.getLogger(SpringSecurityCryptoEncryption.class);
    private static final Map<String, Class<? extends PasswordEncoder>> PASSWORD_ENCODERS;
    private PasswordEncoder passwordEncoder;
    private String encoding;

    static {
        Map<String, Class<? extends PasswordEncoder>> passwordEncoders = new HashMap<>();
        passwordEncoders.put("pbkdf2", Pbkdf2PasswordEncoder.class);
        passwordEncoders.put("bcrypt", BCryptPasswordEncoder.class);
        passwordEncoders.put("scrypt", SCryptPasswordEncoder.class);
        passwordEncoders.put("argon2", Argon2PasswordEncoder.class);
        PASSWORD_ENCODERS = Collections.unmodifiableMap(passwordEncoders);
    }

    /**
     * <p>
     * Default constructor with the encryption algorithm.
     * </p>
     * 
     * @param params encryption parameters
     */
    public SpringSecurityCryptoEncryption(Map<String,String> params) {
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (EncryptionService.ALGORITHM.equalsIgnoreCase(entry.getKey())) {
                if (!PASSWORD_ENCODERS.containsKey(entry.getValue())) {
                    throw new IllegalArgumentException("Unsupported algorithm parameter: " + entry.getValue());
                }
                try {
                    passwordEncoder = PASSWORD_ENCODERS.get(entry.getValue()).newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new IllegalArgumentException("Unsupported encryption parameter: " + entry.getKey());
                }
            } else if (EncryptionService.ENCODING.equalsIgnoreCase(entry.getKey())) {
                encoding = entry.getValue();
            }
        }

        if (passwordEncoder == null) {
            throw new IllegalArgumentException("Digest algorithm must be specified");
        }

        if (encoding != null && encoding.length() > 0
                && !EncryptionService.ENCODING_HEXADECIMAL.equalsIgnoreCase(encoding)
                && !EncryptionService.ENCODING_BASE64.equalsIgnoreCase(encoding)) {
            log.error("Initialization failed. Digest encoding " + encoding + " is not supported.");
            throw new IllegalArgumentException(
                    "Unable to configure login module. Digest Encoding " + encoding + " not supported.");
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.karaf.jaas.modules.Encryption#encryptPassword(java.lang.String)
     */
    public String encryptPassword(String plain) {
        String encryptedPassword = passwordEncoder.encode(plain);
        if (EncryptionService.ENCODING_HEXADECIMAL.equalsIgnoreCase(encoding)) {
            return new String(Hex.encode(encryptedPassword.getBytes(StandardCharsets.UTF_8)));
        } else if (EncryptionService.ENCODING_BASE64.equalsIgnoreCase(encoding)) {
            return Base64.getEncoder().encodeToString(encryptedPassword.getBytes(StandardCharsets.UTF_8));
        }
        return encryptedPassword;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.karaf.jaas.modules.Encryption#checkPassword(java.lang.String, java.lang.String)
     */
    public boolean checkPassword(String password, String input) {
        String decodedInput = input;
        if (EncryptionService.ENCODING_HEXADECIMAL.equalsIgnoreCase(encoding)) {
            decodedInput = new String(Hex.decode(input));
        } else if (EncryptionService.ENCODING_BASE64.equalsIgnoreCase(encoding)) {
            decodedInput = new String(Base64.getDecoder().decode(input), StandardCharsets.UTF_8);
        }
        return passwordEncoder.matches(password, decodedInput);
    }

}
