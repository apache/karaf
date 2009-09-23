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
package org.apache.felix.webconsole.internal.compendium;


import java.io.PrintWriter;

import org.apache.felix.webconsole.internal.AbstractConfigurationPrinter;
import org.apache.felix.webconsole.internal.misc.ConfigurationRender;
import org.osgi.framework.ServiceReference;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;


public class PreferencesConfigurationPrinter extends AbstractConfigurationPrinter
{

    public static final String TITLE = "Preferences";


    public String getTitle()
    {
        return TITLE;
    }


    public void printConfiguration( PrintWriter printWriter )
    {
        ServiceReference sr = getBundleContext().getServiceReference( PreferencesService.class.getName() );
        if ( sr == null )
        {
            printWriter.println( "  Preferences Service not registered" );
        }
        else
        {
            PreferencesService ps = ( PreferencesService ) getBundleContext().getService( sr );
            try
            {
                this.printPreferences( printWriter, ps.getSystemPreferences() );

                String[] users = ps.getUsers();
                for ( int i = 0; users != null && i < users.length; i++ )
                {
                    printWriter.println( "*** User Preferences " + users[i] + ":" );
                    this.printPreferences( printWriter, ps.getUserPreferences( users[i] ) );
                }
            }
            catch ( BackingStoreException bse )
            {
                // todo or not :-)
            }
            finally
            {
                getBundleContext().ungetService( sr );
            }
        }
    }


    private void printPreferences( PrintWriter pw, Preferences prefs ) throws BackingStoreException
    {

        final String[] children = prefs.childrenNames();
        final String[] keys = prefs.keys();

        if ( children.length == 0 && keys.length == 0 )
        {
            pw.println( "No Preferences available" );
        }
        else
        {
            for ( int i = 0; i < children.length; i++ )
            {
                this.printPreferences( pw, prefs.node( children[i] ) );
            }

            for ( int i = 0; i < keys.length; i++ )
            {
                ConfigurationRender
                    .infoLine( pw, null, prefs.absolutePath() + "/" + keys[i], prefs.get( keys[i], null ) );
            }
        }

        pw.println();
    }
}
