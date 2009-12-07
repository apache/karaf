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
 * <p>Credential authenticator used for authenticate base on stored credentials.</p>
 *
 * @version $Rev$ $Date$
 */
public interface CredentialAuthenticator
{
    /**
     * Encrypt provided credential value with one of
     * algorithms Base64, SHA-1, etc.
     * @param credential to be encrypted.
     * @return encrypted value.
     */
    Object encryptCredential(Object credential);

    /**
     * Authenticate provided value against encrypted stored
     * value.
     * @param value to be check against encrypted Value.
     * @param encryptedValue encrypted value.
     * @return true if user is authenticated false if not.
     */
    boolean authenticate(Object value, Object encryptedValue);

    /**
     * This method returns Base64 encoder.
     * @return base64 encoder.
     */
    Base64 getBase64();
}
