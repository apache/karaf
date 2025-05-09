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
package org.apache.karaf.log.core.internal;

import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LogServiceLogbackXmlTest {

    public static final String LOG_LEVEL_TOKEN = "log.level";

    private static String file;
    @BeforeClass
    public static void initClass() {
        Path p;
        try {
            p = Paths.get(LogServiceLogbackXmlImpl.class.getResource("/logback.xml").toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        file = p.toString();
    }

    @Test
    public void testInsertIndentedTabs() throws Exception {
        String xml = "<configuration>" + System.lineSeparator() +
                "</configuration>";

        String out = insertIndented(xml);
        assertEquals(
                "<configuration>" + System.lineSeparator() +
                        "\t<logger/>" + System.lineSeparator() +
                        "</configuration>", out);
    }

    @Test
    public void testInsertIndentedSpaces() throws Exception {
        //this one tests with one logger already added, because with no loggers there is no indentation to decide by and the function will choose tab
        String xml = "<configuration>" + System.lineSeparator() +
                "  <logger/>" + System.lineSeparator() +
                "</configuration>";

        String out = insertIndented(xml);
        assertEquals(
                "<configuration>" + System.lineSeparator() +
                        "  <logger/>" + System.lineSeparator() +
                        "  <logger/>" + System.lineSeparator() +
                        "</configuration>", out);
    }

    @Test
    public void testInsertIndentedTabsWithRoot() throws Exception {
        String xml = "<configuration>" + System.lineSeparator() +
                "\t<root/>" + System.lineSeparator() +
                "</configuration>";

        String out = insertIndented(xml);
        assertEquals(
                "<configuration>" + System.lineSeparator() +
                        "\t<root/>" + System.lineSeparator() +
                        "\t<logger/>" + System.lineSeparator() +
                        "</configuration>", out);
    }

    @Test
    public void testInsertIndentedSpacesWithRoot() throws Exception {
        String xml = "<configuration>" + System.lineSeparator() +
                "  <root/>" + System.lineSeparator() +
                "</configuration>";

        String out = insertIndented(xml);
        assertEquals(
                "<configuration>" + System.lineSeparator() +
                        "  <root/>" + System.lineSeparator() +
                        "  <logger/>" + System.lineSeparator() +
                        "</configuration>", out);
    }

    private String insertIndented(String xml) throws Exception {
        Document doc = LogServiceLog4j2XmlImpl.loadConfig(null, new ByteArrayInputStream(xml.getBytes()));
        Element element = doc.createElement("logger");
        LogServiceLogbackXmlImpl.insertIndented(
                doc.getDocumentElement(),
                element);
        try (StringWriter os = new StringWriter()) {
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(doc, "the.xml"), new StreamResult(os));
            return os.toString();
        }
    }

    @Test
    public void testBasicValue() {
        Properties systemProps = new Properties();
        String resolved = LogServiceLogbackXmlImpl.resolveValue("DEBUG", Collections.emptyMap(), systemProps, Collections.emptyMap());
        assertEquals("DEBUG", resolved);
    }

    @Test
    public void testPropertySubstitution() {
        Map<String, String> properties = new HashMap<>();
        properties.put(LOG_LEVEL_TOKEN, "WARN");
        String resolved = LogServiceLogbackXmlImpl.resolveValue("${log.level:-DEBUG}", properties, new Properties(), Collections.emptyMap());
        assertEquals("WARN", resolved);
    }

    @Test
    public void testSystemPropertiesSubstitution() {
        Properties systemProps = new Properties();
        systemProps.put(LOG_LEVEL_TOKEN, "WARN");
        String resolved = LogServiceLogbackXmlImpl.resolveValue("${log.level:-DEBUG}", Collections.emptyMap(), systemProps, Collections.emptyMap());
        assertEquals("WARN", resolved);
    }

    @Test
    public void testEnvVariableSubstitution() {
        Map<String, String> env = new HashMap<>();
        env.put(LOG_LEVEL_TOKEN, "WARN");
        String resolved = LogServiceLogbackXmlImpl.resolveValue("${log.level:-DEBUG}", Collections.emptyMap(), new Properties(), env);
        assertEquals("WARN", resolved);
    }

    @Test
    public void testPropertyWinsEnvSubstitution() {
        Map<String, String> props = new HashMap<>();
        props.put(LOG_LEVEL_TOKEN, "DEBUG");
        Map<String, String> env = new HashMap<>();
        env.put(LOG_LEVEL_TOKEN, "WARN");
        String resolved = LogServiceLogbackXmlImpl.resolveValue("${log.level}", props , new Properties(), env);
        assertEquals("DEBUG", resolved);
    }

    @Test
    public void testRootLogLevel() {
        LogServiceLogbackXmlImpl logService = getLogService();
        assertEquals("WARN", logService.getLevel(LogServiceInternal.ROOT_LOGGER).get(LogServiceInternal.ROOT_LOGGER));
    }

    @Test
    public void testPropertyLogLevel() {
        String logger = "debugPropertyLogger";
        LogServiceLogbackXmlImpl logService = getLogService();
        assertEquals("DEBUG", logService.getLevel(logger).get(logger));
    }

    @Test
    public void testSystemPropertyLogLevel() {
        String logger = "systemPropertyLogger";
        System.setProperty("LOG_LEVEL", "DEBUG");
        LogServiceLogbackXmlImpl logService = getLogService();
        assertEquals("DEBUG", logService.getLevel(logger).get(logger));
        System.clearProperty("LOG_LEVEL");
    }

    @Test
    public void testDefaultValueLogLevel() {
        String logger = "defaultValueLogger";
        LogServiceLogbackXmlImpl logService = getLogService();
        assertEquals("DEBUG", logService.getLevel(logger).get(logger));
    }

    @Test
    public void testAllLoggerLogLevel() {
        LogServiceLogbackXmlImpl logService = getLogService();
        Map<String, String> levels = logService.getLevel(LogServiceInternal.ALL_LOGGER);
        assertEquals(4, levels.size());
        assertTrue(levels.containsKey(LogServiceInternal.ROOT_LOGGER));
    }

    private LogServiceLogbackXmlImpl getLogService() {
        return new LogServiceLogbackXmlImpl(file);
    }

}
