/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.itests;

import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.ServiceRegistration;

/**
 * The Karaf web container, based on Apache Tomcat.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class KarafWebTest extends BaseTest {

    private static final String PID = "org.apache.karaf.services.web";

    @Configuration
    public Option[] config() {
        String httpPort = Integer.toString(getAvailablePort(Integer.parseInt(MIN_HTTP_PORT), Integer.parseInt(MAX_HTTP_PORT)));
        return combine(super.config(), editConfigurationFilePut("etc/" + PID + ".cfg", "http.port", httpPort));
    }

    @Before
    public void installKarafWeb() throws Exception {
        installAndAssertFeature("karaf-web");
    }

    @Test
    public void contextList() throws Exception {
        assertContains("default", executeCommand("web:context-list"));
    }

    @Test
    public void whiteboardServlet() throws Exception {
        String url = "http://localhost:" + configurationAdmin.getConfiguration(PID, null).getProperties().get("http.port")
                + "/karaf-web-test";
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put("osgi.http.whiteboard.servlet.pattern", "/karaf-web-test");
        ServiceRegistration<?> registration = bundleContext.registerService("jakarta.servlet.Servlet",
                servlet("Hello Karaf Web"), properties);
        try {
            // the whiteboard services are deployed asynchronously
            long timeout = System.currentTimeMillis() + 10000;
            while (status(url) != 200 && System.currentTimeMillis() < timeout) {
                Thread.sleep(200);
            }
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            assertEquals(200, connection.getResponseCode());
            try (InputStream is = connection.getInputStream()) {
                assertEquals("Hello Karaf Web", new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
            assertContains("/karaf-web-test", executeCommand("web:servlet-list"));
        } finally {
            registration.unregister();
        }
        // a servlet is undeployed before its service unregistration returns
        assertEquals(404, status(url));
    }

    private static int status(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            return connection.getResponseCode();
        } finally {
            connection.disconnect();
        }
    }

    /**
     * A servlet writing the given content. The classes of the probe in the org.apache.karaf.itests package are not
     * visible once this package is wired to the common bundle, so the servlet is a proxy rather than a nested class.
     */
    private static Object servlet(String content) {
        return Proxy.newProxyInstance(Servlet.class.getClassLoader(), new Class<?>[]{Servlet.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "service":
                            ((ServletResponse) args[1]).getWriter().print(content);
                            return null;
                        case "getServletInfo":
                        case "toString":
                            return content;
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == args[0];
                        default:
                            return null;
                    }
                });
    }

}
