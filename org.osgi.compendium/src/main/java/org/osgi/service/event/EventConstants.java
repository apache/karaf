/*
 * Copyright (c) OSGi Alliance (2005, 2008). All Rights Reserved.
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

package org.osgi.service.event;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

/**
 * Defines standard names for <code>EventHandler</code> properties.
 * 
 * @version $Revision: 7368 $
 */
public interface EventConstants {

	/**
	 * Service registration property (named <code>event.topics</code>)
	 * specifying the <code>Event</code> topics of interest to a Event Handler
	 * service.
	 * <p>
	 * Event handlers SHOULD be registered with this property. The value of the
	 * property is a string or an array of strings that describe the topics in
	 * which the handler is interested. An asterisk ('*') may be used as a
	 * trailing wildcard. Event Handlers which do not have a value for this
	 * property must not receive events. More precisely, the value of each
	 * string must conform to the following grammar:
	 * 
	 * <pre>
	 *  topic-description := '*' | topic ( '/*' )?
	 *  topic := token ( '/' token )*
	 * </pre>
	 * 
	 * @see Event
	 */
	public static final String	EVENT_TOPIC			= "event.topics";

	/**
	 * Service Registration property (named <code>event.filter</code>)
	 * specifying a filter to further select <code>Event</code> s of interest to
	 * a Event Handler service.
	 * <p>
	 * Event handlers MAY be registered with this property. The value of this
	 * property is a string containing an LDAP-style filter specification. Any
	 * of the event's properties may be used in the filter expression. Each
	 * event handler is notified for any event which belongs to the topics in
	 * which the handler has expressed an interest. If the event handler is also
	 * registered with this service property, then the properties of the event
	 * must also match the filter for the event to be delivered to the event
	 * handler.
	 * <p>
	 * If the filter syntax is invalid, then the Event Handler must be ignored
	 * and a warning should be logged.
	 * 
	 * @see Event
	 * @see Filter
	 */
	public static final String	EVENT_FILTER		= "event.filter";

	/**
	 * The Distinguished Names of the signers of the bundle relevant to the
	 * event. The type of the value for this event property is
	 * <code>String</code> or <code>Collection</code> of <code>String</code>.
	 */
	public static final String	BUNDLE_SIGNER		= "bundle.signer";

	/**
	 * The Bundle Symbolic Name of the bundle relevant to the event. The type of
	 * the value for this event property is <code>String</code>.
	 */
	public static final String	BUNDLE_SYMBOLICNAME	= "bundle.symbolicName";

	/**
	 * The Bundle id of the bundle relevant to the event. The type of the value
	 * for this event property is <code>Long</code>.
	 * 
	 * @since 1.1
	 */
	public static final String	BUNDLE_ID			= "bundle.id";

	/**
	 * The Bundle object of the bundle relevant to the event. The type of the
	 * value for this event property is {@link Bundle}.
	 * 
	 * @since 1.1
	 */
	public static final String	BUNDLE				= "bundle";

	/**
	 * The version of the bundle relevant to the event. The type of the value
	 * for this event property is {@link Version}.
	 * 
	 * @since 1.2
	 */
	public static final String	BUNDLE_VERSION		= "bundle.version";

	/**
	 * The forwarded event object. Used when rebroadcasting an event that was
	 * sent via some other event mechanism. The type of the value for this event
	 * property is <code>Object</code>.
	 */
	public static final String	EVENT				= "event";

	/**
	 * An exception or error. The type of the value for this event property is
	 * <code>Throwable</code>.
	 */
	public static final String	EXCEPTION			= "exception";

	/**
	 * The name of the exception type. Must be equal to the name of the class of
	 * the exception in the event property {@link #EXCEPTION}. The type of the
	 * value for this event property is <code>String</code>.
	 * 
	 * @since 1.1
	 */
	public static final String	EXCEPTION_CLASS		= "exception.class";

	/**
	 * The exception message. Must be equal to the result of calling
	 * <code>getMessage()</code> on the exception in the event property
	 * {@link #EXCEPTION}. The type of the value for this event property is
	 * <code>String</code>.
	 */
	public static final String	EXCEPTION_MESSAGE	= "exception.message";

	/**
	 * A human-readable message that is usually not localized. The type of the
	 * value for this event property is <code>String</code>.
	 */
	public static final String	MESSAGE				= "message";

	/**
	 * A service reference. The type of the value for this event property is
	 * {@link ServiceReference}.
	 */
	public static final String	SERVICE				= "service";

	/**
	 * A service's id. The type of the value for this event property is
	 * <code>Long</code>.
	 */
	public static final String	SERVICE_ID			= Constants.SERVICE_ID;

	/**
	 * A service's objectClass. The type of the value for this event property is
	 * <code>String[]</code>.
	 */
	public static final String	SERVICE_OBJECTCLASS	= "service.objectClass";

	/**
	 * A service's persistent identity. The type of the value for this event
	 * property is <code>String</code>.
	 */
	public static final String	SERVICE_PID			= Constants.SERVICE_PID;

	/**
	 * The time when the event occurred, as reported by
	 * <code>System.currentTimeMillis()</code>. The type of the value for this
	 * event property is <code>Long</code>.
	 */
	public static final String	TIMESTAMP			= "timestamp";

	/**
	 * This constant was released with an incorrectly spelled name. It has been
	 * replaced by {@link #EXCEPTION_CLASS}
	 * 
	 * @deprecated As of 1.1, replaced by EXCEPTION_CLASS
	 */
	public static final String	EXECPTION_CLASS		= "exception.class";
}