/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.bundlerepository.impl.metadataparser;

import java.util.*;

public class MultivalueMap implements Map {
	private Map m_map = null;

	public MultivalueMap() {
		m_map = new HashMap();
	}

	public MultivalueMap(Map map) {
		m_map = map;
	}

	/**
	 * @see java.util.Map#size()
	 */
	public int size() {
		return m_map.size();
	}

	/**
	 * @see java.util.Map#clear()
	 */
	public void clear() {
		m_map.clear();
	}

	/**
	 * @see java.util.Map#isEmpty()
	 */
	public boolean isEmpty() {
		return m_map.isEmpty();
	}

	/**
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	public boolean containsKey(Object arg0) {
		return m_map.containsKey(arg0);
	}

	/**
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	public boolean containsValue(Object arg0) {
		return false;
	}

	/**
	 * @see java.util.Map#values()
	 */
	public Collection values() {
		return null;
	}

	/**
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	public void putAll(Map arg0) {
	}

	/**
	 * @see java.util.Map#entrySet()
	 */
	public Set entrySet() {
		return m_map.entrySet();
	}

	/**
	 * @see java.util.Map#keySet()
	 */
	public Set keySet() {
		return m_map.keySet();
	}

	/**
	 * @see java.util.Map#get(java.lang.Object)
	 */
	public Object get(Object key) {
		return m_map.get(key);
	}

	/**
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	public Object remove(Object arg0) {
		return m_map.remove(arg0);
	}

	/**
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	public Object put(Object key, Object value) {
		Object prev = m_map.get(key);
		if (prev == null) {
            List list = new ArrayList();
            list.add(value);
			m_map.put(key, list);
			return list;
		} else {
            ((List) prev).add(value);
            return prev;
		}
	}

	public String toString() {
		StringBuffer sb=new StringBuffer();
		sb.append("[MultivalueMap:");
		if(m_map.isEmpty()) {
			sb.append("empty");
		} else {
			Set keys=m_map.keySet();
			Iterator iter=keys.iterator();
			while(iter.hasNext()){
				String key=(String)iter.next();
				sb.append("\n\"").append(key).append("\":");
				sb.append(m_map.get(key).toString());		
			}
			sb.append('\n');
		}
		sb.append(']');
		return sb.toString();
	}
}