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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Date;

import org.osgi.framework.Filter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;

import org.osgi.service.wireadmin.Consumer;
import org.osgi.service.wireadmin.Producer;
import org.osgi.service.wireadmin.Wire;
import org.osgi.service.wireadmin.WireConstants;
import org.osgi.service.wireadmin.WireAdminEvent;

/**
 * A connection between a Producer service and a Consumer service.
 *
 * <p>A <tt>Wire</tt> object connects a Producer service
 * to a Consumer service.
 * Both the Producer and Consumer services are identified
 * by their unique <tt>service.pid</tt> values.
 * The Producer and Consumer services may communicate with
 * each other via <tt>Wire</tt> objects that connect them.
 * The Producer service may send updated values to the
 * Consumer service by calling the {@link #update} method.
 * The Consumer service may request an updated value from the
 * Producer service by calling the {@link #poll} method.
 *
 * <p>A Producer service and a Consumer service may be
 * connected through multiple <tt>Wire</tt> objects.
 *
 * <p>Security Considerations. <tt>Wire</tt> objects are available to
 * Producer and Consumer services connected to a given
 * <tt>Wire</tt> object and to bundles which can access the <tt>WireAdmin</tt> service.
 * A bundle must have <tt>ServicePermission[GET,WireAdmin]</tt> to get the <tt>WireAdmin</tt> service to
 * access all <tt>Wire</tt> objects.
 * A bundle registering a Producer service or a Consumer service
 * must have the appropriate <tt>ServicePermission[REGISTER,Consumer|Producer]</tt> to register the service and
 * will be passed <tt>Wire</tt> objects when the service object's
 * <tt>consumersConnected</tt> or <tt>producersConnected</tt> method is called.
 *
 * <p>Scope. Each Wire object can have a scope set with the <tt>setScope</tt> method. This
 * method should be called by a Consumer service when it assumes a Producer service that is
 * composite (supports multiple information items). The names in the scope must be
 * verified by the <tt>Wire</tt> object before it is used in communication. The semantics of the
 * names depend on the Producer service and must not be interpreted by the Wire Admin service.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a> * 
 */
public class WireImpl implements Wire, java.io.Serializable 
{
	static final long serialVersionUID = -3637966367104019136L;
	
    // Persistent attributes
    
    // p. 327 "The Wire admin object contains a Persistend Identity (PID) for
    // a consumer service and a PID for a producer service" These properties
    // are also contained in the dictionary, but they are stored to optimize
    // access. 
    private String m_producerPID;
    private String m_consumerPID;

    private Dictionary m_properties;
    
    // Transient attributes

	transient private boolean m_isValid = true;
    transient private boolean m_isConnected = false;
    transient private Filter m_filter = null;
    
    transient private ServiceReference m_producerServiceRef;
    transient private Producer m_producer;
    transient private boolean m_producerIsComposite = false;
    transient private String [] m_producerScope = null;
    
    transient private ServiceReference m_consumerServiceRef;	
	transient private Consumer m_consumer;
    transient private boolean m_consumerIsComposite = false;
    transient private String [] m_consumerScope = null;
    
    transient private BundleContext m_bundleContext;
    transient private EventManager m_eventManager;
	transient private Object m_lastValue;
	transient private String[] m_scope;
    
    transient private long m_lastUpdate;    
    transient private boolean m_isFirstUpdate;
    transient FilterDictionary m_dictionary;
    
    /**
     * Constructor with package visibility
     * 
     * @param producerPID
     * @param consumerPID
     * @param properties
     */
	WireImpl(String producerPID, String consumerPID, Dictionary properties)
    {
		m_producerPID = producerPID;
		m_consumerPID = consumerPID;
		m_properties = properties;
		
		// set the scope which is the intersection of strings in the WIREADMIN_PRODUCER_SCOPE properties and the strings in the WIREADMIN_CONSUMER_SCOPE
		m_scope = null;
	}
    
    /**
     * Method called after construction and after deserialization
     * 
     * @param ctxt
     * @param eventManager
     */
    void initialize(BundleContext ctxt, EventManager eventManager)
    {
        m_isValid = true;
        m_isConnected = false;

        m_bundleContext = ctxt;
        m_eventManager = eventManager;
        
        m_lastValue = null;
        
        m_lastUpdate = 0;
        
        m_isFirstUpdate = true;
        
        /*
        if(m_date == null)
        {
            m_date = new Date();
        }
        */
        m_dictionary = new FilterDictionary();
    }

	/**
	 * Return the state of this <tt>Wire</tt> object.
	 *
	 * <p>A connected <tt>Wire</tt> must always be disconnected before
	 * becoming invalid.
	 *
	 * @return <tt>false</tt> if this <tt>Wire</tt> object is invalid because it
	 * has been deleted via {@link WireAdmin#deleteWire};
	 * <tt>true</tt> otherwise.
	 */
	public boolean isValid() 
    {
		return m_isValid;
	}

	/**
	 * Return the connection state of this <tt>Wire</tt> object.
	 *
	 * <p>A <tt>Wire</tt> is connected after the Wire Admin service receives
	 * notification that the Producer service and
	 * the Consumer service for this <tt>Wire</tt> object are both registered.
	 * This method will return <tt>true</tt> prior to notifying the Producer
	 * and Consumer services via calls
	 * to their respective <tt>consumersConnected</tt> and <tt>producersConnected</tt>
	 * methods.
	 * <p>A <tt>WireAdminEvent</tt> of type {@link WireAdminEvent#WIRE_CONNECTED}
	 * must be broadcast by the Wire Admin service when
	 * the <tt>Wire</tt> becomes connected.
	 *
	 * <p>A <tt>Wire</tt> object
	 * is disconnected when either the Consumer or Producer
	 * service is unregistered or the <tt>Wire</tt> object is deleted.
	 * <p>A <tt>WireAdminEvent</tt> of type {@link WireAdminEvent#WIRE_DISCONNECTED}
	 * must be broadcast by the Wire Admin service when
	 * the <tt>Wire</tt> becomes disconnected.
	 *
	 * @return <tt>true</tt> if both the Producer and Consumer
	 * for this <tt>Wire</tt> object are connected to the <tt>Wire</tt> object;
	 * <tt>false</tt> otherwise.
	 */
	public boolean isConnected() 
    {
		return m_isConnected;
	}

	/**
	 * Return the list of data types understood by the
	 * Consumer service connected to this <tt>Wire</tt> object. Note that
	 * subclasses of the classes in this list are acceptable data types as well.
	 *
	 * <p>The list is the value of the {@link WireConstants#WIREADMIN_CONSUMER_FLAVORS}
	 * service property of the
	 * Consumer service object connected to this object. If no such
	 * property was registered or the type of the property value is not
	 * <tt>Class[]</tt>, this method must return <tt>null</tt>.
	 *
	 * @return An array containing the list of classes understood by the
	 * Consumer service or <tt>null</tt> if
	 * the <tt>Wire</tt> is not connected,
	 * or the consumer did not register a {@link WireConstants#WIREADMIN_CONSUMER_FLAVORS} property
	 * or the value of the property is not of type <tt>Class[]</tt>.
	 */

	public Class[] getFlavors() 
    {
        if(isConnected())
        {
            try
            {
                return (Class [])m_consumerServiceRef.getProperty(WireConstants.WIREADMIN_CONSUMER_FLAVORS);
            }
            catch(ClassCastException ex)
            {
                return null;
            }
        }
        else
        {
            return null;

        }
	}

	/**
	 * Update the value.
	 *
	 * <p>This methods is called by the Producer service to
	 * notify the Consumer service connected to this <tt>Wire</tt> object
	 * of an updated value.
	 * <p>If the properties of this <tt>Wire</tt> object contain a
	 * {@link WireConstants#WIREADMIN_FILTER} property,
	 * then filtering is performed.
	 * If the Producer service connected to this <tt>Wire</tt>
	 * object was registered with the service
	 * property {@link WireConstants#WIREADMIN_PRODUCER_FILTERS}, the
	 * Producer service will perform the filtering according to the rules specified
	 * for the filter. Otherwise, this <tt>Wire</tt> object
	 * will perform the filtering of the value.
	 * <p>If no filtering is done, or the filter indicates the updated value should
	 * be delivered to the Consumer service, then
	 * this <tt>Wire</tt> object must call
	 * the {@link Consumer#updated} method with the updated value.
	 * If this <tt>Wire</tt> object is not connected, then the Consumer
	 * service must not be called and the value is ignored.<p>
	 * If the value is an <tt>Envelope</tt> object, and the scope name is not permitted, then the
	 * <tt>Wire</tt> object must ignore this call and not transfer the object to the Consumer
	 * service.
	 *
	 * <p>A <tt>WireAdminEvent</tt> of type {@link WireAdminEvent#WIRE_TRACE}
	 * must be broadcast by the Wire Admin service after
	 * the Consumer service has been successfully called.
	 *
	 * @param value The updated value. The value should be an instance of
	 * one of the types returned by {@link #getFlavors}.
	 * @see WireConstants#WIREADMIN_FILTER
	 */
	public void update(Object value) 
    {
        // TODO Implement Access Control (p. 338)
		if (isConnected())
        {       
            if(m_producerIsComposite == true)
            {
                // TODO Implement update for composite producers
                WireAdminImpl.traceln("WireImpl.update: update for composite producers not yet implemented");
                return;
            }
            else // not a composite (Note: p. 341 "Filtering for composite producer services is not supported")
            {
                //long time = m_date.getTime();
                long time = new Date().getTime();
                
                // We ignore filtering the first time...
                if(m_isFirstUpdate == false && m_filter != null)
                {
                    // If the Producer service was registered with the WIREADMIN_PRODUCER_FILTERS  
                    // service property indicating that the Producer service will perform the data 
                    // filtering then the Wire object will not perform data filtering. Otherwise, 
                    // the Wire object must perform basic filtering.
                    try
                    {
                        m_dictionary.reset(value,time);
                        if(!m_filter.match(m_dictionary))
                        {
                            WireAdminImpl.traceln("### Update rejected ("+m_properties.get(WireConstants.WIREADMIN_PID)+") filter evaluated to false:"+m_filter);
                            WireAdminImpl.traceln("  WIREVALUE_CURRENT.class"+m_dictionary.get(WireConstants.WIREVALUE_CURRENT).getClass().getName());
                            WireAdminImpl.traceln("  WIREVALUE_CURRENT="+m_dictionary.get(WireConstants.WIREVALUE_CURRENT));
                            WireAdminImpl.traceln("  WIREVALUE_PREVIOUS="+m_dictionary.get(WireConstants.WIREVALUE_PREVIOUS));
                            WireAdminImpl.traceln("  WIREVALUE_DELTA_ABSOLUTE="+m_dictionary.get(WireConstants.WIREVALUE_DELTA_ABSOLUTE));
                            WireAdminImpl.traceln("  WIREVALUE_DELTA_RELATIVE="+m_dictionary.get(WireConstants.WIREVALUE_DELTA_RELATIVE));
                            WireAdminImpl.traceln("  WIREVALUE_ELAPSED="+m_dictionary.get(WireConstants.WIREVALUE_ELAPSED));
                            return;
                        }
                    }
                    catch(Exception ex)
                    {
                        // Could happen...
                        WireAdminImpl.trace(ex);
                    }
    
                }
                try
                {
        			m_consumer.updated(this, value);
                    if(m_isFirstUpdate == true)
                    {
                        m_isFirstUpdate = false;
                    }
                    m_lastUpdate = time;
                    m_lastValue = value;
                    // Fire event
                    m_eventManager.fireEvent(WireAdminEvent.WIRE_TRACE,this);
                }
                catch(Exception ex)
                {
                    m_eventManager.fireEvent(WireAdminEvent.CONSUMER_EXCEPTION,this,ex);
                }
            }
        }
	}

	/**
	 * Poll for an updated value.
	 *
	 * <p>This methods is normally called by the Consumer service to
	 * request an updated value from the Producer service
	 * connected to this <tt>Wire</tt> object.
	 * This <tt>Wire</tt> object will call
	 * the {@link Producer#polled} method to obtain an updated value.
	 * If this <tt>Wire</tt> object is not connected, then the Producer
	 * service must not be called.<p>
	 *
	 * If this <tt>Wire</tt> object has a scope, then this method
	 * must return an array of <tt>Envelope</tt> objects. The objects returned must
	 * match the scope of this object. The <tt>Wire</tt> object must remove
	 * all <tt>Envelope</tt> objects with a scope name that is not in the <tt>Wire</tt> object's scope.
	 * Thus, the list of objects returned
	 * must only contain <tt>Envelope</tt> objects with a permitted scope name. If the
	 * array becomes empty, <tt>null</tt> must be returned.
	 *
	 * <p>A <tt>WireAdminEvent</tt> of type {@link WireAdminEvent#WIRE_TRACE}
	 * must be broadcast by the Wire Admin service after
	 * the Producer service has been successfully called.
	 *
	 * @return A value whose type should be one of the types
	 * returned by {@link #getFlavors}, <tt>Envelope[]</tt>, or <tt>null</tt> if
	 * the <tt>Wire</tt> object is not connected,
	 * the Producer service threw an exception, or
	 * the Producer service returned a value which is not an instance of
	 * one of the types returned by {@link #getFlavors}.
	 */
	public Object poll() {
        // p.330 "Update filtering must not apply to polling"
		if (isConnected()) 
        {
            try
            {
    			Object value = m_producer.polled(this);
                Class []flavors = getFlavors();
                
                boolean valueOk = false;
                
                // Test if the value is ok with respect to the flavors understood by
                // the consumer
                for(int i=0; i<flavors.length; i++)
                {
                    Class currentClass = flavors[i];
                    if(currentClass.isInstance(value))
                    {
                        valueOk = true;
                    }                    
                }
                if(valueOk)
                {
                    m_eventManager.fireEvent(WireAdminEvent.WIRE_TRACE,this);
                    m_lastValue = value;
        			return m_lastValue;
                }
                else
                {
                    WireAdminImpl.traceln("WireImpl.poll: value returned by producer is not undestood by consumer");
                    return null;
                }
            }
            catch(Exception ex)
            {
                m_eventManager.fireEvent(WireAdminEvent.PRODUCER_EXCEPTION,this,ex);
                return null;
            }
		} 
        else
        {
            // p. 333 "If the poll() method on the wire object is called and the
            // producer is unregistered it must return a null value"
			return null;
        }
	}

	/**
	 * Return the last value sent through this <tt>Wire</tt> object.
	 *
	 * <p>The returned value is the most recent, valid value passed to the
	 * {@link #update} method or returned by the {@link #poll} method
	 * of this object. If filtering is performed by this <tt>Wire</tt> object,
	 * this methods returns the last value provided by the Producer service. This
	 * value may be an <tt>Envelope[]</tt> when the Producer service
	 * uses scoping. If the return value is an Envelope object (or array), it
	 * must be verified that the Consumer service has the proper WirePermission to see it.
	 *
	 * @return The last value passed though this <tt>Wire</tt> object
	 * or <tt>null</tt> if no valid values have been passed or the Consumer service has no permission.
	 */
	public Object getLastValue() 
    {
		return m_lastValue;
	}

	/**
	 * Return the wire properties for this <tt>Wire</tt> object.
	 *
	 * @return The properties for this <tt>Wire</tt> object.
	 * The returned <tt>Dictionary</tt> must be read only.
	 */
	public Dictionary getProperties() 
    {
		return m_properties;
	}

	/**
	 * Return the calculated scope of this <tt>Wire</tt> object.
	 *
	 * The purpose of the <tt>Wire</tt> object's scope is to allow a Producer
	 * and/or Consumer service to produce/consume different types
	 * over a single <tt>Wire</tt> object (this was deemed necessary for efficiency
	 * reasons). Both the Consumer service and the
	 * Producer service must set an array of scope names (their scope) with
	 * the service registration property <tt>WIREADMIN_PRODUCER_SCOPE</tt>, or <tt>WIREADMIN_CONSUMER_SCOPE</tt> when they can
	 * produce multiple types. If a Producer service can produce different types, it should set this property
	 * to the array of scope names it can produce, the Consumer service
	 * must set the array of scope names it can consume. The scope of a <tt>Wire</tt>
	 * object is defined as the intersection of permitted scope names of the
	 * Producer service and Consumer service.
	 * <p>If neither the Consumer, or the Producer service registers scope names with its
	 * service registration, then the <tt>Wire</tt> object's scope must be <tt>null</tt>.
	 * <p>The <tt>Wire</tt> object's scope must not change when a Producer or Consumer services
	 * modifies its scope.
	 * <p>A scope name is permitted for a Producer service when the registering bundle has
	 * <tt>WirePermission[PRODUCE]</tt>, and for a Consumer service when
	 * the registering bundle has <tt>WirePermission[CONSUME]</tt>.<p>
	 * If either Consumer service or Producer service has not set a <tt>WIREADMIN_*_SCOPE</tt> property, then
	 * the returned value must be <tt>null</tt>.<p>
	 * If the scope is set, the <tt>Wire</tt> object must enforce the scope names when <tt>Envelope</tt> objects are
	 * used as a parameter to update or returned from the <tt>poll</tt> method. The <tt>Wire</tt> object must then
	 * remove all <tt>Envelope</tt> objects with a scope name that is not permitted.
	 *
	 * @return A list of permitted scope names or null if the Produce or Consumer service has set no scope names.
	 */
	public String[] getScope() 
    {
		return m_scope;
	}

	/**
	 * Return true if the given name is in this <tt>Wire</tt> object's scope.
	 *
	 * @param name The scope name
	 * @return true if the name is listed in the permitted scope names
	 */
	public boolean hasScope(String name) 
    {
		if (m_scope != null) 
        {
			for (int i = 0; i < m_scope.length; i++) 
            {
				if (name.equals(m_scope[i])) 
                {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() 
    {
		return super.toString()
			+ ":["
			+ getProperties().get(WireConstants.WIREADMIN_PID)
			+ ","
			+ getProperties().get(WireConstants.WIREADMIN_PRODUCER_PID)
			+ ","
			+ getProperties().get(WireConstants.WIREADMIN_CONSUMER_PID)
			+ "]";
	}

    /**
     * Bind the consumer.
     * 
     * @param consumer A <tt>ServiceReference</tt> corresponding to the consumer service
     */
    void bindConsumer(ServiceReference consumerRef)
    {
        if(m_consumerServiceRef != null)
        {
            WireAdminImpl.traceln("WireImpl: consumer already bound!");
            return;
        }
        
        // Pid must be present
        if(consumerRef.getProperty(Constants.SERVICE_PID) == null)
        {
            WireAdminImpl.traceln("WireImpl: Consumer service has no PID "+consumerRef);
            return;
        }
        
        // Flavors must be present
        if(consumerRef.getProperty(WireConstants.WIREADMIN_CONSUMER_FLAVORS) == null)
        {
            WireAdminImpl.traceln("WireImpl: Consumer service has no WIREADMIN_CONSUMER_FLAVORS "+consumerRef);
            return;
        }

        // Is this a composite?
        if(consumerRef.getProperty(WireConstants.WIREADMIN_CONSUMER_COMPOSITE) != null)
        {
            m_consumerIsComposite = true;
            m_consumerScope = (String []) consumerRef.getProperty(WireConstants.WIREADMIN_CONSUMER_SCOPE);

        }

        m_consumerServiceRef = consumerRef;
        m_consumer = (Consumer) m_bundleContext.getService(consumerRef);
        
        if(m_producer != null)
        {
            if(m_producerIsComposite)
            {
                if(matchComposites() == false)
                {
                    WireAdminImpl.traceln("WireImpl.bindConsumer : warning composite identities do not match");
                }
            }
            m_isConnected = true;
            m_eventManager.fireEvent(WireAdminEvent.WIRE_CONNECTED,this);
        }
    }
    
    /**
     * Unbind the consumer
     *
     */
    void unbindConsumer()
    {
    	// This test is made because knopflerfish doesn't support ungetService(null);
    	if(m_consumerServiceRef != null)
    	{
	        m_bundleContext.ungetService(m_consumerServiceRef);
	        if(m_isConnected)
	        {
	            m_isConnected = false;
	            m_eventManager.fireEvent(WireAdminEvent.WIRE_DISCONNECTED,this);
	        }
	        m_consumer = null;
	        m_consumerServiceRef = null;
    	}
    }
    
    /**
     * Bind the producer
     * 
     * @param producer A <tt>ServiceReference</tt> corresponding to the producer service
     */
    void bindProducer(ServiceReference producerRef)
    {
        if(m_producerServiceRef != null)
        {
            WireAdminImpl.traceln("WireImpl: producer already bound!");
            return;
        }
        
        // Pid must be present
        if(producerRef.getProperty(Constants.SERVICE_PID) == null)
        {
            WireAdminImpl.traceln("WireImpl.bindProducer: Producer service has no PID "+producerRef);
            return;
        }

        // Flavors must be present
        if(producerRef.getProperty(WireConstants.WIREADMIN_PRODUCER_FLAVORS) == null)
        {
            WireAdminImpl.traceln("WireImpl: Consumer service has no WIREADMIN_PRODUCER_FLAVORS "+producerRef);
            return;
        }

        // Is this a composite?
        if(producerRef.getProperty(WireConstants.WIREADMIN_PRODUCER_COMPOSITE) != null)
        {
            m_producerIsComposite = true;
            m_producerScope = (String []) producerRef.getProperty(WireConstants.WIREADMIN_PRODUCER_SCOPE);
        }

        m_producerServiceRef = producerRef;
        m_producer = (Producer) m_bundleContext.getService(producerRef);
        
        // p. 329 " If this property (wireadmin.producer.filters) is not set,
        // the Wire object must filter according to the description in CompositeObjects"
        if(producerRef.getProperty(WireConstants.WIREADMIN_PRODUCER_FILTERS) == null)
        {
            String filter = (String) m_properties.get(WireConstants.WIREADMIN_FILTER);
            if(filter != null)
            {
                try
                {
                    m_filter = m_bundleContext.createFilter(filter);
                }
                catch(InvalidSyntaxException ex)
                {
                    WireAdminImpl.traceln("WireImpl.bindProducer: Ignoring filter with invalid syntax "+filter);                    
                }
            }
        }
        
        if(m_consumer != null)
        {
            if(m_consumerIsComposite)
            {
                if(matchComposites() == false)
                {
                    WireAdminImpl.traceln("WireImpl.bindProducer : warning composite identities do not match");
                }
            }
            m_isConnected = true;
            // fire the corresponding event
            m_eventManager.fireEvent(WireAdminEvent.WIRE_CONNECTED,this);
        }
    }
    
    /**
     * Unbind the producer
     *
     */
    void unbindProducer()
    {
    	if(m_producerServiceRef != null)
    	{
	        m_bundleContext.ungetService(m_producerServiceRef);
	        if(m_isConnected)
	        {
	            m_isConnected = false;
	            // fire the corresponding event
	            m_eventManager.fireEvent(WireAdminEvent.WIRE_DISCONNECTED,this);
	        }
	        m_producer = null;
	        m_producerServiceRef = null;
    	}
    }

    /**
     * Check if composites match. Match occurs when "at least one equal composite identity is
     * listed on both the Producer and Consmer composite identity service property" (p. 336)
     * 
     * @return <tt>true</tt> if they match, <tt>false</tt>otherwise
     */    
    private boolean matchComposites()
    {
        String [] producerIdentities = (String []) m_producerServiceRef.getProperty(WireConstants.WIREADMIN_PRODUCER_COMPOSITE);
        String [] consumerIdentities = (String []) m_producerServiceRef.getProperty(WireConstants.WIREADMIN_PRODUCER_COMPOSITE);
        
        boolean match = false;
        
        // confirm matching
        
        for(int i = 0; i< producerIdentities.length; i++)
        {
            String currentProducerIdentity = producerIdentities [i]; 
            for(int j = 0; j < consumerIdentities.length; j++)
            {
                String currentConsumerIdentity = consumerIdentities [j];
                if (currentProducerIdentity.equals(currentConsumerIdentity))
                {
                    match = true;
                    break;
                }
            }
        }
        
        // setup wire scope
        
        if (m_consumerScope != null && m_producerScope != null)
        {
            // p. 337 "It is allowed to register with a wildcard, indicating that all scopenames
            // are supported. In that case, the WIREADMIN_SCOPE_ALL ({"*"}) should be registered as the
            // scope of the serivce. The WireObject's scope is then fully defined by the
            // other service connected to the wire object
            if(m_consumerScope.length == 1 && m_consumerScope[0].equals("*"))
            {
                m_scope = m_producerScope;
            }
            else if(m_producerScope.length == 1 && m_producerScope[0].equals("*"))
            {
                m_scope = m_consumerScope;
            }
            else
            {
                /*
                // TODO Implement wire scope creation based on producer, consumer and wire permissions (p. 338)

                ServiceReference ref = m_bundleContext.getServiceReference(PermissionAdmin.class.getName());
                if(ref != null)
                {
                    PermissionAdmin permAdmin = (PermissionAdmin) m_bundleContext.getService(ref);
                    PermissionInfo [] producerPermissions = permAdmin.getPermissions(m_producerServiceRef.getBundle().getLocation());
                    PermissionInfo [] consumerPermissions = permAdmin.getPermissions(m_consumerServiceRef.getBundle().getLocation()); 
                }
                */
            }
        }
        else
        {
            // p. 337 "Not registering this property (WIREADMIN_CONSUMER_SCOPE or WIREADMIN_PRODUCER_SCOPE)
            // by the Consumer or the Producer service indicates to the WireAdmin service that any
            // Wire object connected to that service must return null for the Wire.getScope() method"
            m_scope = null;
        }
        
        return match;
    }

    /**
     * Called to invalidate the wire
     *
     */
    void invalidate()
    {
    	if(m_isValid)
    	{
	        unbindProducer();
	        unbindConsumer();
	        m_isValid=false;
    	}
    }
    
    /**
     * Update the properties
     * 
     * @param properties new properties
     */
    void updateProperties(Dictionary properties)
    {
        m_properties = properties;
    }

    /**
     * Return the service reference corresponding to the producer
     * 
     * @return a <tt>ServiceReference</tt> corresponding to the producer
     */
    ServiceReference getProducerServiceRef()
    {
        return m_producerServiceRef;
    }

    /**
     * Return the producer service object
     * 
     * @return An <tt>Object</tt> corresponding to the producer
     */
    Producer getProducer()
    {
        return m_producer;
    }

    /**
     * return the producer PID
     * 
     * @return
     */
    String getProducerPID()
    {
        return m_producerPID;
    }

    /**
     * Return the service reference corresponding to the consumer
     * 
     * @return a <tt>ServiceReference</tt> corresponding to the consumer
     */
    ServiceReference getConsumerServiceRef()
    {
        return m_consumerServiceRef;
    }

    /**
     * Returns the consumer service object
     * 
     * @return An <tt>Object</tt> corresponding to the consumer
     */
    Consumer getConsumer()
    {
        return m_consumer;
    }

    /**
     * return the consumer PID
     * 
     * @return
     */
    String getConsumerPID()
    {
        return m_consumerPID;
    }
    
    /**
     * This inner class implements a dictionary that is used to filter out values
     * during calls to update. This design choice was favored to avoid constructing
     * a new dictionary for every call to update and to avoid doing unnecessary
     * calculations.
     */
    class FilterDictionary extends Dictionary
    {
        private Object m_value;
        private long m_time;
        
        /**
         * Must be called prior to evaluating the filter
         * 
         * @param value
         * @param time
         */
        void reset(Object value, long time)
        {
            m_value = value;
            m_time = time;
        }
        
        /**
         * 
         */
        public Object get(Object key)
        {
            if(key.equals(WireConstants.WIREVALUE_CURRENT))
            {
                return m_value;
            }
            else if(key.equals(WireConstants.WIREVALUE_PREVIOUS))
            {
                return m_lastValue;
            }
            else if(m_value instanceof Number && key.equals(WireConstants.WIREVALUE_DELTA_ABSOLUTE))
            {
                return null;
            }
            else if(m_value instanceof Number && key.equals(WireConstants.WIREVALUE_DELTA_RELATIVE))
            {
                return null;
            }
            else if(key.equals(WireConstants.WIREVALUE_ELAPSED))
            {
                if(m_lastUpdate == 0)
                {
                    return new Long(0);    
                }
                else
                {
                    long delay = m_time - m_lastUpdate;
                    //System.out.println("### delay = "+(delay));
                    return new Long(delay);
                }
            }
            else
            {
            	WireAdminImpl.traceln("### key not found:"+key);
                return null;
            }
        }

        /**
         * Never empty
         */
        public boolean isEmpty()
        {
            return false;       
        }
        
        /**
         * Remove not supported
         */
        public Object remove(Object obj)
        {
            return null;
        }
        
        /**
         * Size is static
         */
        public int size()
        {
            return 5;
        }
        
        /**
         * Put not supported
         */
        public Object put(Object key, Object value)
        {
            return null;
        }
        
        /**
         * 
         */
        public Enumeration keys()
        {
        	Vector keys=new Vector();
        	
        	keys.addElement(WireConstants.WIREVALUE_ELAPSED);
        	keys.addElement(WireConstants.WIREVALUE_CURRENT);
        	keys.addElement(WireConstants.WIREVALUE_PREVIOUS);
        	keys.addElement(WireConstants.WIREVALUE_DELTA_ABSOLUTE);
        	keys.addElement(WireConstants.WIREVALUE_DELTA_RELATIVE);
        	
        	return keys.elements();
        }
        
        /**
         * Not supported
         */
        public Enumeration elements()
        {
            return null;
        }
    }
}
