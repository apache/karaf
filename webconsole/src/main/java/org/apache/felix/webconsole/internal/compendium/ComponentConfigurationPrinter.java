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
package org.apache.felix.webconsole.internal.compendium;


import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.Reference;
import org.apache.felix.scr.ScrService;
import org.apache.felix.webconsole.ConfigurationPrinter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;


public class ComponentConfigurationPrinter extends AbstractScrPlugin implements ConfigurationPrinter
{

    private ServiceRegistration registration;


    public void activate( BundleContext bundleContext )
    {
        super.activate( bundleContext );

        registration = bundleContext.registerService( ConfigurationPrinter.SERVICE, this, null );
    }


    public String getTitle()
    {
        return "Declarative Services Components";
    }


    public void printConfiguration( PrintWriter pw )
    {
        ScrService scrService = getScrService();
        if ( scrService != null )
        {
            Component[] components = scrService.getComponents();

            if ( components == null || components.length == 0 )
            {

                pw.println( "  No Components Registered" );

            }
            else
            {

                // order components by id
                TreeMap componentMap = new TreeMap();
                for ( int i = 0; i < components.length; i++ )
                {
                    Component component = components[1];
                    componentMap.put( new Long( component.getId() ), component );
                }

                // render components
                for ( Iterator ci = componentMap.values().iterator(); ci.hasNext(); )
                {
                    Component component = ( Component ) ci.next();
                    component( pw, component );
                }
            }
        }
        else
        {
            pw.println( "  Apache Felix Declarative Service not installed" );
        }
    }


    private void component( PrintWriter pw, Component component )
    {

        pw.print( component.getId() );
        pw.print( "=[" );
        pw.print( component.getName() );
        pw.println( "]" );

        pw.println( "  Bundle" + component.getBundle().getSymbolicName() + " (" + component.getBundle().getBundleId()
            + ")" );
        pw.println( "  State=" + ComponentsServlet.toStateString( component.getState() ) );
        pw.println( "  DefaultState=" + ( component.isDefaultEnabled() ? "enabled" : "disabled" ) );
        pw.println( "  Activation=" + ( component.isImmediate() ? "immediate" : "delayed" ) );

        listServices( pw, component );
        listReferences( pw, component );
        listProperties( pw, component );

        pw.println();
    }


    private void listServices( PrintWriter pw, Component component )
    {
        String[] services = component.getServices();
        if ( services == null )
        {
            return;
        }

        pw.println( "  ServiceType=" + ( component.isServiceFactory() ? "service factory" : "service" ) );

        StringBuffer buf = new StringBuffer();
        for ( int i = 0; i < services.length; i++ )
        {
            if ( i > 0 )
            {
                buf.append( ", " );
            }
            buf.append( services[i] );
        }

        pw.println( "  Services=" + buf );
    }


    private void listReferences( PrintWriter pw, Component component )
    {
        Reference[] refs = component.getReferences();
        if ( refs != null )
        {
            for ( int i = 0; i < refs.length; i++ )
            {

                pw.println( "  Reference=" + refs[i].getName() + ", "
                    + ( refs[i].isSatisfied() ? "Satisfied" : "Unsatisfied" ) );

                pw.println( "    Service Name: " + refs[i].getServiceName() );

                if ( refs[i].getTarget() != null )
                {
                    pw.println( "  Target Filter: " + refs[i].getTarget() );
                }

                pw.println( "    Multiple: " + ( refs[i].isMultiple() ? "multiple" : "single" ) );
                pw.println( "    Optional: " + ( refs[i].isOptional() ? "optional" : "mandatory" ) );
                pw.println( "    Policy: " + ( refs[i].isStatic() ? "static" : "dynamic" ) );

                // list bound services
                ServiceReference[] boundRefs = refs[i].getServiceReferences();
                if ( boundRefs != null && boundRefs.length > 0 )
                {
                    for ( int j = 0; j < boundRefs.length; j++ )
                    {
                        pw.print( "    Bound Service: ID " );
                        pw.print( boundRefs[j].getProperty( Constants.SERVICE_ID ) );

                        String name = ( String ) boundRefs[j].getProperty( ComponentConstants.COMPONENT_NAME );
                        if ( name == null )
                        {
                            name = ( String ) boundRefs[j].getProperty( Constants.SERVICE_PID );
                            if ( name == null )
                            {
                                name = ( String ) boundRefs[j].getProperty( Constants.SERVICE_DESCRIPTION );
                            }
                        }
                        if ( name != null )
                        {
                            pw.print( " (" );
                            pw.print( name );
                            pw.print( ")" );
                        }
                    }
                }
                else
                {
                    pw.print( "    No Services bound" );
                }
                pw.println();
            }
        }
    }


    private void listProperties( PrintWriter pw, Component component )
    {
        Dictionary props = component.getProperties();
        if ( props != null )
        {

            pw.println( "  Properties=" );
            TreeSet keys = new TreeSet( Collections.list( props.keys() ) );
            for ( Iterator ki = keys.iterator(); ki.hasNext(); )
            {
                String key = ( String ) ki.next();
                Object value = props.get( key );
                if ( value.getClass().isArray() )
                {
                    value = Arrays.asList( ( Object[] ) value );
                }
                pw.println( "    " + key + "=" + value );
            }
        }

    }

}
