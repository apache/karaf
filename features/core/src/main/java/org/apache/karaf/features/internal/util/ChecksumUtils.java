/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.features.internal.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;

public final class ChecksumUtils {

    private ChecksumUtils() {
    }

    /**
     * Compute a checksum for the file or directory that consists of the name, length and the last modified date
     * for a file and its children in case of a directory
     *
     * @param is the input stream
     * @return a checksum identifying any change
     * @throws IOException in case of checksum failure.
     */
    public static long checksum(InputStream is) throws IOException {
        CRC32 crc = new CRC32();
        byte[] buffer = new byte[8192];
        int l;
        while ((l = is.read(buffer)) > 0) {
            crc.update(buffer, 0, l);
        }
        return crc.getValue();
    }

    public static class CRCInputStream extends FilterInputStream {

        private final CRC32 crc = new CRC32();

        public CRCInputStream(InputStream in) {
            super(in);
        }

        public long getCRC() {
            return crc.getValue();
        }

        @Override
        public int read() throws IOException {
            byte[] b = new byte[1];
            int nb = read(b, 0, 1);
            return nb == 1 ? b[0] : -1;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int nb = super.read(b, off, len);
            if (nb > 0) {
                crc.update(b, off, nb);
            }
            return nb;
        }
    }

}
