/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.felix.useradmin;

/**
 * This interface represents contract for Base64 encoding. 
 * 
 *
 * @version $Rev$ $Date$
 */
public interface Base64
{
    /**
     * Encrypt value object must be String or byte array with Base64 algorithm.
     * @param value String to be encoded
     * @return encoded value with Base64
     */
    Object encrypt(Object value);

    /**
     * This method is decrypting encoded value with Base64.
     * @param value encoded value.
     * @return decrypted value.
     */
    Object decrypt(Object value);

    /**
     * This method is decrypting encoded value with Base64 to byte[].
     * @param value encoded value.
     * @return decrypted value.
     */
    byte[] decryptToByteArray(Object value);

    /**
     * Verifying two values if there are equal.
     * @param value object to be verified.
     * @param encrypted value.
     * @return true if those 2 values are equal if not false.
     */
    boolean verify(Object value, Object encrypted);

    /**
     * Setting character set.
     * @param charset Character set.
     */
    void setCharset(String charset);
}
