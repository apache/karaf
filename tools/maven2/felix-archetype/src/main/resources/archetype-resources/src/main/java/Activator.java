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
package ${groupId};


import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;


/**
 * This class implements a simple bundle that utilizes the OSGi
 * framework's event mechanism to listen for service events.
 *
 * Upon receiving a service event, it prints out the event's details.
 */
public class Activator implements BundleActivator, ServiceListener 
{
    /**
     * Put your bundle initialization code here...
     *
	 * Implements <code>BundleActivator.start()</code>. Prints a message
     * and adds itself to the bundle context as a service listener.
     *
     * @param bundleContext the framework context for the bundle
     * @throws Exception
     */
    public void start( BundleContext bundleContext ) throws Exception 
    {
        System.out.println( "Starting to listen for service events." );
        bundleContext.addServiceListener( this );
    }

    
    /**
     * Put your bundle finalization code here...
     *
     * Implements <code>BundleActivator.stop()</code>. Prints a message
     * and removes itself from the bundle context as a service listener.
     *
     * @param bundleContext the framework context for the bundle
     * @throws Exception
     */
    public void stop( BundleContext bundleContext ) throws Exception 
    {
        bundleContext.removeServiceListener( this );
        System.out.println( "Stopped listening for service events." );

        // Note: It is not required that we remove the listener here, since
        // the framework will do it automatically anyway.
    }

    
    /**
     * Implements <code>ServiceListener.serviceChanges()</code>. Prints the
     * details of any service event from the framework.
     *
     * @param event the fired service event
     */
    public void serviceChanged( ServiceEvent event ) 
    {
        String[] objectClass = ( String[] ) event.getServiceReference().getProperty( "objectClass" );
        
        switch( event.getType() )
        {
            case( ServiceEvent.REGISTERED ):
                System.out.println( "SimpleBundle: Service of type " + objectClass[0] + " registered." );
                break;
            case( ServiceEvent.UNREGISTERED ):
                System.out.println( "SimpleBundle: Service of type " + objectClass[0] + " unregistered." );
                break;
            case( ServiceEvent.MODIFIED ):
                System.out.println( "SimpleBundle: Service of type " + objectClass[0] + " modified." );
                break;
            default:
                break;
        }
    }
}
