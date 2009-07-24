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
package org.osgi.service.upnp;

/**
 * A representation of a UPnP Service.
 * 
 * Each UPnP device contains zero or more services. The UPnP description for a
 * service defines actions, their arguments, and event characteristics.
 * 
 * @version $Revision: 5673 $
 */
public interface UPnPService {
	/**
	 * Property key for the optional service type uri.
	 * 
	 * The service type property is used when registering UPnP Device services
	 * and UPnP Event Listener services. The property contains a <code>String</code>
	 * array (<code>String[]</code>) of service types. A UPnP Device service can
	 * thus announce what types of services it contains. A UPnP Event Listener
	 * service can announce for what type of UPnP services it wants
	 * notifications. The service version is encoded in the type string as
	 * specified in the UPnP specification. A <code>null</code> value is a
	 * wildcard, matching <b>all </b> service types. Value is
	 * "UPnP.service.type".
	 * 
	 * @see UPnPService#getType()
	 */
	String	TYPE	= "UPnP.service.type";
	/**
	 * Property key for the optional service id.
	 * 
	 * The service id property is used when registering UPnP Device services or
	 * UPnP Event Listener services. The value of the property contains a
	 * <code>String</code> array (<code>String[]</code>) of service ids. A UPnP
	 * Device service can thus announce what service ids it contains. A UPnP
	 * Event Listener service can announce for what UPnP service ids it wants
	 * notifications. A service id does <b>not </b> have to be universally
	 * unique. It must be unique only within a device. A <code>null</code> value
	 * is a wildcard, matching <b>all </b> services. The value is
	 * "UPnP.service.id".
	 */
	String	ID		= "UPnP.service.id";

	/**
	 * Returns the <code>serviceId</code> field in the UPnP service description.
	 * 
	 * 
	 * <p>
	 * For standard services defined by a UPnP Forum working committee, the
	 * serviceId must contain the following components in the indicated order:
	 * <ul>
	 * <li><code>urn:upnp-org:serviceId:</code></li>
	 * <li>service ID suffix</li>
	 * </ul>
	 * Example: <code>urn:upnp-org:serviceId:serviceID</code>.
	 * 
	 * <p>
	 * Note that <code>upnp-org</code> is used instead of
	 * <code>schemas-upnp-org</code> in this example because an XML schema is not
	 * defined for each serviceId.
	 * </p>
	 * 
	 * <p>
	 * For non-standard services specified by UPnP vendors, the serviceId must
	 * contain the following components in the indicated order:
	 * <ul>
	 * <li><code>urn:</code></li>
	 * <li>ICANN domain name owned by the vendor</li>
	 * <li><code>:serviceId:</code></li>
	 * <li>service ID suffix</li>
	 * </ul>
	 * Example: <code>urn:domain-name:serviceId:serviceID</code>.
	 * 
	 * @return The service ID suffix defined by a UPnP Forum working committee
	 *         or specified by a UPnP vendor. Must be &lt;= 64 characters.
	 *         Single URI.
	 */
	String getId();

	/**
	 * Returns the <code>serviceType</code> field in the UPnP service description.
	 * 
	 * <p>
	 * For standard services defined by a UPnP Forum working committee, the
	 * serviceType must contain the following components in the indicated order:
	 * <ul>
	 * <li><code>urn:schemas-upnp-org:service:</code></li>
	 * <li>service type suffix:</li>
	 * <li>integer service version</li>
	 * </ul>
	 * Example: <code>urn:schemas-upnp-org:service:serviceType:v</code>.
	 * 
	 * <p>
	 * For non-standard services specified by UPnP vendors, the
	 * <code>serviceType</code> must contain the following components in the
	 * indicated order:
	 * <ul>
	 * <li><code>urn:</code></li>
	 * <li>ICANN domain name owned by the vendor</li>
	 * <li><code>:service:</code></li>
	 * <li>service type suffix:</li>
	 * <li>integer service version</li>
	 * </ul>
	 * Example: <code>urn:domain-name:service:serviceType:v</code>.
	 * 
	 * @return The service type suffix defined by a UPnP Forum working committee
	 *         or specified by a UPnP vendor. Must be &lt;= 64 characters, not
	 *         including the version suffix and separating colon. Single URI.
	 */
	String getType();

	/**
	 * Returns the version suffix encoded in the <code>serviceType</code> field in
	 * the UPnP service description.
	 * 
	 * @return The integer service version defined by a UPnP Forum working
	 *         committee or specified by a UPnP vendor.
	 */
	String getVersion();

	/**
	 * Locates a specific action by name.
	 * 
	 * Looks up an action by its name.
	 * 
	 * @param name Name of action. Must not contain hyphen or hash characters.
	 *        Should be &lt; 32 characters.
	 * 
	 * @return The requested action or <code>null</code> if no action is found.
	 */
	UPnPAction getAction(String name);

	/**
	 * Lists all actions provided by this service.
	 * 
	 * @return Array of actions (<code>UPnPAction[]</code> )or <code>null</code> if
	 *         no actions are defined for this service.
	 */
	UPnPAction[] getActions();

	/**
	 * Lists all <code>UPnPStateVariable</code> objects provided by this service.
	 * 
	 * @return Array of state variables or <code>null</code> if none are defined
	 *         for this service.
	 */
	UPnPStateVariable[] getStateVariables();

	/**
	 * Gets a <code>UPnPStateVariable</code> objects provided by this service by
	 * name
	 * 
	 * @param name Name of the State Variable
	 * 
	 * @return State variable or <code>null</code> if no such state variable
	 *         exists for this service.
	 */
	UPnPStateVariable getStateVariable(String name);
}
