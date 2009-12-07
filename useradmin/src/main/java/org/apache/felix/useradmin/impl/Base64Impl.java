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
import java.util.Arrays;

import org.apache.felix.useradmin.Base64;

/**
 * Base64 encoding implementation.
 * @see org.apache.felix.useradmin.Base64
 * 
 * @version $Rev$ $Date$
 */
public class Base64Impl implements Base64
{
    private String charset;
    /**
     * 111111 six bit mask to get 6 bits
     */
    private static final int BIT6_MASK = 0x3f;
    /**
     * 11111111 8 bit mask to get 8 bits
     */
    private static final int BIT8_MASK = 0xff;
    /**
     * SIGN used to check if byte is signed
     */
    private static final int SIGN = -128;
    /**
     * used for keeping sign bit of signed numbers.
     * when is added to sign number is keep 8 bit as 1.
     */
    private static final int SIGN256 = 256;

    /**
     * @see org.apache.felix.useradmin.Base64#encrypt(java.lang.Object)
     */
    public Object encrypt(Object value)
    {
        byte[] bytes;
        try
        {
            bytes = value instanceof String ? ((String) value).getBytes(charset)
                : (byte[]) value;
        }
        catch (UnsupportedEncodingException e)
        {
            bytes = ((String) value).getBytes();
        }

        StringBuffer encrypted = new StringBuffer();

        for (int i = 0; i < bytes.length; i += 3)
        {
            // every block of 3 bytes is going to 4*6 bits array of chars
            encrypted.append(encryptBlock(bytes, i));
        }
        if (value instanceof String)
        {
            return encrypted.toString();
        }
        else
        {
            return encrypted.toString().getBytes();
        }
    }

    /**
     * This method is encrypting block of 3 bytes if are available if
     * not pads '=' are added.
     * 
     * @param bytes to be encrypted
     * @param i index
     * @return encrypted block represented by base64 alphabet
     */
    private char[] encryptBlock(byte[] bytes, int i)
    {
        // every block of 3 bytes is going to 4*6 bits array of chars
        char[] base64chars = new char[4];
        // block of 32 bits
        int bits32 = 0;
        // space left on byte array
        int space = bytes.length - i - 1;
        int blockSize = (space >= 2) ? 2 : space;

        for (int k = 0; k <= blockSize; k++)
        {
            // if value is signed we need to keep
            // need to keep sign bit 100000000 if byte is signed.
            int val = (bytes[i + k] & SIGN) == 0 ? bytes[i + k] : bytes[i + k] + SIGN256;
            // shift bits left to build one 32 bits block
            bits32 += val << (8 * (2 - k));
        }

        for (int j = 0; j < 4; j++)
        {
            // shift bits to right and take 6 bits using mask
            int bit6 = bits32 >>> (6 * (3 - j)) & BIT6_MASK;
            base64chars[j] = (convertToBase64Alpabet(bit6));
            if (space < 1)
            {
                base64chars[2] = '=';
            }
            if (space < 2)
            {
                base64chars[3] = '=';
            }

        }
        return base64chars;
    }

    /**
     * @see org.apache.felix.useradmin.Base64#decrypt(java.lang.Object)
     */
    public Object decrypt(Object val)
    {
        byte[] decrypted = decryptToByteArray(val);

        if (val instanceof String)
        {
            return convertToString(decrypted);
        }
        return decrypted;
    }

    /**
     * @see org.apache.felix.useradmin.Base64#decryptToByteArray(Object) 
     */
    public byte[] decryptToByteArray(Object val)
    {
        String value;
        try
        {
            value = val instanceof String ? (String) val : new String((byte[]) val, charset);
        }
        catch (UnsupportedEncodingException e)
        {
            value = new String((byte[]) val);
        }

        int pads = 0;
        // looking for pads
        for (int i = value.length() - 1; value.charAt(i) == '='; i--)
        {
            pads++;
        }
        // lenght of byte array
        int length = value.length() * 6 / 8 - pads;
        byte[] decrypted = new byte[length];
        int rawIndex = 0;
        for (int i = 0; i < value.length(); i += 4)
        {
            int block = (getValueFromBase64Alphabet(value.charAt(i)) << 18) + (getValueFromBase64Alphabet(value.charAt(i + 1)) << 12) + (getValueFromBase64Alphabet(value.charAt(i + 2)) << 6) + (getValueFromBase64Alphabet(value.charAt(i + 3)));
            for (int j = 0; j < 3 && rawIndex + j < decrypted.length; j++)
            {
                decrypted[rawIndex + j] = (byte) ((block >> (8 * (2 - j))) & BIT8_MASK);
            }
            rawIndex += 3;
        }
        return decrypted;
    }

    private String convertToString(byte[] raw)
    {
        try
        {
            return new String(raw, charset);
        }
        catch (UnsupportedEncodingException e)
        {
            // log
            return new String(raw);
        }
    }

    /**
     * <p>This method is converting 6 bit number to char.
     * The number should be from 0-63 range. Then need
     * to convert to ASCI [A-Z][a-z]{0-9}'+'/'.</p>
     * @param number 6 bit number to be converted.
     * @return converted 6bit number to char.
     */
    private char convertToBase64Alpabet(int number)
    {
        // 65-90
        if (number >= 0 && number <= 25)
        {
            return (char) ('A' + number);
        }
        // 97-122
        if (number > 25 && number <= 51)
        {
            return (char) ('a' + (number - 26));
        }
        // 47-57
        if (number > 51 && number <= 62)
        {
            return (char) ('0' + (number - 52));
        }

        if (number == 62)
        {
            return '+';
        }
        if (number == 63)
        {
            return '/';
        }

        return '?';
    }

    /**
     * <p>This method is converting characters from base64 to int value</p>.
     * @param character char for which int value is taken.
     * @return int value of provided character
     */
    private int getValueFromBase64Alphabet(char character)
    {
        if (character >= 'A' && character <= 'Z')
        {
            return character - 'A';
        }
        if (character >= 'a' && character <= 'z')
        {
            return character - 'a' + 26;
        }
        if (character >= '0' && character <= '9')
        {
            return character - '0' + 52;
        }
        if (character == '+')
        {
            return 62;
        }
        if (character == '/')
        {
            return 63;
        }
        if (character == '=')
        {
            return 0;
        }
        return -1;
    }

    /**
     * @see org.apache.felix.useradmin.Base64#verify(java.lang.Object, java.lang.Object)
     */
    public boolean verify(Object value, Object encrypted)
    {
        if (value instanceof String && encrypted instanceof String)
        {
            String encryptedValue = (String) encrypt(value);
            return encryptedValue.equals(encrypted);
        }
        else if (value instanceof byte[] && encrypted instanceof byte[])
        {
            byte[] encryptedValue = (byte[]) encrypt(value);
            return Arrays.equals(encryptedValue, (byte[]) encrypted);
        }
        return false;

    }

    /**
     * @see org.apache.felix.useradmin.Base64#setCharset(java.lang.String)
     */
    public void setCharset(String charset)
    {
        this.charset = charset;
    }
}
