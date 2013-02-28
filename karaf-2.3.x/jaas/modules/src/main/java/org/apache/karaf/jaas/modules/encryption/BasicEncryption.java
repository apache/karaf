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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

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

    private static final byte[] hexTable = {
        (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7',
        (byte) '8', (byte) '9', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f'
    };

    public static String hexEncode(byte[] in) {
        int inOff = 0;
        int length = in.length;
        byte[] out = new byte[length * 2];
        for (int i = 0, j = 0; i < length; i++, j += 2) {
            out[j] = hexTable[(in[inOff] >> 4) & 0x0f];
            out[j + 1] = hexTable[in[inOff] & 0x0f];
            inOff++;
        }
        return new String(out);
    }

    private static final byte[] encodingTable = {
        (byte)'A', (byte)'B', (byte)'C', (byte)'D', (byte)'E', (byte)'F', (byte)'G',
        (byte)'H', (byte)'I', (byte)'J', (byte)'K', (byte)'L', (byte)'M', (byte)'N',
        (byte)'O', (byte)'P', (byte)'Q', (byte)'R', (byte)'S', (byte)'T', (byte)'U',
        (byte)'V', (byte)'W', (byte)'X', (byte)'Y', (byte)'Z',
        (byte)'a', (byte)'b', (byte)'c', (byte)'d', (byte)'e', (byte)'f', (byte)'g',
        (byte)'h', (byte)'i', (byte)'j', (byte)'k', (byte)'l', (byte)'m', (byte)'n',
        (byte)'o', (byte)'p', (byte)'q', (byte)'r', (byte)'s', (byte)'t', (byte)'u',
        (byte)'v',
        (byte)'w', (byte)'x', (byte)'y', (byte)'z',
        (byte)'0', (byte)'1', (byte)'2', (byte)'3', (byte)'4', (byte)'5', (byte)'6',
        (byte)'7', (byte)'8', (byte)'9',
        (byte)'+', (byte)'/'
    };

    private static byte padding = (byte)'=';

    /**
     * encode the input data producing a base 64 encoded byte array.
     *
     * @return a byte array containing the base 64 encoded data.
     */
    public static String base64Encode(byte[] data) {
        ByteArrayOutputStream    bOut = new ByteArrayOutputStream();
        try {
            base64Encode(data, 0, data.length, bOut);
        } catch (IOException e) {
            throw new RuntimeException("exception encoding base64 string: " + e.getMessage(), e);
        }
        return new String(bOut.toByteArray());
    }

    /**
     * encode the input data producing a base 64 output stream.
     *
     * @return the number of bytes produced.
     */
    public static int base64Encode(byte[] data, int off, int length, OutputStream out) throws IOException {
        int modulus = length % 3;
        int dataLength = (length - modulus);
        int a1, a2, a3;
        for (int i = off; i < off + dataLength; i += 3) {
            a1 = data[i] & 0xff;
            a2 = data[i + 1] & 0xff;
            a3 = data[i + 2] & 0xff;
            out.write(encodingTable[(a1 >>> 2) & 0x3f]);
            out.write(encodingTable[((a1 << 4) | (a2 >>> 4)) & 0x3f]);
            out.write(encodingTable[((a2 << 2) | (a3 >>> 6)) & 0x3f]);
            out.write(encodingTable[a3 & 0x3f]);
        }
        /*
         * process the tail end.
         */
        int b1, b2, b3;
        int d1, d2;
        switch (modulus) {
            case 0:        /* nothing left to do */
                break;
            case 1:
                d1 = data[off + dataLength] & 0xff;
                b1 = (d1 >>> 2) & 0x3f;
                b2 = (d1 << 4) & 0x3f;
                out.write(encodingTable[b1]);
                out.write(encodingTable[b2]);
                out.write(padding);
                out.write(padding);
                break;
            case 2:
                d1 = data[off + dataLength] & 0xff;
                d2 = data[off + dataLength + 1] & 0xff;
                b1 = (d1 >>> 2) & 0x3f;
                b2 = ((d1 << 4) | (d2 >>> 4)) & 0x3f;
                b3 = (d2 << 2) & 0x3f;
                out.write(encodingTable[b1]);
                out.write(encodingTable[b2]);
                out.write(encodingTable[b3]);
                out.write(padding);
                break;
        }
        return (dataLength / 3) * 4 + ((modulus == 0) ? 0 : 4);
    }


}