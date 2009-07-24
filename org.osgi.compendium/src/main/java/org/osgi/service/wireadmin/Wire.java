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

import java.util.Dictionary;

/**
 * A connection between a Producer service and a Consumer service.
 * 
 * <p>
 * A <code>Wire</code> object connects a Producer service to a Consumer service.
 * Both the Producer and Consumer services are identified by their unique
 * <code>service.pid</code> values. The Producer and Consumer services may
 * communicate with each other via <code>Wire</code> objects that connect them.
 * The Producer service may send updated values to the Consumer service by
 * calling the {@link #update} method. The Consumer service may request an
 * updated value from the Producer service by calling the {@link #poll} method.
 * 
 * <p>
 * A Producer service and a Consumer service may be connected through multiple
 * <code>Wire</code> objects.
 * 
 * <p>
 * Security Considerations. <code>Wire</code> objects are available to Producer
 * and Consumer services connected to a given <code>Wire</code> object and to
 * bundles which can access the <code>WireAdmin</code> service. A bundle must have
 * <code>ServicePermission[WireAdmin,GET]</code> to get the <code>WireAdmin</code>
 * service to access all <code>Wire</code> objects. A bundle registering a
 * Producer service or a Consumer service must have the appropriate
 * <code>ServicePermission[Consumer|Producer,REGISTER]</code> to register the
 * service and will be passed <code>Wire</code> objects when the service object's
 * <code>consumersConnected</code> or <code>producersConnected</code> method is
 * called.
 * 
 * <p>
 * Scope. Each Wire object can have a scope set with the <code>setScope</code>
 * method. This method should be called by a Consumer service when it assumes a
 * Producer service that is composite (supports multiple information items). The
 * names in the scope must be verified by the <code>Wire</code> object before it
 * is used in communication. The semantics of the names depend on the Producer
 * service and must not be interpreted by the Wire Admin service.
 * 
 * @version $Revision: 5673 $
 */
public interface Wire {
	/**
	 * Return the state of this <code>Wire</code> object.
	 * 
	 * <p>
	 * A connected <code>Wire</code> must always be disconnected before becoming
	 * invalid.
	 * 
	 * @return <code>false</code> if this <code>Wire</code> object is invalid
	 *         because it has been deleted via {@link WireAdmin#deleteWire};
	 *         <code>true</code> otherwise.
	 */
	public boolean isValid();

	/**
	 * Return the connection state of this <code>Wire</code> object.
	 * 
	 * <p>
	 * A <code>Wire</code> is connected after the Wire Admin service receives
	 * notification that the Producer service and the Consumer service for this
	 * <code>Wire</code> object are both registered. This method will return
	 * <code>true</code> prior to notifying the Producer and Consumer services via
	 * calls to their respective <code>consumersConnected</code> and
	 * <code>producersConnected</code> methods.
	 * <p>
	 * A <code>WireAdminEvent</code> of type {@link WireAdminEvent#WIRE_CONNECTED}
	 * must be broadcast by the Wire Admin service when the <code>Wire</code>
	 * becomes connected.
	 * 
	 * <p>
	 * A <code>Wire</code> object is disconnected when either the Consumer or
	 * Producer service is unregistered or the <code>Wire</code> object is
	 * deleted.
	 * <p>
	 * A <code>WireAdminEvent</code> of type
	 * {@link WireAdminEvent#WIRE_DISCONNECTED} must be broadcast by the Wire
	 * Admin service when the <code>Wire</code> becomes disconnected.
	 * 
	 * @return <code>true</code> if both the Producer and Consumer for this
	 *         <code>Wire</code> object are connected to the <code>Wire</code>
	 *         object; <code>false</code> otherwise.
	 */
	public boolean isConnected();

	/**
	 * Return the list of data types understood by the Consumer service
	 * connected to this <code>Wire</code> object. Note that subclasses of the
	 * classes in this list are acceptable data types as well.
	 * 
	 * <p>
	 * The list is the value of the
	 * {@link WireConstants#WIREADMIN_CONSUMER_FLAVORS} service property of the
	 * Consumer service object connected to this object. If no such property was
	 * registered or the type of the property value is not <code>Class[]</code>,
	 * this method must return <code>null</code>.
	 * 
	 * @return An array containing the list of classes understood by the
	 *         Consumer service or <code>null</code> if the <code>Wire</code> is not
	 *         connected, or the consumer did not register a
	 *         {@link WireConstants#WIREADMIN_CONSUMER_FLAVORS} property or the
	 *         value of the property is not of type <code>Class[]</code>.
	 */
	public Class[] getFlavors();

	/**
	 * Update the value.
	 * 
	 * <p>
	 * This methods is called by the Producer service to notify the Consumer
	 * service connected to this <code>Wire</code> object of an updated value.
	 * <p>
	 * If the properties of this <code>Wire</code> object contain a
	 * {@link WireConstants#WIREADMIN_FILTER} property, then filtering is
	 * performed. If the Producer service connected to this <code>Wire</code>
	 * object was registered with the service property
	 * {@link WireConstants#WIREADMIN_PRODUCER_FILTERS}, the Producer service
	 * will perform the filtering according to the rules specified for the
	 * filter. Otherwise, this <code>Wire</code> object will perform the filtering
	 * of the value.
	 * <p>
	 * If no filtering is done, or the filter indicates the updated value should
	 * be delivered to the Consumer service, then this <code>Wire</code> object
	 * must call the {@link Consumer#updated} method with the updated value. If
	 * this <code>Wire</code> object is not connected, then the Consumer service
	 * must not be called and the value is ignored.
	 * <p>
	 * If the value is an <code>Envelope</code> object, and the scope name is not
	 * permitted, then the <code>Wire</code> object must ignore this call and not
	 * transfer the object to the Consumer service.
	 * 
	 * <p>
	 * A <code>WireAdminEvent</code> of type {@link WireAdminEvent#WIRE_TRACE}
	 * must be broadcast by the Wire Admin service after the Consumer service
	 * has been successfully called.
	 * 
	 * @param value The updated value. The value should be an instance of one of
	 *        the types returned by {@link #getFlavors}.
	 * @see WireConstants#WIREADMIN_FILTER
	 */
	public void update(Object value);

	/**
	 * Poll for an updated value.
	 * 
	 * <p>
	 * This methods is normally called by the Consumer service to request an
	 * updated value from the Producer service connected to this <code>Wire</code>
	 * object. This <code>Wire</code> object will call the {@link Producer#polled}
	 * method to obtain an updated value. If this <code>Wire</code> object is not
	 * connected, then the Producer service must not be called.
	 * <p>
	 * 
	 * If this <code>Wire</code> object has a scope, then this method must return
	 * an array of <code>Envelope</code> objects. The objects returned must match
	 * the scope of this object. The <code>Wire</code> object must remove all
	 * <code>Envelope</code> objects with a scope name that is not in the
	 * <code>Wire</code> object's scope. Thus, the list of objects returned must
	 * only contain <code>Envelope</code> objects with a permitted scope name. If
	 * the array becomes empty, <code>null</code> must be returned.
	 * 
	 * <p>
	 * A <code>WireAdminEvent</code> of type {@link WireAdminEvent#WIRE_TRACE}
	 * must be broadcast by the Wire Admin service after the Producer service
	 * has been successfully called.
	 * 
	 * @return A value whose type should be one of the types returned by
	 *         {@link #getFlavors},<code>Envelope[]</code>, or <code>null</code>
	 *         if the <code>Wire</code> object is not connected, the Producer
	 *         service threw an exception, or the Producer service returned a
	 *         value which is not an instance of one of the types returned by
	 *         {@link #getFlavors}.
	 */
	public Object poll();

	/**
	 * Return the last value sent through this <code>Wire</code> object.
	 * 
	 * <p>
	 * The returned value is the most recent, valid value passed to the
	 * {@link #update} method or returned by the {@link #poll} method of this
	 * object. If filtering is performed by this <code>Wire</code> object, this
	 * methods returns the last value provided by the Producer service. This
	 * value may be an <code>Envelope[]</code> when the Producer service uses
	 * scoping. If the return value is an Envelope object (or array), it must be
	 * verified that the Consumer service has the proper WirePermission to see
	 * it.
	 * 
	 * @return The last value passed though this <code>Wire</code> object or
	 *         <code>null</code> if no valid values have been passed or the
	 *         Consumer service has no permission.
	 */
	public Object getLastValue();

	/**
	 * Return the wire properties for this <code>Wire</code> object.
	 * 
	 * @return The properties for this <code>Wire</code> object. The returned
	 *         <code>Dictionary</code> must be read only.
	 */
	public Dictionary getProperties();

	/**
	 * Return the calculated scope of this <code>Wire</code> object.
	 * 
	 * The purpose of the <code>Wire</code> object's scope is to allow a Producer
	 * and/or Consumer service to produce/consume different types over a single
	 * <code>Wire</code> object (this was deemed necessary for efficiency
	 * reasons). Both the Consumer service and the Producer service must set an
	 * array of scope names (their scope) with the service registration property
	 * <code>WIREADMIN_PRODUCER_SCOPE</code>, or
	 * <code>WIREADMIN_CONSUMER_SCOPE</code> when they can produce multiple types.
	 * If a Producer service can produce different types, it should set this
	 * property to the array of scope names it can produce, the Consumer service
	 * must set the array of scope names it can consume. The scope of a
	 * <code>Wire</code> object is defined as the intersection of permitted scope
	 * names of the Producer service and Consumer service.
	 * <p>
	 * If neither the Consumer, or the Producer service registers scope names
	 * with its service registration, then the <code>Wire</code> object's scope
	 * must be <code>null</code>.
	 * <p>
	 * The <code>Wire</code> object's scope must not change when a Producer or
	 * Consumer services modifies its scope.
	 * <p>
	 * A scope name is permitted for a Producer service when the registering
	 * bundle has <code>WirePermission[name,PRODUCE]</code>, and for a Consumer
	 * service when the registering bundle has <code>WirePermission[name,CONSUME]</code>.
	 * <p>
	 * If either Consumer service or Producer service has not set a
	 * <code>WIREADMIN_*_SCOPE</code> property, then the returned value must be
	 * <code>null</code>.
	 * <p>
	 * If the scope is set, the <code>Wire</code> object must enforce the scope
	 * names when <code>Envelope</code> objects are used as a parameter to update
	 * or returned from the <code>poll</code> method. The <code>Wire</code> object
	 * must then remove all <code>Envelope</code> objects with a scope name that
	 * is not permitted.
	 * 
	 * @return A list of permitted scope names or null if the Produce or
	 *         Consumer service has set no scope names.
	 */
	public String[] getScope();

	/**
	 * Return true if the given name is in this <code>Wire</code> object's scope.
	 * 
	 * @param name The scope name
	 * @return true if the name is listed in the permitted scope names
	 */
	public boolean hasScope(String name);
}
