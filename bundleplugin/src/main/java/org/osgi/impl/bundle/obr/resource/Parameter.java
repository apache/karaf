/*
 * $Id: Parameter.java 44 2007-07-13 20:49:41Z hargrave@us.ibm.com $
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

class Parameter {
	final static int	ATTRIBUTE	= 1;
	final static int	DIRECTIVE	= 2;
	final static int	SINGLE		= 0;

	int					type;
	String				key;
	String				value;

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(key);
		switch (type) {
			case ATTRIBUTE :
				sb.append("=");
				break;
			case DIRECTIVE :
				sb.append(":=");
				break;
			case SINGLE :
				return sb.toString();
		}
		sb.append(value);
		return sb.toString();
	}

	boolean is(String s, int type) {
		return this.type == type && key.equalsIgnoreCase(s);
	}
}
