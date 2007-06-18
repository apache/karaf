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
package org.apache.felix.examples.dictionaryclient2;


import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.felix.examples.dictionaryservice.DictionaryService;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;


/**
 * This class implements a bundle that uses a dictionary service to check for
 * the proper spelling of a word by checking for its existence in the
 * dictionary. This bundle is more complex than the bundle in Example 3 because
 * it monitors the dynamic availability of the dictionary services. In other
 * words, if the service it is using departs, then it stops using it gracefully,
 * or if it needs a service and one arrives, then it starts using it
 * automatically. As before, the bundle uses the first service that it finds and
 * uses the calling thread of the start() method to read words from standard
 * input. You can stop checking words by entering an empty line, but to start
 * checking words again you must stop and then restart the bundle.
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
    private DictionaryService m_dictionary = null;


    /**
     * Implements BundleActivator.start(). Adds itself as a listener for service
     * events, then queries for available dictionary services. If any
     * dictionaries are found it gets a reference to the first one available and
     * then starts its "word checking loop". If no dictionaries are found, then
     * it just goes directly into its "word checking loop", but it will not be
     * able to check any words until a dictionary service arrives; any arriving
     * dictionary service will be automatically used by the client if a
     * dictionary is not already in use. Once it has dictionary, it reads words
     * from standard input and checks for their existence in the dictionary that
     * it is using. (NOTE: It is very bad practice to use the calling thread to
     * perform a lengthy process like this; this is only done for the purpose of
     * the tutorial.)
     * 
     * @param context the framework context for the bundle.
     */
    public void start( BundleContext context ) throws Exception
    {
        m_context = context;

        // Listen for events pertaining to dictionary services.
        m_context.addServiceListener( this, "(&(objectClass=" + DictionaryService.class.getName() + ")"
            + "(Language=*))" );

        // Query for any service references matching any language.
        ServiceReference[] refs = m_context.getServiceReferences( DictionaryService.class.getName(), "(Language=*)" );

        // If we found any dictionary services, then just get
        // a reference to the first one so we can use it.
        if ( refs != null )
        {
            m_ref = refs[0];
            m_dictionary = ( DictionaryService ) m_context.getService( m_ref );
        }

        try
        {
            System.out.println( "Enter a blank line to exit." );
            String word = "";
            BufferedReader in = new BufferedReader( new InputStreamReader( System.in ) );

            // Loop endlessly.
            while ( true )
            {
                // Ask the user to enter a word.
                System.out.print( "Enter word: " );
                word = in.readLine();

                // If the user entered a blank line, then
                // exit the loop.
                if ( word.length() == 0 )
                {
                    break;
                }
                // If there is no dictionary, then say so.
                else if ( m_dictionary == null )
                {
                    System.out.println( "No dictionary available." );
                }
                // Otherwise print whether the word is correct or not.
                else if ( m_dictionary.checkWord( word ) )
                {
                    System.out.println( "Correct." );
                }
                else
                {
                    System.out.println( "Incorrect." );
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
     * @param event
     *            the fired service event.
     */
    public void serviceChanged( ServiceEvent event )
    {
        // If a dictionary service was registered, see if we
        // need one. If so, get a reference to it.
        if ( event.getType() == ServiceEvent.REGISTERED )
        {
            if ( m_ref == null )
            {
                // Get a reference to the service object.
                m_ref = event.getServiceReference();
                m_dictionary = ( DictionaryService ) m_context.getService( m_ref );
            }
        }
        // If a dictionary service was unregistered, see if it
        // was the one we were using. If so, unget the service
        // and try to query to get another one.
        else if ( event.getType() == ServiceEvent.UNREGISTERING )
        {
            if ( event.getServiceReference() == m_ref )
            {
                // Unget service object and null references.
                m_context.ungetService( m_ref );
                m_ref = null;
                m_dictionary = null;

                // Query to see if we can get another service.
                ServiceReference[] refs = null;
                
                try
                {
                    refs = m_context.getServiceReferences( DictionaryService.class.getName(), "(Language=*)" );
                }
                catch ( InvalidSyntaxException e )
                {
                    e.printStackTrace();
                }

                if ( refs != null )
                {
                    // Get a reference to the first service object.
                    m_ref = refs[0];
                    m_dictionary = ( DictionaryService ) m_context.getService( m_ref );
                }
            }
        }
    }
}
