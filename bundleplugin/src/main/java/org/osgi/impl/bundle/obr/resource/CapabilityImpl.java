/*
 * $Id: CapabilityImpl.java 84 2008-08-28 08:11:30Z peter.kriens@aqute.biz $
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

import java.lang.reflect.Array;
import java.util.*;

import org.osgi.service.obr.Capability;
import org.xmlpull.v1.XmlPullParser;



public class CapabilityImpl implements Capability {
	String				name;
	Map	properties	= new TreeMap();

	public CapabilityImpl(String name) {
		this.name = name;
	}

	public CapabilityImpl(XmlPullParser parser) throws Exception {
		parser.require(XmlPullParser.START_TAG, null, "capability");
		name = parser.getAttributeValue(null,"name");
		while ( parser.nextTag() == XmlPullParser.START_TAG ) {
			if ( parser.getName().equals("p")) {
				String name = parser.getAttributeValue(null,"n");
				String value = parser.getAttributeValue(null,"v");
				String type = parser.getAttributeValue(null,"t");
				Object v = value;

				if ( "nummeric".equals(type))
					v = new Long(value);
				else if ( "version".equals(type))
					v = new VersionRange(value);
				addProperty(name,v);
			}
			parser.next();
			parser.require(XmlPullParser.END_TAG, null, "p" );
		}
		parser.require(XmlPullParser.END_TAG, null, "capability" );
	}


	public void addProperty(String key, Object value) {
		List values = (List) properties.get(key);
		if (values == null) {
			values = new ArrayList();
			properties.put(key, values);
		}
		values.add(value);
	}

	public Tag toXML() {
		return toXML(this);
	}
	
	public static Tag toXML(Capability capability) {
		Tag tag = new Tag("capability");
		tag.addAttribute("name", capability.getName());
		Map properties = capability.getProperties();
		for ( Iterator k= properties.keySet().iterator(); k.hasNext(); ) {
			String key = (String) k.next();
			List values = (List) properties.get(key);
			for ( Iterator v = values.iterator(); v.hasNext(); ) {
				Object value = v.next();
				Tag p = new Tag("p");
				tag.addContent(p);
				p.addAttribute("n", key);
				if ( value != null ) {
					p.addAttribute("v", valueString(value));
					String type = null;
					if (value instanceof Number )
						type = "number";
					else if (value.getClass() == VersionRange.class)
						type = "version";
					else if ( value.getClass().isArray() ) {
						type = "set";
					}
					
					if (type != null)
						p.addAttribute("t", type);
				}
				else
					System.out.println("Missing value " + key);
			}
		}
		return tag;
	}


	private static String valueString(Object value) {
		if ( value.getClass().isArray() ) {
			StringBuffer buf = new StringBuffer();
			for ( int i = 0; i < Array.getLength(value); i++) {
				if ( i > 0 ) {
					buf.append( "," );
				}
				buf.append( Array.get(value, i).toString() );
			}
			return buf.toString();
		}
		else {
			return value.toString();
		}
	}

	public String getName() {
		return name;
	}


	public Map getProperties() {
		return properties;
	}

}
