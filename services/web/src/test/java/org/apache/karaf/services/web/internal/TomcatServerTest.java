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
package org.apache.karaf.services.web.internal;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.util.Hashtable;

import org.junit.Test;

public class TomcatServerTest {

    @Test
    public void stopConnectorsReleasesThePorts() throws Exception {
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put("host", "127.0.0.1");
        properties.put("http.port", "0");
        TomcatServer server = new TomcatServer(WebConfig.from(properties),
                Files.createTempDirectory("tomcat").toFile());
        server.start();
        try {
            int port = new URL(server.getEndpoints().get(0)).getPort();

            server.stopConnectors();

            // the next web container can bind the port while the contexts are still being undeployed
            try (ServerSocket socket = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"))) {
                socket.setReuseAddress(true);
            }
        } finally {
            server.stop();
        }
    }

}
