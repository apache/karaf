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
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.Render;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
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

        pw.println( "<tr class='content' id='factoryField'>" );
        pw.println( "<td class='content'>Factory Configurations</th>" );
        pw.println( "<td class='content'>" );
        this.listFactoryConfigurations( pw, optionalMetaType, getLocale( request ) );
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
            // sorted map of options
            SortedMap optionsPlain = new TreeMap( String.CASE_INSENSITIVE_ORDER );

            // find all ManagedServices to get the PIDs
            ServiceReference[] refs = this.getBundleContext().getServiceReferences( ManagedService.class.getName(),
                null );
            for ( int i = 0; refs != null && i < refs.length; i++ )
            {
                Object pidObject = refs[i].getProperty( Constants.SERVICE_PID );
                if ( pidObject instanceof String )
                {
                    String pid = ( String ) pidObject;
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
                        optionsPlain.put( pid, name );
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
                if ( optionsPlain.containsKey( pid ) )
                {
                    continue;
                }

                // insert and entry for the pid
                ObjectClassDefinition ocd = this.getObjectClassDefinition( cfgs[i], locale );
                String name;
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
                    optionsPlain.put( pid, name );
                }
            }

            //            pw.println( "<form method='post' name='configSelection' onSubmit='configure(); return false;'" );
            pw.println( "<select class='select' name='pid' id='configSelection_pid' onChange='configure();'>" );
            for ( Iterator ei = optionsPlain.entrySet().iterator(); ei.hasNext(); )
            {
                Entry entry = ( Entry ) ei.next();
                pw.print( "<option value='" + entry.getKey() + "'>" );
                pw.print( entry.getValue() );
                pw.println( "</option>" );
            }
            pw.println( "</select>" );
            pw.println( "&nbsp;&nbsp;" );
            pw.println( "<input class='submit' type='button' value='Configure' onClick='configure();' />" );
            //            pw.println( "</form>" );
        }
        catch ( Exception e )
        {
            // write a message or ignore
        }
    }


    private void listFactoryConfigurations( PrintWriter pw, boolean optionalMetaType, Locale loc )
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
            // sorted map of options
            SortedMap optionsFactory = new TreeMap( String.CASE_INSENSITIVE_ORDER );

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
                        optionsFactory.put( pid, name );
                    }
                }
            }

            pw.println( "<select class='select' name='pid' id='configSelection_factory' onChange='create();'>" );
            for ( Iterator ei = optionsFactory.entrySet().iterator(); ei.hasNext(); )
            {
                Entry entry = ( Entry ) ei.next();
                pw.print( "<option value='" + entry.getKey() + "'>" );
                pw.print( entry.getValue() );
                pw.println( "</option>" );
            }
            pw.println( "</select>" );
            pw.println( "&nbsp;&nbsp;" );
            pw.println( "<input class='submit' type='button' value='Create' onClick='create();' />" );
        }
        catch ( Exception e )
        {
            // write a message or ignore
        }
    }
}
