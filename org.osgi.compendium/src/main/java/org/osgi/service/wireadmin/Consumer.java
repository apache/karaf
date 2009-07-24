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

/**
 * Data Consumer, a service that can receive udpated values from
 * {@link Producer} services.
 * 
 * <p>
 * Service objects registered under the <code>Consumer</code> interface are
 * expected to consume values from a Producer service via a <code>Wire</code>
 * object. A Consumer service may poll the Producer service by calling the
 * {@link Wire#poll} method. The Consumer service will also receive an updated
 * value when called at it's {@link #updated} method. The Producer service
 * should have coerced the value to be an instance of one of the types specified
 * by the {@link Wire#getFlavors} method, or one of their subclasses.
 * 
 * <p>
 * Consumer service objects must register with a <code>service.pid</code> and a
 * {@link WireConstants#WIREADMIN_CONSUMER_FLAVORS} property. It is recommended
 * that Consumer service objects also register with a
 * <code>service.description</code> property.
 * 
 * <p>
 * If an <code>Exception</code> is thrown by any of the <code>Consumer</code>
 * methods, a <code>WireAdminEvent</code> of type
 * {@link WireAdminEvent#CONSUMER_EXCEPTION} is broadcast by the Wire Admin
 * service.
 * 
 * <p>
 * Security Considerations - Data consuming bundles will require
 * <code>ServicePermission[Consumer,REGISTER]</code>. In general, only the Wire
 * Admin service bundle should have this permission. Thus only the Wire Admin
 * service may directly call a Consumer service. Care must be taken in the
 * sharing of <code>Wire</code> objects with other bundles.
 * <p>
 * Consumer services must be registered with their scope when they can receive
 * different types of objects from the Producer service. The Consumer service
 * should have <code>WirePermission</code> for each of these scope names.
 * 
 * @version $Revision: 5673 $
 */
public interface Consumer {
	/**
	 * Update the value. This Consumer service is called by the <code>Wire</code>
	 * object with an updated value from the Producer service.
	 * 
	 * <p>
	 * Note: This method may be called by a <code>Wire</code> object prior to this
	 * object being notified that it is connected to that <code>Wire</code> object
	 * (via the {@link #producersConnected} method).
	 * <p>
	 * When the Consumer service can receive <code>Envelope</code> objects, it
	 * must have registered all scope names together with the service object,
	 * and each of those names must be permitted by the bundle's
	 * <code>WirePermission</code>. If an <code>Envelope</code> object is delivered
	 * with the <code>updated</code> method, then the Consumer service should
	 * assume that the security check has been performed.
	 * 
	 * @param wire The <code>Wire</code> object which is delivering the updated
	 *        value.
	 * @param value The updated value. The value should be an instance of one of
	 *        the types specified by the {@link Wire#getFlavors} method.
	 */
	public void updated(Wire wire, Object value);

	/**
	 * Update the list of <code>Wire</code> objects to which this Consumer service
	 * is connected.
	 * 
	 * <p>
	 * This method is called when the Consumer service is first registered and
	 * subsequently whenever a <code>Wire</code> associated with this Consumer
	 * service becomes connected, is modified or becomes disconnected.
	 * 
	 * <p>
	 * The Wire Admin service must call this method asynchronously. This implies
	 * that implementors of Consumer can be assured that the callback will not
	 * take place during registration when they execute the registration in a
	 * synchronized method.
	 * 
	 * @param wires An array of the current and complete list of <code>Wire</code>
	 *        objects to which this Consumer service is connected. May be
	 *        <code>null</code> if the Consumer service is not currently connected
	 *        to any <code>Wire</code> objects.
	 */
	public void producersConnected(Wire[] wires);
}
