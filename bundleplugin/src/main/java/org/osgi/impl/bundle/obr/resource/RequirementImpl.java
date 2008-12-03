/*
 * $Id: RequirementImpl.java 44 2007-07-13 20:49:41Z hargrave@us.ibm.com $
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

import org.osgi.service.obr.*;
import org.xmlpull.v1.XmlPullParser;



/**
 * Implements the Requirement interface.
 * 
 * 
 * @version $Revision: 44 $
 */
public class RequirementImpl implements Requirement {
	int		id;
	String	name;
	String	filter="()";
	FilterImpl	_filter;
	String	comment;
	boolean optional;
	boolean multiple;
	boolean extend;
	
	/**
	 * Create a requirement with the given name.
	 * 
	 * @param name
	 */
	public RequirementImpl(String name) {
		this.name = name;
	}


	/**
	 * Parse the requirement from the pull parser.
	 * 
	 * @param parser
	 * @throws Exception
	 */
	public RequirementImpl(XmlPullParser parser) throws Exception {
		parser.require(XmlPullParser.START_TAG, null, null );
		name = parser.getAttributeValue(null, "name");
		filter = parser.getAttributeValue(null, "filter");
		
		String opt = parser.getAttributeValue(null,"optional");
		String mul = parser.getAttributeValue(null,"multiple");
		String ext = parser.getAttributeValue(null,"extend");
		optional = "true".equalsIgnoreCase(opt);
		multiple = "true".equalsIgnoreCase(mul);
		extend = "true".equalsIgnoreCase(ext);
		
		
		StringBuffer sb = new StringBuffer();
		while ( parser.next() == XmlPullParser.TEXT ) {
			sb.append( parser.getText() );
		}
		if ( sb.length() > 0 )
			setComment(sb.toString().trim());
			
		parser.require(XmlPullParser.END_TAG, null, null );
	}

	public void setFilter(String filter) {
		this.filter = filter;
		_filter= null;
	}

	public String getFilter() {
		return filter;
	}

	public Tag toXML(String name) {
		Tag tag = toXML(this);
		tag.rename(name);
		return tag;
	}


	public String getName() {
		return name;
	}

	public boolean isSatisfied(Capability capability) {
		if (_filter == null)
			_filter = new FilterImpl(filter);

		boolean result = _filter.match(capability.getProperties());
		return result;
	}

	public String toString() {
		return name + " " + filter;
	}


	public String getComment() {
		return comment;
	}


	public void setComment(String comment) {
		this.comment=comment;
	}


	public static Tag toXML(Requirement requirement) {
		Tag req = new Tag("require");
		req.addAttribute("name", requirement.getName());
		req.addAttribute("filter", requirement.getFilter());
		
		req.addAttribute("optional", requirement.isOptional()+"");
		req.addAttribute("multiple", requirement.isMultiple()+"");
		req.addAttribute("extend", requirement.isExtend()+"");
		
		if ( requirement.getComment() != null )
			req.addContent(requirement.getComment());
		
		return req;
	}


	public boolean isMultiple() {
		return multiple;
	}


	public boolean isOptional() {
		return optional;
	}


	public void setOptional(boolean b) {
		optional = b;
	}

	public void setMultiple(boolean b) {
		multiple = b;
	}


	public boolean equals(Object o) {
		if ( ! (o instanceof Requirement) )
			return false;
		
		Requirement r2 = (Requirement)o;
		return filter.equals(r2.getFilter()) && name.equals(r2.getName()); 
	}
	
	public int hashCode() {
		return filter.hashCode() ^ name.hashCode();
	}
	
	public boolean isExtend() {
		return extend;
	}
	
	public void setExtend(boolean extend) {
		this.extend = extend;
	}
}
