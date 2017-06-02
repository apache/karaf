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
package org.apache.karaf.util.json;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 */
public final class JsonWriter {

    private JsonWriter() {
    }

    public static void write(OutputStream stream, Object value) throws IOException {
        write(stream, value, false);
    }

    public static void write(OutputStream stream, Object value, boolean format) throws IOException {
        Writer writer = new OutputStreamWriter(stream);
        write(writer, value, format);
        writer.flush();
    }

    public static void write(Writer writer, Object value) throws IOException {
        write(writer, value, false);
    }

    public static void write(Writer writer, Object value, boolean format) throws IOException {
        int indent = format ? 0 : -1;
        write(writer, value, indent);
    }

    @SuppressWarnings("rawtypes")
    private static void write(Writer writer, Object value, int indent) throws IOException {
        if (value instanceof Map) {
            writeObject(writer, (Map) value, indent);
        } else if (value instanceof Collection) {
            writeArray(writer, (Collection) value, indent);
        } else if (value instanceof Number) {
            writeNumber(writer, (Number) value);
        } else if (value instanceof String) {
            writeString(writer, (String) value);
        } else if (value instanceof Boolean) {
            writeBoolean(writer, (Boolean) value);
        } else if (value == null) {
            writeNull(writer);
        } else {
            throw new IllegalArgumentException("Unsupported value: " + value);
        }
    }

    @SuppressWarnings("rawtypes")
    private static void writeObject(Writer writer, Map<?, ?> value, int indent) throws IOException {
        writer.append('{');
        boolean first = true;
        for (Map.Entry entry : value.entrySet()) {
            if (!first) {
                writer.append(',');
            } else {
                first = false;
            }
            if (indent >= 0) {
                indent(writer, indent + 1);
            }
            writeString(writer, (String) entry.getKey());
            if (indent >= 0) {
                writer.append(' ');
            }
            writer.append(':');
            if (indent >= 0) {
                writer.append(' ');
            }
            write(writer, entry.getValue(), indent >= 0 ? indent + 1 : -1);
        }
        if (indent >= 0) {
            indent(writer, indent);
        }
        writer.append('}');
    }

    private static void writeString(Writer writer, String value) throws IOException {
        writer.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
            case '\"':
                writer.append("\\\"");
                break;
            case '\\':
                writer.append("\\\\");
                break;
            case '\b':
                writer.append("\\b");
                break;
            case '\f':
                writer.append("\\f");
                break;
            case '\n':
                writer.append("\\n");
                break;
            case '\r':
                writer.append("\\r");
                break;
            case '\t':
                writer.append("\\t");
                break;
            default:
                if (c < ' ' || (c >= '\u0080' && c < '\u00a0') || (c >= '\u2000' && c < '\u2100')) {
                    String s = Integer.toHexString(c);
                    writer.append('\\');
                    writer.append('u');
                    for (int j = s.length(); j < 4; j++) {
                        writer.append('0');
                    }
                    writer.append(s);
                } else {
                    writer.append(c);
                }
                break;
            }
        }
        writer.append('"');
    }

    private static void writeNumber(Writer writer, Number value) throws IOException {
        writer.append(value.toString());
    }

    private static void writeBoolean(Writer writer, Boolean value) throws IOException {
        writer.append(Boolean.toString(value));
    }

    private static void writeArray(Writer writer, Collection<?> value, int indent) throws IOException {
        writer.append('[');
        boolean first = true;
        for (Object obj : value) {
            if (!first) {
                writer.append(',');
            } else {
                first = false;
            }
            if (indent >= 0) {
                indent(writer, indent + 1);
            }
            write(writer, obj, indent + 1);
        }
        if (indent >= 0) {
            indent(writer, indent);
        }
        writer.append(']');
    }

    private static void writeNull(Writer writer) throws IOException {
        writer.append("null");
    }

    static char[] INDENT;
    static {
        INDENT = new char[1];
        Arrays.fill(INDENT, '\t');
    }

    private static void indent(Writer writer, int indent) throws IOException {
        writer.write("\n");
        while (indent > INDENT.length) {
            char[] a = new char[INDENT.length * 2];
            Arrays.fill(a, '\t');
            INDENT = a;
        }
        writer.write(INDENT, 0, indent);
    }
}
