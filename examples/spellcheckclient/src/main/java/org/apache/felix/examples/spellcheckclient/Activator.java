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
package org.apache.felix.examples.spellcheckclient;


import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.felix.examples.spellcheckservice.SpellCheckService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;


/**
 * 
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Activator implements BundleActivator, ServiceListener
{
    // Bundle's context.
    private BundleContext m_context = null;

    // The service reference being used.
    private ServiceReference m_ref = null;

    // The service object being used.
    private SpellCheckService m_checker = null;


    /**
     * Implements BundleActivator.start(). Adds itself as a listener for service
     * events, then queries for all available spell check services. If none are
     * found it goes into its normal "passage checking loop" and waits for a
     * spell check service to arrive. Once it has a spell check service it reads
     * passages from standard input and checks their spelling using the spell
     * check service. (NOTE: It is very bad practice to use the calling thread
     * to perform a lengthy process like this; this is only done for the purpose
     * of the tutorial.)
     * 
     * @param context the framework context for the bundle.
     */
    public void start( BundleContext context ) throws Exception
    {
        m_context = context;

        // Listen for events pertaining to dictionary services.
        m_context.addServiceListener( this, "(objectClass=" + SpellCheckService.class.getName() + ")" );

        // Query for a spell check service.
        m_ref = m_context.getServiceReference( SpellCheckService.class.getName() );

        // If we found a spell check service, then get
        // a reference so we can use it.
        if ( m_ref != null )
        {
            m_checker = ( SpellCheckService ) m_context.getService( m_ref );
        }

        try
        {
            System.out.println( "Enter a blank line to exit." );
            String passage = "";
            BufferedReader in = new BufferedReader( new InputStreamReader( System.in ) );

            // Loop endlessly.
            while ( true )
            {
                // Ask the user to enter a passage.
                System.out.print( "Enter passage: " );
                passage = in.readLine();

                // If the user entered a blank line, then
                // exit the loop.
                if ( passage.length() == 0 )
                {
                    break;
                }
                // If there is no spell checker, then say so.
                else if ( m_checker == null )
                {
                    System.out.println( "No spell checker available." );
                }
                // Otherwise check passage and print misspelled words.
                else
                {
                    String[] errors = m_checker.check( passage );

                    if ( errors == null )
                    {
                        System.out.println( "Passage is correct." );
                    }
                    else
                    {
                        System.out.println( "Incorrect word(s):" );
                        for ( int i = 0; i < errors.length; i++ )
                        {
                            System.out.println( "    " + errors[i] );
                        }
                    }
                }
            }
        }
        catch ( Exception ex )
        {
        }
    }


    /**
     * Implements BundleActivator.stop(). Does nothing since the framework will
     * automatically unget any used services.
     * 
     * @param context the framework context for the bundle.
     */
    public void stop( BundleContext context )
    {
        // NOTE: The service is automatically released.
    }


    /**
     * Implements ServiceListener.serviceChanged(). Checks to see if the service
     * we are using is leaving or tries to get a service if we need one.
     * 
     * @param event the fired service event.
     */
    public void serviceChanged( ServiceEvent event )
    {
        // If a spell check service was registered, see if we
        // need one. If so, get a reference to it.
        if ( event.getType() == ServiceEvent.REGISTERED )
        {
            if ( m_ref == null )
            {
                // Get a reference to the service object.
                m_ref = event.getServiceReference();
                m_checker = ( SpellCheckService ) m_context.getService( m_ref );
            }
        }
        // If a spell check service was unregistered, see if it
        // was the one we were using. If so, unget the service
        // and try to query to get another one.
        else if ( event.getType() == ServiceEvent.UNREGISTERING )
        {
            if ( event.getServiceReference() == m_ref )
            {
                // Unget service object and null references.
                m_context.ungetService( m_ref );
                m_ref = null;
                m_checker = null;

                // Query to see if we can get another service.
                m_ref = m_context.getServiceReference( SpellCheckService.class.getName() );
                if ( m_ref != null )
                {
                    // Get a reference to the service object.
                    m_checker = ( SpellCheckService ) m_context.getService( m_ref );
                }
            }
        }
    }
}
