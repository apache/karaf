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
package org.apache.felix.metatype;


import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.felix.metatype.internal.Activator;
import org.kxml2.io.KXmlParser;
import org.osgi.service.log.LogService;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;


/**
 * The <code>MetaDataReader</code> provides two methods to read meta type
 * documents according to the MetaType schema (105.8 XML Schema). The
 * {@link #parse(URL)} and {@link #parse(InputStream)} methods may be called
 * multiple times to parse such documents.
 * <p>
 * While reading the XML document java objects are created to hold the data.
 * These objects are created by factory methods. Users of this may extend this
 * class by overwriting the the factory methods to create specialized versions.
 * One notable use of this is the extension of the {@link AD} class to overwrite
 * the {@link AD#validate(String)} method. In this case, the {@link #createAD()}
 * method would be overwritten to return an instance of the extending class. 
 * <p>
 * This class is not thread safe. Using instances of this class in multiple
 * threads concurrently is not supported and will fail.
 * 
 * @author fmeschbe
 */
public class MetaDataReader
{

    /** The XML parser used to read the XML documents */
    private KXmlParser parser = new KXmlParser();


    /**
     * Parses the XML document provided by the <code>url</code>. The XML document
     * must be at the beginning of the stream contents.
     * <p>
     * This method is almost identical to
     * <code>return parse(url.openStream());</code> but also sets the string
     * representation of the URL as a location helper for error messages.
     * 
     * @param url The <code>URL</code> providing access to the XML document.
     * 
     * @return A {@link MetaData} providing access to the
     *      raw contents of the XML document.
     *      
     * @throws IOException If an I/O error occurrs accessing the stream. 
     * @throws XmlPullParserException If an error occurrs parsing the XML
     *      document.
     */
    public MetaData parse( URL url ) throws IOException, XmlPullParserException
    {
        InputStream ins = null;
        try
        {
            ins = url.openStream();
            parser.setProperty( "http://xmlpull.org/v1/doc/properties.html#location", url.toString() );
            return parse( ins );
        }
        finally
        {
            if ( ins != null )
            {
                try
                {
                    ins.close();
                }
                catch ( IOException ioe )
                {
                    // ignore
                }
            }
        }
    }


    /**
     * Parses the XML document in the given input stream.
     * <p>
     * This method starts reading at the current position of the input stream
     * and returns immediately after completely reading a single meta type
     * document. The stream is not closed by this method.
     * 
     * @param ins The <code>InputStream</code> providing the XML document
     * 
     * @return A {@link MetaData} providing access to the
     *      raw contents of the XML document.
     *      
     * @throws IOException If an I/O error occurrs accessing the stream. 
     * @throws XmlPullParserException If an error occurrs parsing the XML
     *      document.
     */
    public MetaData parse( InputStream ins ) throws IOException, XmlPullParserException
    {
        // set the parser input, use null encoding to force detection with <?xml?>
        parser.setInput( ins, null );

        MetaData mti = null;

        int eventType = parser.getEventType();
        while ( eventType != XmlPullParser.END_DOCUMENT )
        {
            if ( eventType == XmlPullParser.START_TAG )
            {
                if ( "MetaData".equals( parser.getName() ) )
                {
                    mti = readMetaData();
                }
                else
                {
                    ignoreElement();
                }
            }
            eventType = parser.next();
        }

        return mti;
    }


    private MetaData readMetaData() throws IOException, XmlPullParserException
    {
        MetaData mti = createMetaData();
        mti.setLocalePrefix( getOptionalAttribute( "localization" ) );

        int eventType = parser.next();
        while ( eventType != XmlPullParser.END_DOCUMENT )
        {
            if ( eventType == XmlPullParser.START_TAG )
            {
                if ( "OCD".equals( parser.getName() ) )
                {
                    mti.addObjectClassDefinition( readOCD() );
                }
                else if ( "Designate".equals( parser.getName() ) )
                {
                    mti.addDesignate( readDesignate() );
                }
                else
                {
                    ignoreElement();
                }
            }
            else if ( eventType == XmlPullParser.END_TAG )
            {
                if ( "MetaData".equals( parser.getName() ) )
                {
                    break;
                }

                throw unexpectedElement();
            }
            eventType = parser.next();
        }

        return mti;
    }


    private OCD readOCD() throws IOException, XmlPullParserException
    {
        OCD ocd = createOCD();
        ocd.setId( getRequiredAttribute( "id" ) );
        ocd.setName( getRequiredAttribute( "name" ) );
        ocd.setDescription( getOptionalAttribute( "description" ) );

        int eventType = parser.next();
        while ( eventType != XmlPullParser.END_DOCUMENT )
        {
            if ( eventType == XmlPullParser.START_TAG )
            {
                if ( "AD".equals( parser.getName() ) )
                {
                    ocd.addAttributeDefinition( readAD() );
                }
                else if ( "Icon".equals( parser.getName() ) )
                {
                    String res = getRequiredAttribute( "resource" );
                    String sizeString = getRequiredAttribute( "size" );
                    try
                    {
                        Integer size = Integer.decode( sizeString );
                        ocd.addIcon( size, res );
                    }
                    catch ( NumberFormatException nfe )
                    {
                        Activator.log( LogService.LOG_DEBUG, "readOCD: Icon size '" + sizeString
                            + "' is not a valid number" );
                    }
                }
                else
                {
                    ignoreElement();
                }
            }
            else if ( eventType == XmlPullParser.END_TAG )
            {
                if ( "OCD".equals( parser.getName() ) )
                {
                    break;
                }
                else if ( !"Icon".equals( parser.getName() ) )
                {
                    throw unexpectedElement();
                }
            }
            eventType = parser.next();
        }

        return ocd;
    }


    private Designate readDesignate() throws IOException, XmlPullParserException
    {
        Designate designate = createDesignate();
        designate.setPid( getRequiredAttribute( "pid" ) );
        designate.setFactoryPid( getOptionalAttribute( "factoryPid" ) );
        designate.setBundleLocation( getOptionalAttribute( "bundle" ) );
        designate.setOptional( getOptionalAttribute( "optional", false ) );
        designate.setMerge( getOptionalAttribute( "merge", false ) );

        int eventType = parser.next();
        while ( eventType != XmlPullParser.END_DOCUMENT )
        {
            if ( eventType == XmlPullParser.START_TAG )
            {
                if ( "Object".equals( parser.getName() ) )
                {
                    designate.setObject( readObject() );
                }
                else
                {
                    ignoreElement();
                }
            }
            else if ( eventType == XmlPullParser.END_TAG )
            {
                if ( "Designate".equals( parser.getName() ) )
                {
                    break;
                }

                throw unexpectedElement();
            }
            eventType = parser.next();
        }

        return designate;
    }


    private AD readAD() throws IOException, XmlPullParserException
    {
        AD ad = createAD();
        ad.setID( getRequiredAttribute( "id" ) );
        ad.setName( getRequiredAttribute( "name" ) );
        ad.setDescription( getOptionalAttribute( "description" ) );
        ad.setType( getRequiredAttribute( "type" ) );
        ad.setCardinality( getOptionalAttribute( "cardinality", 0 ) );
        ad.setMin( getOptionalAttribute( "min" ) );
        ad.setMax( getOptionalAttribute( "min" ) );
        ad.setDefaultValue( getOptionalAttribute( "default" ) );
        ad.setRequired( getOptionalAttribute( "required", true ) );

        Map options = new LinkedHashMap();
        int eventType = parser.next();
        while ( eventType != XmlPullParser.END_DOCUMENT )
        {
            if ( eventType == XmlPullParser.START_TAG )
            {
                if ( "Option".equals( parser.getName() ) )
                {
                    String value = getRequiredAttribute( "value" );
                    String label = getRequiredAttribute( "label" );
                    options.put( value, label );
                }
                else
                {
                    ignoreElement();
                }
            }
            else if ( eventType == XmlPullParser.END_TAG )
            {
                if ( "AD".equals( parser.getName() ) )
                {
                    break;
                }
                else if ( !"Option".equals( parser.getName() ) )
                {
                    throw unexpectedElement();
                }
            }
            eventType = parser.next();
        }

        ad.setOptions( options );

        return ad;
    }


    private DesignateObject readObject() throws IOException, XmlPullParserException
    {
        DesignateObject oh = createDesignateObject();
        oh.setOcdRef( getRequiredAttribute( "ocdref" ) );

        int eventType = parser.next();
        while ( eventType != XmlPullParser.END_DOCUMENT )
        {
            if ( eventType == XmlPullParser.START_TAG )
            {
                if ( "Attribute".equals( parser.getName() ) )
                {
                    oh.addAttribute( readAttribute() );
                }
                else
                {
                    ignoreElement();
                }
            }
            else if ( eventType == XmlPullParser.END_TAG )
            {
                if ( "Object".equals( parser.getName() ) )
                {
                    break;
                }
                throw unexpectedElement();
            }
            eventType = parser.next();
        }

        return oh;
    }


    private Attribute readAttribute() throws IOException, XmlPullParserException
    {
        Attribute ah = createAttribute();
        ah.setAdRef( getRequiredAttribute( "adref" ) );
        ah.addContent( getOptionalAttribute( "content" ) );

        int eventType = parser.next();
        while ( eventType != XmlPullParser.END_DOCUMENT )
        {
            if ( eventType == XmlPullParser.START_TAG )
            {
                if ( "Value".equals( parser.getName() ) )
                {
                    ah.addContent( parser.nextText() );
                    eventType = parser.getEventType();
                    continue;
                }
                else
                {
                    ignoreElement();
                }
            }
            else if ( eventType == XmlPullParser.END_TAG )
            {
                if ( "Attribute".equals( parser.getName() ) )
                {
                    break;
                }
                else if ( !"Value".equals( parser.getName() ) )
                {
                    throw unexpectedElement();
                }
            }
            eventType = parser.next();
        }

        return ah;
    }


    //---------- Attribute access helper --------------------------------------

    private String getRequiredAttribute( String attrName ) throws XmlPullParserException
    {
        String attrVal = parser.getAttributeValue( null, attrName );
        if ( attrVal != null )
        {
            return attrVal;
        }

        // fail if value is missing
        throw missingAttribute( attrName );
    }


    private String getOptionalAttribute( String attrName )
    {
        return getOptionalAttribute( attrName, ( String ) null );
    }


    private String getOptionalAttribute( String attrName, String defaultValue )
    {
        String attrVal = parser.getAttributeValue( null, attrName );
        return ( attrVal != null ) ? attrVal : defaultValue;
    }


    private boolean getOptionalAttribute( String attrName, boolean defaultValue )
    {
        String attrVal = parser.getAttributeValue( null, attrName );
        return ( attrVal != null ) ? "true".equalsIgnoreCase( attrVal ) : defaultValue;
    }


    private int getOptionalAttribute( String attrName, int defaultValue )
    {
        String attrVal = parser.getAttributeValue( null, attrName );
        if ( attrVal != null && attrVal.length() > 0 )
        {
            try
            {
                return Integer.decode( attrVal ).intValue();
            }
            catch ( NumberFormatException nfe )
            {
                Activator.log( LogService.LOG_DEBUG, "getOptionalAttribute: Value '" + attrVal + "' of attribute "
                    + attrName + " is not a valid number. Using default value " + defaultValue );
            }
        }

        // fallback to default
        return defaultValue;
    }


    //---------- Error Handling support ---------------------------------------

    private void ignoreElement() throws IOException, XmlPullParserException
    {
        String ignoredElement = parser.getName();

        int depth = 0; // enable nested ignored elements
        int eventType = parser.next();
        while ( eventType != XmlPullParser.END_DOCUMENT )
        {
            if ( eventType == XmlPullParser.START_TAG )
            {
                if ( ignoredElement.equals( parser.getName() ) )
                {
                    depth++;
                }
            }
            else if ( eventType == XmlPullParser.END_TAG )
            {
                if ( ignoredElement.equals( parser.getName() ) )
                {
                    if ( depth <= 0 )
                    {
                        return;
                    }

                    depth--;
                }
            }
            eventType = parser.next();
        }
    }


    private XmlPullParserException missingAttribute( String attrName )
    {
        String message = "Missing Attribute " + attrName + " in element " + parser.getName();
        return new XmlPullParserException( message, parser, null );
    }


    private XmlPullParserException unexpectedElement()
    {
        String message = "Illegal Element " + parser.getName();
        return new XmlPullParserException( message, parser, null );
    }


    //---------- Factory methods ----------------------------------------------

    /**
     * Creates a new {@link MetaData} object to hold the contents of the
     * <code>MetaData</code> element.
     * <p>
     * This method may be overwritten to return a customized extension.
     */
    protected MetaData createMetaData()
    {
        return new MetaData();
    }


    /**
     * Creates a new {@link OCD} object to hold the contents of the
     * <code>OCD</code> element.
     * <p>
     * This method may be overwritten to return a customized extension.
     */
    protected OCD createOCD()
    {
        return new OCD();
    }


    /**
     * Creates a new {@link AD} object to hold the contents of the
     * <code>AD</code> element.
     * <p>
     * This method may be overwritten to return a customized extension.
     */
    protected AD createAD()
    {
        return new AD();
    }


    /**
     * Creates a new {@link DesignateObject} object to hold the contents of the
     * <code>Object</code> element.
     * <p>
     * This method may be overwritten to return a customized extension.
     */
    protected DesignateObject createDesignateObject()
    {
        return new DesignateObject();
    }


    /**
     * Creates a new {@link Attribute} object to hold the contents of the
     * <code>Attribute</code> element.
     * <p>
     * This method may be overwritten to return a customized extension.
     */
    protected Attribute createAttribute()
    {
        return new Attribute();
    }


    /**
     * Creates a new {@link Designate} object to hold the contents of the
     * <code>Designate</code> element.
     * <p>
     * This method may be overwritten to return a customized extension.
     */
    protected Designate createDesignate()
    {
        return new Designate();
    }
}
