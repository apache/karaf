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
package org.apache.felix.bundlerepository;

import java.io.InputStream;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Repository XML parser based on StaX 
 */
public class StaxParser implements RepositoryImpl.RepositoryParser
{
    private static final String REPOSITORY = "repository";
    private static final String NAME = "name";
    private static final String LASTMODIFIED = "lastmodified";
    private static final String REFERRAL = "referral";
    private static final String RESOURCE = "resource";
    private static final String DEPTH = "depth";
    private static final String URL = "url";
    private static final String CATEGORY = "category";
    private static final String ID = "id";
    private static final String CAPABILITY = "capability";
    private static final String REQUIRE = "require";
    private static final String P = "p";
    private static final String N = "n";
    private static final String T = "t";
    private static final String V = "v";
    private static final String FILTER = "filter";
    private static final String EXTEND = "extend";
    private static final String MULTIPLE = "multiple";
    private static final String OPTIONAL = "optional";

    static XMLInputFactory factory;

    public StaxParser()
    {
        synchronized (StaxParser.class)
        {
            if (factory == null)
            {
                factory = XMLInputFactory.newInstance();
                setProperty(factory, XMLInputFactory.IS_NAMESPACE_AWARE, false);
                setProperty(factory, XMLInputFactory.IS_VALIDATING, false);
                setProperty(factory, XMLInputFactory.IS_COALESCING, false);
            }
        }
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

    public void parse(RepositoryImpl repository, InputStream is) throws Exception
    {
        XMLStreamReader reader = factory.createXMLStreamReader(is);
        try
        {
            int event = reader.nextTag();
            if (event != XMLStreamConstants.START_ELEMENT || !REPOSITORY.equals(reader.getLocalName()))
            {
                throw new Exception("Expected element 'repository' at the root of the document");
            }
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
                    repository.setLastmodified(value);
                }
            }
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
        }
        finally
        {
            reader.close();
        }
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
                    CategoryImpl category = parseCategory(reader);
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
                    ignoreTag(reader);
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

    private CategoryImpl parseCategory(XMLStreamReader reader) throws XMLStreamException
    {
        CategoryImpl category = new CategoryImpl();
        for (int i = 0, nb = reader.getAttributeCount(); i < nb; i++)
        {
            if (ID.equals(reader.getAttributeLocalName(i)))
            {
                category.setId(reader.getAttributeValue(i));
            }
        }
        sanityCheckEndElement(reader, reader.nextTag(), CATEGORY);
        return category;
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
                capability.addP(prop);
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
                requirement.setExtend(value);
            }
            else if (MULTIPLE.equals(name))
            {
                requirement.setMultiple(value);
            }
            else if (OPTIONAL.equals(name))
            {
                requirement.setOptional(value);
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