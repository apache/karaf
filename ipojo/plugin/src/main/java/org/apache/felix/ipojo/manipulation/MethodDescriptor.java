package org.apache.felix.ipojo.manipulation;

import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.Type;

public class MethodDescriptor {
	
	private String m_name;
	private String m_returnType;
	private String[] m_arguments;
	
	public MethodDescriptor(String name, String desc) {
		m_name = name;
		Type ret = Type.getReturnType(desc);
		Type[] args = Type.getArgumentTypes(desc);
		
		m_returnType = getType(ret);
		m_arguments = new String[args.length];
		for(int i = 0; i < args.length; i++) {
			m_arguments[i] = getType(args[i]);
		}
	}

	public Element getElement() {
		Element method = new Element("method", "");
		method.addAttribute(new Attribute("name", m_name));
        
        // Add return
        if(!m_returnType.equals("void")) {
            method.addAttribute(new Attribute("return", m_returnType));
        }
        
        // Add arguments
        if(m_arguments.length > 0) {
            String args = "{";
			args += m_arguments[0];
			for(int i = 1; i < m_arguments.length; i++) {
				args += "," + m_arguments[i];
			}
            args += "}";
            method.addAttribute(new Attribute("arguments", args));
		}
		
		
		return method;
	}
	
	
	private String getType(Type type) {
		switch(type.getSort()) {
		case Type.ARRAY : 
			Type elemType = type.getElementType();
			return getType(elemType)+"[]";
		case Type.BOOLEAN : 
			return "boolean";
		case Type.BYTE :
			return "byte";
		case Type.CHAR : 
			return "char";
		case Type.DOUBLE : 
			return "double";
		case Type.FLOAT : 
			return "float";
		case Type.INT : 
			return "int";
		case Type.LONG : 
			return "long";
		case Type.OBJECT : 
			return type.getClassName();
		case Type.SHORT : 
			return "short";
		case Type.VOID : 
			return "void";
		default : 
			return "unknown";
		}
	}
	
	public String getName() { return m_name; }

}
