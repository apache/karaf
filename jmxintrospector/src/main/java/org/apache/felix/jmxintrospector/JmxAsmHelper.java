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
package org.apache.felix.jmxintrospector;

import java.util.Hashtable;

import org.objectweb.asm.Type;

/**
 * This class just provides a mapping between JMX types and ASM ones 
 *
 */
public class JmxAsmHelper {
	private static Hashtable<String, Class> primitives=new Hashtable<String, Class>();

	static{
		primitives.put("long", Long.TYPE);
		primitives.put("boolean", Boolean.TYPE);
		primitives.put("short", Short.TYPE);
		primitives.put("int", Integer.TYPE);
		primitives.put("float", Float.TYPE);
		primitives.put("double", Double.TYPE);
		primitives.put("char", Character.TYPE);
		primitives.put("primitive", Byte.TYPE);
		primitives.put("void", Void.TYPE);
	}
	public static Type getAsmType(String jmxType) throws ClassNotFoundException{
		//FIXME: does it work with primitive arrays? and with multidimensional ones?
		Class clazz;
		if(primitives.containsKey(jmxType)) clazz=primitives.get(jmxType);
		else clazz=Class.forName(jmxType, true, JmxAsmHelper.class.getClassLoader());
		return Type.getType(clazz);
	}
}
