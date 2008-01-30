/*
 * $Header: /cvshome/build/org.osgi.service.event/src/org/osgi/service/event/Event.java,v 1.8 2006/07/12 13:17:04 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2005, 2006). All Rights Reserved.
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

import java.util.*;

import org.osgi.framework.Filter;

/**
 * An event.
 * 
 * <code>Event</code> objects are delivered to <code>EventHandler</code>
 * services which subsrcibe to the topic of the event.
 * 
 * @version $Revision: 1.8 $
 */
public class Event {
	/**
	 * The topic of this event.
	 */
	String	topic;
	/**
	 * The properties carried by this event. Keys are strings and values are
	 * objects
	 */
	Hashtable	properties;

	/**
	 * Constructs an event.
	 * 
	 * @param topic The topic of the event.
	 * @param properties The event's properties (may be <code>null</code>).
	 * 
	 * @throws IllegalArgumentException If topic is not a valid topic name.
	 */
	public Event(String topic, Dictionary properties) {
		this.topic = topic;
		validateTopicName();
		this.properties = new Hashtable();
		if (properties != null) {
			for (Enumeration e = properties.keys(); e.hasMoreElements();) {
				String key = (String) e.nextElement();
				Object value = properties.get(key);
				this.properties.put(key, value);
			}
		}
		this.properties.put(EventConstants.EVENT_TOPIC, topic);
	}

	/**
	 * Retrieves a property.
	 * 
	 * @param name the name of the property to retrieve
	 * 
	 * @return The value of the property, or <code>null</code> if not found.
	 */
	public final Object getProperty(String name) {
		return properties.get(name);
	}

	/**
	 * Returns a list of this event's property names.
	 * 
	 * @return A non-empty array with one element per property.
	 */
	public final String[] getPropertyNames() {
		String[] names = new String[properties.size()];
		Enumeration keys = properties.keys();
		for (int i = 0; keys.hasMoreElements(); i++) {
			names[i] = (String) keys.nextElement();
		}
		return names;
	}

	/**
	 * Returns the topic of this event.
	 * 
	 * @return The topic of this event.
	 */
	public final String getTopic() {
		return topic;
	}

	/**
	 * Tests this event's properties against the given filter.
	 * 
	 * @param filter The filter to test.
	 * 
	 * @return true If this event's properties match the filter, false
	 *         otherwise.
	 */
	public final boolean matches(Filter filter) {
		return filter.matchCase(properties);
	}

	/**
	 * Compares this <code>Event</code> object to another object.
	 * 
	 * <p>
	 * An event is considered to be <b>equal to </b> another
	 * event if the topic is equal and the properties are equal.
	 * 
	 * @param object The <code>Event</code> object to be compared.
	 * @return <code>true</code> if <code>object</code> is a
	 *         <code>Event</code> and is equal to this object;
	 *         <code>false</code> otherwise.
	 */
	public boolean equals(Object object) {
		if (object == this) { // quicktest
			return true;
		}

		if (!(object instanceof Event)) {
			return false;
		}

		Event event = (Event) object;
		return topic.equals(event.topic) && properties.equals(event.properties);
	}

	/**
	 * Returns a hash code value for the object.
	 * 
	 * @return An integer which is a hash code value for this object.
	 */
	public int hashCode() {
		return topic.hashCode() ^ properties.hashCode();
	}

	/**
	 * Returns the string representation of this event.
	 * 
	 * @return The string representation of this event.
	 */
	public String toString() {
		return getClass().getName() + " [topic=" + topic + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static final String	SEPARATOR	= "/"; //$NON-NLS-1$

	/**
	 * Called by the constructor to validate the topic name.
	 * 
	 * @throws IllegalArgumentException If the topic name is invalid.
	 */
	private void validateTopicName() {
		try {
			StringTokenizer st = new StringTokenizer(topic, SEPARATOR, true);
			validateToken(st.nextToken());

			while (st.hasMoreTokens()) {
				st.nextToken(); // consume delimiter
				validateToken(st.nextToken());
			}
		}
		catch (NoSuchElementException e) {
			throw new IllegalArgumentException("invalid topic"); //$NON-NLS-1$
		}
	}

	private static final String	tokenAlphabet	= "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-"; //$NON-NLS-1$

	/**
	 * Validate a token.
	 * 
	 * @throws IllegalArgumentException If the token is invalid.
	 */
	private void validateToken(String token) {
		int length = token.length();
		if (length < 1) {	// token must contain at least one character
			throw new IllegalArgumentException("invalid topic"); //$NON-NLS-1$
		}
		for (int i = 0; i < length; i++) { // each character in the token must be from the token alphabet
			if (tokenAlphabet.indexOf(token.charAt(i)) == -1) { //$NON-NLS-1$
				throw new IllegalArgumentException("invalid topic"); //$NON-NLS-1$
			}
		}
	}
}
