/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.servicebinder.parser;

import org.apache.felix.servicebinder.XmlHandler;
import org.kxml.parser.XmlParser;
import org.kxml.parser.ParseEvent;
import org.kxml.Xml;
import org.kxml.Attribute;

import java.io.Reader;

import java.util.Properties;

/**
 * The KxmlParser extends the XmlParser from kxml. This is a very
 * simple parser that does not take into account the DTD
 *
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class KxmlParser extends XmlParser
{
    /**
    * The constructor for a parser, it receives a java.io.Reader.
    *
    * @param   r  The reader
    * @exception   java.io.IOException thrown by the superclass
    */
    public KxmlParser(Reader r) throws java.io.IOException
    {
        super(r);
    }

    /**
    * Parser from the reader provided in the constructor, and call
    * the startElement and endElement in a KxmlHandler
    *
    * @param   handler The handler
    * @exception   java.io.IOException thrown by the superclass
    */
    public void parseXML(XmlHandler handler) throws java.io.IOException, ParseException
    {
        ParseEvent evt=null;
        do
        {
            evt = read();
            if (evt.getType() == Xml.START_TAG)
            {
                Properties props = new Properties();
                for (int i=0; i<evt.getAttributeCount();i++)
                {
                    Attribute attr = evt.getAttribute(i);
                    props.put(attr.getName(),attr.getValue());
                }
                handler.startElement("uri",evt.getName(),evt.getName(),props);
            }
            if (evt.getType() == Xml.END_TAG)
            {
                handler.endElement("uri",evt.getName(),evt.getName());
            }
        }while(evt.getType()!=Xml.END_DOCUMENT);
    }
}
