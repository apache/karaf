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

import javax.xml.stream.util.XMLEventAllocator;
import javax.xml.transform.Source;

public abstract class XMLInputFactory {
    
    public static final String IS_NAMESPACE_AWARE= "javax.xml.stream.isNamespaceAware";

    public static final String IS_VALIDATING= "javax.xml.stream.isValidating";

    public static final String IS_COALESCING= "javax.xml.stream.isCoalescing";

    public static final String IS_REPLACING_ENTITY_REFERENCES= "javax.xml.stream.isReplacingEntityReferences";

    public static final String IS_SUPPORTING_EXTERNAL_ENTITIES= "javax.xml.stream.isSupportingExternalEntities";

    public static final String SUPPORT_DTD= "javax.xml.stream.supportDTD";

    public static final String REPORTER= "javax.xml.stream.reporter";

    public static final String RESOLVER= "javax.xml.stream.resolver";

    public static final String ALLOCATOR= "javax.xml.stream.allocator";

    private static final String DEFAULT_IMPL = "com.sun.xml.internal.stream.XMLInputFactoryImpl";

    protected XMLInputFactory(){}

    private static void setProperties(XMLInputFactory factory) {
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setXMLResolver((publicID, systemID, baseURI, namespace) -> {
            throw new XMLStreamException("Reading external entities is disabled");
        });
    }
    
    public static XMLInputFactory newDefaultFactory() {
        XMLInputFactory factory = $FactoryFinder.newInstance(XMLInputFactory.class, DEFAULT_IMPL, null, false, true);
        setProperties(factory);
        return factory;
    }
    
    public static XMLInputFactory newInstance() throws FactoryConfigurationError {
        XMLInputFactory factory = $FactoryFinder.find(XMLInputFactory.class, DEFAULT_IMPL);
        setProperties(factory);
        return factory;
    }

    @Deprecated
    public static XMLInputFactory newFactory() throws FactoryConfigurationError {
        return newInstance();
    }

    public static XMLInputFactory newFactory(String factoryId, ClassLoader classLoader) throws FactoryConfigurationError {
        XMLInputFactory factory = $FactoryFinder.find(XMLInputFactory.class, factoryId, classLoader, null);
        setProperties(factory);
        return factory;
    }

    @Deprecated
    public static XMLInputFactory newInstance(String factoryId, ClassLoader classLoader) throws FactoryConfigurationError {
        return newFactory(factoryId, classLoader);
    }
    
    public abstract XMLStreamReader createXMLStreamReader(java.io.Reader reader) throws XMLStreamException;
    
    public abstract XMLStreamReader createXMLStreamReader(Source source) throws XMLStreamException;
    
    public abstract XMLStreamReader createXMLStreamReader(java.io.InputStream stream) throws XMLStreamException;

    public abstract XMLStreamReader createXMLStreamReader(java.io.InputStream stream, String encoding) throws XMLStreamException;
    
    public abstract XMLStreamReader createXMLStreamReader(String systemId, java.io.InputStream stream) throws XMLStreamException;

    public abstract XMLStreamReader createXMLStreamReader(String systemId, java.io.Reader reader) throws XMLStreamException;

    public abstract XMLEventReader createXMLEventReader(java.io.Reader reader) throws XMLStreamException;

    public abstract XMLEventReader createXMLEventReader(String systemId, java.io.Reader reader) throws XMLStreamException;
    
    public abstract XMLEventReader createXMLEventReader(XMLStreamReader reader) throws XMLStreamException;

    public abstract XMLEventReader createXMLEventReader(Source source) throws XMLStreamException;

    public abstract XMLEventReader createXMLEventReader(java.io.InputStream stream) throws XMLStreamException;
    
    public abstract XMLEventReader createXMLEventReader(java.io.InputStream stream, String encoding) throws XMLStreamException;
    
    public abstract XMLEventReader createXMLEventReader(String systemId, java.io.InputStream stream) throws XMLStreamException;

    public abstract XMLStreamReader createFilteredReader(XMLStreamReader reader, StreamFilter filter) throws XMLStreamException;
    
    public abstract XMLEventReader createFilteredReader(XMLEventReader reader, EventFilter filter) throws XMLStreamException;
    
    public abstract XMLResolver getXMLResolver();

    public abstract void  setXMLResolver(XMLResolver resolver);
    
    public abstract XMLReporter getXMLReporter();

    public abstract void setXMLReporter(XMLReporter reporter);

    public abstract void setProperty(java.lang.String name, Object value) throws IllegalArgumentException;
    
    public abstract Object getProperty(java.lang.String name) throws IllegalArgumentException;

    public abstract boolean isPropertySupported(String name);

    public abstract void setEventAllocator(XMLEventAllocator allocator);

    public abstract XMLEventAllocator getEventAllocator();

}
