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

import static org.junit.Assert.assertEquals;

public class LogServiceLogbackXmlTest {

    @Test
    public void testInsertIndentedTabs() throws Exception {
        String xml = "<configuration>\n" +
                "</configuration>";

        String out = insertIndented(xml);
        assertEquals(
                "<configuration>\n" +
                        "\t<logger/>\n" +
                        "</configuration>", out);
    }

    @Test
    public void testInsertIndentedSpaces() throws Exception {
        //this one tests with one logger already added, because with no loggers there is no indentation to decide by and the function will choose tab
        String xml = "<configuration>\n" +
                "  <logger/>\n" +
                "</configuration>";

        String out = insertIndented(xml);
        assertEquals(
                "<configuration>\n" +
                        "  <logger/>\n" +
                        "  <logger/>\n" +
                        "</configuration>", out);
    }

    @Test
    public void testInsertIndentedTabsWithRoot() throws Exception {
        String xml = "<configuration>\n" +
                "\t<root/>\n" +
                "</configuration>";

        String out = insertIndented(xml);
        assertEquals(
                "<configuration>\n" +
                        "\t<root/>\n" +
                        "\t<logger/>\n" +
                        "</configuration>", out);
    }

    @Test
    public void testInsertIndentedSpacesWithRoot() throws Exception {
        String xml = "<configuration>\n" +
                "  <root/>\n" +
                "</configuration>";

        String out = insertIndented(xml);
        assertEquals(
                "<configuration>\n" +
                        "  <root/>\n" +
                        "  <logger/>\n" +
                        "</configuration>", out);
    }

    private String insertIndented(String xml) throws Exception {
        Document doc = LogServiceLog4j2XmlImpl.loadConfig(null, new ByteArrayInputStream(xml.getBytes()));
        Element element = doc.createElement("logger");
        LogServiceLogbackXmlImpl.insertIndented(
                (Element) doc.getDocumentElement(),
                element);
        try (StringWriter os = new StringWriter()) {
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(doc, "the.xml"), new StreamResult(os));
            return os.toString();
        }
    }
}
