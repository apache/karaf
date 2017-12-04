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
import org.apache.karaf.audit.util.FastDateFormat;

import java.io.IOException;

public class SimpleLayout extends AbstractLayout {

    protected String hdr;

    protected FastDateFormat fastDateFormat = new FastDateFormat();

    public SimpleLayout() {
        super(new Buffer(Buffer.Format.Json, 4096));
        hdr = " " + hostName + " " + appName + " " + procId;
    }

    @Override
    protected void header(Event event) throws IOException {
        datetime(event.timestamp());
        buffer.append(hdr);
    }

    @Override
    protected void footer(Event event) throws IOException {
    }

    @Override
    protected void append(String key, Object val) throws IOException {
        if (val != null) {
            switch (key) {
                case "subject":
                case "type":
                case "subtype":
                    buffer.append(' ').format(val);
                    break;
                default:
                    buffer.append(' ').append(key).append('=').append('"').format(val).append('"');
                    break;
            }
        }
    }

    protected void datetime(long millis) throws IOException {
        buffer.append(fastDateFormat.getDate(millis, FastDateFormat.YYYY_MM_DD));
        buffer.append('T');
        fastDateFormat.writeTime(millis, true, buffer);
        buffer.append(fastDateFormat.getDate(millis, FastDateFormat.XXX));
    }

}
