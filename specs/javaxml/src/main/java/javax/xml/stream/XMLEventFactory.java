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

import java.util.Iterator;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.events.*;

public abstract class XMLEventFactory {

    private static final String DEFAULT_IMPL = "com.sun.xml.internal.stream.events.XMLEventFactoryImpl";

    protected XMLEventFactory(){}

    public static XMLEventFactory newDefaultFactory() {
        return $FactoryFinder.newInstance(XMLEventFactory.class, DEFAULT_IMPL, null, false, true);
    }

    public static XMLEventFactory newInstance() throws FactoryConfigurationError {
        return $FactoryFinder.find(XMLEventFactory.class, DEFAULT_IMPL);
    }

    public static XMLEventFactory newFactory() throws FactoryConfigurationError {
        return $FactoryFinder.find(XMLEventFactory.class, DEFAULT_IMPL);
    }

    @Deprecated
    public static XMLEventFactory newInstance(String factoryId, ClassLoader classLoader) throws FactoryConfigurationError {
        return $FactoryFinder.find(XMLEventFactory.class, factoryId, classLoader, null);
    }

    public static XMLEventFactory newFactory(String factoryId, ClassLoader classLoader) throws FactoryConfigurationError {
        return $FactoryFinder.find(XMLEventFactory.class, factoryId, classLoader, null);
    }

    public abstract void setLocation(Location location);

    public abstract Attribute createAttribute(String prefix, String namespaceURI, String localName, String value);

    public abstract Attribute createAttribute(String localName, String value);

    public abstract Attribute createAttribute(QName name, String value);

    public abstract Namespace createNamespace(String namespaceURI);
    
    public abstract Namespace createNamespace(String prefix, String namespaceUri);

    public abstract StartElement createStartElement(QName name, Iterator<? extends Attribute> attributes, Iterator<? extends Namespace> namespaces);

    public abstract StartElement createStartElement(String prefix, String namespaceUri, String localName);
    
    public abstract StartElement createStartElement(String prefix, String namespaceUri, String localName, Iterator<? extends Attribute> attributes, Iterator<? extends Namespace> namespaces);

    public abstract StartElement createStartElement(String prefix, String namespaceUri, String localName, Iterator<? extends Attribute> attributes, Iterator<? extends Namespace> namespaces, NamespaceContext context);

    public abstract EndElement createEndElement(QName name, Iterator<? extends Namespace> namespaces);

    public abstract EndElement createEndElement(String prefix, String namespaceUri, String localName);
    
    public abstract EndElement createEndElement(String prefix, String namespaceUri, String localName, Iterator<? extends Namespace> namespaces);

    public abstract Characters createCharacters(String content);

    public abstract Characters createCData(String content);

    public abstract Characters createSpace(String content);
    
    public abstract Characters createIgnorableSpace(String content);

    public abstract StartDocument createStartDocument();

    public abstract StartDocument createStartDocument(String encoding, String version, boolean standalone);

    public abstract StartDocument createStartDocument(String encoding, String version);

    public abstract StartDocument createStartDocument(String encoding);

    public abstract EndDocument createEndDocument();

    public abstract EntityReference createEntityReference(String name, EntityDeclaration declaration);
    
    public abstract Comment createComment(String text);

    public abstract ProcessingInstruction createProcessingInstruction(String target, String data);

    public abstract DTD createDTD(String dtd);
}
