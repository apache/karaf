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
package org.apache.karaf.jaas.modules;

import java.util.Map;

public interface EncryptionService {

    String ALGORITHM = "algorithm";

    String ALGORITHM_MD2 = "MD2";
    String ALGORITHM_MD5 = "MD5";
    String ALGORITHM_SHA1 = "SHA-1";
    String ALGORITHM_SHA256 = "SHA-256";
    String ALGORITHM_SHA384 = "SHA-384";
    String ALGORITHM_SHA512 = "SHA-512";

    String ENCODING = "encoding";

    String ENCODING_HEXADECIMAL = "hexadecimal";
    String ENCODING_BASE64 = "base64";

    /**
     * Create an encryption service with the specified parameters.
     * If the parameters are not supported, a <code>null</code> should
     * be returned or an IllegalArgumentException thrown.
     *
     * @param params define the encryption configuration.
     * @return the {@link Encryption}.
     * @throws IllegalArgumentException if the {@link Encryption} can't be created.
     */
    Encryption createEncryption(Map<String,String> params) throws IllegalArgumentException;

}
