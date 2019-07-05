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
package org.apache.karaf.audit.logger;

import org.apache.karaf.audit.Event;
import org.apache.karaf.audit.EventLayout;
import org.apache.karaf.audit.EventLogger;
import org.apache.karaf.audit.MapEvent;
import org.apache.karaf.audit.layout.GelfLayout;
import org.apache.karaf.audit.layout.Rfc3164Layout;
import org.apache.karaf.audit.layout.Rfc5424Layout;
import org.junit.Test;

import javax.management.ObjectName;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EventLoggerTest {

    private static final String INVOKE = "invoke";
    private static final String[] INVOKE_SIG = new String[] {ObjectName.class.getName(), String.class.getName(), Object[].class.getName(), String[].class.getName()};

    @Test
    public void testUdp() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("type", Event.TYPE_JMX);
        map.put("subtype", INVOKE);
        map.put("method", INVOKE);
        map.put("signature", INVOKE_SIG);
        map.put("params", new Object[] { new ObjectName("org.apache.karaf.Mbean:type=foo"), "myMethod", new Object[] { String.class.getName() }, new String[] { "the-param "}});
        Event event = new MapEvent(map, 1510902000000L);

        int port = getNewPort();

        DatagramSocket socket = new DatagramSocket(port);
        List<DatagramPacket> packets = new ArrayList<>();
        new Thread(() -> {
            try {
                DatagramPacket dp = new DatagramPacket(new byte[1024], 1024);
                socket.receive(dp);
                packets.add(dp);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }).start();

        Thread.sleep(100);

        EventLayout layout = new Rfc3164Layout(16, 5, Rfc5424Layout.DEFAULT_ENTERPRISE_NUMBER, TimeZone.getTimeZone("GMT+01:00"), Locale.ENGLISH);
        EventLogger logger = new UdpEventLogger("localhost", port, "UTF-8", layout);
        logger.write(event);

        Thread.sleep(100);

        assertEquals(1, packets.size());
        DatagramPacket p = packets.get(0);
        String str = new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8);
        assertTrue(str.startsWith("<133>Nov 17 08:00:00 "));
        assertTrue(str.endsWith(" jmx [jmx@18060 type=\"jmx\" subtype=\"invoke\" method=\"invoke\" signature=\"[javax.management.ObjectName, java.lang.String, [Ljava.lang.Object;, [Ljava.lang.String;\\]\" params=\"[org.apache.karaf.Mbean:type=foo, myMethod, [java.lang.String\\], [the-param \\]\\]\"]"));
        System.out.println(str);
    }

    @Test
    public void testTcp() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("type", Event.TYPE_JMX);
        map.put("subtype", INVOKE);
        map.put("method", INVOKE);
        map.put("signature", INVOKE_SIG);
        map.put("params", new Object[] { new ObjectName("org.apache.karaf.Mbean:type=foo"), "myMethod", new Object[] { String.class.getName() }, new String[] { "the-param "}});
        Event event = new MapEvent(map, 1510902000000L);

        int port = getNewPort();

        List<String> packets = new ArrayList<>();
        new Thread(() -> {
            try (ServerSocket ssocket = new ServerSocket(port)) {
                ssocket.setReuseAddress(true);
                try (Socket socket = ssocket.accept()) {
                    byte[] buffer = new byte[1024];
                    int nb = socket.getInputStream().read(buffer);
                    packets.add(new String(buffer, 0, nb, StandardCharsets.UTF_8));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }).start();

        Thread.sleep(100);

        EventLayout layout = new Rfc5424Layout(16, 5, Rfc5424Layout.DEFAULT_ENTERPRISE_NUMBER, TimeZone.getTimeZone("GMT+01:00"));
        EventLogger logger = new TcpEventLogger("localhost", port, "UTF-8", layout);
        logger.write(event);
        logger.flush();

        Thread.sleep(100);

        assertEquals(1, packets.size());
        String str = packets.get(0);
        System.out.println(str);
        assertTrue(str.startsWith("<133>1 2017-11-17T08:00:00.000+01:00 "));
        assertTrue(str.indexOf(" jmx [jmx@18060 type=\"jmx\" subtype=\"invoke\" method=\"invoke\" signature=\"[javax.management.ObjectName, java.lang.String, [Ljava.lang.Object;, [Ljava.lang.String;\\]\" params=\"[org.apache.karaf.Mbean:type=foo, myMethod, [java.lang.String\\], [the-param \\]\\]\"]") > 0);
    }

    @Test
    public void testFile() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("type", Event.TYPE_JMX);
        map.put("subtype", INVOKE);
        map.put("method", INVOKE);
        map.put("signature", INVOKE_SIG);
        map.put("params", new Object[] { new ObjectName("org.apache.karaf.Mbean:type=foo"), "myMethod", new Object[] { String.class.getName() }, new String[] { "the-param "}});

        EventLayout layout = new GelfLayout();
        Path path = Files.createTempDirectory("file-logger");
        String file = path.resolve("file.log").toString();
        EventLogger logger = new FileEventLogger(file, "UTF-8", "daily", 2, false, Executors.defaultThreadFactory(), layout, TimeZone.getTimeZone("GMT+01:00"));

        logger.write(new MapEvent(map, 1510902000000L));
        logger.write(new MapEvent(map, 1510984800000L));
        logger.close();

        Thread.sleep(100);

        List<Path> paths = Files.list(path).sorted().collect(Collectors.toList());
        Collections.sort(paths);
        assertEquals(2, paths.size());
        assertEquals("file-2017-11-18.log", paths.get(0).getFileName().toString());
        assertEquals("file.log", paths.get(1).getFileName().toString());

        List<String> lines = Files.readAllLines(paths.get(0), StandardCharsets.UTF_8);
        assertEquals(1, lines.size());
        String str = lines.get(0);
        System.out.println(str);
        assertTrue(str.startsWith("{ version=\"1.1\" host=\""));
        assertTrue(str.endsWith("timestamp=1510902000.000 short_message=\"jmx.invoke\" _type=\"jmx\" _subtype=\"invoke\" _method=\"invoke\" _signature=\"[javax.management.ObjectName, java.lang.String, [Ljava.lang.Object;, [Ljava.lang.String;]\" _params=\"[org.apache.karaf.Mbean:type=foo, myMethod, [java.lang.String], [the-param ]]\" }"));

        lines = Files.readAllLines(paths.get(1), StandardCharsets.UTF_8);
        assertEquals(1, lines.size());
        str = lines.get(0);
        System.out.println(str);
        assertTrue(str.startsWith("{ version=\"1.1\" host=\""));
        assertTrue(str.endsWith("timestamp=1510984800.000 short_message=\"jmx.invoke\" _type=\"jmx\" _subtype=\"invoke\" _method=\"invoke\" _signature=\"[javax.management.ObjectName, java.lang.String, [Ljava.lang.Object;, [Ljava.lang.String;]\" _params=\"[org.apache.karaf.Mbean:type=foo, myMethod, [java.lang.String], [the-param ]]\" }"));
    }

    @Test
    public void testFileMaxFiles() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("type", Event.TYPE_SHELL);
        map.put("subtype", "executed");
        map.put("script", "a-script");

        EventLayout layout = new GelfLayout();
        Path path = Files.createTempDirectory("file-logger");
        String file = path.resolve("file.log").toString();
        EventLogger logger = new FileEventLogger(file, "UTF-8", "daily", 2, false, Executors.defaultThreadFactory(), layout, TimeZone.getTimeZone("GMT+01:00"));

        for (int i = 0; i < 10; i++) {
            logger.write(new MapEvent(map, 1510902000000L + TimeUnit.DAYS.toMillis(i)));
        }
        logger.close();

        Thread.sleep(100);

        List<String> paths = Files.list(path)
                .map(Path::getFileName).map(Path::toString)
                .sorted().collect(Collectors.toList());
        assertEquals(Arrays.asList("file-2017-11-25.log", "file-2017-11-26.log", "file.log"), paths);
    }

    @Test
    public void testFileSize() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("type", Event.TYPE_SHELL);
        map.put("subtype", "executed");
        map.put("script", "a-script");

        EventLayout layout = new GelfLayout();
        Path path = Files.createTempDirectory("file-logger");
        String file = path.resolve("file.log").toString();
        EventLogger logger = new FileEventLogger(file, "UTF-8", "size(10)", 2, false, Executors.defaultThreadFactory(), layout, TimeZone.getTimeZone("GMT+01:00"));
        for (int i = 0; i < 10; i++) {
            logger.write(new MapEvent(map, LocalDateTime.of(2017, 11, 17, 7, 0).toInstant(ZoneOffset.of("+01:00")).toEpochMilli()
                                           + TimeUnit.HOURS.toMillis(i)));
        }
        logger.close();

        Thread.sleep(100);

        List<String> paths = Files.list(path)
                .map(Path::getFileName).map(Path::toString)
                .sorted().collect(Collectors.toList());
        assertEquals(Arrays.asList("file-2017-11-17-2.log", "file-2017-11-17.log", "file.log"), paths);
    }

    @Test
    public void testFileSizeCompress() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("type", Event.TYPE_SHELL);
        map.put("subtype", "executed");
        map.put("script", "a-script");

        EventLayout layout = new GelfLayout();
        Path path = Files.createTempDirectory("file-logger");
        String file = path.resolve("file.log").toString();
        EventLogger logger = new FileEventLogger(file, "UTF-8", "size(10)", 2, true, Executors.defaultThreadFactory(), layout, TimeZone.getTimeZone("GMT+01:00"));

        for (int i = 0; i < 10; i++) {
            logger.write(new MapEvent(map, LocalDateTime.of(2017, 11, 17, 7, 0).toInstant(ZoneOffset.of("+01:00")).toEpochMilli()
                                           + TimeUnit.HOURS.toMillis(i)));
        }
        logger.close();

        Thread.sleep(100);

        List<String> paths = Files.list(path)
                .map(Path::getFileName).map(Path::toString)
                .sorted().collect(Collectors.toList());
        assertEquals(Arrays.asList("file-2017-11-17-2.log.gz", "file-2017-11-17.log.gz", "file.log"), paths);
    }

    private int getNewPort() throws IOException {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress("localhost", 0));
            return socket.getLocalPort();
        }
    }

}
