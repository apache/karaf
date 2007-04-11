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


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

import org.osgi.service.metatype.AttributeDefinition;
import org.xmlpull.v1.XmlPullParserException;


/**
 * The <code>MetaDataReaderTest</code> class tests the
 * <code>MetaDataReader</code> class.
 *
 * @author fmeschbe
 */
public class MetaDataReaderTest extends TestCase
{

    private MetaDataReader reader;


    protected void setUp() throws Exception
    {
        super.setUp();

        reader = new MetaDataReader();
    }


    protected void tearDown() throws Exception
    {
        reader = null;

        super.tearDown();
    }


    public void testEmpty() throws IOException, XmlPullParserException
    {
        String empty = "<MetaData />";
        MetaData mti = read( empty );

        assertNull( mti.getLocalePrefix() );
        assertNull( mti.getObjectClassDefinitions() );
    }


    public void testEmptyLocalization() throws IOException, XmlPullParserException
    {
        String testLoc = "OSGI-INF/folder/base";
        String empty = "<MetaData localization=\"" + testLoc + "\"/>";
        MetaData mti = read( empty );

        assertEquals( testLoc, mti.getLocalePrefix() );
    }


    public void testSingleEmptyOCD() throws IOException, XmlPullParserException
    {
        String ocdName = "ocd0";
        String ocdId = "id.ocd0";
        String ocdDescription = "ocd0 description";

        String empty = "<MetaData><OCD id=\"" + ocdId + "\" name=\"" + ocdName + "\" description=\"" + ocdDescription
            + "\" /></MetaData>";
        MetaData mti = read( empty );

        assertNull( mti.getLocalePrefix() );
        assertNotNull( mti.getObjectClassDefinitions() );
        assertEquals( 1, mti.getObjectClassDefinitions().size() );

        OCD ocd = ( OCD ) mti.getObjectClassDefinitions().values().iterator().next();
        assertEquals( ocdId, ocd.getID() );
        assertEquals( ocdName, ocd.getName() );
        assertEquals( ocdDescription, ocd.getDescription() );

        assertNull( ocd.getAttributeDefinitions() );
    }


    public void testSingleOCDSingleRequiredAttr() throws IOException, XmlPullParserException
    {
        String ocdName = "ocd0";
        String ocdId = "id.ocd0";
        String ocdDescription = "ocd0 description";

        String adId = "id.ad0";
        String adName = "ad0";
        String adDescription = "ad0 description";
        String adType = "String";
        int adCardinality = 789;
        String adDefault = "    a    ,   b    ,    c    ";

        String empty = "<MetaData>" + "<OCD id=\"" + ocdId + "\" name=\"" + ocdName + "\" description=\""
            + ocdDescription + "\">" + "<AD id=\"" + adId + "\" name=\"" + adName + "\" type=\"" + adType
            + "\" description=\"" + adDescription + "\" cardinality=\"" + adCardinality + "\" default=\"" + adDefault
            + "\">" + "</AD>" + "</OCD>" + "</MetaData>";
        MetaData mti = read( empty );

        assertNull( mti.getLocalePrefix() );
        assertNotNull( mti.getObjectClassDefinitions() );
        assertEquals( 1, mti.getObjectClassDefinitions().size() );

        OCD ocd = ( OCD ) mti.getObjectClassDefinitions().values().iterator().next();

        assertNotNull( ocd.getAttributeDefinitions() );
        assertEquals( 1, ocd.getAttributeDefinitions().size() );

        AD ad = ( AD ) ocd.getAttributeDefinitions().values().iterator().next();
        assertEquals( adId, ad.getID() );
        assertEquals( adName, ad.getName() );
        assertEquals( adDescription, ad.getDescription() );
        assertEquals( AttributeDefinition.STRING, ad.getType() );
        assertEquals( adCardinality, ad.getCardinality() );
        assertNotNull( ad.getDefaultValue() );
        assertEquals( 3, ad.getDefaultValue().length );

        String[] defaultValue = ad.getDefaultValue();
        assertEquals( "a", defaultValue[0] );
        assertEquals( "b", defaultValue[1] );
        assertEquals( "c", defaultValue[2] );
    }


    private MetaData read( String data ) throws IOException, XmlPullParserException
    {
        InputStream input = new ByteArrayInputStream( data.getBytes( "UTF-8" ) );
        return reader.parse( input );
    }
}
