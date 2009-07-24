/*
 * Copyright (c) OSGi Alliance (2001, 2008). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.osgi.service.cm;

import java.util.Dictionary;

/**
 * A service that can receive configuration data from a Configuration Admin
 * service.
 * 
 * <p>
 * A Managed Service is a service that needs configuration data. Such an object
 * should be registered with the Framework registry with the
 * <code>service.pid</code> property set to some unique identifier called a
 * PID.
 * 
 * <p>
 * If the Configuration Admin service has a <code>Configuration</code> object
 * corresponding to this PID, it will callback the <code>updated()</code>
 * method of the <code>ManagedService</code> object, passing the properties of
 * that <code>Configuration</code> object.
 * 
 * <p>
 * If it has no such <code>Configuration</code> object, then it calls back
 * with a <code>null</code> properties argument. Registering a Managed Service
 * will always result in a callback to the <code>updated()</code> method
 * provided the Configuration Admin service is, or becomes active. This callback
 * must always be done asynchronously.
 * 
 * <p>
 * Else, every time that either of the <code>updated()</code> methods is
 * called on that <code>Configuration</code> object, the
 * <code>ManagedService.updated()</code> method with the new properties is
 * called. If the <code>delete()</code> method is called on that
 * <code>Configuration</code> object, <code>ManagedService.updated()</code>
 * is called with a <code>null</code> for the properties parameter. All these
 * callbacks must be done asynchronously.
 * 
 * <p>
 * The following example shows the code of a serial port that will create a port
 * depending on configuration information.
 * 
 * <pre>
 *  
 *   class SerialPort implements ManagedService {
 *  
 *     ServiceRegistration registration;
 *     Hashtable configuration;
 *     CommPortIdentifier id;
 *  
 *     synchronized void open(CommPortIdentifier id,
 *     BundleContext context) {
 *       this.id = id;
 *       registration = context.registerService(
 *         ManagedService.class.getName(),
 *         this,
 *         getDefaults()
 *       );
 *     }
 *  
 *     Hashtable getDefaults() {
 *       Hashtable defaults = new Hashtable();
 *       defaults.put( &quot;port&quot;, id.getName() );
 *       defaults.put( &quot;product&quot;, &quot;unknown&quot; );
 *       defaults.put( &quot;baud&quot;, &quot;9600&quot; );
 *       defaults.put( Constants.SERVICE_PID,
 *         &quot;com.acme.serialport.&quot; + id.getName() );
 *       return defaults;
 *     }
 *  
 *     public synchronized void updated(
 *       Dictionary configuration  ) {
 *       if ( configuration == 
 * <code>
 * null
 * </code>
 *   )
 *         registration.setProperties( getDefaults() );
 *       else {
 *         setSpeed( configuration.get(&quot;baud&quot;) );
 *         registration.setProperties( configuration );
 *       }
 *     }
 *     ...
 *   }
 *   
 * </pre>
 * 
 * <p>
 * As a convention, it is recommended that when a Managed Service is updated, it
 * should copy all the properties it does not recognize into the service
 * registration properties. This will allow the Configuration Admin service to
 * set properties on services which can then be used by other applications.
 * 
 * @version $Revision: 5673 $
 */
public interface ManagedService {
	/**
	 * Update the configuration for a Managed Service.
	 * 
	 * <p>
	 * When the implementation of <code>updated(Dictionary)</code> detects any
	 * kind of error in the configuration properties, it should create a new
	 * <code>ConfigurationException</code> which describes the problem. This
	 * can allow a management system to provide useful information to a human
	 * administrator.
	 * 
	 * <p>
	 * If this method throws any other <code>Exception</code>, the
	 * Configuration Admin service must catch it and should log it.
	 * <p>
	 * The Configuration Admin service must call this method asynchronously
	 * which initiated the callback. This implies that implementors of Managed
	 * Service can be assured that the callback will not take place during
	 * registration when they execute the registration in a synchronized method.
	 * 
	 * @param properties A copy of the Configuration properties, or
	 *        <code>null</code>. This argument must not contain the
	 *        "service.bundleLocation" property. The value of this property may
	 *        be obtained from the <code>Configuration.getBundleLocation</code>
	 *        method.
	 * @throws ConfigurationException when the update fails
	 */
	public void updated(Dictionary properties) throws ConfigurationException;
}
