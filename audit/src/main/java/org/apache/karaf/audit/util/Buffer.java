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
package org.apache.karaf.audit.util;

import org.apache.karaf.jaas.boot.principal.ClientPrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;

import javax.security.auth.Subject;
import java.io.IOException;
import java.io.Writer;
import java.security.Principal;

public final class Buffer implements Appendable, CharSequence {

    public enum Format {
        Json, Syslog
    }

    private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7',
                                       '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    protected final Format format;
    protected final int capacity;
    protected char[] buffer;
    protected int position = 0;

    public Buffer(Format format) {
        this(format, 1024);
    }

    public Buffer(Format format, int size) {
        this.format = format;
        this.capacity = size;
        this.buffer = new char[size];
    }

    public char[] buffer() {
        return buffer;
    }

    public int position() {
        return position;
    }

    public void clear() {
        position = 0;
        if (this.buffer.length > capacity) {
            this.buffer = new char[capacity];
        }
    }

    public String toString() {
        return new String(buffer, 0, position);
    }

    public void writeTo(Appendable out) throws IOException {
        if (out instanceof Writer) {
            ((Writer) out).write(buffer, 0, position);
        } else if (out instanceof StringBuilder) {
            ((StringBuilder) out).append(buffer, 0, position);
        } else {
            out.append(this);
        }
    }

    private final void require(int nb) {
        if (position + nb >= buffer.length) {
            char[] b = new char[buffer.length * 2];
            System.arraycopy(buffer, 0, b, 0, position);
            buffer = b;
        }
    }

    @Override
    public Buffer append(CharSequence csq) throws IOException {
        return append(csq, 0, csq.length());
    }

    @Override
    public Buffer append(CharSequence csq, int start, int end) throws IOException {
        if (csq instanceof String) {
            return append((String) csq, start, end);
        } else {
            require(end - start);
            for (int i = start; i < end; i++) {
                buffer[position++] = csq.charAt(i);
            }
            return this;
        }
    }

    public Buffer append(String str) throws IOException {
        return append(str, 0, str.length());
    }

    public Buffer append(String str, int start, int end) throws IOException {
        int nb = end - start;
        require(nb);
        str.getChars(start, end, buffer, position);
        position += nb;
        return this;
    }

    @Override
    public Buffer append(char c) throws IOException {
        require(1);
        buffer[position++] = c;
        return this;
    }

    @Override
    public int length() {
        return position;
    }

    @Override
    public char charAt(int index) {
        return buffer[index];
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new String(buffer, start, end);
    }

    public Buffer format(Object object) throws IOException {
        if (object == null) {
            require(4);
            buffer[position++] = 'n';
            buffer[position++] = 'u';
            buffer[position++] = 'l';
            buffer[position++] = 'l';
            return this;
        } else if (object.getClass().isArray()) {
            return format((Object[]) object);
        } else if (object instanceof Subject) {
            return format((Subject) object);
        } else {
            return format(object.toString());
        }
    }

    public Buffer format(Object[] array) throws IOException {
        require(array.length * 10);
        buffer[position++] = '[';
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                buffer[position++] = ',';
                buffer[position++] = ' ';
            }
            format(array[i]);
        }
        if (format == Format.Syslog) {
            buffer[position++] = '\\';
        }
        buffer[position++] = ']';
        return this;
    }

    public Buffer format(Subject subject) throws IOException {
        String up = null;
        String cp = null;
        for (Principal p : subject.getPrincipals()) {
            if (p instanceof UserPrincipal) {
                up = p.getName();
            } else if (p instanceof ClientPrincipal) {
                cp = p.getName();
            }
        }
        if (up != null) {
            append(up);
        } else {
            append('?');
        }
        if (cp != null) {
            append('@');
            append(cp);
        }
        return this;
    }

    public Buffer format(String cs) throws IOException {
        switch (format) {
            case Json:
                formatJson(cs);
                break;
            case Syslog:
                formatSyslog(cs);
                break;
        }
        return this;
    }

    public Buffer format(int i) throws IOException {
        require(11);
        position = NumberOutput.outputInt(i, buffer, position);
        return this;
    }

    public Buffer format(long i) throws IOException {
        require(20);
        position = NumberOutput.outputLong(i, buffer, position);
        return this;
    }

    private void formatJson(String value) throws IOException {
        int len = value.length();
        require(len * 4);
        position = transferJson(position, buffer, value, 0, len);
    }

    private void formatSyslog(String value) throws IOException {
        int end = value.length();
        int max = Math.min(end, 255);
        require(max * 4);
        position = transferSyslog(position, buffer, value, 0, max);
        if (end > max) {
            require(3);
            buffer[position++] = '.';
            buffer[position++] = '.';
            buffer[position++] = '.';
        }
    }

    private int transferJson(int position, char[] d, String s, int start, int end) {
        for (int i = start; i < end; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\"':
                    d[position++] = '\\';
                    d[position++] = '"';
                    break;
                case '\\':
                    d[position++] = '\\';
                    d[position++] = '\\';
                    break;
                case '\b':
                    d[position++] = '\\';
                    d[position++] = 'b';
                    break;
                case '\f':
                    d[position++] = '\\';
                    d[position++] = 'f';
                    break;
                case '\n':
                    d[position++] = '\\';
                    d[position++] = 'n';
                    break;
                case '\r':
                    d[position++] = '\\';
                    d[position++] = 'r';
                    break;
                case '\t':
                    d[position++] = '\\';
                    d[position++] = 't';
                    break;
                default:
                    if (c < ' ' || (c >= '\u0080' && c < '\u00a0') || (c >= '\u2000' && c < '\u2100')) {
                        d[position++] = '\\';
                        d[position++] = 'u';
                        d[position++] = HEX_DIGITS[c >> 12];
                        d[position++] = HEX_DIGITS[(c >> 8) & 0x0F];
                        d[position++] = HEX_DIGITS[(c >> 4) & 0x0F];
                        d[position++] = HEX_DIGITS[c & 0x0F];
                    } else {
                        d[position++] = c;
                    }
                    break;
            }
        }
        return position;
    }

    private int transferSyslog(int position, char[] d, String s, int start, int end) {
        for (int i = start; i < end; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                case '\\':
                case ']':
                    d[position++] = '\\';
                    d[position++] = c;
                    break;
                default:
                    d[position++] = c;
                    break;
            }
        }
        return position;
    }

}
