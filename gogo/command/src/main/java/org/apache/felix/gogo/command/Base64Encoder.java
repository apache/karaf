/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.gogo.command;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Base64Encoder
{
    private static final byte encTab[] = { 0x41, 0x42, 0x43, 0x44, 0x45, 0x46,
        0x47, 0x48, 0x49, 0x4a, 0x4b, 0x4c, 0x4d, 0x4e, 0x4f, 0x50, 0x51, 0x52,
        0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5a, 0x61, 0x62, 0x63, 0x64,
        0x65, 0x66, 0x67, 0x68, 0x69, 0x6a, 0x6b, 0x6c, 0x6d, 0x6e, 0x6f, 0x70,
        0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7a, 0x30, 0x31,
        0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x2b, 0x2f };

    public static String base64Encode(String s) throws IOException
    {
        return encode(s.getBytes(), 0);
    }

    /**
     * Encode a raw byte array to a Base64 String.
     * 
     * @param in Byte array to encode.
     * @param len Length of Base64 lines. 0 means no line breaks.
    **/
    public static String encode(byte[] in, int len) throws IOException
    {
        ByteArrayOutputStream baos = null;
        ByteArrayInputStream bais = null;
        try
        {
            baos = new ByteArrayOutputStream();
            bais = new ByteArrayInputStream(in);
            encode(bais, baos, len);
            // ASCII byte array to String
            return (new String(baos.toByteArray()));
        }
        finally
        {
            if (baos != null)
            {
                baos.close();
            }
            if (bais != null)
            {
                bais.close();
            }
        }
    }

    public static void encode(InputStream in, OutputStream out, int len)
        throws IOException
    {

        // Check that length is a multiple of 4 bytes
        if (len % 4 != 0)
        {
            throw new IllegalArgumentException("Length must be a multiple of 4");
        }

        // Read input stream until end of file
        int bits = 0;
        int nbits = 0;
        int nbytes = 0;
        int b;

        while ((b = in.read()) != -1)
        {
            bits = (bits << 8) | b;
            nbits += 8;
            while (nbits >= 6)
            {
                nbits -= 6;
                out.write(encTab[0x3f & (bits >> nbits)]);
                nbytes++;
                // New line
                if (len != 0 && nbytes >= len)
                {
                    out.write(0x0d);
                    out.write(0x0a);
                    nbytes -= len;
                }
            }
        }

        switch (nbits)
        {
            case 2:
                out.write(encTab[0x3f & (bits << 4)]);
                out.write(0x3d); // 0x3d = '='
                out.write(0x3d);
                break;
            case 4:
                out.write(encTab[0x3f & (bits << 2)]);
                out.write(0x3d);
                break;
        }

        if (len != 0)
        {
            if (nbytes != 0)
            {
                out.write(0x0d);
                out.write(0x0a);
            }
            out.write(0x0d);
            out.write(0x0a);
        }
    }
}