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
package org.apache.felix.dm.resources;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;

import org.osgi.framework.Bundle;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class BundleResourceRepository {
	private final Bundle m_bundle;
	
	public BundleResourceRepository(Bundle bundle) {
		m_bundle = bundle;
	}

	public synchronized void addHandler(ServiceReference ref, ResourceHandler handler) {
		String filter = (String) ref.getProperty("filter"); // "(&(repository=a)(path=b)(name=*.xml))"
		Filter filterObject = null;
		try {
			filterObject = FrameworkUtil.createFilter(filter);
		} 
		catch (InvalidSyntaxException e) {
			e.printStackTrace();
			return;
		}
		Enumeration entries = m_bundle.findEntries("/", null, true);
		while (entries.hasMoreElements()) {
			EntryResource resource = new EntryResource(m_bundle, (URL) entries.nextElement());
			if (filterObject.match(resource)) {
                handler.added(resource);
			}
		}
	}

	public synchronized void removeHandler(ServiceReference ref, ResourceHandler handler) {
		String filter = (String) ref.getProperty("filter"); // "(&(repository=a)(path=b)(name=*.xml))"
		Filter filterObject = null;
		try {
			filterObject = FrameworkUtil.createFilter(filter);
		}
		catch (InvalidSyntaxException e) {
			e.printStackTrace();
			return;
		}
		Enumeration entries = m_bundle.findEntries("/", null, true);
        while (entries.hasMoreElements()) {
            EntryResource resource = new EntryResource(m_bundle, (URL) entries.nextElement());
            if (filterObject.match(resource)) {
                handler.removed(resource);
            }
        }
	}

	static class EntryResource extends Dictionary implements Resource {
		private final URL m_entry;
        private final String m_id;
        private final String m_repository;
        private final String m_path;
        private final String m_name;
        private static Object[] m_keys;
        private Object[] m_values;

		public EntryResource(Bundle bundle, URL entry) {
			m_entry = entry;
			// TODO is this unique? can we have the same url in more than one repository?
			m_id = m_entry.toString();
			m_repository = bundle.getSymbolicName() + "_" + bundle.getHeaders().get("Bundle-Version");
			String path = entry.getPath();
			int i = path.lastIndexOf('/');
			if (i == -1) {
			    m_path = "/";
			    m_name = path;
			}
			else {
			    if (path.length() > (i + 1)) {
    			    m_path = path.substring(0, i);
    			    m_name = path.substring(i + 1);
			    }
			    else {
			        m_path = path;
			        m_name = "";
			    }
			}
		}

		public final String getID() {
		    return m_id;
		}
		
		public final String getName() {
			return m_name;
		}

		public final String getPath() {
			return m_path;
		}
		
		public final String getRepository() {
			return m_repository;
		}

		public final InputStream openStream() throws IOException {
			return m_entry.openStream();
		}

        public Enumeration elements() {
            if (m_values == null) {
                m_values = new Object[] { m_id, m_repository, m_path, m_name };
            }
            return new ArrayEnumeration(m_values);
        }

        public Object get(Object key) {
            if (Resource.ID.equals(key)) {
                return m_id;
            }
            else if (Resource.REPOSITORY.equals(key)) {
                return m_repository;
            }
            else if (Resource.PATH.equals(key)) {
                return m_path;
            }
            else if (Resource.NAME.equals(key)) {
                return m_name;
            }
            return null;
        }

        public boolean isEmpty() {
            return false;
        }

        public Enumeration keys() {
            if (m_keys == null) {
                m_keys = new Object[] { Resource.ID, Resource.REPOSITORY, Resource.PATH, Resource.NAME };
            }
            return new ArrayEnumeration(m_keys);
        }

        public Object put(Object key, Object value) {
            return null;
        }

        public Object remove(Object key) {
            return null;
        }

        public int size() {
            return 4;
        }
	}
	
	static class ArrayEnumeration implements Enumeration {
	    private int m_counter = 0;
	    private Object[] m_elements;
	    
	    public ArrayEnumeration(Object[] array) {
	        m_elements = array;
	    }
	    
	    public boolean hasMoreElements() {
	        return (m_counter < m_elements.length);
	    }
	    
	    public Object nextElement() {
	        return m_elements[m_counter++];
	    }
	}
}
