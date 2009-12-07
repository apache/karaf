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
package org.apache.felix.useradmin.impl;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.apache.felix.useradmin.Base64;
import org.apache.felix.useradmin.CredentialAuthenticator;
import org.apache.felix.useradmin.MessageDigester;

/**
 * <p>
 * This <tt>CredentialAuthenticatorImpl</tt> class is used for
 * authentication of credentials.
 * It provides methods for encrypting credentials.
 * Based on system properties it will choose between Base64 encoding or different
 * algorithm.</p>
 * 
 * @see org.apache.felix.useradmin.CredentialAuthenticator
 * @see java.security.MessageDigest
 * @see org.apache.felix.useradmin.Base64
 * @version $Rev$ $Date$
 */
public class CredentialAuthenticatorImpl implements CredentialAuthenticator
{
    private static final String DEFAULT_CHARSET = "UTF-8";
    private MessageDigester digester;
    private Base64 base64;
    private SecureRandom secureRandom;
    private static final String SECURE_RANDOM_ALGORITHM = "SHA1PRNG";
    private static final String SECURE_DEFAULT_ALGORITHM = "Base64";
    private static final String SECURE_ALOGRITHM_PROP = "org.apache.felix.useradmin.algorithm";
    private static final String SECURE_RNG_ALOGRITHM_PROP = "org.apache.felix.useradmin.rng.algorithm";
    private static final String CHARSET_PROP = "org.apache.felix.useradmin.charset";
    private String charset;
    private final int DEFAULT_BYTES_LENGTH = 20;
    private boolean useDefaultEncryption = true;

    /**
     * <p>
     * Construct new CredentialAuthenticator.
     * Its reading system properties about algorithm which should be used for encoding,charset,
     * secure random number generator algorithm.
     * Default algorithm is Base64 which could be overridden.</p>
     */
    public CredentialAuthenticatorImpl()
    {
        String algorithm = System.getProperty(SECURE_ALOGRITHM_PROP, SECURE_DEFAULT_ALGORITHM);
        // random number generator algorithm used for generating salts.
        String rngAlorithm = System.getProperty(SECURE_RNG_ALOGRITHM_PROP, SECURE_RANDOM_ALGORITHM);
        this.charset = System.getProperty(CHARSET_PROP, DEFAULT_CHARSET);
        this.base64 = new Base64Impl();
        this.base64.setCharset(charset);
        if (!algorithm.equals(SECURE_DEFAULT_ALGORITHM))
        {
            try
            {
                this.digester = new MessageDigesterImpl(algorithm, rngAlorithm);
                this.digester.setCharset(charset);
                this.secureRandom = SecureRandom.getInstance(SECURE_RANDOM_ALGORITHM);
                this.secureRandom.setSeed(System.currentTimeMillis());
                this.useDefaultEncryption = false;
            }
            catch (NoSuchAlgorithmException e)
            {
                // default encryption will be used
            }
        }
    }

    /**
     * @see org.apache.felix.useradmin.CredentialAuthenticator#encryptCredential(Object)
     */
    public Object encryptCredential(Object credential)
    {
        if (useDefaultEncryption)
        {
            return base64.encrypt(credential);
        }

        byte[] salt = digester.generateSalt(DEFAULT_BYTES_LENGTH);
        byte[] digest = digester.encrypt(credential, salt);
        return base64.encrypt(digest);

    }

    /**
     * @see org.apache.felix.useradmin.CredentialAuthenticator#authenticate(Object, Object)
     */
    public boolean authenticate(Object value, Object encryptedValue)
    {
        if (useDefaultEncryption)
        {
            return base64.verify(value, encryptedValue);
        }
        byte[] digest = base64.decryptToByteArray(encryptedValue);
        return digester.verify(value, digest, DEFAULT_BYTES_LENGTH);
    }

    /**
     * @see org.apache.felix.useradmin.CredentialAuthenticator#getBase64()
     */
    public Base64 getBase64()
    {
        return base64;
    }
}
