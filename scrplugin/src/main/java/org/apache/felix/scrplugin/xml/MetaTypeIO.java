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
package org.apache.felix.scrplugin.xml;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.apache.felix.scrplugin.om.metatype.*;
import org.apache.maven.plugin.MojoExecutionException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;


/**
 * <code>MetaType</code>
 *
 * is a helper class to read and write meta type service files.
 *
 */
public class MetaTypeIO {

    public static final String NAMESPACE_URI = "http://www.osgi.org/xmlns/metatype/v1.0.0";

    public static final String INNER_NAMESPACE_URI = "";

    public static final String PREFIX = "metatype";

    protected static final String METADATA_ELEMENT = "MetaData";
    protected static final String METADATA_ELEMENT_QNAME = PREFIX + ':' + METADATA_ELEMENT;

    protected static final String OCD_ELEMENT = "OCD";
    protected static final String OCD_ELEMENT_QNAME = OCD_ELEMENT;

    protected static final String DESIGNATE_ELEMENT = "Designate";
    protected static final String DESIGNATE_ELEMENT_QNAME = DESIGNATE_ELEMENT;

    protected static final String OBJECT_ELEMENT = "Object";
    protected static final String OBJECT_ELEMENT_QNAME = OBJECT_ELEMENT;

    protected static final String AD_ELEMENT = "AD";
    protected static final String AD_ELEMENT_QNAME = AD_ELEMENT;

    protected static final String OPTION_ELEMENT = "Option";
    protected static final String OPTION_ELEMENT_QNAME = OPTION_ELEMENT;

    public static void write(MetaData metaData, File file)
    throws MojoExecutionException {
        try {
            generateXML(metaData, IOUtils.getSerializer(file));
        } catch (TransformerException e) {
            throw new MojoExecutionException("Unable to write xml to " + file, e);
        } catch (SAXException e) {
            throw new MojoExecutionException("Unable to generate xml for " + file, e);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to write xml to " + file, e);
        }
    }

    /**
     * Generate the xml top level element and start streaming
     * the meta data.
     * @param metaData
     * @param contentHandler
     * @throws SAXException
     */
    protected static void generateXML(MetaData metaData, ContentHandler contentHandler)
    throws SAXException {
        contentHandler.startDocument();
        contentHandler.startPrefixMapping(PREFIX, NAMESPACE_URI);

        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, "localization", metaData.getLocalization());

        contentHandler.startElement(NAMESPACE_URI, METADATA_ELEMENT, METADATA_ELEMENT_QNAME, ai);
        IOUtils.newline(contentHandler);

        final Iterator i = metaData.getDescriptors().iterator();
        while ( i.hasNext() ) {
            final Object obj = i.next();
            if ( obj instanceof OCD ) {
                generateXML((OCD)obj, contentHandler);
            } else {
                generateXML((Designate)obj, contentHandler);
            }
        }
        // end wrapper element
        contentHandler.endElement(NAMESPACE_URI, METADATA_ELEMENT, METADATA_ELEMENT_QNAME);
        IOUtils.newline(contentHandler);
        contentHandler.endPrefixMapping(PREFIX);
        contentHandler.endDocument();
    }

    protected static void generateXML(OCD ocd, ContentHandler contentHandler)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, "id", ocd.getId());
        IOUtils.addAttribute(ai, "name", ocd.getName());
        IOUtils.addAttribute(ai, "description", ocd.getDescription());
        IOUtils.indent(contentHandler, 1);
        contentHandler.startElement(INNER_NAMESPACE_URI, OCD_ELEMENT, OCD_ELEMENT_QNAME, ai);

        if ( ocd.getProperties().size() > 0 ) {
            IOUtils.newline(contentHandler);
            final Iterator i = ocd.getProperties().iterator();
            while ( i.hasNext() ) {
                final AttributeDefinition ad = (AttributeDefinition) i.next();
                generateXML(ad, contentHandler);
            }
            IOUtils.indent(contentHandler, 1);
        }

        contentHandler.endElement(INNER_NAMESPACE_URI, OCD_ELEMENT, OCD_ELEMENT_QNAME);
        IOUtils.newline(contentHandler);
    }

    protected static void generateXML(AttributeDefinition ad, ContentHandler contentHandler)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, "id", ad.getId());
        IOUtils.addAttribute(ai, "type", ad.getType());
        if ( ad.getDefaultMultiValue() != null ) {
            final StringBuffer buf = new StringBuffer();
            for(int i=0; i<ad.getDefaultMultiValue().length; i++) {
                if ( i > 0 ) {
                    buf.append(',');
                }
                buf.append(ad.getDefaultMultiValue()[i]);
            }
            IOUtils.addAttribute(ai, "default", buf);
        } else {
            IOUtils.addAttribute(ai, "default", ad.getDefaultValue());
        }
        IOUtils.addAttribute(ai, "name", ad.getName());
        IOUtils.addAttribute(ai, "description", ad.getDescription());
        IOUtils.addAttribute(ai, "cardinality", ad.getCardinality());
        IOUtils.indent(contentHandler, 2);
        contentHandler.startElement(INNER_NAMESPACE_URI, AD_ELEMENT, AD_ELEMENT_QNAME, ai);

        if (ad.getOptions() != null && ad.getOptions().size() > 0) {
            IOUtils.newline(contentHandler);
            for (Iterator oi=ad.getOptions().entrySet().iterator(); oi.hasNext(); ) {
                final Map.Entry entry = (Map.Entry) oi.next();
                ai.clear();
                IOUtils.addAttribute(ai, "value", String.valueOf(entry.getKey()));
                IOUtils.addAttribute(ai, "label", String.valueOf(entry.getValue()));
                IOUtils.indent(contentHandler, 3);
                contentHandler.startElement(INNER_NAMESPACE_URI, OPTION_ELEMENT, OPTION_ELEMENT_QNAME, ai);
                contentHandler.endElement(INNER_NAMESPACE_URI, OPTION_ELEMENT, OPTION_ELEMENT_QNAME);
                IOUtils.newline(contentHandler);
            }
            IOUtils.indent(contentHandler, 2);
        }

        contentHandler.endElement(INNER_NAMESPACE_URI, AD_ELEMENT, AD_ELEMENT_QNAME);
        IOUtils.newline(contentHandler);
    }

    protected static void generateXML(Designate designate, ContentHandler contentHandler)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, "pid", designate.getPid());
        IOUtils.indent(contentHandler, 1);
        contentHandler.startElement(INNER_NAMESPACE_URI, DESIGNATE_ELEMENT, DESIGNATE_ELEMENT_QNAME, ai);
        IOUtils.newline(contentHandler);

        generateXML(designate.getObject(), contentHandler);

        IOUtils.indent(contentHandler, 1);
        contentHandler.endElement(INNER_NAMESPACE_URI, DESIGNATE_ELEMENT, DESIGNATE_ELEMENT_QNAME);
        IOUtils.newline(contentHandler);
    }

    protected static void generateXML(MTObject obj, ContentHandler contentHandler)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, "ocdref", obj.getOcdref());
        IOUtils.indent(contentHandler, 2);
        contentHandler.startElement(INNER_NAMESPACE_URI, OBJECT_ELEMENT, OBJECT_ELEMENT_QNAME, ai);
        contentHandler.endElement(INNER_NAMESPACE_URI, OBJECT_ELEMENT, OBJECT_ELEMENT_QNAME);
        IOUtils.newline(contentHandler);
    }
}
