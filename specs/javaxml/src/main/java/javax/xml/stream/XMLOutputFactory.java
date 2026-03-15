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
package javax.xml.stream;

import javax.xml.transform.Result;
import java.io.OutputStream;
import java.io.Writer;

public abstract class XMLOutputFactory {

    public static final String IS_REPAIRING_NAMESPACES = "javax.xml.stream.isRepairingNamespaces";

    private static final String DEFAULT_IMPL = "com.sun.xml.internal.stream.XMLOutputFactoryImpl";

    protected XMLOutputFactory() {
    }

    public static XMLOutputFactory newDefaultFactory() {
        return $FactoryFinder.newInstance(XMLOutputFactory.class, DEFAULT_IMPL, null, false, true);
    }

    public static XMLOutputFactory newInstance() throws FactoryConfigurationError {
        return $FactoryFinder.find(XMLOutputFactory.class, DEFAULT_IMPL);
    }

    public static XMLOutputFactory newFactory() throws FactoryConfigurationError {
        return $FactoryFinder.find(XMLOutputFactory.class, DEFAULT_IMPL);
    }

    @Deprecated
    public static XMLInputFactory newInstance(String factoryId, ClassLoader classLoader) throws FactoryConfigurationError {
        return $FactoryFinder.find(XMLInputFactory.class, factoryId, classLoader, null);
    }

    public static XMLOutputFactory newFactory(String factoryId, ClassLoader classLoader) throws FactoryConfigurationError {
        return $FactoryFinder.find(XMLOutputFactory.class, factoryId, classLoader, null);
    }

    public abstract XMLStreamWriter createXMLStreamWriter(Writer stream) throws XMLStreamException;

    public abstract XMLStreamWriter createXMLStreamWriter(OutputStream stream) throws XMLStreamException;

    public abstract XMLStreamWriter createXMLStreamWriter(OutputStream stream, String encoding) throws XMLStreamException;

    public abstract XMLStreamWriter createXMLStreamWriter(Result result) throws XMLStreamException;

    public abstract XMLEventWriter createXMLEventWriter(Result result) throws XMLStreamException;

    public abstract XMLEventWriter createXMLEventWriter(OutputStream stream) throws XMLStreamException;

    public abstract XMLEventWriter createXMLEventWriter(OutputStream stream, String encoding) throws XMLStreamException;

    public abstract XMLEventWriter createXMLEventWriter(Writer stream) throws XMLStreamException;

    public abstract void setProperty(String name, Object value) throws IllegalArgumentException;

    public abstract Object getProperty(String name) throws IllegalArgumentException;

    public abstract boolean isPropertySupported(String name);
}
