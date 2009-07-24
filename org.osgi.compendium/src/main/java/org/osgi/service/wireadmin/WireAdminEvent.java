/*
 * Copyright (c) OSGi Alliance (2002, 2008). All Rights Reserved.
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
package org.osgi.service.wireadmin;

import org.osgi.framework.ServiceReference;

/**
 * A Wire Admin Event.
 * 
 * <p>
 * <code>WireAdminEvent</code> objects are delivered to all registered
 * <code>WireAdminListener</code> service objects which specify an interest in the
 * <code>WireAdminEvent</code> type. Events must be delivered in chronological
 * order with respect to each listener. For example, a <code>WireAdminEvent</code>
 * of type {@link #WIRE_CONNECTED} must be delivered before a
 * <code>WireAdminEvent</code> of type {@link #WIRE_DISCONNECTED} for a particular
 * <code>Wire</code> object.
 * 
 * <p>
 * A type code is used to identify the type of event. The following event types
 * are defined:
 * <ul>
 * <li>{@link #WIRE_CREATED}
 * <li>{@link #WIRE_CONNECTED}
 * <li>{@link #WIRE_UPDATED}
 * <li>{@link #WIRE_TRACE}
 * <li>{@link #WIRE_DISCONNECTED}
 * <li>{@link #WIRE_DELETED}
 * <li>{@link #PRODUCER_EXCEPTION}
 * <li>{@link #CONSUMER_EXCEPTION}
 * </ul>
 * Additional event types may be defined in the future.
 * 
 * <p>
 * Event type values must be unique and disjoint bit values. Event types must be
 * defined as a bit in a 32 bit integer and can thus be bitwise OR'ed together.
 * <p>
 * Security Considerations. <code>WireAdminEvent</code> objects contain
 * <code>Wire</code> objects. Care must be taken in the sharing of <code>Wire</code>
 * objects with other bundles.
 * 
 * @see WireAdminListener
 * 
 * @version $Revision: 5673 $
 */
public class WireAdminEvent {
	/**
	 * The WireAdmin service which created this event.
	 */
	private ServiceReference	reference;
	/**
	 * The <code>Wire</code> object associated with this event.
	 */
	private Wire				wire;
	/**
	 * Type of this event.
	 * 
	 * @see #getType
	 */
	private int					type;
	/**
	 * Exception associates with this the event.
	 */
	private Throwable			throwable;
	/**
	 * A Producer service method has thrown an exception.
	 * 
	 * <p>
	 * This <code>WireAdminEvent</code> type indicates that a Producer service
	 * method has thrown an exception. The {@link WireAdminEvent#getThrowable}
	 * method will return the exception that the Producer service method raised.
	 * 
	 * <p>
	 * The value of <code>PRODUCER_EXCEPTION</code> is 0x00000001.
	 */
	public final static int		PRODUCER_EXCEPTION	= 0x00000001;
	/**
	 * A Consumer service method has thrown an exception.
	 * 
	 * <p>
	 * This <code>WireAdminEvent</code> type indicates that a Consumer service
	 * method has thrown an exception. The {@link WireAdminEvent#getThrowable}
	 * method will return the exception that the Consumer service method raised.
	 * 
	 * <p>
	 * The value of <code>CONSUMER_EXCEPTION</code> is 0x00000002.
	 */
	public final static int		CONSUMER_EXCEPTION	= 0x00000002;
	/**
	 * A <code>Wire</code> has been created.
	 * 
	 * <p>
	 * This <code>WireAdminEvent</code> type that indicates that a new
	 * <code>Wire</code> object has been created.
	 * 
	 * An event is broadcast when {@link WireAdmin#createWire} is called. The
	 * {@link WireAdminEvent#getWire} method will return the <code>Wire</code>
	 * object that has just been created.
	 * 
	 * <p>
	 * The value of <code>WIRE_CREATED</code> is 0x00000004.
	 */
	public final static int		WIRE_CREATED		= 0x00000004;
	/**
	 * A <code>Wire</code> has been updated.
	 * 
	 * <p>
	 * This <code>WireAdminEvent</code> type that indicates that an existing
	 * <code>Wire</code> object has been updated with new properties.
	 * 
	 * An event is broadcast when {@link WireAdmin#updateWire} is called with a
	 * valid wire. The {@link WireAdminEvent#getWire} method will return the
	 * <code>Wire</code> object that has just been updated.
	 * 
	 * <p>
	 * The value of <code>WIRE_UPDATED</code> is 0x00000008.
	 */
	public final static int		WIRE_UPDATED		= 0x00000008;
	/**
	 * A <code>Wire</code> has been deleted.
	 * 
	 * <p>
	 * This <code>WireAdminEvent</code> type that indicates that an existing wire
	 * has been deleted.
	 * 
	 * An event is broadcast when {@link WireAdmin#deleteWire} is called with a
	 * valid wire. {@link WireAdminEvent#getWire} will return the <code>Wire</code>
	 * object that has just been deleted.
	 * 
	 * <p>
	 * The value of <code>WIRE_DELETED</code> is 0x00000010.
	 */
	public final static int		WIRE_DELETED		= 0x00000010;
	/**
	 * The <code>WireAdminEvent</code> type that indicates that an existing
	 * <code>Wire</code> object has become connected.
	 * 
	 * The Consumer object and the Producer object that are associated with the
	 * <code>Wire</code> object have both been registered and the <code>Wire</code>
	 * object is connected. See {@link Wire#isConnected} for a description of
	 * the connected state. This event may come before the
	 * <code>producersConnected</code> and <code>consumersConnected</code> method
	 * have returned or called to allow synchronous delivery of the events. Both
	 * methods can cause other <code>WireAdminEvent</code> s to take place and
	 * requiring this event to be send before these methods are returned would
	 * mandate asynchronous delivery.
	 * 
	 * <p>
	 * The value of <code>WIRE_CONNECTED</code> is 0x00000020.
	 */
	public final static int		WIRE_CONNECTED		= 0x00000020;
	/**
	 * The <code>WireAdminEvent</code> type that indicates that an existing
	 * <code>Wire</code> object has become disconnected.
	 * 
	 * The Consumer object or/and Producer object is/are unregistered breaking
	 * the connection between the two. See {@link Wire#isConnected} for a
	 * description of the connected state.
	 * 
	 * <p>
	 * The value of <code>WIRE_DISCONNECTED</code> is 0x00000040.
	 */
	public final static int		WIRE_DISCONNECTED	= 0x00000040;
	/**
	 * The <code>WireAdminEvent</code> type that indicates that a new value is
	 * transferred over the <code>Wire</code> object.
	 * 
	 * This event is sent after the Consumer service has been notified by
	 * calling the {@link Consumer#updated} method or the Consumer service
	 * requested a new value with the {@link Wire#poll} method. This is an
	 * advisory event meaning that when this event is received, another update
	 * may already have occurred and this the {@link Wire#getLastValue} method
	 * returns a newer value then the value that was communicated for this
	 * event.
	 * 
	 * <p>
	 * The value of <code>WIRE_TRACE</code> is 0x00000080.
	 */
	public final static int		WIRE_TRACE			= 0x00000080;

	/**
	 * Constructs a <code>WireAdminEvent</code> object from the given
	 * <code>ServiceReference</code> object, event type, <code>Wire</code> object
	 * and exception.
	 * 
	 * @param reference The <code>ServiceReference</code> object of the Wire Admin
	 *        service that created this event.
	 * @param type The event type. See {@link #getType}.
	 * @param wire The <code>Wire</code> object associated with this event.
	 * @param exception An exception associated with this event. This may be
	 *        <code>null</code> if no exception is associated with this event.
	 */
	public WireAdminEvent(ServiceReference reference, int type, Wire wire,
			Throwable exception) {
		this.reference = reference;
		this.wire = wire;
		this.type = type;
		this.throwable = exception;
	}

	/**
	 * Return the <code>ServiceReference</code> object of the Wire Admin service
	 * that created this event.
	 * 
	 * @return The <code>ServiceReference</code> object for the Wire Admin service
	 *         that created this event.
	 */
	public ServiceReference getServiceReference() {
		return reference;
	}

	/**
	 * Return the <code>Wire</code> object associated with this event.
	 * 
	 * @return The <code>Wire</code> object associated with this event or
	 *         <code>null</code> when no <code>Wire</code> object is associated with
	 *         the event.
	 */
	public Wire getWire() {
		return wire;
	}

	/**
	 * Return the type of this event.
	 * <p>
	 * The type values are:
	 * <ul>
	 * <li>{@link #WIRE_CREATED}
	 * <li>{@link #WIRE_CONNECTED}
	 * <li>{@link #WIRE_UPDATED}
	 * <li>{@link #WIRE_TRACE}
	 * <li>{@link #WIRE_DISCONNECTED}
	 * <li>{@link #WIRE_DELETED}
	 * <li>{@link #PRODUCER_EXCEPTION}
	 * <li>{@link #CONSUMER_EXCEPTION}
	 * </ul>
	 * 
	 * @return The type of this event.
	 */
	public int getType() {
		return type;
	}

	/**
	 * Returns the exception associated with the event, if any.
	 * 
	 * @return An exception or <code>null</code> if no exception is associated
	 *         with this event.
	 */
	public Throwable getThrowable() {
		return throwable;
	}
}
