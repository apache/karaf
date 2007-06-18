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
package org.apache.felix.servicebinder.parser;

import org.apache.felix.servicebinder.XmlHandler;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

/**
 * The KxmlParser extends the XmlParser from kxml. This is a very
 * simple parser that does not take into account the DTD
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class KxmlParser extends KXmlParser
{
    /**
     * The constructor for a parser, it receives a java.io.Reader.
     * 
     * @param reader The reader
     * @throws XmlPullParserException thrown by the super class.
     */
    public KxmlParser(final Reader reader) throws XmlPullParserException
    {
        super();
        setInput(reader);
    }

    /**
     * Parser from the reader provided in the constructor, and call the
     * startElement and endElement in a KxmlHandler
     * 
     * @param handler The handler
     * @throws XmlPullParserException thrown by the super class.
     * @throws IOException thrown by the super class.
     * @throws ParseException thrown by the handler.
     */
    public void parseXML(final XmlHandler handler)
        throws XmlPullParserException, IOException, ParseException
    {
        while (next() != XmlPullParser.END_DOCUMENT)
        {
            switch (getEventType())
            {
                case XmlPullParser.START_TAG:
                    Properties props = new Properties();
                    for (int i = 0; i < getAttributeCount(); i++)
                    {
                        props.put(getAttributeName(i), getAttributeValue(i));
                    }
                    handler.startElement(getNamespace(), getName(), getName(), props);
                    break;
                case XmlPullParser.END_TAG:
                    handler.endElement(getNamespace(), getName(), getName());
                    break;
                default:
                    continue;
            }
        }
    }
}
