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
package org.apache.karaf.audit;

import org.apache.karaf.audit.layout.Rfc3164Layout;
import org.apache.karaf.audit.layout.Rfc5424Layout;
import org.apache.karaf.audit.util.Buffer;
import org.junit.Ignore;
import org.junit.Test;

import javax.management.ObjectName;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Callable;

@Ignore
public class TestPerf {

    private static final String INVOKE = "invoke";
    private static final String[] INVOKE_SIG = new String[] {ObjectName.class.getName(), String.class.getName(), Object[].class.getName(), String[].class.getName()};


    @Test
    public void testFormatString() throws Exception {
        int iterations = 10000000;

        for (int i = 0; i < 10; i++) {
            final Buffer buffer0 = new Buffer(Buffer.Format.Json);
            long t0 = measure(() -> {
                buffer0.clear();
                buffer0.format("This is \"\n\tquite\n\tq\n\ta\n\tlong\n\tquote.\"\nIndeed !\n");
                return null;
            }, iterations);
            System.out.println("json = " + t0);

            final Buffer buffer1 = new Buffer(Buffer.Format.Syslog);
            long t1 = measure(() -> {
                buffer1.clear();
                buffer1.format("This is \"\n\tquite\n\tq\n\ta\n\tlong\n\tquote.\"\nIndeed !\n");
                return null;
            }, iterations);
            System.out.println("syslog = " + t1);
        }
    }

    @Test
    public void testGelfTimestamp() throws Exception {
        long timestamp = System.currentTimeMillis();

        int iterations = 1000000;

        for (int p = 0; p < 10; p++) {
            Buffer buffer = new Buffer(Buffer.Format.Json);
            long t0 = measure(() -> {
                buffer.clear();
                long secs = timestamp / 1000;
                int ms = (int) (timestamp - secs * 1000);
                buffer.format(secs);
                buffer.append('.');
                int temp = ms / 100;
                buffer.append((char) (temp + '0'));
                ms -= 100 * temp;
                temp = ms / 10;
                buffer.append((char) (temp + '0'));
                ms -= 10 * temp;
                buffer.append((char) (ms + '0'));
                return null;
            }, iterations);
            System.out.println("t0 = " + t0);
            long t1 = measure(() -> {
                buffer.clear();
                long secs = timestamp / 1000;
                int ms = (int) (timestamp - secs * 1000);
                buffer.format(Long.toString(secs));
                buffer.append('.');
                int temp = ms / 100;
                buffer.append((char) (temp + '0'));
                ms -= 100 * temp;
                temp = ms / 10;
                buffer.append((char) (temp + '0'));
                ms -= 10 * temp;
                buffer.append((char) (ms + '0'));
                return null;
            }, iterations);
            System.out.println("t1 = " + t1);
            long t2 = measure(() -> {
                buffer.clear();
                new Formatter(buffer).format("%.3f", ((double) timestamp) / 1000.0);
                return null;
            }, iterations);
            System.out.println("t2 = " + t2);
        }
    }

    @Test
    public void testSerialize() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("type", Event.TYPE_JMX);
        map.put("subtype", INVOKE);
        map.put("method", INVOKE);
        map.put("signature", INVOKE_SIG);
        map.put("params", new Object[] { new ObjectName("org.apache.karaf.Mbean:type=foo"), "myMethod", new Object[] { String.class.getName() }, new String[] { "the-param "}});
        Event event = new MapEvent(map);

        EventLayout layout = new Rfc3164Layout(16, 5, Rfc5424Layout.DEFAULT_ENTERPRISE_NUMBER, TimeZone.getTimeZone("CET"), Locale.ENGLISH);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer writer = new BufferedWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));

        long dt0 = measure(() -> { baos.reset(); layout.format(event, writer); writer.flush(); return null; }, 10000000);
        System.out.println(dt0);

        long dt1 = measure(() -> { baos.reset(); layout.format(event, writer); writer.flush(); return null; }, 10000000);
        System.out.println(dt1);
    }

    private <T> long measure(Callable<T> runnable, int runs) throws Exception {
        System.gc();
        for (int i = 0; i < runs / 100; i++) {
            runnable.call();
        }
        System.gc();
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < runs; i++) {
            runnable.call();
        }
        long t1 = System.currentTimeMillis();
        return t1 - t0;
    }

}
