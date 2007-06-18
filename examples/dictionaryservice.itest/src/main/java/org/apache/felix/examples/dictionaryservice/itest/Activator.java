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
package org.apache.felix.examples.dictionaryservice.itest;


import org.apache.felix.examples.dictionaryservice.DictionaryService;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;


/**
 * This Activator grabs a handle on a dictionary service to check for 
 * correct operation of the dictionary while it is running inside felix.
 * This is an integration test of the dictionaryservice bundle.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Activator implements BundleActivator
{
    /**
     * Implements BundleActivator.start(). Queries for all available dictionary
     * services. If none are found it blows chunks, otherwise it checks for the
     * existence of the words in the first dictionary that it finds.
     * 
     * @param context the framework context for the bundle.
     */
    public void start( BundleContext context ) throws Exception
    {
        // Query for all service references matching any language.
        ServiceReference[] refs = context.getServiceReferences( DictionaryService.class.getName(), "(Language=*)" );

        if ( refs != null )
        {
            // First, get a dictionary service and then check if the word is correct.
            DictionaryService dictionary = ( DictionaryService ) context.getService( refs[0] );
            assertTrue( "welcome definition presence", dictionary.checkWord( "welcome" ) );
            assertFalse( "blah definition absense", dictionary.checkWord( "blah" ) );
            
            // Unget the dictionary service.
            context.ungetService( refs[0] );
        }
        else
        {
            throw new RuntimeException( "I need a dictionary service to test it properly." );
        }
    }

    
    public void assertTrue( String msg, boolean isTrue )
    {
        if ( ! isTrue )
        {
            throw new RuntimeException( msg + " expected to be true but was false." );
        }
    }
    

    public void assertFalse( String msg, boolean isFalse )
    {
        if ( isFalse )
        {
            throw new RuntimeException( msg + " expected to be false but was true." );
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
