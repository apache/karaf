/*
 * Copyright (c) OSGi Alliance (2005, 2009). All Rights Reserved.
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

import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.osgi.framework.Filter;

/**
 * An event.
 * 
 * <code>Event</code> objects are delivered to <code>EventHandler</code>
 * services which subscribe to the topic of the event.
 * 
 * @Immutable
 * @version $Revision: 7003 $
 */
public class Event {
	/**
	 * The topic of this event.
	 */
	private final String	topic;
	/**
	 * The properties carried by this event. Keys are strings and values are
	 * objects
	 */
	private final Map		/* <String,Object> */properties;

	/**
	 * Constructs an event.
	 * 
	 * @param topic The topic of the event.
	 * @param properties The event's properties (may be <code>null</code>). A
	 *        property whose key is not of type <code>String</code> will be
	 *        ignored.
	 * @throws IllegalArgumentException If topic is not a valid topic name.
	 * @since 1.2
	 */
	public Event(String topic, Map/* <String,Object> */properties) {
		validateTopicName(topic);
		this.topic = topic;
		int size = (properties == null) ? 1 : (properties.size() + 1);
		Map p = new HashMap(size);
		if (properties != null) {
			for (Iterator iter = properties.keySet().iterator(); iter.hasNext();) {
				Object key = iter.next();
				if (key instanceof String) {
					Object value = properties.get(key);
					p.put(key, value);
				}
			}
		}
		p.put(EventConstants.EVENT_TOPIC, topic);
		this.properties = p; // safely publish the map
	}

	/**
	 * Constructs an event.
	 * 
	 * @param topic The topic of the event.
	 * @param properties The event's properties (may be <code>null</code>). A
	 *        property whose key is not of type <code>String</code> will be
	 *        ignored.
	 * @throws IllegalArgumentException If topic is not a valid topic name.
	 */
	public Event(String topic, Dictionary/* <String,Object> */properties) {
		validateTopicName(topic);
		this.topic = topic;
		int size = (properties == null) ? 1 : (properties.size() + 1);
		Map p = new HashMap(size);
		if (properties != null) {
			for (Enumeration e = properties.keys(); e.hasMoreElements();) {
				Object key = e.nextElement();
				if (key instanceof String) {
					Object value = properties.get(key);
					p.put(key, value);
				}
			}
		}
		p.put(EventConstants.EVENT_TOPIC, topic);
		this.properties = p; // safely publish the map
	}

	/**
	 * Retrieves a property.
	 * 
	 * @param name the name of the property to retrieve
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
		return (String[]) properties.keySet().toArray(
				new String[properties.size()]);
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
	 * Tests this event's properties against the given filter using a case
	 * sensitive match.
	 * 
	 * @param filter The filter to test.
	 * @return true If this event's properties match the filter, false
	 *         otherwise.
	 */
	public final boolean matches(Filter filter) {
		return filter.matchCase(new UnmodifiableDictionary(properties));
	}

	/**
	 * Compares this <code>Event</code> object to another object.
	 * 
	 * <p>
	 * An event is considered to be <b>equal to</b> another event if the topic
	 * is equal and the properties are equal.
	 * 
	 * @param object The <code>Event</code> object to be compared.
	 * @return <code>true</code> if <code>object</code> is a <code>Event</code>
	 *         and is equal to this object; <code>false</code> otherwise.
	 */
	public boolean equals(Object object) {
		if (object == this) { // quick test
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
		int h = 31 * 17 + topic.hashCode();
		h = 31 * h + properties.hashCode();
		return h;
	}

	/**
	 * Returns the string representation of this event.
	 * 
	 * @return The string representation of this event.
	 */
	public String toString() {
		return getClass().getName() + " [topic=" + topic + "]";  
	}

	/**
	 * Called by the constructor to validate the topic name.
	 * 
	 * @param topic The topic name to validate.
	 * @throws IllegalArgumentException If the topic name is invalid.
	 */
	private static void validateTopicName(String topic) {
	    char[] chars = topic.toCharArray();
	    int length = chars.length;
	    if (length == 0) {
			throw new IllegalArgumentException("empty topic");
		}
		for (int i = 0; i < length; i++) {
	        char ch = chars[i];
	        if (ch == '/') {
	        	// Can't start or end with a '/' but anywhere else is okay
				if (i == 0 || (i == length - 1)) {
	                throw new IllegalArgumentException(
							"invalid topic: "
							+ topic); 
	            }
	            // Can't have "//" as that implies empty token
	            if (chars[i-1] == '/') {
	                throw new IllegalArgumentException(
							"invalid topic: "
							+ topic); 
	            }
	            continue;
	        }
	        if (('A' <= ch) && (ch <= 'Z')) {
	            continue;
	        }
	        if (('a' <= ch) && (ch <= 'z')) {
	            continue;
	        }
	        if (('0' <= ch) && (ch <= '9')) {
	            continue;
	        }
	        if ((ch == '_') || (ch == '-')) {
	            continue;
	        }
	        throw new IllegalArgumentException("invalid topic: " + topic); 
	    }
	}
	
	/**
	 * Unmodifiable wrapper for Dictionary.
	 */
	private static class UnmodifiableDictionary extends Dictionary {
		private final Map	wrapped;
		UnmodifiableDictionary(Map wrapped) {
			this.wrapped = wrapped;
		}
		public Enumeration elements() {
			return Collections.enumeration(wrapped.values());
		}
		public Object get(Object key) {
			return wrapped.get(key);
		}
		public boolean isEmpty() {
			return wrapped.isEmpty();
		}
		public Enumeration keys() {
			return Collections.enumeration(wrapped.keySet());
		}
		public Object put(Object key, Object value) {
			throw new UnsupportedOperationException();
		}
		public Object remove(Object key) {
			throw new UnsupportedOperationException();
		}
		public int size() {
			return wrapped.size();
		}
	}
}
