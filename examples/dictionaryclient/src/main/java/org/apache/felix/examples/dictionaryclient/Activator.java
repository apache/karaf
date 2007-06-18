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
package org.apache.felix.examples.dictionaryclient;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.felix.examples.dictionaryservice.DictionaryService;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;


/**
 * This class implements a bundle that uses a dictionary service to check for
 * the proper spelling of a word by check for its existence in the dictionary.
 * This bundle uses the first service that it finds and does not monitor the
 * dynamic availability of the service (i.e., it does not listen for the arrival
 * or departure of dictionary services). When starting this bundle, the thread
 * calling the start() method is used to read words from standard input. You can
 * stop checking words by entering an empty line, but to start checking words
 * again you must stop and then restart the bundle.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Activator implements BundleActivator
{
    /**
     * Implements BundleActivator.start(). Queries for all available dictionary
     * services. If none are found it simply prints a message and returns,
     * otherwise it reads words from standard input and checks for their
     * existence from the first dictionary that it finds. (NOTE: It is very bad
     * practice to use the calling thread to perform a lengthy process like
     * this; this is only done for the purpose of the tutorial.)
     * 
     * @param context the framework context for the bundle.
     */
    public void start( BundleContext context ) throws Exception
    {
        // Query for all service references matching any language.
        ServiceReference[] refs = context.getServiceReferences( DictionaryService.class.getName(), "(Language=*)" );

        if ( refs != null )
        {
            try
            {
                System.out.println( "Enter a blank line to exit." );
                BufferedReader in = new BufferedReader( new InputStreamReader( System.in ) );
                String word = "";

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

                    // First, get a dictionary service and then check
                    // if the word is correct.
                    DictionaryService dictionary = ( DictionaryService ) context.getService( refs[0] );
                    if ( dictionary.checkWord( word ) )
                    {
                        System.out.println( "Correct." );
                    }
                    else
                    {
                        System.out.println( "Incorrect." );
                    }

                    // Unget the dictionary service.
                    context.ungetService( refs[0] );
                }
            }
            catch ( IOException ex )
            {
            }
        }
        else
        {
            System.out.println( "Couldn't find any dictionary service..." );
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
}
