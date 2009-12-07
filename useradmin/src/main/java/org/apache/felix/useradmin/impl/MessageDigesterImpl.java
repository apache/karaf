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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.apache.felix.useradmin.MessageDigester;

/**
 * <p>This class <tt>MessageDigesterImpl</tt> implements MessageDigester.
 * Used for encrypting credentials with MessageDigest.</p>
 * 
 * @version $Rev$ $Date$
 */
public class MessageDigesterImpl implements MessageDigester
{
    private String charset;
    private MessageDigest digester;
    private SecureRandom secureRandom;

    /**
     * Constructs new MessageDigester.
     * 
     * @param algorithm name of algorithm to use.
     * @throws NoSuchAlgorithmException
     */
    public MessageDigesterImpl(String algorithm, String randomGenerator) throws NoSuchAlgorithmException
    {
        this.digester = MessageDigest.getInstance(algorithm);
        //RNG Random Number Generator
        this.secureRandom = SecureRandom.getInstance(randomGenerator);
        this.secureRandom.setSeed(System.currentTimeMillis());
    }

    /** 
     * @see org.apache.felix.useradmin.MessageDigester#encryptCredential(java.lang.Object, byte[])
     */
    public synchronized byte[] encrypt(Object credential, byte[] salt)
    {
        byte[] ccredential;
        try
        {
            ccredential = credential instanceof String ? ((String) credential).getBytes(charset)
                : (byte[]) credential;
        }
        catch (UnsupportedEncodingException e)
        {
            //log
            ccredential = ((String) credential).getBytes();
        }
        digester.reset();
        byte[] digest = null;
        digester.update(salt);
        digester.update(ccredential);
        digest = digester.digest();
        // perform iteration safe is to do more than 1000
        for (int i = 0; i <= 1001; i++)
        {
            digester.reset();
            digest = digester.digest(digest);
        }
        return concatenate(salt, digest);
    }

    /**
     * @see org.apache.felix.useradmin.MessageDigester#verify(java.lang.Object, byte[], int)
     */
    public boolean verify(Object plainCredential, byte[] digest, int lenghBytes)
    {
        byte[] salt = getSalt(digest, 0, lenghBytes);
        byte[] encCredential = encrypt(plainCredential, salt);
        return MessageDigest.isEqual(encCredential, digest);
    }

    /**
     * Getting salt from encoded bytes.
     * 
     * @param array digest.
     * @param startIndexInclusive inclussive index.
     * @param endIndexExclusive end exclusive index.
     * @return salt byte array.
     */
    private byte[] getSalt(byte[] array, int startIndexInclusive, int endIndexExclusive)
    {
        if (array == null)
        {
            return null;
        }
        if (startIndexInclusive < 0)
        {
            startIndexInclusive = 0;
        }
        if (endIndexExclusive > array.length)
        {
            endIndexExclusive = array.length;
        }
        int newSize = endIndexExclusive - startIndexInclusive;
        if (newSize <= 0)
        {
            return new byte[0];
        }
        byte[] subarray = new byte[newSize];
        System.arraycopy(array, startIndexInclusive, subarray, 0, newSize);
        return subarray;
    }

    /**
     * @see org.apache.felix.useradmin.MessageDigester#generateSalt(int)
     */
    public synchronized byte[] generateSalt(int lengthBytes)
    {
        byte[] salt = new byte[lengthBytes];
        this.secureRandom.nextBytes(salt);

        return salt;
    }

    /**
     * Concatenates two arrays of bytes. 
     * 
     * @param arraya byte array.
     * @param arrayb byte array.
     * @return concatenated array.
     */
    private byte[] concatenate(byte[] arraya, byte[] arrayb)
    {
        byte[] result = new byte[arraya.length + arrayb.length];
        System.arraycopy(arraya, 0, result, 0, arraya.length);
        System.arraycopy(arrayb, 0, result, arraya.length, arrayb.length);
        return result;
    }

    /**
     * @see org.apache.felix.useradmin.MessageDigester#setCharset(String)
     */
    public void setCharset(String charset)
    {
        this.charset = charset;
    }
}
