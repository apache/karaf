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
package org.apache.felix.webconsole.internal.misc;


import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.internal.BaseWebConsolePlugin;
import org.apache.felix.webconsole.internal.Util;
import org.apache.felix.webconsole.internal.servlet.OsgiManager;
import org.json.JSONException;
import org.json.JSONWriter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;


public class EventAdminServlet extends BaseWebConsolePlugin implements EventHandler
{

    public static final String LABEL = "events";

    public static final String TITLE = "Event Admin";

    /** Number of events to be displayed. */
    private int maxSize = 50;

    private final List events = new ArrayList();

    /** Custom event renderers hashed by topic. */
    private final Map eventRenderers = new HashMap();

    // the service registration of this plugin
    private ServiceRegistration eventReceiver;


    public EventAdminServlet()
    {
        eventRenderers.put( ServiceEvent.class.getName().replace( '.', '/' ) + "/", new ServiceEventInfoProvider() );
        eventRenderers.put( BundleEvent.class.getName().replace( '.', '/' ) + "/", new BundleEventInfoProvider() );
    }


    public String getLabel()
    {
        return LABEL;
    }


    public String getTitle()
    {
        return TITLE;
    }


    /**
     * Activate this component.
     */
    public void activate( BundleContext context )
    {
        super.activate( context );

        this.events.clear();

        // register as EventHandler service to receive events
        Dictionary props = new Hashtable();
        props.put( Constants.SERVICE_DESCRIPTION, "EventAdmin plugin for the Felix Web Console" );
        props.put( Constants.SERVICE_VENDOR, "The Apache Software Foundation" );
        props.put( "event.topics", "*" );
        eventReceiver = context.registerService( EventHandler.class.getName(), this, props );
    }


    /**
     * Deactivate this component.
     */
    public void deactivate()
    {
        if ( eventReceiver != null )
        {
            eventReceiver.unregister();
            eventReceiver = null;
        }

        this.events.clear();

        super.deactivate();
    }


    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    public void handleEvent( Event event )
    {
        // we add everything which is not a log event
        if ( !event.getTopic().startsWith( "org/osgi/service/log" ) )
        {
            synchronized ( this.events )
            {
                this.events.add( new EventInfo( event ) );
                if ( events.size() > this.maxSize )
                {
                    events.remove( 0 );
                }
            }
        }
    }


    protected void doPost( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException
    {
        final String action = getParameter( req, "action" );
        // for now we only have the clear action
        if ( "clear".equals( action ) )
        {
            synchronized ( this.events )
            {
                this.events.clear();
            }
        }
        // we always send back the json data
        resp.setContentType( "application/json" );
        resp.setCharacterEncoding( "utf-8" );

        renderJSON( resp.getWriter() );
    }


    private void renderJSON( final PrintWriter pw ) throws IOException
    {
        List copiedEvents;
        synchronized ( this.events )
        {
            copiedEvents = new ArrayList( this.events );
        }
        // create status line
        final EventAdmin admin = ( EventAdmin ) this.getService( EventAdmin.class.getName() );
        StringBuffer statusLine = new StringBuffer();
        if ( admin == null )
        {
            statusLine.append( "Event Admin is not installed/running." );
        }
        else
        {
            statusLine.append( "Event Admin is running." );
        }
        statusLine.append( " " );
        statusLine.append( copiedEvents.size() );
        statusLine.append( " Events received" );
        if ( !copiedEvents.isEmpty() )
        {
            statusLine.append( " since " );
            Date d = new Date();
            d.setTime( ( ( EventInfo ) copiedEvents.get( 0 ) ).received );
            statusLine.append( d );
        }
        statusLine.append( "." );

        JSONWriter jw = new JSONWriter( pw );
        try
        {
            jw.object();

            jw.key( "status" );
            jw.value( statusLine );

            jw.key( "data" );

            jw.array();

            // display list in reverse order
            for ( int index = copiedEvents.size() - 1; index >= 0; index-- )
            {
                eventJson( jw, ( EventInfo ) copiedEvents.get( index ), index );
            }

            jw.endArray();

            jw.endObject();

        }
        catch ( JSONException je )
        {
            throw new IOException( je.toString() );
        }

    }


    protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
        IOException
    {

        final String info = request.getPathInfo();
        if ( info.endsWith( ".json" ) )
        {
            response.setContentType( "text/javascript" );
            response.setCharacterEncoding( "UTF-8" );

            PrintWriter pw = response.getWriter();
            this.renderJSON( pw );

            // nothing more to do
            return;
        }

        super.doGet( request, response );
    }


    protected void renderContent( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
        IOException
    {
        final PrintWriter pw = response.getWriter();

        String appRoot = ( String ) request.getAttribute( OsgiManager.ATTR_APP_ROOT );
        pw.println( "<script src='" + appRoot + "/res/ui/jquery-1.2.6.min.js' language='JavaScript'></script>" );
        pw.println( "<script src='" + appRoot
            + "/res/ui/jquery.tablesorter-2.0.3.min.js' language='JavaScript'></script>" );
        pw.println( "<script src='" + appRoot + "/res/ui/events.js' language='JavaScript'></script>" );

        Util.startScript( pw );
        pw.println( "renderEvents( );" );
        Util.endScript( pw );
    }


    private void eventJson( JSONWriter jw, EventInfo info, int index ) throws JSONException
    {
        final Event e = info.event;

        // check if we have an info provider
        final Iterator iter = this.eventRenderers.entrySet().iterator();
        String infoText = null;
        while ( infoText == null && iter.hasNext() )
        {
            final Map.Entry entry = ( Map.Entry ) iter.next();
            if ( e.getTopic().startsWith( entry.getKey().toString() ) )
            {
                infoText = ( ( EventInfoProvider ) entry.getValue() ).getInfo( e );
            }
        }

        jw.object();
        jw.key( "id" );
        jw.value( String.valueOf( index ) );
        jw.key( "received" );
        jw.value( info.received );
        jw.key( "topic" );
        jw.value( e.getTopic() );
        if ( infoText != null )
        {
            jw.key( "info" );
            jw.value( infoText );
        }
        jw.key( "properties" );
        jw.object();
        final String[] names = e.getPropertyNames();
        if ( names != null && names.length > 0 )
        {
            for ( int i = 0; i < names.length; i++ )
            {
                jw.key( names[i] );
                jw.value( e.getProperty( names[i] ).toString() );
            }
        }
        jw.endObject();

        jw.endObject();
    }

    private static final class EventInfo
    {

        public final Event event;
        public final long received;


        public EventInfo( final Event e )
        {
            this.event = e;
            this.received = System.currentTimeMillis();
        }
    }

    private static interface EventInfoProvider
    {
        String getInfo( Event event );
    }

    private static final class ServiceEventInfoProvider implements EventInfoProvider
    {

        /**
         * @see org.apache.felix.webconsole.internal.misc.EventAdminServlet.EventInfoProvider#getInfo(org.osgi.service.event.Event)
         */
        public String getInfo( Event event )
        {
            final ServiceEvent serviceEvent = ( ServiceEvent ) event.getProperty( EventConstants.EVENT );
            if ( serviceEvent == null )
            {
                return null;
            }
            final StringBuffer buffer = new StringBuffer( "Service " );
            buffer.append( serviceEvent.getServiceReference().getProperty( Constants.SERVICE_ID ) );
            buffer.append( ' ' );
            switch ( serviceEvent.getType() )
            {
                case ServiceEvent.REGISTERED:
                    buffer.append( "registered" );
                    break;
                case ServiceEvent.MODIFIED:
                    buffer.append( "modified" );
                    break;
                case ServiceEvent.UNREGISTERING:
                    buffer.append( "unregistering" );
                    break;
                default:
                    return null; // IGNOREE
            }

            return buffer.toString();
        }
    }

    private static final class BundleEventInfoProvider implements EventInfoProvider
    {

        /**
         * @see org.apache.felix.webconsole.internal.misc.EventAdminServlet.EventInfoProvider#getInfo(org.osgi.service.event.Event)
         */
        public String getInfo( Event event )
        {
            final BundleEvent bundleEvent = ( BundleEvent ) event.getProperty( EventConstants.EVENT );
            if ( bundleEvent == null )
            {
                return null;
            }
            final StringBuffer buffer = new StringBuffer( "Bundle " );
            buffer.append( bundleEvent.getBundle().getSymbolicName() );
            buffer.append( ' ' );
            switch ( bundleEvent.getType() )
            {
                case BundleEvent.INSTALLED:
                    buffer.append( "installed" );
                    break;
                case BundleEvent.RESOLVED:
                    buffer.append( "resolved" );
                    break;
                case BundleEvent.STARTED:
                    buffer.append( "started" );
                    break;
                case BundleEvent.STARTING:
                    buffer.append( "starting" );
                    break;
                case BundleEvent.STOPPED:
                    buffer.append( "stopped" );
                    break;
                case BundleEvent.STOPPING:
                    buffer.append( "stopping" );
                    break;
                case BundleEvent.UNINSTALLED:
                    buffer.append( "uninstalled" );
                    break;
                case BundleEvent.UNRESOLVED:
                    buffer.append( "unresolved" );
                    break;
                case BundleEvent.UPDATED:
                    buffer.append( "updated" );
                    break;
                default:
                    return null; // IGNOREE
            }

            return buffer.toString();
        }
    }
}
