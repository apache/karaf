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
 * Identifies a contained value.
 * 
 * An <code>Envelope</code> object combines a status value, an identification
 * object and a scope name. The <code>Envelope</code> object allows the use of
 * standard Java types when a Producer service can produce more than one kind of
 * object. The <code>Envelope</code> object allows the Consumer service to
 * recognize the kind of object that is received. For example, a door lock could
 * be represented by a <code>Boolean</code> object. If the <code>Producer</code>
 * service would send such a <code>Boolean</code> object, then the Consumer
 * service would not know what door the <code>Boolean</code> object represented.
 * The <code>Envelope</code> object contains an identification object so the
 * Consumer service can discriminate between different kinds of values. The
 * identification object may be a simple <code>String</code> object, but it can
 * also be a domain specific object that is mutually agreed by the Producer and
 * the Consumer service. This object can then contain relevant information that
 * makes the identification easier.
 * <p>
 * The scope name of the envelope is used for security. The Wire object must
 * verify that any <code>Envelope</code> object send through the <code>update</code>
 * method or coming from the <code>poll</code> method has a scope name that
 * matches the permissions of both the Producer service and the Consumer service
 * involved. The wireadmin package also contains a class <code>BasicEnvelope</code>
 * that implements the methods of this interface.
 * 
 * @see WirePermission
 * @see BasicEnvelope
 * 
 * @version $Revision: 5673 $
 */
public interface Envelope {
	/**
	 * Return the value associated with this <code>Envelope</code> object.
	 * 
	 * @return the value of the status item, or <code>null</code> when no item is
	 *         associated with this object.
	 */
	public Object getValue();

	/**
	 * Return the identification of this <code>Envelope</code> object.
	 * 
	 * An identification may be of any Java type. The type must be mutually
	 * agreed between the Consumer and Producer services.
	 * 
	 * @return an object which identifies the status item in the address space
	 *         of the composite producer, must not be null.
	 */
	public Object getIdentification();

	/**
	 * Return the scope name of this <code>Envelope</code> object.
	 * 
	 * Scope names are used to restrict the communication between the Producer
	 * and Consumer services. Only <code>Envelopes</code> objects with a scope
	 * name that is permitted for the Producer and the Consumer services must be
	 * passed through a <code>Wire</code> object.
	 * 
	 * @return the security scope for the status item, must not be null.
	 */
	public String getScope();
}
