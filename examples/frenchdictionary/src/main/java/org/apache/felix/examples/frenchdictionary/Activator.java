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
package org.apache.felix.examples.frenchdictionary;


import java.util.Properties;

import org.apache.felix.examples.dictionaryservice.DictionaryService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;


/**
 * This class implements a simple bundle that uses the bundle context to
 * register an French language dictionary service with the OSGi framework. The
 * dictionary service interface is defined in a separate class file and is
 * implemented by an inner class. This class is identical to the class in
 * Example 2, except that the dictionary contains French words.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Activator implements BundleActivator
{
    /**
     * Implements BundleActivator.start(). Registers an instance of a dictionary
     * service using the bundle context; attaches properties to the service that
     * can be queried when performing a service look-up.
     * 
     * @param context the framework context for the bundle.
     */
    public void start( BundleContext context )
    {
        Properties props = new Properties();
        props.put( "Language", "French" );
        context.registerService( DictionaryService.class.getName(), new DictionaryImpl(), props );
    }


    /**
     * Implements BundleActivator.stop(). Does nothing since the framework will
     * automatically unregister any registered services.
     * 
     * @param context
     *            the framework context for the bundle.
     */
    public void stop( BundleContext context )
    {
        // NOTE: The service is automatically unregistered.
    }

    
    /**
     * A private inner class that implements a dictionary service; see
     * DictionaryService for details of the service.
     */
    private static class DictionaryImpl implements DictionaryService
    {
        // The set of words contained in the dictionary.
        String[] m_dictionary = { "bienvenue", "au", "tutoriel", "osgi" };

        /**
         * Implements DictionaryService.checkWord(). Determines if the passed in
         * word is contained in the dictionary.
         * 
         * @param word
         *            the word to be checked.
         * @return true if the word is in the dictionary, false otherwise.
         */
        public boolean checkWord( String word )
        {
            word = word.toLowerCase();

            // This is very inefficient
            for ( int i = 0; i < m_dictionary.length; i++ )
            {
                if ( m_dictionary[i].equals( word ) )
                {
                    return true;
                }
            }
            return false;
        }
    }
}
