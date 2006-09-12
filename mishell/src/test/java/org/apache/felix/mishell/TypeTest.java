package org.apache.felix.mishell;

import org.objectweb.asm.Type;

public class TypeTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		StringBuffer s=new StringBuffer();
		s.append("bolean type: ");
		s.append(Type.BOOLEAN_TYPE.getDescriptor()+"\n");
		s.append("String: "+Type.getType((new String()).getClass()));
		System.out.println(s);

	}

}
