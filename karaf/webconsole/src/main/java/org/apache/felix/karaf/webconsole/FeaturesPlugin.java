/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.karaf.webconsole;


import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.felix.karaf.gshell.features.FeaturesService;
import org.apache.felix.karaf.gshell.features.Repository;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;

import org.json.JSONException;
import org.json.JSONWriter;

import org.osgi.framework.BundleContext;


/**
 * The <code>FeaturesPlugin</code>
 */
public class FeaturesPlugin extends AbstractWebConsolePlugin
{

    /** Pseudo class version ID to keep the IDE quite. */
    private static final long serialVersionUID = 1L;

    public static final String NAME = "features";

    public static final String LABEL = "Features";

    private Log log = LogFactory.getLog(FeaturesPlugin.class);

    private ClassLoader classLoader;

    private String featuresJs = "/features/res/ui/features.js";

    private FeaturesService featuresService;
    
    private BundleContext bundleContext;


    /*
     * Blueprint lifecycle callback methods
     */
    
    public void start()
    {
        super.activate( bundleContext );

        this.classLoader = this.getClass().getClassLoader();

        this.log.info( LABEL + " plugin activated" );
    }

    public void stop()
    {
        this.log.info( LABEL + " plugin deactivated" );
        super.deactivate();
    }

    //
    // AbstractWebConsolePlugin interface
    //    
    public String getLabel()
    {
        return NAME;
    }


    public String getTitle()
    {
        return LABEL;
    }


    protected void doPost( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException
    {
        boolean success = false;

        final String action = req.getParameter( "action" );
        final String feature = req.getParameter( "feature" );
        final String version = req.getParameter( "version" );
        final String url = req.getParameter( "url" );

        if ( action == null )
        {
            success = true;
        }
        else if ( "installFeature".equals( action ) )
        {
            success = this.installFeature(feature, version);
        }
        else if ( "uninstallFeature".equals( action ) )
        {
            success = this.uninstallFeature( feature, version );
        }
        else if ( "refreshRepository".equals( action ) )
        {
            success = this.refreshRepository( url );
        }
        else if ( "removeRepository".equals( action ) )
        {
            success = this.removeRepository( url );
        }
        else if ( "addRepository".equals( action ) )
        {
            success = this.addRepository( url );
        }

        if ( success )
        {
            // let's wait a little bit to give the framework time
            // to process our request
            try
            {
                Thread.sleep( 800 );
            }
            catch ( InterruptedException e )
            {
                // we ignore this
            }
            this.renderJSON( resp, null );
        }
        else
        {
            super.doPost( req, resp );
        }
    }


    protected void renderContent( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {

        // get request info from request attribute
        final PrintWriter pw = response.getWriter();

        String appRoot = ( String ) request
            .getAttribute( "org.apache.felix.webconsole.internal.servlet.OsgiManager.appRoot" );
        final String featuresScriptTag = "<script src='" + appRoot + this.featuresJs
            + "' language='JavaScript'></script>";
        pw.println( featuresScriptTag );

        pw.println( "<script type='text/javascript'>" );
        pw.println( "// <![CDATA[" );
        pw.println( "var imgRoot = '" + appRoot + "/res/imgs';" );
        pw.println( "// ]]>" );
        pw.println( "</script>" );

        pw.println( "<div id='plugin_content'/>" );

        pw.println( "<script type='text/javascript'>" );
        pw.println( "// <![CDATA[" );
        pw.print( "renderFeatures( " );
        writeJSON( pw );
        pw.println( " )" );
        pw.println( "// ]]>" );
        pw.println( "</script>" );
    }


    //
    // Additional methods
    //

    protected URL getResource( String path )
    {
        path = path.substring( NAME.length() + 1 );
        URL url = this.classLoader.getResource( path );
        try
        {
            InputStream ins = url.openStream();
            if ( ins == null )
            {
                this.log.error( "failed to open " + url );
            }
        }
        catch ( IOException e )
        {
            this.log.error( e.getMessage(), e );
        }
        return url;
    }


    private boolean installFeature(String feature, String version) {
        boolean success = false;
        if ( featuresService == null )
        {
            this.log.error( "GShell Features service is unavailable." );
        }
        try
        {
            featuresService.installFeature( feature, version );
            success = true;
        }
        catch ( Exception e )
        {
            this.log.error( "failed to install feature: ", e );
        }
        return success;
    }


    private boolean uninstallFeature(String feature, String version) {
        boolean success = false;
        if ( featuresService == null )
        {
            this.log.error( "GShell Features service is unavailable." );
        }
        try
        {
            featuresService.uninstallFeature( feature, version );
            success = true;
        }
        catch ( Exception e )
        {
            this.log.error( "failed to install feature: ", e );
        }
        return success;
    }


    private boolean removeRepository(String url) {
        boolean success = false;
        if ( featuresService == null )
        {
            this.log.error( "GShell Features service is unavailable." );
        }
        try
        {
            featuresService.removeRepository( new URI( url ) );
            success = true;
        }
        catch ( Exception e )
        {
            this.log.error( "failed to install feature: ", e );
        }
        return success;
    }


    private boolean refreshRepository(String url) {
        boolean success = false;
        if ( featuresService == null )
        {
            this.log.error( "GShell Features service is unavailable." );
        }
        try
        {
            featuresService.removeRepository( new URI( url ) );
            featuresService.addRepository( new URI( url ) );
            success = true;
        }
        catch ( Exception e )
        {
            this.log.error( "failed to install feature: ", e );
        }
        return success;
    }


    private boolean addRepository(String url) {
        boolean success = false;
        if ( featuresService == null )
        {
            this.log.error( "GShell Features service is unavailable." );
        }
        try
        {
            featuresService.addRepository( new URI( url ) );
            success = true;
        }
        catch ( Exception e )
        {
            this.log.error( "failed to install feature: ", e );
        }
        return success;
    }


    private void renderJSON( final HttpServletResponse response, final String feature ) throws IOException
    {
        response.setContentType( "application/json" );
        response.setCharacterEncoding( "UTF-8" );

        final PrintWriter pw = response.getWriter();
        writeJSON( pw );
    }


    private void writeJSON( final PrintWriter pw ) throws IOException
    {
        final Feature[] features = this.getFeatures();
        final String statusLine = this.getStatusLine( features );
        final String[] repositories = this.getRepositories();

        final JSONWriter jw = new JSONWriter( pw );

        try
        {
            jw.object();

            jw.key( "status" );
            jw.value( statusLine );

            jw.key( "features" );
            jw.array();
            for ( int i = 0; i < features.length; i++ )
            {
                featureInfo( jw, features[i] );
            }
            jw.endArray();

            jw.key( "repositories" );
            jw.array();
            for ( int i = 0; i < repositories.length; i++ )
            {
                jw.object();
                jw.key( "url" );
                jw.value( repositories[i] );
                jw.key( "actions" );
                jw.array();
                action( jw, true, "refreshRepository", "Refresh", "refresh" );
                action( jw, true, "removeRepository", "Uninstall", "delete" );
                jw.endArray();
                jw.endObject();
            }
            jw.endArray();

            jw.endObject();

        }
        catch ( JSONException je )
        {
            throw new IOException( je.toString() );
        }

    }


    private String[] getRepositories()
    {
        String[] repositories = new String[0];

        if ( featuresService == null )
        {
            this.log.error( "GShell Features service is unavailable." );
            return repositories;
        }

        Repository[] repositoryInfo = null;
        try
        {
            repositoryInfo = featuresService.listRepositories();
        }
        catch ( Exception e )
        {
            this.log.error( e.getMessage() );
            return new String[0];
        }

        repositories = new String[repositoryInfo.length];
        for ( int i = 0; i < repositoryInfo.length; i++ )
        {
            repositories[i] = repositoryInfo[i].getURI().toString();
        }
        return repositories;
    }


    private Feature[] getFeatures()
    {
        Feature[] features = new Feature[0];

        if ( featuresService == null )
        {
            this.log.error( "GShell Features service is unavailable." );
            return features;
        }

        String[] featureInfo = null;
        try
        {
            featureInfo = featuresService.listFeatures();
        }
        catch ( Exception e )
        {
            this.log.error( e.getMessage() );
            return new Feature[0];
        }

        features = new Feature[featureInfo.length];
        for ( int i = 0; i < featureInfo.length; i++ )
        {
            String[] temp;
            temp = getBracketedToken( featureInfo[i], 0 );
            Feature.State state;
            if ( "installed  ".equals( temp[0] ) )
            {
                state = Feature.State.INSTALLED;
            }
            else if ( "uninstalled".equals( temp[0] ) )
            {
                state = Feature.State.UNINSTALLED;
            }
            else
            {
                state = Feature.State.UNKNOWN;
            }
            temp = getBracketedToken( temp[1], 0 );
            String version = temp[0];
            features[i] = new Feature( temp[1].trim(), version, state );
        }
        Arrays.sort( features, new FeatureComparator() );
        return features;
    }

    private String[] getBracketedToken( String str, int startIndex )
    {
        int start = str.indexOf( '[', startIndex ) + 1;
        int end = str.indexOf( ']', start );
        String token = str.substring( start, end );
        String remainder = str.substring( end + 1 );
        return new String[]
            { token, remainder };
    }


    class FeatureComparator implements Comparator<Feature>
    {
        public int compare( Feature o1, Feature o2 )
        {
            return o1.name.toLowerCase().compareTo( o2.name.toLowerCase() );
        }
    }


    private String getStatusLine( final Feature[] features )
    {
        int installed = 0;
        for ( int i = 0; i < features.length; i++ )
        {
            if ( features[i].state == Feature.State.INSTALLED )
            {
                installed++;
            }
        }
        final StringBuffer buffer = new StringBuffer();
        buffer.append( "Feature information: " );
        appendFeatureInfoCount( buffer, "in total", features.length );
        if ( installed == features.length )
        {
            buffer.append( " - all " );
            appendFeatureInfoCount( buffer, "active.", features.length );
        }
        else
        {
            if ( installed != 0 )
            {
                buffer.append( ", " );
                appendFeatureInfoCount( buffer, "installed", installed );
            }
            buffer.append( '.' );
        }
        return buffer.toString();
    }


    private void appendFeatureInfoCount( final StringBuffer buf, String msg, int count )
    {
        buf.append( count );
        buf.append( " feature" );
        if ( count != 1 )
            buf.append( 's' );
        buf.append( ' ' );
        buf.append( msg );
    }


    private void featureInfo( JSONWriter jw, Feature feature ) throws JSONException
    {
        jw.object();
        jw.key( "name" );
        jw.value( feature.name );
        jw.key( "version" );
        jw.value( feature.version );
        jw.key( "state" );
        jw.value( feature.state );

        jw.key( "actions" );
        jw.array();

        if ( feature.state == Feature.State.INSTALLED )
        {
            action( jw, true, "uninstallFeature", "Uninstall", "delete" );
        }
        else
        {
            action( jw, true, "installFeature", "Install", "start" );
        }
        jw.endArray();

        jw.endObject();
    }


    private void action( JSONWriter jw, boolean enabled, String op, String title, String image ) throws JSONException
    {
        jw.object();
        jw.key( "enabled" ).value( enabled );
        jw.key( "op" ).value( op );
        jw.key( "title" ).value( title );
        jw.key( "image" ).value( image );
        jw.endObject();
    }
    
    // DI setters
    public void setFeaturesService(FeaturesService featuresService) 
    {
        this.featuresService = featuresService;
    }
    
    public void setBundleContext(BundleContext bundleContext) 
    {
        this.bundleContext = bundleContext;
    }
}
