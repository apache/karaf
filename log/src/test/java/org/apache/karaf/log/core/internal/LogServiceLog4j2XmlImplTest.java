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

public class LogServiceLog4j2XmlImplTest {

    @Test
    public void testInsertIndentedTabs() throws Exception {
        String xml = "<Configuration>" + System.lineSeparator() +
                "\t<Loggers>" + System.lineSeparator() +
                "\t</Loggers>" + System.lineSeparator() +
                "</Configuration>";

        String out = insertIndented(xml, false);
        assertEquals(
                "<Configuration>" + System.lineSeparator() +
                        "\t<Loggers>" + System.lineSeparator() +
                        "\t\t<Logger/>" + System.lineSeparator() +
                        "\t</Loggers>" + System.lineSeparator() +
                        "</Configuration>", out);
        out = insertIndented(xml, true);
        assertEquals(
                "<Configuration>" + System.lineSeparator() +
                        "\t<Loggers>" + System.lineSeparator() +
                        "\t\t<Logger/>" + System.lineSeparator() +
                        "\t</Loggers>" + System.lineSeparator() +
                        "</Configuration>", out);
    }

    @Test
    public void testInsertIndentedSpaces() throws Exception {
        String xml = "<Configuration>" + System.lineSeparator() +
                "  <Loggers>" + System.lineSeparator() +
                "  </Loggers>" + System.lineSeparator() +
                "</Configuration>";

        String out = insertIndented(xml, false);
        assertEquals(
                "<Configuration>" + System.lineSeparator() +
                        "  <Loggers>" + System.lineSeparator() +
                        "    <Logger/>" + System.lineSeparator() +
                        "  </Loggers>" + System.lineSeparator() +
                        "</Configuration>", out);
        out = insertIndented(xml, true);
        assertEquals(
                "<Configuration>" + System.lineSeparator() +
                        "  <Loggers>" + System.lineSeparator() +
                        "    <Logger/>" + System.lineSeparator() +
                        "  </Loggers>" + System.lineSeparator() +
                        "</Configuration>", out);
    }

    @Test
    public void testInsertIndentedTabsWithRoot() throws Exception {
        String xml = "<Configuration>" + System.lineSeparator() +
                "\t<Loggers>" + System.lineSeparator() +
                "\t\t<Root/>" + System.lineSeparator() +
                "\t</Loggers>" + System.lineSeparator() +
                "</Configuration>";

        String out = insertIndented(xml, false);
        assertEquals(
                "<Configuration>" + System.lineSeparator() +
                        "\t<Loggers>" + System.lineSeparator() +
                        "\t\t<Root/>" + System.lineSeparator() +
                        "\t\t<Logger/>" + System.lineSeparator() +
                        "\t</Loggers>" + System.lineSeparator() +
                        "</Configuration>", out);
        out = insertIndented(xml, true);
        assertEquals(
                "<Configuration>" + System.lineSeparator() +
                        "\t<Loggers>" + System.lineSeparator() +
                        "\t\t<Logger/>" + System.lineSeparator() +
                        "\t\t<Root/>" + System.lineSeparator() +
                        "\t</Loggers>" + System.lineSeparator() +
                        "</Configuration>", out);
    }

    @Test
    public void testInsertIndentedSpacesWithRoot() throws Exception {
        String xml = "<Configuration>" + System.lineSeparator() +
                "  <Loggers>" + System.lineSeparator() +
                "    <Root/>" + System.lineSeparator() +
                "  </Loggers>" + System.lineSeparator() +
                "</Configuration>";
        
        String out = insertIndented(xml, false);
        assertEquals(
                "<Configuration>" + System.lineSeparator() +
                        "  <Loggers>" + System.lineSeparator() +
                        "    <Root/>" + System.lineSeparator() +
                        "    <Logger/>" + System.lineSeparator() +
                        "  </Loggers>" + System.lineSeparator() +
                        "</Configuration>", out);
        out = insertIndented(xml, true);
        assertEquals(
                "<Configuration>" + System.lineSeparator() +
                        "  <Loggers>" + System.lineSeparator() +
                        "    <Logger/>" + System.lineSeparator() +
                        "    <Root/>" + System.lineSeparator() +
                        "  </Loggers>" + System.lineSeparator() +
                        "</Configuration>", out);
    }

    private String insertIndented(String xml, boolean atBeginning) throws Exception {
        Document doc = LogServiceLog4j2XmlImpl.loadConfig(null, new ByteArrayInputStream(xml.getBytes()));
        Element element = doc.createElement("Logger");
        LogServiceLog4j2XmlImpl.insertIndented(
                (Element) doc.getDocumentElement().getElementsByTagName("Loggers").item(0),
                element,
                atBeginning);
        try (StringWriter os = new StringWriter()) {
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(doc, "the.xml"), new StreamResult(os));
            return os.toString();
        }
    }
}
