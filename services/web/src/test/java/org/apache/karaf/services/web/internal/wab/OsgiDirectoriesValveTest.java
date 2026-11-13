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
package org.apache.karaf.services.web.internal.wab;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Hashtable;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardWrapper;
import org.apache.karaf.services.web.internal.Contexts;
import org.apache.karaf.services.web.internal.TomcatServer;
import org.apache.karaf.services.web.internal.WebConfig;
import org.apache.tomcat.SimpleInstanceManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Runs the embedded Tomcat with a context answering all the requests, but the ones for the OSGi directories.
 */
public class OsgiDirectoriesValveTest {

    private TomcatServer server;
    private String base;

    @Before
    public void setUp() throws Exception {
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put("host", "127.0.0.1");
        properties.put("http.port", "0");
        WebConfig config = WebConfig.from(properties);
        server = new TomcatServer(config, Files.createTempDirectory("tomcat").toFile());
        server.start();
        base = server.getEndpoints().get(0);

        StandardContext context = Contexts.create("/wab", getClass().getClassLoader(), null, config);
        Contexts.markConfigured(context);
        context.setInstanceManager(new SimpleInstanceManager());
        context.getPipeline().addValve(new OsgiDirectoriesValve());
        server.getHost().addChild(context);
        StandardWrapper wrapper = (StandardWrapper) context.createWrapper();
        wrapper.setName("all");
        wrapper.setServlet(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest request, HttpServletResponse response) {
                response.setStatus(HttpServletResponse.SC_OK);
            }
        });
        context.addChild(wrapper);
        context.addServletMappingDecoded("/", "all");
        wrapper.load();
    }

    @After
    public void tearDown() {
        server.stop();
    }

    @Test
    public void osgiDirectories() throws Exception {
        assertEquals(404, status("/wab/OSGI-INF/blueprint/config.xml"));
        assertEquals(404, status("/wab/osgi-inf/component.xml"));
        assertEquals(404, status("/wab/OSGI-INF"));
        assertEquals(404, status("/wab/OSGI-OPT/src/Foo.java"));
    }

    @Test
    public void otherPaths() throws Exception {
        assertEquals(200, status("/wab/index.html"));
        assertEquals(200, status("/wab/OSGI-INFO/file.txt"));
        assertEquals(200, status("/wab/static/OSGI-INF/file.txt"));
    }

    private int status(String path) throws IOException {
        return ((HttpURLConnection) new URL(base + path).openConnection()).getResponseCode();
    }

}
