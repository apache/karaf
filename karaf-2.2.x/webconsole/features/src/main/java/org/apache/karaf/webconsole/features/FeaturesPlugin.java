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
package org.apache.karaf.webconsole.features;


import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.json.JSONException;
import org.json.JSONWriter;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The <code>FeaturesPlugin</code>
 */
public class FeaturesPlugin extends AbstractWebConsolePlugin
{

    /** Pseudo class version ID to keep the IDE quite. */
    private static final long serialVersionUID = 1L;

    private final Logger log = LoggerFactory.getLogger(FeaturesPlugin.class);

    public static final String NAME = "features";

    public static final String LABEL = "Features";

    private ClassLoader classLoader;

    private String featuresJs = "/features/res/ui/features.js";

    private FeaturesService featuresService;
    
    private BundleContext bundleContext;


    //
    // Blueprint lifecycle callback methods
    //
    
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
        if (url != null) {
            InputStream ins = null;
            try {
                ins = url.openStream();
                if (ins == null) {
                    this.log.error("failed to open " + url);
                    url = null;
                }
            } catch (IOException e) {
                this.log.error(e.getMessage(), e);
                url = null;
            } finally {
                if (ins != null) {
                    try {
                        ins.close();
                    } catch (IOException e) {
                        this.log.error(e.getMessage(), e);
                    }
                }
            }
        }

        return url;
    }


    private boolean installFeature(String feature, String version) {
        boolean success = false;
        if ( featuresService == null )
        {
            this.log.error( "Shell Features service is unavailable." );
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
            this.log.error( "Shell Features service is unavailable." );
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
            this.log.error( "Shell Features service is unavailable." );
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
            this.log.error( "Shell Features service is unavailable." );
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
            this.log.error( "Shell Features service is unavailable." );
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
        final List<Repository> repositories = this.getRepositories();
        final List<ExtendedFeature> features = this.getFeatures( repositories );
        final String statusLine = this.getStatusLine( features );

        final JSONWriter jw = new JSONWriter( pw );

        try
        {
            jw.object();

            jw.key( "status" );
            jw.value( statusLine );

            jw.key( "repositories" );
            jw.array();
            for ( Repository r : repositories )
            {
                jw.object();
                jw.key( "name" );
                jw.value( r.getName() );
                jw.key( "url" );
                String uri = r.getURI().toString();
                jw.value( uri );
                jw.key( "actions" );
                jw.array();
                boolean enable = true;
                if ( uri.startsWith( "bundle" ) ) {
                    enable = false;
                }
                action( jw, enable, "refreshRepository", "Refresh", "refresh" );
                action( jw, enable, "removeRepository", "Remove", "delete" );
                jw.endArray();
                jw.endObject();
            }
            jw.endArray();

            jw.key( "features" );
            jw.array();
            for ( ExtendedFeature f : features )
            {
                featureInfo( jw, f );
            }
            jw.endArray();

            jw.endObject();

        }
        catch ( JSONException je )
        {
            throw new IOException( je.toString() );
        }

    }


    private List<Repository> getRepositories()
    {
        List<Repository> repositories = new ArrayList<Repository>();

        if ( featuresService == null )
        {
            this.log.error( "Shell Features service is unavailable." );
            return repositories;
        }

        try
        {
            for ( Repository r : featuresService.listRepositories() ) {
                repositories.add( r );
            }
        }
        catch ( Exception e )
        {
            this.log.error( e.getMessage() );
        }

        return repositories;
    }


    private List<ExtendedFeature> getFeatures( List<Repository> repositories )
    {
        List<ExtendedFeature> features = new ArrayList<ExtendedFeature>();

        if ( featuresService == null )
        {
            this.log.error( "Shell Features service is unavailable." );
            return features;
        }

        try
        {
            for ( Repository r : repositories )
            {
                for ( Feature f : r.getFeatures() )
                {
                    ExtendedFeature.State state =
                        featuresService.isInstalled(f) ? ExtendedFeature.State.INSTALLED : ExtendedFeature.State.UNINSTALLED;
                    features.add( new ExtendedFeature(  state, r.getName(), f ) );
                }
            }
        }
        catch ( Exception e )
        {
            this.log.error( e.getMessage() );
        }

        Collections.sort( features, new ExtendedFeatureComparator() );
        return features;
    }


    class ExtendedFeatureComparator implements Comparator<ExtendedFeature>
    {
        public int compare( ExtendedFeature o1, ExtendedFeature o2 )
        {
            return o1.getName().toLowerCase().compareTo( o2.getName().toLowerCase() );
        }
    }


    private String getStatusLine( final List<ExtendedFeature> features )
    {
        int installed = 0;
        for ( ExtendedFeature f : features )
        {
            if ( f.getState() == ExtendedFeature.State.INSTALLED )
            {
                installed++;
            }
        }
        final StringBuffer buffer = new StringBuffer();
        buffer.append( "Feature information: " );
        appendFeatureInfoCount( buffer, "in total", features.size() );
        if ( installed == features.size() )
        {
            buffer.append( " - all " );
            appendFeatureInfoCount( buffer, "active.", features.size() );
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


    private void featureInfo( JSONWriter jw, ExtendedFeature feature ) throws JSONException
    {
        jw.object();
        jw.key( "id" );
        jw.value( feature.getId() );
        jw.key( "name" );
        jw.value( feature.getName() );
        jw.key( "version" );
        jw.value( feature.getVersion() );
        jw.key( "repository" );
        jw.value( feature.getRepository() );
        jw.key( "state" );
        ExtendedFeature.State state = feature.getState();
        jw.value( state.toString() );

        jw.key( "actions" );
        jw.array();

        if ( state == ExtendedFeature.State.INSTALLED )
        {
            action( jw, true, "uninstallFeature", "Uninstall", "delete" );
        }
        else if ( state == ExtendedFeature.State.UNINSTALLED )
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


    //
    // Dependency Injection setters
    //

    public void setFeaturesService(FeaturesService featuresService) 
    {
        this.featuresService = featuresService;
    }


    public void setBundleContext(BundleContext bundleContext) 
    {
        this.bundleContext = bundleContext;
    }
}
