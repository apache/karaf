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
package org.apache.felix.examples.spellcheckservice.impl;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.apache.felix.examples.dictionaryservice.DictionaryService;
import org.apache.felix.examples.spellcheckservice.SpellCheckService;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;


/**
 * This class implements a bundle that implements a spell check service. The
 * spell check service uses all available dictionary services to check for the
 * existence of words in a given sentence. This bundle not only monitors the
 * dynamic availability of dictionary services, but it manages the aggregation
 * of all available dictionary services as they arrive and depart. The spell
 * check service is only registered if there are dictionary services available,
 * thus the spell check service will appear and disappear as dictionary services
 * appear and disappear, respectively.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Activator implements BundleActivator, ServiceListener
{
    // Bundle's context.
    private BundleContext m_context = null;

    // List of available dictionary service references.
    private ArrayList m_refList = new ArrayList();

    // Maps service references to service objects.
    private HashMap m_refToObjMap = new HashMap();

    // The spell check service registration.
    private ServiceRegistration m_reg = null;


    /**
     * Implements BundleActivator.start(). Adds itself as a service listener and
     * queries for all currently available dictionary services. Any available
     * dictionary services are added to the service reference list. If
     * dictionary services are found, then the spell check service is
     * registered.
     * 
     * @param context the framework context for the bundle.
     */
    public void start( BundleContext context ) throws Exception
    {
        m_context = context;

        // Listen for events pertaining to dictionary services.
        m_context.addServiceListener( this, "(&(objectClass=" + DictionaryService.class.getName() + ")"
            + "(Language=*))" );

        // Query for all dictionary services.
        ServiceReference[] refs = m_context.getServiceReferences( DictionaryService.class.getName(), "(Language=*)" );

        // Add any dictionaries to the service reference list.
        if ( refs != null )
        {
            // Lock the list.
            synchronized ( m_refList )
            {
                for ( int i = 0; i < refs.length; i++ )
                {
                    // Get the service object.
                    Object service = m_context.getService( refs[i] );

                    // Make that the service is not being duplicated.
                    if ( ( service != null ) && ( m_refToObjMap.get( refs[i] ) == null ) )
                    {
                        // Add to the reference list.
                        m_refList.add( refs[i] );
                        // Map reference to service object for easy look up.
                        m_refToObjMap.put( refs[i], service );
                    }
                }

                // Register spell check service if there are any dictionary services.
                if ( m_refList.size() > 0 )
                {
                    m_reg = m_context.registerService( SpellCheckService.class.getName(), 
                        new SpellCheckServiceImpl(), null );
                }
            }
        }
    }


    /**
     * Implements BundleActivator.stop(). Does nothing since the framework will
     * automatically unregister any registered services, release any used
     * services, and remove any event listeners.
     * 
     * @param context
     *            the framework context for the bundle.
     */
    public void stop( BundleContext context )
    {
        // NOTE: The services automatically released.
    }


    /**
     * Implements ServiceListener.serviceChanged(). Monitors the arrival and
     * departure of dictionary services, adding and removing them from the
     * service reference list, respectively. In the case where no more
     * dictionary services are available, the spell check service is registered.
     * As soon as any dictionary spell check becomes available, the spell check
     * service is reregistered.
     * 
     * @param event the fired service event.
     */
    public void serviceChanged( ServiceEvent event )
    {
        // Add the new dictionary service to the service list.
        if ( event.getType() == ServiceEvent.REGISTERED )
        {
            synchronized ( m_refList )
            {
                // Get the service object.
                Object service = m_context.getService( event.getServiceReference() );

                // Make that the service is not being duplicated.
                if ( ( service != null ) && ( m_refToObjMap.get( event.getServiceReference() ) == null ) )
                {
                    // Add to the reference list.
                    m_refList.add( event.getServiceReference() );
                    // Map reference to service object for easy look up.
                    m_refToObjMap.put( event.getServiceReference(), service );

                    // Register spell check service if necessary.
                    if ( m_reg == null )
                    {
                        m_reg = m_context.registerService( SpellCheckService.class.getName(),
                            new SpellCheckServiceImpl(), null );
                    }
                }
                else if ( service != null )
                {
                    m_context.ungetService( event.getServiceReference() );
                }
            }
        }
        // Remove the departing service from the service list.
        else if ( event.getType() == ServiceEvent.UNREGISTERING )
        {
            synchronized ( m_refList )
            {
                // Make sure the service is in the list.
                if ( m_refToObjMap.get( event.getServiceReference() ) != null )
                {
                    // Unget the service object.
                    m_context.ungetService( event.getServiceReference() );
                    // Remove service reference.
                    m_refList.remove( event.getServiceReference() );
                    // Remove service reference from map.
                    m_refToObjMap.remove( event.getServiceReference() );

                    // If there are no more dictionary services,
                    // then unregister spell check service.
                    if ( m_refList.size() == 0 )
                    {
                        m_reg.unregister();
                        m_reg = null;
                    }
                }
            }
        }
    }

    
    /**
     * A private inner class that implements a spell check service; see
     * SpellCheckService for details of the service.
     */
    private class SpellCheckServiceImpl implements SpellCheckService
    {
        /**
         * Implements SpellCheckService.check(). Checks the given passage for
         * misspelled words.
         * 
         * @param passage
         *            the passage to spell check.
         * @return An array of misspelled words or null if no words are
         *         misspelled.
         */
        public String[] check( String passage )
        {
            // No misspelled words for an empty string.
            if ( ( passage == null ) || ( passage.length() == 0 ) )
            {
                return null;
            }

            ArrayList errorList = new ArrayList();

            // Tokenize the passage using spaces and punctionation.
            StringTokenizer st = new StringTokenizer( passage, " ,.!?;:" );

            // Lock the service list.
            synchronized ( m_refList )
            {
                // Loop through each word in the passage.
                while ( st.hasMoreTokens() )
                {
                    String word = st.nextToken();

                    boolean correct = false;

                    // Check each available dictionary for the current word.
                    for ( int i = 0; ( !correct ) && ( i < m_refList.size() ); i++ )
                    {
                        DictionaryService dictionary = ( DictionaryService ) m_refToObjMap.get( m_refList.get( i ) );

                        if ( dictionary.checkWord( word ) )
                        {
                            correct = true;
                        }
                    }

                    // If the word is not correct, then add it
                    // to the incorrect word list.
                    if ( !correct )
                    {
                        errorList.add( word );
                    }
                }
            }

            // Return null if no words are incorrect.
            if ( errorList.size() == 0 )
            {
                return null;
            }

            // Return the array of incorrect words.
            return ( String[] ) errorList.toArray( new String[errorList.size()] );
        }
    }
}
