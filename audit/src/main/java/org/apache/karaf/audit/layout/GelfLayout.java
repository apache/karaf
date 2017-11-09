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
package org.apache.karaf.audit.layout;

import org.apache.karaf.audit.Event;
import org.apache.karaf.audit.util.Buffer;

import java.io.IOException;

public class GelfLayout extends AbstractLayout {

    public GelfLayout() {
        super(new Buffer(Buffer.Format.Json));
    }

    @Override
    protected void header(Event event) throws IOException {
        buffer.append('{');
        append("version", "1.1", false);
        append("host", hostName, false);
        datetime(event.timestamp());
        append("short_message", event.type() + "." + event.subtype(), false);
    }

    private void datetime(long timestamp) throws IOException {
        buffer.append(" timestamp=");
        long secs = timestamp / 1000;
        int ms = (int)(timestamp - secs * 1000);
        buffer.format(secs);
        buffer.append('.');
        int temp = ms / 100;
        buffer.append((char) (temp + '0'));
        ms -= 100 * temp;
        temp = ms / 10;
        buffer.append((char) (temp + '0'));
        ms -= 10 * temp;
        buffer.append((char) (ms + '0'));
    }

    @Override
    protected void footer(Event event) throws IOException {
        buffer.append(' ');
        buffer.append('}');
    }

    @Override
    protected void append(String key, Object val) throws IOException {
        append(key, val, true);
    }

    protected void append(String key, Object val, boolean custom) throws IOException {
        if (val != null) {
            buffer.append(' ');
            if (custom) {
                buffer.append('_');
            }
            buffer.append(key);
            buffer.append('=');
            if (val instanceof Number) {
                if (val instanceof Long) {
                    buffer.format(((Long) val).longValue());
                } else if (val instanceof Integer) {
                    buffer.format(((Integer) val).intValue());
                } else {
                    buffer.append(val.toString());
                }
            } else {
                buffer.append('"');
                buffer.format(val);
                buffer.append('"');
            }
        }
    }

}
