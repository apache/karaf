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

package org.apache.felix.wireadmin;

import java.io.PrintStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.wireadmin.Consumer;
import org.osgi.service.wireadmin.Producer;
import org.osgi.service.wireadmin.Wire;
import org.osgi.service.wireadmin.WireAdmin;
import org.osgi.service.wireadmin.WireConstants;
import org.osgi.service.wireadmin.WireAdminEvent;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;

/**
 * Wire Administration service.
 *
 * <p>This service can be used to create <tt>Wire</tt> objects connecting
 * a Producer service and a Consumer service.
 * <tt>Wire</tt> objects also have wire properties that may be specified
 * when a <tt>Wire</tt> object is created. The Producer and Consumer
 * services may use the <tt>Wire</tt> object's properties to manage or control their
 * interaction.
 * The use of <tt>Wire</tt> object's properties by a Producer or Consumer
 * services is optional.
 *
 * <p>Security Considerations.
 * A bundle must have <tt>ServicePermission[GET,WireAdmin]</tt> to get the Wire Admin service to
 * create, modify, find, and delete <tt>Wire</tt> objects.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class WireAdminImpl implements WireAdmin, ServiceListener {

	private BundleContext m_bundleContext;

    // A Map containing a service reference associated to a producer and a List
    // of wire objects	
    private Map m_consumers = new HashMap(); /* ServiceReferences, List */
	
    private Map m_producers = new HashMap(); /* ServiceReferences, List */

	private List m_wires; // List containing the wires

	//private BindingController wireAdminListenersBindingController;

    // Filter corresponding to a consumer service
	private Filter m_consumerFilter;
    
    //  Filter corresponding to a producer service
	private Filter m_producerFilter;

    // EventManager
    private EventManager m_eventManager;

	private static int m_wireCount = 0;
    
    private AsyncMethodCaller m_asyncMethodCaller = new AsyncMethodCaller();        //m_eventDispatcher.stop();
    
    private static PrintStream m_traceout = null;
    
    private static PrintStream m_errorout = System.err;

	/**
     * Constructor with package visibility
     * 
	 * @param bundleContext the bundle context
	 */
	WireAdminImpl(BundleContext bundleContext) 
    {
		m_bundleContext = bundleContext;
        
        if(bundleContext.getProperty("fr.imag.adele.wireadmin.trace") != null)
        {
            String value = bundleContext.getProperty("fr.imag.adele.wireadmin.trace");
            if(value.equals("true"))
            {
                m_traceout = System.out;
            }
        }
        // Create the event manager (the event manager will start its own thread)       
        m_eventManager = new EventManager(m_bundleContext);
        
		try 
        {
			m_producerFilter = m_bundleContext.createFilter(
					"(objectClass=org.osgi.service.wireadmin.Producer)");
			m_consumerFilter = m_bundleContext.createFilter(
					"(objectClass=org.osgi.service.wireadmin.Consumer)");
		} 
        catch (InvalidSyntaxException e) 
        {
			// never thrown since LDAP expressions are correct
		}

        // Recover persistent wires
        getPersistentWires();

        // Activate thread that does asynchronous calls to
        // the producersConnected and consummersConnected methods
        new Thread(m_asyncMethodCaller).start();

        // Gets all producers and consumers that are present at the
        // moment the wire admin is created
        try 
        {
            // Registration for events must be done first, as some service
            // can be registered during initialization
            
            m_bundleContext.addServiceListener(this,"(|"+m_producerFilter.toString()+m_consumerFilter.toString()+")");

            // Replacement for the two following lines which work under OSCAR, 
            // but not work under IBM's SMF
            //m_bundleContext.addServiceListener(this,m_consumerFilter.toString());
            //m_bundleContext.addServiceListener(this,m_producerFilter.toString());
            
            // Get all producers
            ServiceReference[] producerRefs = m_bundleContext.getServiceReferences(Producer.class.getName(),null);
            
            if(producerRefs!=null)
            {
                // lock the producers Map to avoid concurrent modifications due
                // to service events
                synchronized(m_producers)
                {
                    for(int i=0;i<producerRefs.length;i++)
                    {
                        ServiceReference currentRef=(ServiceReference)producerRefs[i];
                        
                        Iterator wireIt = m_wires.iterator();
                        while(wireIt.hasNext())
                        {
                            WireImpl currentWire = (WireImpl) wireIt.next();
                            if(currentWire.getProducerPID().equals(currentRef.getProperty(Constants.SERVICE_PID)))
                            {
                                currentWire.bindProducer(currentRef);
                            }
                        }
                        m_producers.put(currentRef,new ArrayList());
                    }
                }
            }

            // Get all the consumers
            ServiceReference[] consumerRefs = m_bundleContext.getServiceReferences(Consumer.class.getName(),null);
            
            if(consumerRefs!=null)
            {
                for(int i=0;i<consumerRefs.length;i++)
                {
                    // lock the consumers to avoid concurrent modifications due
                    // to service events
                    synchronized(m_consumers)
                    {
                        ServiceReference currentRef=(ServiceReference)consumerRefs[i];

                        Iterator wireIt = m_wires.iterator();
                        while(wireIt.hasNext())
                        {
                            WireImpl currentWire = (WireImpl) wireIt.next();
                            if(currentWire.getConsumerPID().equals(currentRef.getProperty(Constants.SERVICE_PID)))
                            {
                                currentWire.bindConsumer(currentRef);
                            }
                        }
                        m_consumers.put(currentRef,new ArrayList());
                    }
                }
            }
        } 
        catch (InvalidSyntaxException e) 
        {
            trace(e);
        }

        // Iterate over all the wires, when a wire is connected
        // add it to the list of wires associated to a particular
        // producer or consumer
        synchronized(m_wires)
        {
            Iterator wireIterator = m_wires.iterator();
            while(wireIterator.hasNext())
            {
                WireImpl currentWire = (WireImpl) wireIterator.next();
                if(currentWire.isConnected())
                {                
                    // p. 327 "If both Producer and consumer services are registered
                    // with the framework, they are connected by the WireAdmin service"
                    List wires = (List) m_producers.get(currentWire.getProducerServiceRef());
                    wires.add(currentWire);
                    m_asyncMethodCaller.consumersConnected(currentWire.getProducer(),(Wire[])wires.toArray(new Wire[wires.size()]));
                    
                    wires = (List) m_consumers.get(currentWire.getConsumerServiceRef());
                    wires.add(currentWire);
                    m_asyncMethodCaller.producersConnected(currentWire.getConsumer(),(Wire[])wires.toArray(new Wire[wires.size()]));
                }            
            }
        }       
	}
    
    /**
     * Pass the service reference to the event dispatcher
     * 
     * @param ref the service reference
     */
    void setServiceReference(ServiceReference ref)
    {
        m_eventManager.setServiceReference(ref);
    }

	/**
	 * Create a new <tt>Wire</tt> object that connects a Producer
	 * service to a Consumer service.
	 *
	 * The Producer service and Consumer service do not
	 * have to be registered when the <tt>Wire</tt> object is created.
	 *
	 * <p>The <tt>Wire</tt> configuration data must be persistently stored.
	 * All <tt>Wire</tt> connections are reestablished when the
	 * <tt>WireAdmin</tt> service is registered.
	 * A <tt>Wire</tt> can be permanently removed by using the
	 * {@link #deleteWire} method.
	 *
	 * <p>The <tt>Wire</tt> object's properties must have case
	 * insensitive <tt>String</tt> objects as keys (like the Framework).
	 * However, the case of the key must be preserved.
	 * The type of the value of the property must be one of the following:
	 *
	 * <pre>
	 * type        = basetype
	 *  | vector | arrays
	 *
	 * basetype = String | Integer | Long
	 *  | Float | Double | Byte
	 *  | Short | Character
	 *  | Boolean
	 *
	 * primitive   = long | int | short
	 *  | char | byte | double | float
	 *
	 * arrays   = primitive '[]' | basetype '[]'
	 *
	 * vector   = Vector of basetype
	 * </pre>
	 *
	 * <p>The <tt>WireAdmin</tt> service must automatically add the
	 * following <tt>Wire</tt> properties:
	 * <ul>
	 * <li>
	 * {@link WireConstants#WIREADMIN_PID} set to the value of the <tt>Wire</tt> object's
	 * persistent identity (PID). This value is generated by the
	 * Wire Admin service when a <tt>Wire</tt> object is created.
	 * </li>
	 * <li>
	 * {@link WireConstants#WIREADMIN_PRODUCER_PID} set to the value of
	 * Producer service's PID.
	 * </li>
	 * <li>
	 * {@link WireConstants#WIREADMIN_CONSUMER_PID} set to the value of
	 * Consumer service's PID.
	 * </li>
	 * </ul>
	 * If the <tt>properties</tt> argument
	 * already contains any of these keys, then the supplied values
	 * are replaced with the values assigned by the Wire Admin service.
	 *
	 * <p>The Wire Admin service must broadcast a <tt>WireAdminEvent</tt> of type
	 * {@link WireAdminEvent#WIRE_CREATED}
	 * after the new <tt>Wire</tt> object becomes available from {@link #getWires}.
	 *
	 * @param producerPID The <tt>service.pid</tt> of the Producer service
	 * to be connected to the <tt>Wire</tt> object.
	 * @param consumerPID The <tt>service.pid</tt> of the Consumer service
	 * to be connected to the <tt>Wire</tt> object.
	 * @param properties The <tt>Wire</tt> object's properties. This argument may be <tt>null</tt>
	 * if the caller does not wish to define any <tt>Wire</tt> object's properties.
	 * @return The <tt>Wire</tt> object for this connection.
	 * @throws java.lang.IllegalArgumentException If
	 * <tt>properties</tt> contains case variants of the same key name.
	 */
	public Wire createWire(String producerPID, String consumerPID, Dictionary props) 
    {
        ServiceReference producerServiceRef = null;
        ServiceReference consumerServiceRef = null;

        Dictionary properties;
        
        if(props == null)
        {
        	properties = new Hashtable();
        }
        else
        {
        	//Clone the dictionary
        	properties = cloneProperties(props);
        }

        // Addition of mandatory properties
        properties.put(WireConstants.WIREADMIN_CONSUMER_PID, consumerPID);
        properties.put(WireConstants.WIREADMIN_PRODUCER_PID, producerPID);
        properties.put(WireConstants.WIREADMIN_PID, generateWirePID());

        // p.327 "Wire objects can be created when the producer or consumer
        // service is not registered
        WireImpl wire = new WireImpl(producerPID, consumerPID, properties);

        // Initialize the wire
        wire.initialize(m_bundleContext,m_eventManager);

        // Add the wire to the list 
        synchronized(m_wires)
        {
            m_wires.add(wire);            
        }

        // p. 357 "The Wire Admin service must broadcast a WireAdminEvent of 
        // type WireAdminEvent.WIRE_CREATED  after the new Wire object becomes 
        // available from getWires(java.lang.String)."
        m_eventManager.fireEvent(WireAdminEvent.WIRE_CREATED,wire);

        synchronized (m_producers)
        {
            Iterator producerIterator = m_producers.keySet().iterator();
            while(producerIterator.hasNext())
            {
                producerServiceRef = (ServiceReference) producerIterator.next();
                if (producerServiceRef.getProperty(Constants.SERVICE_PID).equals(producerPID)) 
                {
                    wire.bindProducer(producerServiceRef);                    
                    break;
                }
            }
        }        
        
        synchronized (m_consumers)
        {
            Iterator consumerIterator = m_consumers.keySet().iterator();
            while(consumerIterator.hasNext())
            {
                consumerServiceRef = (ServiceReference) consumerIterator.next();
                if (consumerServiceRef.getProperty(Constants.SERVICE_PID).equals(consumerPID)) 
                {
                    wire.bindConsumer(consumerServiceRef);
                    break;
                }
                
            }
        }
        
        
        // p.327 If both Producer and Consumer services are registered, they are 
        // connected by the wire admin service. 
        if(wire.isConnected())
        {
            List wires = (List) m_producers.get(producerServiceRef);
            wires.add(wire);
            m_asyncMethodCaller.consumersConnected(wire.getProducer(),(Wire[])wires.toArray(new Wire[wires.size()]));

            wires = (List) m_consumers.get(consumerServiceRef);
            wires.add(wire);
            m_asyncMethodCaller.producersConnected(wire.getConsumer(),(Wire[])wires.toArray(new Wire[wires.size()]));
        }

        // Newly created wires are immediately persisted to avoid information
        // loss in case of crashes.  (spec not clear about this)        
        persistWires();

        return wire;
    }

	/**
	 * Delete a <tt>Wire</tt> object.
	 *
	 * <p>The <tt>Wire</tt> object representing a connection between
	 * a Producer service and a Consumer service must be
	 * removed.
	 * The persistently stored configuration data for the <tt>Wire</tt> object
	 * must destroyed. The <tt>Wire</tt> object's method {@link Wire#isValid} will return <tt>false</tt>
	 * after it is deleted.
	 *
	 * <p>The Wire Admin service must broadcast a <tt>WireAdminEvent</tt> of type
	 * {@link WireAdminEvent#WIRE_DELETED}
	 * after the <tt>Wire</tt> object becomes invalid.
	 *
	 * @param wire The <tt>Wire</tt> object which is to be deleted.
	 */
	public void deleteWire(Wire wire) 
    {
        if(m_wires.contains(wire))
        {
            WireImpl wireImpl = (WireImpl) wire;
            m_wires.remove(wire);
            if(wireImpl.isConnected())
            {
                List wires = (List) m_producers.get(wireImpl.getProducerServiceRef());
                wires.remove(wireImpl);
                m_asyncMethodCaller.consumersConnected(wireImpl.getProducer(),(Wire[])wires.toArray(new Wire[wires.size()]));

                wires = (List) m_consumers.get(wireImpl.getConsumerServiceRef());
                wires.remove(wireImpl);
                m_asyncMethodCaller.producersConnected(wireImpl.getConsumer(),(Wire[])wires.toArray(new Wire[wires.size()]));
            }
            
            wireImpl.invalidate();

            // fire an event
            m_eventManager.fireEvent(WireAdminEvent.WIRE_DELETED,wireImpl);
            
            // Persist state to avoid losses in case of crashes (spec not clear about this).        
            persistWires();
        }
        else
        {
            traceln("WireAdminImpl: Cannot delete a wire that is not managed by this service");
        }

	}

	/**
	 * Update the properties of a <tt>Wire</tt> object.
	 *
	 * The persistently stored configuration data for the <tt>Wire</tt> object
	 * is updated with the new properties and then the Consumer and Producer
	 * services will be called at the respective {@link Consumer#producersConnected}
	 * and {@link Producer#consumersConnected} methods.
	 *
	 * <p>The Wire Admin service must broadcast a <tt>WireAdminEvent</tt> of type
	 * {@link WireAdminEvent#WIRE_UPDATED}
	 * after the updated properties are available from the <tt>Wire</tt> object.
	 *
	 * @param wire The <tt>Wire</tt> object which is to be updated.
	 * @param properties The new <tt>Wire</tt> object's properties or <tt>null</tt> if no properties are required.
	 */
	public void updateWire(Wire wire, Dictionary props) 
    {
        if(m_wires.contains(wire) == false)
        {
            traceln("WireAdminImpl: Cannot update a wire that is not managed by this service");
            return;
        }
        
        // Clone the dictionary
        Dictionary properties = cloneProperties(props);

        // Put again the mandatory properties, in case they are not set
        properties.put(WireConstants.WIREADMIN_CONSUMER_PID,wire.getProperties().get(WireConstants.WIREADMIN_CONSUMER_PID));
        properties.put(WireConstants.WIREADMIN_PRODUCER_PID,wire.getProperties().get(WireConstants.WIREADMIN_PRODUCER_PID));
        properties.put(WireConstants.WIREADMIN_PID,wire.getProperties().get(WireConstants.WIREADMIN_PID));
        
        WireImpl wireImpl = (WireImpl) wire;
        
        wireImpl.updateProperties(properties);
        
        // Call methods on Consumer and Producer
        if(wireImpl.isConnected())
        {
            List wires = (List) m_producers.get(wireImpl.getProducerServiceRef());
            m_asyncMethodCaller.consumersConnected(wireImpl.getProducer(),(Wire[])wires.toArray(new Wire[wires.size()]));

            wires = (List) m_consumers.get(wireImpl.getConsumerServiceRef());
            m_asyncMethodCaller.producersConnected(wireImpl.getConsumer(),(Wire[])wires.toArray(new Wire[wires.size()]));
        }

        // fire an event
        m_eventManager.fireEvent(WireAdminEvent.WIRE_UPDATED,wireImpl);
	}

	/**
	 * Return the <tt>Wire</tt> objects that match the given <tt>filter</tt>.
	 *
	 * <p>The list of available <tt>Wire</tt> objects is matched against the
	 * specified <tt>filter</tt>. <tt>Wire</tt> objects which match the
	 * <tt>filter</tt> must be returned. These <tt>Wire</tt> objects are not necessarily
	 * connected. The Wire Admin service should not return
	 * invalid <tt>Wire</tt> objects, but it is possible that a <tt>Wire</tt>
	 * object is deleted after it was placed in the list.
	 *
	 * <p>The filter matches against the <tt>Wire</tt> object's properties including
	 * {@link WireConstants#WIREADMIN_PRODUCER_PID}, {@link WireConstants#WIREADMIN_CONSUMER_PID}
	 * and {@link WireConstants#WIREADMIN_PID}.
	 *
	 * @param filter Filter string to select <tt>Wire</tt> objects
	 * or <tt>null</tt> to select all <tt>Wire</tt> objects.
	 * @return An array of <tt>Wire</tt> objects which match the <tt>filter</tt>
	 * or <tt>null</tt> if no <tt>Wire</tt> objects match the <tt>filter</tt>.
	 * @throws org.osgi.framework.InvalidSyntaxException If the specified <tt>filter</tt>
	 * has an invalid syntax.
	 * @see org.osgi.framework.Filter
	 */
	public Wire[] getWires(String filter) throws InvalidSyntaxException 
    {
		List res = null;
		if (filter == null) 
        {
            return (Wire [])m_wires.toArray(new Wire[m_wires.size()]);
		} 
        else 
        {
			Filter tempFilter = m_bundleContext.createFilter(filter);
			Iterator iter = m_wires.iterator();
			while (iter.hasNext()) 
            {
				WireImpl currentWire = (WireImpl) iter.next();
				if (tempFilter.match(currentWire.getProperties())) 
                {
					if (res == null)
                    {
						res = new ArrayList();
                    }
					res.add(currentWire);
				}
			}
		}
		if (res == null) 
        {
			return null;
		} 
        else 
        {
            return (Wire [])res.toArray(new Wire[res.size()]);
		}
	}

    /**
     * listens Producer and Consumer services changes
     * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
     */
    public void serviceChanged(ServiceEvent e) 
    {
		ServiceReference serviceRef = e.getServiceReference();
		// A consumer service changed
		if (m_consumerFilter.match(serviceRef)) 
        {
			switch (e.getType()) 
            {
                case ServiceEvent.REGISTERED :
                    traceln("consumer registered");
                    
                    List wires = new ArrayList();

                    synchronized(m_consumers)
                    {
                        m_consumers.put(serviceRef,wires);
                    }
                    synchronized(m_wires)
                    {
                        Iterator wireIt = m_wires.iterator();
                        boolean called = false;
                        // Iterate over all existing wires
                        while(wireIt.hasNext())
                        {
                            WireImpl currentWire = (WireImpl) wireIt.next();

                            if(currentWire.getConsumerPID().equals(serviceRef.getProperty(Constants.SERVICE_PID)))
                            {
                                // This wire is associated to the newly arrived consumer
                                currentWire.bindConsumer(serviceRef);
                                if(currentWire.isConnected())
                                {
                                    // The wire has been connected, both producer and consumer
                                    // must be updated
                                    wires.add(currentWire);
                                    called = true;
                                    m_asyncMethodCaller.producersConnected(currentWire.getConsumer(),(Wire[])wires.toArray(new Wire[wires.size()]));
                                    List producerWires = (List) m_producers.get(currentWire.getProducerServiceRef());
                                    producerWires.add(currentWire);
                                    m_asyncMethodCaller.consumersConnected(currentWire.getProducer(),(Wire[])producerWires.toArray(new Wire[producerWires.size()]));                                    
                                }
                            }
                        }
                        if(!called)
                        {
                            // P. 329 "If the Consumer service has no Wire objects attached when it
                            // is registered, the WireAdmin service must always call producersConnected(null)
                            m_asyncMethodCaller.producersConnected((Consumer) m_bundleContext.getService(serviceRef),null);
                        }
                    }
                    break;
                case ServiceEvent.UNREGISTERING :
                    traceln("consumer unregistering");
                    
                    synchronized(m_consumers)
                    {
                        m_consumers.remove(serviceRef);
                    }
                    synchronized(m_wires)
                    {
                        Iterator wireIt = m_wires.iterator();
                        while(wireIt.hasNext())
                        {
                            WireImpl currentWire = (WireImpl) wireIt.next();
                            if(currentWire.getConsumerPID().equals(serviceRef.getProperty(Constants.SERVICE_PID)))
                            {
                                // p. 328 "When a Consumer or Producer service is unregistered
                                // from the OSGi framework, the other object in the association
                                // is informed that the Wire object is no longer valid"

                                if(currentWire.isConnected())
                                {
                                    currentWire.unbindConsumer();                               
                                    List producerWires = (List) m_producers.get(currentWire.getProducerServiceRef());
                                    producerWires.remove(currentWire);
                                    m_asyncMethodCaller.consumersConnected(currentWire.getProducer(),(Wire[])producerWires.toArray(new Wire[producerWires.size()]));
                                }
                                else
                                {
                                    currentWire.unbindConsumer();    
                                }
                            }
                        }
                    }
                    break;
                case ServiceEvent.MODIFIED :
                    // TODO Respond to consumer service modification
                    traceln("consumer service modified");
                    break;

            }
        }
        // Removed else to manage services which are both producers AND consumers
		if (m_producerFilter.match(serviceRef)) 
        {
            switch (e.getType()) 
            {
                case ServiceEvent.REGISTERED :
                    traceln("producer registered");
                    
                    List wires = new ArrayList();

                    synchronized(m_producers)
                    {
                        m_producers.put(serviceRef,wires);
                    }
                    synchronized(m_wires)
                    {
                        Iterator wireIt = m_wires.iterator();
                        boolean called = false;
                        // Iterate over all existing wires
                        while(wireIt.hasNext())
                        {
                            WireImpl currentWire = (WireImpl) wireIt.next();
                            if(currentWire.getProducerPID().equals(serviceRef.getProperty(Constants.SERVICE_PID)))
                            {
                                // This wire is associated to the newly arrived producer
                                currentWire.bindProducer(serviceRef);
                                if(currentWire.isConnected())
                                {
                                    // The wire has been connected, both producer and consumer
                                    // must be updated
                                    wires.add(currentWire);
                                    called = true;
                                    m_asyncMethodCaller.consumersConnected(currentWire.getProducer(),(Wire[])wires.toArray(new Wire[wires.size()]));
                                    List consumerWires = (List) m_consumers.get(currentWire.getConsumerServiceRef());
                                    consumerWires.add(currentWire);
                                    m_asyncMethodCaller.producersConnected(currentWire.getConsumer(),(Wire[])consumerWires.toArray(new Wire[consumerWires.size()]));                                    
                                }
                            }
                        }
                        if(!called)
                        {
                            // P. 329 "If the Producer service has no Wire objects attached when it
                            // is registered, the WireAdmin service must always call consumersConnected(null)
                            m_asyncMethodCaller.consumersConnected((Producer) m_bundleContext.getService(serviceRef),null);
                        }
                    }
                    break;
                case ServiceEvent.UNREGISTERING :
                    traceln("Producer unregistering");
                    
                    synchronized(m_producers)
                    {
                        m_producers.remove(serviceRef);
                    }
                    synchronized(m_wires)
                    {
                        Iterator wireIt = m_wires.iterator();
                        while(wireIt.hasNext())
                        {
                            WireImpl currentWire = (WireImpl) wireIt.next();
                            if(currentWire.getProducerPID().equals(serviceRef.getProperty(Constants.SERVICE_PID)))
                            {
                                // p. 328 "When a Consumer or Producer service is unregistered
                                // from the OSGi framework, the other object in the association
                                // is informed that the Wire object is no longer valid"

                                if(currentWire.isConnected())
                                {
                                    currentWire.unbindProducer();                               
                                    List consumerWires = (List) m_consumers.get(currentWire.getConsumerServiceRef());
                                    consumerWires.remove(currentWire);
                                    m_asyncMethodCaller.producersConnected(currentWire.getConsumer(),(Wire[])consumerWires.toArray(new Wire[consumerWires.size()]));
                                }
                                else
                                {
                                    currentWire.unbindProducer();    
                                }
                            }
                        }
                    }
                    break;
                case ServiceEvent.MODIFIED :
                    // TODO Respond to producer service modification
                    traceln("producer service modified");
                    break;
            }
        }        
    }


	/**
	 * release all references before stop
	 */
	synchronized void releaseAll() 
    {
        Iterator wireIt = m_wires.iterator();
        while(wireIt.hasNext())
        {
            WireImpl currentWire = (WireImpl) wireIt.next();
            currentWire.invalidate();
        }
        
        Iterator producerIt = m_producers.keySet().iterator();        
        while (producerIt.hasNext())
        {
            ServiceReference producerRef = (ServiceReference) producerIt.next();
            ((Producer)m_bundleContext.getService(producerRef)).consumersConnected(null);            
        }
        
        Iterator consumerIt = m_consumers.keySet().iterator();        
        while (consumerIt.hasNext())
        {
            ServiceReference consumerRef = (ServiceReference) consumerIt.next();
            ((Consumer)m_bundleContext.getService(consumerRef)).producersConnected(null);            
        }

        // Stop the thread
        m_asyncMethodCaller.stop();
        
        // Notify the event manager so that it stops its thread
        m_eventManager.stop();
        
        persistWires();

	}

    /**
     * This method generates a PID. The pid is generated from the bundle id,
     * a hash code from the current time and a counter.
     * 
     * @return a wire PID
     */
    private String generateWirePID()
    {
        Date d = new Date();
        String PID="wire."+m_bundleContext.getBundle().getBundleId()+d.hashCode()+m_wireCount;
        m_wireCount ++;
        
        // Maybe the counter should go above 9?
        if(m_wireCount>9)
        {
            m_wireCount = 0;
        }
        return PID;
    }
    
    /**
     * Recover persistent wires 
     *
     */
    private void getPersistentWires()
    {
        
        try
        {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(m_bundleContext.getDataFile("wires.ser")));
            m_wires = (ArrayList) ois.readObject();
            ois.close();
            if(m_wires!=null)
            {
                traceln("Deserialized "+m_wires.size()+" wires");
                Iterator wireIt = m_wires.iterator();
                while(wireIt.hasNext())
                {
                    WireImpl currentWire = (WireImpl) wireIt.next();
                    currentWire.initialize(m_bundleContext,m_eventManager);     
                }
            }
            else
            {
                traceln("Couldn't Deserialize wires");
                m_wires = new ArrayList();
            }
        }
        catch(FileNotFoundException ex)
        {
            // do not show anything as this exception is thrown every
            // time the wire admin service is launched for the first
            // time
            m_wires = new ArrayList();
        }
        catch(Exception ex)
        {
            trace(ex);
            m_wires = new ArrayList();
        }
    }
    
    /**
     * Persist existing wires
     *
     */
    private void persistWires()
    {
        try
        {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(m_bundleContext.getDataFile("wires.ser")));
            oos.writeObject(m_wires);
            oos.close();
            traceln("Serialized "+m_wires.size()+" wires");
        }
        catch(Exception ex)
        {
            trace(ex);
        }
    }

    /**
     * print an error 
     * @param message message to error 
     */
    static void error(String message)
    {
        if (m_errorout != null) 
        {
            m_errorout.println(message);
        }
    }

	/**
	 * print a trace 
	 * @param message message to trace
	 */
	static void traceln(String message)
    {
		if (m_traceout != null) 
        {
			trace(message);
			trace("\n");
		}
	}

	/**
	 * print a trace 
	 * @param message message to trace
	 */
	static void trace(String message)
    {
		if (m_traceout != null) 
        {
			m_traceout.print(message);
		}
	}
	/**
	 * print a trace 
	 * @param e exception to trace
	 */
	static void trace(Exception e) 
    {
		if (m_traceout != null) 
        {
			e.printStackTrace(m_traceout);
		}
	}

	/**
	 * Clone a dictionary
	 * 
	 * @param dictionary The dictionary to clone
	 * @return a copy of the dicionary
	 */
    private Dictionary cloneProperties(Dictionary dictionary){
        Dictionary properties=new Hashtable();
        
        if (dictionary == null) {
            properties = new Hashtable();
        } else {
            Enumeration enumeration=dictionary.keys();
            while(enumeration.hasMoreElements()){
                Object key=enumeration.nextElement();
                Object value=dictionary.get(key);
                properties.put(key,value);
            }
        }
        
        return properties;
    }
	
    /**
     * This class enables calls to Producer.consumersConnected and Consumer.producersConnected
     * to be done asynchronously
     * 
     * p.333 "The WireAdmin service can call the consumersConnected or producersConnected
     * methods during the registration of the consumer of producer service"
     *
    **/
    class AsyncMethodCaller implements Runnable
    {
        private boolean m_stop = false;

        private List m_methodCallStack = new ArrayList();

        public void run()
        {
            while (!m_stop)
            {
                Object nextTarget[] = null;

                synchronized (m_methodCallStack)
                {
                    while (m_methodCallStack.size() == 0)
                    {
                        try
                        {
                            m_methodCallStack.wait();
                        } 
                        catch (InterruptedException ex)
                        {
                            // Ignore.
                        }
                    }
                    nextTarget = (Object[]) m_methodCallStack.remove(0);
                }
                
                if(nextTarget[0] instanceof Producer)
                {
                    try
                    {
                        ((Producer)nextTarget[0]).consumersConnected((Wire[])nextTarget[1]);
                    }
                    catch(Exception ex)
                    {
                        trace(ex);
                    }
                }
                // Removed else because nextTarget can be both producer and consumer                
                if(nextTarget[0] instanceof Consumer)
                {
                    try
                    {
                        ((Consumer)nextTarget[0]).producersConnected((Wire[])nextTarget[1]);
                    }
                    catch(Exception ex)
                    {
                        trace(ex);
                    }
                }
            }
        }

        /**
         * Place a call to Consumer.producersConnected on the stack
         * 
         * @param c the consumer
         * @param wires the wires
         */
        public void producersConnected(Consumer c,Wire []wires)
        {
            synchronized (m_methodCallStack)
            {
                m_methodCallStack.add(new Object[]{c,wires});
                m_methodCallStack.notify();
            }
        }

        /**
         * Place a call to Producer.consumersConnected on the stack
         * 
         * @param p the producer
         * @param wires the wires
         */
        public void consumersConnected(Producer p,Wire []wires)
        {
            synchronized (m_methodCallStack)
            {
                m_methodCallStack.add(new Object[]{p,wires});
                m_methodCallStack.notify();
            }
        }

        /**
         * stop the dispatcher
         *
         */
        void stop()
        {
            m_stop = true;
        }
    }
}
