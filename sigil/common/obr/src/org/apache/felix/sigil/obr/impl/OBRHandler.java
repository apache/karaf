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

package org.apache.felix.sigil.obr.impl;


import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.apache.felix.sigil.common.osgi.LDAPExpr;
import org.apache.felix.sigil.common.osgi.LDAPParseException;
import org.apache.felix.sigil.common.osgi.LDAPParser;
import org.apache.felix.sigil.common.osgi.SimpleTerm;
import org.apache.felix.sigil.common.osgi.VersionRange;
import org.apache.felix.sigil.model.ModelElementFactory;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.model.osgi.IBundleModelElement;
import org.apache.felix.sigil.model.osgi.IPackageExport;
import org.apache.felix.sigil.model.osgi.IPackageImport;
import org.apache.felix.sigil.model.osgi.IRequiredBundle;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.osgi.framework.Version;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;


final class OBRHandler extends DefaultHandler
{
    private static final String PACKAGE = "package";
    private static final String URI = "uri";
    private static final String PRESENTATION_NAME = "presentationname";
    private static final String VERSION = "version";
    private static final String SYMBOLIC_NAME = "symbolicname";
    private final File cacheDir;
    private final URL obrURL;
    private final OBRListener listener;

    private HashSet<String> sanity = new HashSet<String>();
    private Locator locator;
    private ISigilBundle bundle;
    private IPackageExport export;
    private int depth;


    public OBRHandler( URL obrURL, File bundleCache, OBRListener listener )
    {
        this.obrURL = obrURL;
        this.cacheDir = bundleCache;
        this.listener = listener;
    }


    public void setDocumentLocator( Locator locator )
    {
        this.locator = locator;
    }


    public void startElement( String uri, String localName, String qName, Attributes attributes ) throws SAXException
    {
        if ( depth++ == 0 && !"repository".equals( qName ) ) {
            throw new SAXParseException("Invalid OBR document, expected repository top level element", locator);
        }
        else if ( "resource".equals( qName ) )
        {
            startResource( attributes );
        }
        else if ( "capability".equals( qName ) )
        {
            startCapability( attributes );
        }
        else if ( "require".equals( qName ) )
        {
            startRequire( attributes );
        }
        else if ( "p".equals( qName ) )
        {
            startProperty( attributes );
        }
    }


    public void endElement( String uri, String localName, String qName ) throws SAXException
    {
        depth--;
        if ( "resource".equals( qName ) )
        {
            endResource();
        }
        else if ( "capability".equals( qName ) )
        {
            endCapability();
        }
        else if ( "require".equals( qName ) )
        {
            endRequire();
        }
        else if ( "p".equals( qName ) )
        {
            endProperty();
        }
    }


    private void startResource( Attributes attributes ) throws SAXException
    {
        try
        {
            String uri = attributes.getValue( "", URI );
            if ( uri.endsWith( ".jar" ) )
            {
                if ( !sanity.add( uri ) )
                {
                    throw new RuntimeException( uri );
                }
                ISigilBundle b = ModelElementFactory.getInstance().newModelElement( ISigilBundle.class );
                IBundleModelElement info = ModelElementFactory.getInstance()
                    .newModelElement( IBundleModelElement.class );
                info.setSymbolicName( attributes.getValue( "", SYMBOLIC_NAME ) );
                info.setVersion( new Version( attributes.getValue( "", VERSION ) ) );
                info.setName( attributes.getValue( "", PRESENTATION_NAME ) );
                URI l = makeAbsolute( uri );
                info.setUpdateLocation( l );
                if ( "file".equals(  l.getScheme() ) ) {
                    b.setLocation( new Path( new File( l ).getAbsolutePath() ) );
                }
                else {
                    b.setLocation( cachePath( info ) );
                }
                b.setBundleInfo( info );
                bundle = b;
            }
        }
        catch ( Exception e )
        {
            throw new SAXParseException( "Failed to build bundle info", locator, e );
        }
    }


    private URI makeAbsolute( String uri ) throws URISyntaxException
    {
        URI l = new URI( uri );
        if ( !l.isAbsolute() )
        {
            String base = obrURL.toExternalForm();
            int i = base.lastIndexOf( "/" );
            if ( i != -1 )
            {
                base = base.substring( 0, i );
                l = new URI( base + ( uri.startsWith( "/" ) ? "" : "/" ) + uri );
            }
        }
        return l;
    }


    private IPath cachePath( IBundleModelElement info )
    {
        return new Path( cacheDir.getAbsolutePath() )
            .append( info.getSymbolicName() + "_" + info.getVersion() + ".jar" );
    }


    private void startCapability( Attributes attributes )
    {
        if ( bundle != null )
        {
            if ( PACKAGE.equals( attributes.getValue( "", "name" ) ) )
            {
                export = ModelElementFactory.getInstance().newModelElement( IPackageExport.class );
            }
        }
    }


    private void startRequire( Attributes attributes ) throws SAXParseException
    {
        if ( bundle != null )
        {
            String name = attributes.getValue( "name" );
            if ( PACKAGE.equals( name ) )
            {
                IPackageImport pi = ModelElementFactory.getInstance().newModelElement( IPackageImport.class );
                try
                {
                    LDAPExpr expr = LDAPParser.parseExpression( attributes.getValue( "filter" ) );
                    pi.setPackageName( decodePackage( expr, locator ) );
                    pi.setVersions( decodeVersions( expr, locator ) );
                    pi.setOptional( Boolean.valueOf( attributes.getValue( "optional" ) ) );
                    bundle.getBundleInfo().addImport( pi );
                }
                catch ( LDAPParseException e )
                {
                    throw new SAXParseException( "Failed to parse filter", locator, e );
                }
            }
            else if ( "bundle".equals( name ) )
            {
                IRequiredBundle b = ModelElementFactory.getInstance().newModelElement( IRequiredBundle.class );
                try
                {
                    LDAPExpr expr = LDAPParser.parseExpression( attributes.getValue( "filter" ) );
                    b.setSymbolicName( decodeSymbolicName( expr, locator ) );
                    b.setVersions( decodeVersions( expr, locator ) );
                    b.setOptional( Boolean.valueOf( attributes.getValue( "optional" ) ) );
                    bundle.getBundleInfo().addRequiredBundle( b );
                }
                catch ( Exception e )
                {
                    System.err.println( "Failed to parse filter in bundle " + bundle.getBundleInfo().getSymbolicName() );
                    throw new SAXParseException( "Failed to parse filter in bundle "
                        + bundle.getBundleInfo().getSymbolicName(), locator, e );
                }
            }
            //else if ( "ee".equals( name ) ) {
            // TODO ignore for now...
            //}
            //else if ( "service".equals( name ) ) {
            // TODO ignore for now...
            //}
            //else {
            //	for ( int i = 0; i < attributes.getLength(); i++ ) {
            //		System.out.println( "Found requirement " + attributes.getValue(i) );				
            //	}
            //}
        }
    }


    private static VersionRange decodeVersions( LDAPExpr expr, Locator locator ) throws SAXParseException
    {
        try
        {
            return VersionRangeHelper.decodeVersions( expr );
        }
        catch ( NumberFormatException e )
        {
            throw new SAXParseException( e.getMessage(), locator );
        }
    }


    private void startProperty( Attributes attributes )
    {
        if ( export != null )
        {
            String name = attributes.getValue( "", "n" );
            String value = attributes.getValue( "", "v" );
            if ( PACKAGE.equals( name ) )
            {
                export.setPackageName( value );
            }
            else if ( "uses".equals( name ) )
            {
                String[] split = value.split( "," );
                export.setUses( Arrays.asList( split ) );
            }
            else if ( "version".equals( name ) )
            {
                export.setVersion( new Version( value ) );
            }
        }
    }


    private void endResource()
    {
        if ( bundle != null )
        {
            listener.handleBundle( bundle );
            bundle = null;
        }
    }


    private void endCapability()
    {
        if ( bundle != null )
        {
            if ( export != null )
            {
                bundle.getBundleInfo().addExport( export );
                export = null;
            }
        }
    }


    private void endRequire()
    {
        // TODO Auto-generated method stub

    }


    private void endProperty()
    {
        // TODO Auto-generated method stub

    }


    private static String decodePackage( LDAPExpr expr, Locator locator ) throws SAXParseException
    {
        ArrayList<SimpleTerm> terms = new ArrayList<SimpleTerm>( 1 );

        findTerms( "package", expr, terms );

        if ( terms.isEmpty() )
        {
            throw new SAXParseException( "Missing name filter in " + expr, locator );
        }

        return terms.get( 0 ).getRval();
    }


    private static String decodeSymbolicName( LDAPExpr expr, Locator locator ) throws SAXParseException
    {
        ArrayList<SimpleTerm> terms = new ArrayList<SimpleTerm>( 1 );

        findTerms( "symbolicname", expr, terms );

        if ( terms.isEmpty() )
        {
            throw new SAXParseException( "Missing name filter in " + expr, locator );
        }

        return terms.get( 0 ).getRval();
    }


    private static void findTerms( String string, LDAPExpr expr, List<SimpleTerm> terms ) throws SAXParseException
    {
        if ( expr instanceof SimpleTerm )
        {
            SimpleTerm term = ( SimpleTerm ) expr;
            if ( term.getName().equals( string ) )
            {
                terms.add( term );
            }
        }
        else
        {
            for ( LDAPExpr c : expr.getChildren() )
            {
                findTerms( string, c, terms );
            }
        }
    }
}
