package org.apache.felix.jmxintrospector;

import java.util.Hashtable;

import org.objectweb.asm.Type;

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
