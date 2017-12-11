/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.util.xml;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

public class IndentingXMLEventWriter implements XMLEventWriter {

    private static final XMLEventFactory factory = XMLEventFactory.newFactory();

    private XMLEventWriter wrappedWriter;
    private int depth = 0;

    private boolean newLineBeforeStartElement = false;
    private boolean indentBeforeEndElement = false;

    private String indentationString = "    ";

    /**
     * @param wrappedWriter
     * @param indentation
     */
    public IndentingXMLEventWriter(XMLEventWriter wrappedWriter, String indentation) {
        this.wrappedWriter = wrappedWriter;
        this.indentationString = indentation;
    }

    @Override
    public void close() throws XMLStreamException {
        this.wrappedWriter.close();
    }

    @Override
    public void add(XMLEvent event) throws XMLStreamException {
        switch (event.getEventType()) {
            case XMLStreamConstants.START_DOCUMENT:
                this.wrappedWriter.add(event);
                this.wrappedWriter.add(factory.createCharacters("\n"));
                break;
            case XMLStreamConstants.START_ELEMENT:
                if (this.newLineBeforeStartElement)
                    this.wrappedWriter.add(factory.createCharacters("\n"));
                this.newLineBeforeStartElement = true;
                this.indentBeforeEndElement = false;
                this.possiblyIndent();
                this.wrappedWriter.add(event);
                this.depth++;
                break;
            case XMLStreamConstants.END_ELEMENT:
                this.newLineBeforeStartElement = false;
                this.depth--;
                if (this.indentBeforeEndElement)
                    this.possiblyIndent();
                this.indentBeforeEndElement = true;
                this.wrappedWriter.add(event);
                this.wrappedWriter.add(factory.createCharacters("\n"));
                break;
            case XMLStreamConstants.COMMENT:
            case XMLStreamConstants.PROCESSING_INSTRUCTION:
                this.wrappedWriter.add(event);
                this.wrappedWriter.add(factory.createCharacters("\n"));
                this.newLineBeforeStartElement = false;
                this.indentBeforeEndElement = true;
                break;
            default:
                this.wrappedWriter.add(event);
        }
    }

    /**
     * Indent at non-zero depth
     * @throws XMLStreamException
     */
    private void possiblyIndent() throws XMLStreamException {
        if (this.depth > 0) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < this.depth; i++)
                sb.append(this.indentationString);
            this.wrappedWriter.add(factory.createCharacters(sb.toString()));
        }
    }

    @Override
    public void add(XMLEventReader reader) throws XMLStreamException {
        this.wrappedWriter.add(reader);
    }

    @Override
    public String getPrefix(String uri) throws XMLStreamException {
        return this.wrappedWriter.getPrefix(uri);
    }

    @Override
    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        this.wrappedWriter.setPrefix(prefix, uri);
    }

    @Override
    public void setDefaultNamespace(String uri) throws XMLStreamException {
        this.wrappedWriter.setDefaultNamespace(uri);
    }

    @Override
    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
        this.wrappedWriter.setNamespaceContext(context);
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return this.wrappedWriter.getNamespaceContext();
    }

    @Override
    public void flush() throws XMLStreamException {
        this.wrappedWriter.flush();
    }

    public void setIndentationString(String indentationString) {
        this.indentationString = indentationString;
    }

}
