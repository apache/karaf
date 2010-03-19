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
package org.apache.felix.bundlerepository.impl;

import java.io.InputStream;
import java.io.Reader;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Repository XML xml based on StaX
 */
public class StaxParser extends RepositoryParser
{

    static XMLInputFactory factory;

    public static synchronized void setFactory(XMLInputFactory factory)
    {
        StaxParser.factory = factory;
    }

    public static synchronized XMLInputFactory getFactory()
    {
        if (factory == null)
        {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            setProperty(factory, XMLInputFactory.IS_NAMESPACE_AWARE, false);
            setProperty(factory, XMLInputFactory.IS_VALIDATING, false);
            setProperty(factory, XMLInputFactory.IS_COALESCING, false);
            StaxParser.factory = factory;
        }
        return factory;
    }

    public StaxParser()
    {
    }

    protected static boolean setProperty(XMLInputFactory factory, String name, boolean value)
    {
        try
        {
            factory.setProperty(name, Boolean.valueOf(value));
            return true;
        }
        catch (Throwable t)
        {
        }
        return false;
    }

    public RepositoryImpl parseRepository(InputStream is) throws Exception
    {
        XMLStreamReader reader = getFactory().createXMLStreamReader(is);
        int event = reader.nextTag();
        if (event != XMLStreamConstants.START_ELEMENT || !REPOSITORY.equals(reader.getLocalName()))
        {
            throw new Exception("Expected element 'repository' at the root of the document");
        }
        return parseRepository(reader);
    }

    public RepositoryImpl parseRepository(Reader r) throws Exception
    {
        XMLStreamReader reader = getFactory().createXMLStreamReader(r);
        int event = reader.nextTag();
        if (event != XMLStreamConstants.START_ELEMENT || !REPOSITORY.equals(reader.getLocalName()))
        {
            throw new Exception("Expected element 'repository' at the root of the document");
        }
        return parseRepository(reader);
    }

    public ResourceImpl parseResource(Reader r) throws Exception
    {
        XMLStreamReader reader = getFactory().createXMLStreamReader(r);
        int event = reader.nextTag();
        if (event != XMLStreamConstants.START_ELEMENT || !RESOURCE.equals(reader.getLocalName()))
        {
            throw new Exception("Expected element 'resource'");
        }
        return parseResource(reader);
    }

    public CapabilityImpl parseCapability(Reader r) throws Exception
    {
        XMLStreamReader reader = getFactory().createXMLStreamReader(r);
        int event = reader.nextTag();
        if (event != XMLStreamConstants.START_ELEMENT || !CAPABILITY.equals(reader.getLocalName()))
        {
            throw new Exception("Expected element 'capability'");
        }
        return parseCapability(reader);
    }

    public PropertyImpl parseProperty(Reader r) throws Exception
    {
        XMLStreamReader reader = getFactory().createXMLStreamReader(r);
        int event = reader.nextTag();
        if (event != XMLStreamConstants.START_ELEMENT || !P.equals(reader.getLocalName()))
        {
            throw new Exception("Expected element 'p'");
        }
        return parseProperty(reader);
    }

    public RequirementImpl parseRequirement(Reader r) throws Exception 
    {
        XMLStreamReader reader = getFactory().createXMLStreamReader(r);
        int event = reader.nextTag();
        if (event != XMLStreamConstants.START_ELEMENT || !REQUIRE.equals(reader.getLocalName()))
        {
            throw new Exception("Expected element 'require'");
        }
        return parseRequire(reader);
    }

    public RepositoryImpl parseRepository(XMLStreamReader reader) throws Exception
    {
        RepositoryImpl repository = new RepositoryImpl();
        for (int i = 0, nb = reader.getAttributeCount(); i < nb; i++)
        {
            String name = reader.getAttributeLocalName(i);
            String value = reader.getAttributeValue(i);
            if (NAME.equals(name))
            {
                repository.setName(value);
            }
            else if (LASTMODIFIED.equals(name))
            {
                repository.setLastModified(value);
            }
        }
        int event;
        while ((event = reader.nextTag()) == XMLStreamConstants.START_ELEMENT)
        {
            String element = reader.getLocalName();
            if (REFERRAL.equals(element))
            {
                Referral referral = parseReferral(reader);
                repository.addReferral(referral);
            }
            else if (RESOURCE.equals(element))
            {
                ResourceImpl resource = parseResource(reader);
                repository.addResource(resource);
            }
            else
            {
                ignoreTag(reader);
            }
        }
        // Sanity check
        sanityCheckEndElement(reader, event, REPOSITORY);
        return repository;
    }

    private void sanityCheckEndElement(XMLStreamReader reader, int event, String element)
    {
        if (event != XMLStreamConstants.END_ELEMENT || !element.equals(reader.getLocalName()))
        {
            throw new IllegalStateException("Unexpected state while finishing element " + element);
        }
    }

    private Referral parseReferral(XMLStreamReader reader) throws Exception
    {
        Referral referral = new Referral();
        for (int i = 0, nb = reader.getAttributeCount(); i < nb; i++)
        {
            String name = reader.getAttributeLocalName(i);
            String value = reader.getAttributeValue(i);
            if (DEPTH.equals(name))
            {
                referral.setDepth(value);
            }
            else if (URL.equals(name))
            {
                referral.setUrl(value);
            }
        }
        sanityCheckEndElement(reader, reader.nextTag(), REFERRAL);
        return referral;
    }

    private ResourceImpl parseResource(XMLStreamReader reader) throws Exception
    {
        ResourceImpl resource = new ResourceImpl();
        try
        {
            for (int i = 0, nb = reader.getAttributeCount(); i < nb; i++)
            {
                resource.put(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
            }
            int event;
            while ((event = reader.nextTag()) == XMLStreamConstants.START_ELEMENT)
            {
                String element = reader.getLocalName();
                if (CATEGORY.equals(element))
                {
                    String category = parseCategory(reader);
                    resource.addCategory(category);
                }
                else if (CAPABILITY.equals(element))
                {
                    CapabilityImpl capability = parseCapability(reader);
                    resource.addCapability(capability);
                }
                else if (REQUIRE.equals(element))
                {
                    RequirementImpl requirement = parseRequire(reader);
                    resource.addRequire(requirement);
                }
                else
                {
                    StringBuffer sb = null;
                    String type = reader.getAttributeValue(null, "type");
                    while ((event = reader.next()) != XMLStreamConstants.END_ELEMENT)
                    {
                        switch (event)
                        {
                            case XMLStreamConstants.START_ELEMENT:
                                throw new Exception("Unexpected element inside <require/> element");
                            case XMLStreamConstants.CHARACTERS:
                                if (sb == null)
                                {
                                    sb = new StringBuffer();
                                }
                                sb.append(reader.getText());
                                break;
                        }
                    }
                    if (sb != null)
                    {
                        resource.put(element, sb.toString().trim(), type);
                    }
                }
            }
            // Sanity check
            if (event != XMLStreamConstants.END_ELEMENT || !RESOURCE.equals(reader.getLocalName()))
            {
                throw new Exception("Unexpected state");
            }
            return resource;
        }
        catch (Exception e)
        {
            Location loc = reader.getLocation();
            if (loc != null) {
                throw new Exception("Error while parsing resource " + resource.getId() + " at line " + loc.getLineNumber() + " and column " + loc.getColumnNumber(), e);
            }
            else
            {
                throw new Exception("Error while parsing resource " + resource.getId(), e);
            }
        }
    }

    private String parseCategory(XMLStreamReader reader) throws XMLStreamException
    {
        String id = null;
        for (int i = 0, nb = reader.getAttributeCount(); i < nb; i++)
        {
            if (ID.equals(reader.getAttributeLocalName(i)))
            {
                id = reader.getAttributeValue(i);
            }
        }
        sanityCheckEndElement(reader, reader.nextTag(), CATEGORY);
        return id;
    }

    private CapabilityImpl parseCapability(XMLStreamReader reader) throws Exception
    {
        CapabilityImpl capability = new CapabilityImpl();
        for (int i = 0, nb = reader.getAttributeCount(); i < nb; i++)
        {
            String name = reader.getAttributeLocalName(i);
            String value = reader.getAttributeValue(i);
            if (NAME.equals(name))
            {
                capability.setName(value);
            }
        }
        int event;
        while ((event = reader.nextTag()) == XMLStreamConstants.START_ELEMENT)
        {
            String element = reader.getLocalName();
            if (P.equals(element))
            {
                PropertyImpl prop = parseProperty(reader);
                capability.addProperty(prop);
            }
            else
            {
                ignoreTag(reader);
            }
        }
        // Sanity check
        sanityCheckEndElement(reader, event, CAPABILITY);
        return capability;
    }

    private PropertyImpl parseProperty(XMLStreamReader reader) throws Exception
    {
        String n = null, t = null, v = null;
        for (int i = 0, nb = reader.getAttributeCount(); i < nb; i++)
        {
            String name = reader.getAttributeLocalName(i);
            String value = reader.getAttributeValue(i);
            if (N.equals(name))
            {
                n = value;
            }
            else if (T.equals(name))
            {
                t = value;
            }
            else if (V.equals(name))
            {
                v = value;
            }
        }
        PropertyImpl prop = new PropertyImpl(n, t, v);
        // Sanity check
        sanityCheckEndElement(reader, reader.nextTag(), P);
        return prop;
    }

    private RequirementImpl parseRequire(XMLStreamReader reader) throws Exception
    {
        RequirementImpl requirement = new RequirementImpl();
        for (int i = 0, nb = reader.getAttributeCount(); i < nb; i++)
        {
            String name = reader.getAttributeLocalName(i);
            String value = reader.getAttributeValue(i);
            if (NAME.equals(name))
            {
                requirement.setName(value);
            }
            else if (FILTER.equals(name))
            {
                requirement.setFilter(value);
            }
            else if (EXTEND.equals(name))
            {
                requirement.setExtend(Boolean.parseBoolean(value));
            }
            else if (MULTIPLE.equals(name))
            {
                requirement.setMultiple(Boolean.parseBoolean(value));
            }
            else if (OPTIONAL.equals(name))
            {
                requirement.setOptional(Boolean.parseBoolean(value));
            }
        }
        int event;
        StringBuffer sb = null;
        while ((event = reader.next()) != XMLStreamConstants.END_ELEMENT)
        {
            switch (event)
            {
                case XMLStreamConstants.START_ELEMENT:
                    throw new Exception("Unexpected element inside <require/> element");
                case XMLStreamConstants.CHARACTERS:
                    if (sb == null)
                    {
                        sb = new StringBuffer();
                    }
                    sb.append(reader.getText());
                    break;
            }
        }
        if (sb != null)
        {
            requirement.addText(sb.toString());
        }
        // Sanity check
        sanityCheckEndElement(reader, event, REQUIRE);
        return requirement;
    }

    private void ignoreTag(XMLStreamReader reader) throws XMLStreamException
    {
        int level = 1;
        int event = 0;
        while (level > 0)
        {
            event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT)
            {
                level++;
            }
            else if (event == XMLStreamConstants.END_ELEMENT)
            {
                level--;
            }
        }
    }
}