/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.scr.impl;


import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.Reference;
import org.apache.felix.scr.ScrService;
import org.apache.felix.shell.Command;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;


public class ScrCommand implements Command
{

    private static final String HELP_CMD = "help";
    private static final String LIST_CMD = "list";
    private static final String INFO_CMD = "info";
    private static final String ENABLE_CMD = "enable";
    private static final String DISABLE_CMD = "disable";

    private final BundleContext bundleContext;
    private final ScrService scrService;


    ScrCommand( BundleContext bundleContext, ScrService scrService )
    {
        this.bundleContext = bundleContext;
        this.scrService = scrService;
    }


    public String getName()
    {
        return "scr";
    }


    public String getUsage()
    {
        return "scr help";
    }


    public String getShortDescription()
    {
        return "Declarative Services Runtime";
    }


    public void execute( String commandLine, PrintStream out, PrintStream err )
    {
        // Parse the commandLine to get the OBR command.
        StringTokenizer st = new StringTokenizer( commandLine );
        // Ignore the invoking command.
        st.nextToken();
        // Try to get the OBR command, default is HELP command.
        String command = HELP_CMD;
        try
        {
            command = st.nextToken();
        }
        catch ( Exception ex )
        {
            // Ignore.
        }

        // Perform the specified command.
        if ( ( command == null ) || ( command.equals( HELP_CMD ) ) )
        {
            help( out, st );
        }
        else
        {
            if ( command.equals( LIST_CMD ) )
            {
                list( st, out, err );
            }
            else if ( command.equals( INFO_CMD ) )
            {
                info( st, out, err );
            }
            else if ( command.equals( ENABLE_CMD ) )
            {
                change( st, out, err, true );
            }
            else if ( command.equals( DISABLE_CMD ) )
            {
                change( st, out, err, false );
            }
            else
            {
                err.println( "Unknown command: " + command );
            }
        }
    }


    private void list( StringTokenizer st, PrintStream out, PrintStream err )
    {
        Component[] components;
        if ( st.hasMoreTokens() )
        {
            String bid = st.nextToken();
            try
            {
                long bundleId = Long.parseLong( bid );
                Bundle bundle = bundleContext.getBundle( bundleId );
                if ( bundle == null )
                {
                    err.println( "Missing bundle with ID " + bundleId );
                    return;
                }
                if ( ComponentRegistry.isBundleActive( bundle ) )
                {
                    components = scrService.getComponents( bundle );
                    if ( components == null )
                    {
                        out.println( "Bundle " + bundleId + " declares no components" );
                        return;
                    }
                }
                else
                {
                    out.println( "Bundle " + bundleId + " is not active" );
                    return;
                }
            }
            catch ( NumberFormatException nfe )
            {
                err.println( "Cannot parse " + bid + " to a bundleId" );
                return;
            }
        }
        else
        {
            components = scrService.getComponents();
            if ( components == null )
            {
                out.println( "No components registered" );
                return;
            }
        }

        out.println( "   Id   State          Name" );
        for ( int i = 0; i < components.length; i++ )
        {
            out.print( '[' );
            out.print( pad( String.valueOf( components[i].getId() ), -4 ) );
            out.print( "] [" );
            out.print( pad( toStateString( components[i].getState() ), 13 ) );
            out.print( "] " );
            out.print( components[i].getName() );
            out.println();
        }
    }


    private void info( StringTokenizer st, PrintStream out, PrintStream err )
    {
        Component component = getComponentFromArg( st, err );
        if ( component == null )
        {
            return;
        }

        out.print( "ID: " );
        out.println( component.getId() );
        out.print( "Name: " );
        out.println( component.getName() );
        out.print( "Bundle: " );
        out.println( component.getBundle().getSymbolicName() + " (" + component.getBundle().getBundleId() + ")" );
        out.print( "State: " );
        out.println( toStateString( component.getState() ) );
        out.print( "Default State: " );
        out.println( component.isDefaultEnabled() ? "enabled" : "disabled" );
        out.print( "Activation: " );
        out.println( component.isImmediate() ? "immediate" : "delayed" );

        // DS 1.1 new features
        out.print( "Configuration Policy: " );
        out.println( component.getConfigurationPolicy() );
        out.print( "Activate Method: " );
        out.print( component.getActivate() );
        if ( component.isActivateDeclared() )
        {
            out.print( " (declared in the descriptor)" );
        }
        out.println();
        out.print( "Deactivate Method: " );
        out.print( component.getDeactivate() );
        if ( component.isDeactivateDeclared() )
        {
            out.print( " (declared in the descriptor)" );
        }
        out.println();
        out.print( "Modified Method: " );
        if ( component.getModified() != null )
        {
            out.print( component.getModified() );
        }
        else
        {
            out.print( "-" );
        }
        out.println();

        if ( component.getFactory() != null )
        {
            out.print( "Factory: " );
            out.println( component.getFactory() );
        }

        String[] services = component.getServices();
        if ( services != null )
        {
            out.print( "Services: " );
            out.println( services[0] );
            for ( int i = 1; i < services.length; i++ )
            {
                out.print( "          " );
                out.println( services[i] );
            }
            out.print( "Service Type: " );
            out.println( component.isServiceFactory() ? "service factory" : "service" );
        }

        Reference[] refs = component.getReferences();
        if ( refs != null )
        {
            for ( int i = 0; i < refs.length; i++ )
            {
                out.print( "Reference: " );
                out.println( refs[i].getName() );
                out.print( "    Satisfied: " );
                out.println( refs[i].isSatisfied() ? "satisfied" : "unsatisfied" );
                out.print( "    Service Name: " );
                out.println( refs[i].getServiceName() );
                if ( refs[i].getTarget() != null )
                {
                    out.print( "    Target Filter: " );
                    out.println( refs[i].getTarget() );
                }
                out.print( "    Multiple: " );
                out.println( refs[i].isMultiple() ? "multiple" : "single" );
                out.print( "    Optional: " );
                out.println( refs[i].isOptional() ? "optional" : "mandatory" );
                out.print( "    Policy: " );
                out.println( refs[i].isStatic() ? "static" : "dynamic" );
            }
        }

        Dictionary props = component.getProperties();
        if ( props != null )
        {
            out.println( "Properties:" );
            TreeSet keys = new TreeSet( Collections.list( props.keys() ) );
            for ( Iterator ki = keys.iterator(); ki.hasNext(); )
            {
                Object key = ki.next();
                out.print( "    " );
                out.print( key );
                out.print( " = " );

                Object prop = props.get( key );
                if ( prop.getClass().isArray() )
                {
                    prop = Arrays.asList( ( Object[] ) prop );
                }
                out.print( prop );

                out.println();
            }
        }
    }


    private void change( StringTokenizer st, PrintStream out, PrintStream err, boolean enable )
    {
        Component component = getComponentFromArg( st, err );
        if ( component == null )
        {
            return;
        }

        if ( component.getState() == Component.STATE_DISPOSED )
        {
            err.println( "Component " + component.getName() + " already disposed, cannot change state" );
        }
        else if ( enable )
        {
            if ( component.getState() == Component.STATE_DISABLED )
            {
                component.enable();
                out.println( "Component " + component.getName() + " enabled" );
            }
            else
            {
                out.println( "Component " + component.getName() + " already enabled" );
            }
        }
        else
        {
            if ( component.getState() != Component.STATE_DISABLED )
            {
                component.disable();
                out.println( "Component " + component.getName() + " disabled" );
            }
            else
            {
                out.println( "Component " + component.getName() + " already disabled" );
            }
        }

    }


    private void help( PrintStream out, StringTokenizer st )
    {
        String command = HELP_CMD;
        if ( st.hasMoreTokens() )
        {
            command = st.nextToken();
        }
        if ( command.equals( LIST_CMD ) )
        {
            out.println( "" );
            out.println( "scr " + LIST_CMD + " [ <bundleId> ]" );
            out.println( "" );
            out.println( "This command lists registered components. If a bundle ID is\n"
                + "added, only the components of the selected bundles are listed." );
            out.println( "" );
        }
        else if ( command.equals( INFO_CMD ) )
        {
            out.println( "" );
            out.println( "scr " + INFO_CMD + " <componentId>" );
            out.println( "" );
            out.println( "This command dumps information of the component whose\n"
                + "component ID is given as command argument." );
            out.println( "" );
        }
        else if ( command.equals( ENABLE_CMD ) )
        {
            out.println( "" );
            out.println( "scr " + ENABLE_CMD + " <componentId>" );
            out.println( "" );
            out.println( "This command enables the component whose component ID\n" + "is given as command argument." );
            out.println( "" );
        }
        else if ( command.equals( DISABLE_CMD ) )
        {
            out.println( "" );
            out.println( "scr " + DISABLE_CMD + " <componentId>" );
            out.println( "" );
            out.println( "This command disables the component whose component ID\n" + "is given as command argument." );
            out.println( "" );
        }
        else
        {
            out.println( "scr " + HELP_CMD + " [" + LIST_CMD + "]" );
            out.println( "scr " + LIST_CMD + " [ <bundleId> ]" );
            out.println( "scr " + INFO_CMD + " <componentId>" );
            out.println( "scr " + ENABLE_CMD + " <componentId>" );
            out.println( "scr " + DISABLE_CMD + " <componentId>" );
        }
    }


    private String pad( String value, int size )
    {
        boolean right = size < 0;
        size = right ? -size : size;

        if ( value.length() >= size )
        {
            return value;
        }

        char[] buf = new char[size];
        int padLen = size - value.length();
        int valOff = right ? padLen : 0;
        int padOff = right ? 0 : value.length();

        value.getChars( 0, value.length(), buf, valOff );
        Arrays.fill( buf, padOff, padOff + padLen, ' ' );

        return new String( buf );
    }


    private String toStateString( int state )
    {
        switch ( state )
        {
            case Component.STATE_DISABLED:
                return "disabled";
            case Component.STATE_ENABLING:
                return "enabling";
            case Component.STATE_ENABLED:
                return "enabled";
            case Component.STATE_UNSATISFIED:
                return "unsatisfied";
            case Component.STATE_ACTIVATING:
                return "activating";
            case Component.STATE_ACTIVE:
                return "active";
            case Component.STATE_REGISTERED:
                return "registered";
            case Component.STATE_FACTORY:
                return "factory";
            case Component.STATE_DEACTIVATING:
                return "deactivating";
            case Component.STATE_DISABLING:
                return "disabling";
            case Component.STATE_DISPOSING:
                return "disposing";
            case Component.STATE_DISPOSED:
                return "disposed";
            default:
                return String.valueOf( state );
        }
    }


    private Component getComponentFromArg( StringTokenizer st, PrintStream err )
    {
        Component component = null;

        if ( st.hasMoreTokens() )
        {
            String cid = st.nextToken();
            try
            {
                long componentId = Long.parseLong( cid );
                component = scrService.getComponent( componentId );
                if ( component == null )
                {
                    err.println( "Missing Component with ID " + componentId );
                }
            }
            catch ( NumberFormatException nfe )
            {
                err.println( "Cannot parse " + cid + " to a componentId" );
            }
        }
        else
        {

            err.println( "Component ID required" );
        }

        return component;
    }
}
