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
 * <p>This interface represents MessageDigester which using MessageDigest to 
 * encrypt credential with one of algorithms SHA-1,etc. and verify hashes.</p>
 * 
 * @version $Rev$ $Date$
 */
public interface MessageDigester
{
    /**
     * Encrypting provided value with one of available algorithms like SHA-1.
     * 
     * @param value to be encrypted.
     * @param salt will be use to encrypts the value.
     * @return encrypted value.
     */
    byte[] encrypt(Object value, byte[] salt);

    /**
     * <p>Verify not encoded credential against encoded one.
     * This method is encoding provided value and compare generated hash with provided
     * hash(digest param).</p>
     * 
     * @param value not encoded value to be verified.
     * @param digest encoded value (hash).
     * @param lenghBytes length of salt byte value.
     * @return true if hash of provided value matches digest param.
     */
    boolean verify(Object value, byte[] digest, int lenghBytes);

    /**
     * Generate salt used by digester to digest message.
     * @param lengthBytes length of salt.
     * @return salt value.
     */
    byte[] generateSalt(int lengthBytes);

    /**
     * Setting char set for digester.
     * @param charset char set.
     */
    void setCharset(String charset);
}
