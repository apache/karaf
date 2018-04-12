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
import java.util.Locale;
import java.util.TimeZone;

public class Rfc3164Layout extends AbstractLayout {

    public static final int DEFAULT_ENTERPRISE_NUMBER = 18060;

    protected final int facility;
    protected final int priority;
    protected final int enterpriseNumber;
    protected final FastDateFormat fastDateFormat;

    protected String hdr1;
    protected String hdr2;
    protected String hdr3;

    public Rfc3164Layout(int facility, int priority, int enterpriseNumber, TimeZone timeZone, Locale locale) {
        super(new Buffer(Buffer.Format.Syslog));
        this.facility = facility;
        this.priority = priority;
        this.enterpriseNumber = enterpriseNumber;
        this.fastDateFormat = new FastDateFormat(timeZone, locale);

        hdr1 = "<" + ((facility << 3) + priority) + ">";
        hdr2 = " " + hostName + " " + appName + " " + procId + " ";
        hdr3 = enterpriseNumber > 0 ? "@" + enterpriseNumber : "";
    }

    @Override
    protected void header(Event event) throws IOException {
        buffer.append(hdr1);
        datetime(event.timestamp());
        buffer.append(hdr2);
        buffer.append(event.type());
        buffer.append(' ');
        buffer.append('[');
        buffer.append(event.type());
        buffer.append(hdr3);
    }

    @Override
    protected void footer(Event event) throws IOException {
        buffer.append(']');
    }

    @Override
    protected void append(String key, Object val) throws IOException {
        if (val != null) {
            buffer.append(' ')
                    .append(key)
                    .append('=')
                    .append('"')
                    .format(val)
                    .append('"');
        }
    }

    protected void datetime(long millis) throws IOException {
        buffer.append(fastDateFormat.getDate(millis, FastDateFormat.MMM_D2));
        buffer.append(' ');
        fastDateFormat.writeTime(millis, false, buffer);
    }

}
