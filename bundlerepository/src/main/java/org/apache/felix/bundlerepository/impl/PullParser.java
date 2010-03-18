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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Repository XML parser based on StaX
 */
public class PullParser implements RepositoryImpl.RepositoryParser
{

    public PullParser()
    {
    }

    public void parse(RepositoryImpl repository, InputStream is) throws Exception
    {
        KXmlParser reader = new KXmlParser();
        reader.setInput(new BufferedReader(new InputStreamReader(is)));
        int event = reader.nextTag();
        if (event != XmlPullParser.START_TAG || !REPOSITORY.equals(reader.getName()))
        {
            throw new Exception("Expected element 'repository' at the root of the document");
        }
        for (int i = 0, nb = reader.getAttributeCount(); i < nb; i++)
        {
            String name = reader.getAttributeName(i);
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
        while ((event = reader.nextTag()) == XmlPullParser.START_TAG)
        {
            String element = reader.getName();
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

    private void sanityCheckEndElement(KXmlParser reader, int event, String element)
    {
        if (event != XmlPullParser.END_TAG || !element.equals(reader.getName()))
        {
            throw new IllegalStateException("Unexpected state while finishing element " + element);
        }
    }

    private Referral parseReferral(KXmlParser reader) throws Exception
    {
        Referral referral = new Referral();
        for (int i = 0, nb = reader.getAttributeCount(); i < nb; i++)
        {
            String name = reader.getAttributeName(i);
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

    private ResourceImpl parseResource(KXmlParser reader) throws Exception
    {
        ResourceImpl resource = new ResourceImpl();
        try
        {
            for (int i = 0, nb = reader.getAttributeCount(); i < nb; i++)
            {
                resource.put(reader.getAttributeName(i), reader.getAttributeValue(i));
            }
            int event;
            while ((event = reader.nextTag()) == XmlPullParser.START_TAG)
            {
                String element = reader.getName();
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
                    StringBuffer sb = null;
                    String type = reader.getAttributeValue(null, "type");
                    while ((event = reader.next()) != XmlPullParser.END_TAG)
                    {
                        switch (event)
                        {
                            case XmlPullParser.START_TAG:
                                throw new Exception("Unexpected element inside <require/> element");
                            case XmlPullParser.TEXT:
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
            if (event != XmlPullParser.END_TAG || !RESOURCE.equals(reader.getName()))
            {
                throw new Exception("Unexpected state");
            }
            return resource;
        }
        catch (Exception e)
        {
            throw new Exception("Error while parsing resource " + resource.getId() + " at line " + reader.getLineNumber() + " and column " + reader.getColumnNumber(), e);
        }
    }

    private CategoryImpl parseCategory(KXmlParser reader) throws IOException, XmlPullParserException
    {
        CategoryImpl category = new CategoryImpl();
        for (int i = 0, nb = reader.getAttributeCount(); i < nb; i++)
        {
            if (ID.equals(reader.getAttributeName(i)))
            {
                category.setId(reader.getAttributeValue(i));
            }
        }
        sanityCheckEndElement(reader, reader.nextTag(), CATEGORY);
        return category;
    }

    private CapabilityImpl parseCapability(KXmlParser reader) throws Exception
    {
        CapabilityImpl capability = new CapabilityImpl();
        for (int i = 0, nb = reader.getAttributeCount(); i < nb; i++)
        {
            String name = reader.getAttributeName(i);
            String value = reader.getAttributeValue(i);
            if (NAME.equals(name))
            {
                capability.setName(value);
            }
        }
        int event;
        while ((event = reader.nextTag()) == XmlPullParser.START_TAG)
        {
            String element = reader.getName();
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

    private PropertyImpl parseProperty(KXmlParser reader) throws Exception
    {
        String n = null, t = null, v = null;
        for (int i = 0, nb = reader.getAttributeCount(); i < nb; i++)
        {
            String name = reader.getAttributeName(i);
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

    private RequirementImpl parseRequire(KXmlParser reader) throws Exception
    {
        RequirementImpl requirement = new RequirementImpl();
        for (int i = 0, nb = reader.getAttributeCount(); i < nb; i++)
        {
            String name = reader.getAttributeName(i);
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
        while ((event = reader.next()) != XmlPullParser.END_TAG)
        {
            switch (event)
            {
                case XmlPullParser.START_TAG:
                    throw new Exception("Unexpected element inside <require/> element");
                case XmlPullParser.TEXT:
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

    private void ignoreTag(KXmlParser reader) throws IOException, XmlPullParserException {
        int level = 1;
        int event = 0;
        while (level > 0)
        {
            event = reader.next();
            if (event == XmlPullParser.START_TAG)
            {
                level++;
            }
            else if (event == XmlPullParser.END_TAG)
            {
                level--;
            }
        }
    }
}