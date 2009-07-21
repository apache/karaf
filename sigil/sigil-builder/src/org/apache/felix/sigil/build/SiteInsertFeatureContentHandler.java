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
package org.apache.felix.sigil.build;


import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;


class SiteInsertFeatureContentHandler implements ContentHandler
{

    private final ContentHandler output;
    private final List<org.apache.felix.sigil.build.Feature> featureList;


    public SiteInsertFeatureContentHandler( ContentHandler output, List<Feature> featureList )
    {
        this.output = output;
        this.featureList = featureList;
    }


    public void characters( char[] ch, int start, int length ) throws SAXException
    {
        output.characters( ch, start, length );
    }


    public void endDocument() throws SAXException
    {
        output.endDocument();
    }


    public void endElement( String uri, String localName, String name ) throws SAXException
    {
        output.endElement( uri, localName, name );
    }


    public void endPrefixMapping( String prefix ) throws SAXException
    {
        output.endPrefixMapping( prefix );
    }


    public void ignorableWhitespace( char[] ch, int start, int length ) throws SAXException
    {
        //output.ignorableWhitespace(ch, start, length);
    }


    public void processingInstruction( String target, String data ) throws SAXException
    {
        output.processingInstruction( target, data );
    }


    public void setDocumentLocator( Locator locator )
    {
        output.setDocumentLocator( locator );
    }


    public void skippedEntity( String name ) throws SAXException
    {
        output.skippedEntity( name );
    }


    public void startDocument() throws SAXException
    {
        output.startDocument();
    }


    public void startElement( String uri, String localName, String name, Attributes atts ) throws SAXException
    {
        output.startElement( uri, localName, name, atts );

        if ( "site".equals( name ) )
        {
            for ( Feature feature : featureList )
            {
                AttributesImpl featureAtts = new AttributesImpl();
                featureAtts.addAttribute( "", "", "url", "CDATA", feature.url );
                featureAtts.addAttribute( "", "", "id", "CDATA", feature.id );
                featureAtts.addAttribute( "", "", "version", "CDATA", feature.version );
                output.startElement( "", "", "feature", featureAtts );

                for ( int i = 0; i < feature.categories.length; i++ )
                {
                    AttributesImpl categoryAtts = new AttributesImpl();
                    categoryAtts.addAttribute( "", "", "name", "CDATA", feature.categories[i] );
                    output.startElement( "", "", "category", categoryAtts );
                    output.endElement( "", "", "category" );
                }

                output.endElement( "", "", "feature" );
            }
        }
    }


    public void startPrefixMapping( String prefix, String uri ) throws SAXException
    {
        output.startPrefixMapping( prefix, uri );
    }

}
