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


import java.io.IOException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.Render;
import org.apache.felix.webconsole.internal.Util;
import org.apache.felix.webconsole.internal.core.SetStartLevelAction;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.metatype.ObjectClassDefinition;


/**
 * The <code>ConfigManager</code> TODO
 */
public class ConfigManager extends ConfigManagerBase implements Render
{

    public static final String NAME = "configMgr";

    public static final String LABEL = "Configuration";

    public static final String PID = "pid";


    public String getLabel()
    {
        return LABEL;
    }


    public String getName()
    {
        return NAME;
    }


    public void render( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {

        // true if MetaType service information is not required
        boolean optionalMetaType = false;

        PrintWriter pw = response.getWriter();

        pw.println( "<script type='text/javascript' src='res/ui/configmanager.js'></script>" );

        pw.println( "<table class='content' cellpadding='0' cellspacing='0' width='100%'>" );

        pw.println( "<tr class='content' id='configField'>" );
        pw.println( "<td class='content'>Configurations</th>" );
        pw.println( "<td class='content'>" );
        this.listConfigurations( pw, optionalMetaType, getLocale( request ) );
        pw.println( "</td>" );
        pw.println( "</tr>" );

        pw.println( "</table>" );
    }


    private void listConfigurations( PrintWriter pw, boolean optionalMetaType, Locale loc )
    {

        ConfigurationAdmin ca = this.getConfigurationAdmin();
        if ( ca == null )
        {
            pw.print( "Configuration Admin Service not available" );
            return;
        }

        String locale = ( loc != null ) ? loc.toString() : null;

        try
        {
            // get a list of all pids for which MetaData exists
            Map metaDataPids = this.getMetadataPids();

            // sorted map of options
            SortedMap options = new TreeMap( String.CASE_INSENSITIVE_ORDER );

            // find all ManagedServiceFactories to get the factoryPIDs
            ServiceReference[] refs = this.getBundleContext().getServiceReferences(
                ManagedServiceFactory.class.getName(), null );
            for ( int i = 0; refs != null && i < refs.length; i++ )
            {
                Object factoryPid = refs[i].getProperty( Constants.SERVICE_PID );
                if ( factoryPid instanceof String )
                {
                    String pid = ( String ) factoryPid;
                    String name;
                    ObjectClassDefinition ocd = this.getObjectClassDefinition( refs[i].getBundle(), pid, locale );
                    if ( ocd != null )
                    {
                        name = ocd.getName() + " (";
                        name += pid + ")";
                    }
                    else
                    {
                        name = pid;
                    }

                    if ( ocd != null || optionalMetaType )
                    {
                        options.put( "factoryPid=" + pid, name );
                    }
                }
            }

            // get a sorted list of configuration PIDs
            Configuration[] cfgs = ca.listConfigurations( null );
            for ( int i = 0; cfgs != null && i < cfgs.length; i++ )
            {

                // ignore configuration object if an entry already exists in the
                // map
                String pid = cfgs[i].getPid();
                if ( options.containsKey( "pid=" + pid ) || options.containsKey( "factoryPid=" + pid ) )
                {
                    continue;
                }

                Dictionary props = cfgs[i].getProperties();

                // insert and entry for the pid
                ObjectClassDefinition ocd = this.getObjectClassDefinition( cfgs[i], locale );
                String name;
                if ( ocd != null )
                {
                    name = ocd.getName() + " (";
                    name += pid + ")";

                    // remove from the list of known pids
                    metaDataPids.remove( pid );

                }
                else
                {
                    name = pid;
                }

                if ( ocd != null || optionalMetaType )
                {
                    options.put( "pid=" + pid, name );
                }

                // if the configuration is part of a factory, ensure an entry
                // for the factory
                if ( cfgs[i].getFactoryPid() != null )
                {
                    pid = cfgs[i].getFactoryPid();
                    if ( options.containsValue( "factoryPid=" + pid ) )
                    {
                        continue;
                    }

                    String existing = ( String ) options.remove( "pid=" + pid );
                    if ( existing != null )
                    {
                        options.put( "factoryPid=" + pid, existing );
                    }
                    else
                    {
                        Bundle bundle = this.getBundle( cfgs[i].getBundleLocation() );
                        ocd = this.getObjectClassDefinition( bundle, pid, locale );
                        if ( ocd != null )
                        {
                            options.put( "factoryPid=" + pid, ocd.getName() );
                        }
                        else if ( optionalMetaType )
                        {
                            options.put( "factoryPid=" + pid, pid );
                        }
                    }
                }
            }

            // If there are any meta data PIDs for which there is no existing
            // configuration, we add them to the list to create configuration
            if ( !metaDataPids.isEmpty() )
            {
                for ( Iterator mdpi = metaDataPids.entrySet().iterator(); mdpi.hasNext(); )
                {
                    Entry mdp = ( Entry ) mdpi.next();
                    ObjectClassDefinition ocd = this.getObjectClassDefinition( ( Bundle ) mdp.getValue(),
                        ( String ) mdp.getKey(), locale );
                    options.put( "pid=" + mdp.getKey(), ocd.getName() + " (" + mdp.getKey() + ")" );
                }
            }

            pw.println( "<form method='post' name='configSelection' onSubmit='configure(); return false;'" );
            pw.println( "<input type='hidden' name='" + Util.PARAM_ACTION + "' value='" + SetStartLevelAction.NAME
                + "'>" );
            pw.println( "<select class='select' name='pid' onChange='configure();'>" );
            for ( Iterator ei = options.entrySet().iterator(); ei.hasNext(); )
            {
                Entry entry = ( Entry ) ei.next();
                pw.print( "<option value='" + entry.getKey() + "'>" );
                pw.print( entry.getValue() );
                pw.println( "</option>" );
            }
            pw.println( "</select>" );
            pw.println( "&nbsp;&nbsp;" );
            pw.println( "<input class='submit' type='submit' value='Configure' />" );
            pw.println( "</form>" );

        }
        catch ( Exception e )
        {
            // write a message or ignore
        }
    }
}
