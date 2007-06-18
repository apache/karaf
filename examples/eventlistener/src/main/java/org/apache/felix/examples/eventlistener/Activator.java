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
package org.apache.felix.examples.eventlistener;


import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceEvent;


/**
 * This class implements a simple bundle that utilizes the OSGi framework's
 * event mechanism to listen for service events. Upon receiving a service event,
 * it prints out the event's details.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Activator implements BundleActivator, ServiceListener
{
    /**
     * Implements BundleActivator.start(). Prints a message and adds itself to
     * the bundle context as a service listener.
     * 
     * @param context the framework context for the bundle.
     */
    public void start( BundleContext context )
    {
        System.out.println( "Starting to listen for service events." );
        context.addServiceListener( this );
    }


    /**
     * Implements BundleActivator.stop(). Prints a message and removes itself
     * from the bundle context as a service listener.
     * 
     * @param context the framework context for the bundle.
     */
    public void stop( BundleContext context )
    {
        context.removeServiceListener( this );
        System.out.println( "Stopped listening for service events." );

        // Note: It is not required that we remove the listener here,
        // since the framework will do it automatically anyway.
    }


    /**
     * Implements ServiceListener.serviceChanged(). Prints the details of any
     * service event from the framework.
     * 
     * @param event the fired service event.
     */
    public void serviceChanged( ServiceEvent event )
    {
        String[] objectClass = ( String[] ) event.getServiceReference().getProperty( "objectClass" );

        if ( event.getType() == ServiceEvent.REGISTERED )
        {
            System.out.println( "Ex1: Service of type " + objectClass[0] + " registered." );
        }
        else if ( event.getType() == ServiceEvent.UNREGISTERING )
        {
            System.out.println( "Ex1: Service of type " + objectClass[0] + " unregistered." );
        }
        else if ( event.getType() == ServiceEvent.MODIFIED )
        {
            System.out.println( "Ex1: Service of type " + objectClass[0] + " modified." );
        }
    }
}
