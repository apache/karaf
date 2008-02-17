/*
 * $Id: ManifestEntry.java 44 2007-07-13 20:49:41Z hargrave@us.ibm.com $
 * 
 * Copyright (c) OSGi Alliance (2002, 2006, 2007). All Rights Reserved.
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
package org.osgi.impl.bundle.obr.resource;

import java.util.*;


public class ManifestEntry implements Comparable {
	String		name;
	VersionRange	version;
	Map			attributes;
	public Map	directives;
	public Set	uses;

	public ManifestEntry(String name) {
		this.name = name;
	}

	public ManifestEntry(String name, VersionRange version) {
		this.name = name;
		this.version = version;
	}

	public String toString() {
		if (version == null)
			return name;
		return name + " ;version=" + version;
	}

	public String getName() {
		return name;
	}

	public VersionRange getVersion() {
		if (version != null)
			return version;
		return new VersionRange("0");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object o) {
		ManifestEntry p = (ManifestEntry) o;
		return name.compareTo(p.name);
	}

	/**
	 * @return
	 */
	public Object getPath() {
		return getName().replace('.', '/');
	}

	public Map getDirectives() {
		return directives;
	}

	public Map getAttributes() {
		return attributes;
	}

	/**
	 * @param parameter
	 */
	public void addParameter(Parameter parameter) {
		switch (parameter.type) {
			case Parameter.ATTRIBUTE :
				if (attributes == null)
					attributes = new HashMap();
				attributes.put(parameter.key, parameter.value);
				if (parameter.key.equalsIgnoreCase("version")
						|| parameter.key
								.equalsIgnoreCase("specification-version"))
					this.version = new VersionRange(parameter.value);
				break;

			case Parameter.DIRECTIVE :
				if (directives == null)
					directives = new HashMap();
				directives.put(parameter.key, parameter.value);
				break;
		}
	}

	public ManifestEntry getAlias(String key) {
		ManifestEntry me = new ManifestEntry(key);
		me.attributes = attributes;
		me.directives = directives;
		me.version = version;
		return me;
	}

}
