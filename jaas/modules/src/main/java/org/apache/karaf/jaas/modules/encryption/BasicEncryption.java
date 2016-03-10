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
package org.apache.karaf.jaas.modules.encryption;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.apache.karaf.jaas.modules.Encryption;
import org.apache.karaf.jaas.modules.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicEncryption implements Encryption {

    private static final Logger log = LoggerFactory.getLogger(BasicEncryption.class);

    private String algorithm;
    private String encoding;
    private MessageDigest md;

    public BasicEncryption(Map<String, String> params) {
        for (String key : params.keySet()) {
            if (EncryptionService.ALGORITHM.equalsIgnoreCase(key)) {
                algorithm = params.get(key);
            } else if (EncryptionService.ENCODING.equalsIgnoreCase(key)) {
                encoding = params.get(key);
            } else {
                throw new IllegalArgumentException("Unsupported encryption parameter: " + key);
            }
        }
        if (algorithm == null) {
            throw new IllegalArgumentException("Digest algorithm must be specified");
        }
        // Check if the algorithm algorithm is available
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            log.error("Initialization failed. Digest algorithm " + algorithm + " is not available.", e);
            throw new IllegalArgumentException("Unable to configure login module: " + e.getMessage(), e);
        }
        if (encoding != null && encoding.length() > 0
                && !EncryptionService.ENCODING_HEXADECIMAL.equalsIgnoreCase(encoding)
                && !EncryptionService.ENCODING_BASE64.equalsIgnoreCase(encoding)) {
            log.error("Initialization failed. Digest encoding " + encoding + " is not supported.");
            throw new IllegalArgumentException(
                    "Unable to configure login module. Digest Encoding " + encoding + " not supported.");
        }
    }

    public String encryptPassword(String password) {
        if (password == null) {
            return null;
        }
        // Digest the user provided password
        byte[] data = md.digest(password.getBytes());
        if (encoding == null || encoding.length() == 0 || EncryptionService.ENCODING_HEXADECIMAL.equalsIgnoreCase(encoding)) {
            return hexEncode(data);
        } else if (EncryptionService.ENCODING_BASE64.equalsIgnoreCase(encoding)) {
            return base64Encode(data);
        } else {
            throw new IllegalArgumentException(
                    "Unable to configure login module. Digest Encoding " + encoding + " not supported.");
        }
    }

    public boolean checkPassword(String provided, String real) {
        if (real == null && provided == null) {
            return true;
        }
        if (real == null || provided == null) {
            return false;
        }
        // both are non-null
        String encoded = encryptPassword(provided);
        if (encoding == null || encoding.length() == 0 || EncryptionService.ENCODING_HEXADECIMAL.equalsIgnoreCase(encoding)) {
            return real.equalsIgnoreCase(encoded);
        } else if (EncryptionService.ENCODING_BASE64.equalsIgnoreCase(encoding)) {
            return real.equals(encoded);
        }
        return false;
    }

    public static String hexEncode(byte[] in) {
        return DatatypeConverter.printHexBinary(in);
    }

    /**
     * Encode the input data producing a base 64 encoded byte array.
     *
     * @param input the String to encore as an array of byte.
     * @return a byte array containing the base 64 encoded data.
     */
    public static String base64Encode(byte[] input) {
        return DatatypeConverter.printBase64Binary(input);
    }

}