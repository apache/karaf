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
package org.apache.felix.bundlerepository.metadataparser;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import org.apache.felix.bundlerepository.metadataparser.kxmlsax.KXml2SAXHandler;
import org.xml.sax.SAXException;


/**
 * SAX handler for the XML file
 */
public class XmlCommonHandler implements KXml2SAXHandler {

	private static final String PI_MAPPING = "mapping";

	public static final String METADATAPARSER_PIS = "METADATAPARSER_PIS";

	public static final String METADATAPARSER_TYPES = "METADATAPARSER_TYPES";

	private int columnNumber;

	private int lineNumber;

	private boolean traceFlag = false;

	private static String VALUE = "value";

	//
	// Data
	//

	private XmlStackElement root;

	private Stack elementStack;
	
	private Map pis;

	private boolean missingPIExceptionFlag;

	private Map types;

	private TypeEntry defaultType;

	private StringBuffer currentText;

	private Map context;

	private class XmlStackElement {
		
		public final String qname;
		public Object object;
		
		public XmlStackElement(String qname, Object object) {
			super();
			this.qname = qname;
			this.object = object;
		};
	}

	public class TypeEntry {
		public final Object instanceFactory;
		public final Class instanceClass;
		public final Method newInstanceMethod;
		public final Class castClass;
		public final Method defaultAddMethod;
		
		public TypeEntry(Object instanceFactory, Class castClass, Method defaultAddMethod) throws Exception {
			super();
			this.instanceFactory = instanceFactory;
						
			try {
				if (instanceFactory instanceof Class) {
					newInstanceMethod = instanceFactory.getClass()
						.getDeclaredMethod("newInstance", null);
					if (castClass == null) {
						this.castClass = (Class) instanceFactory;
					} else {
						if (!castClass.isAssignableFrom((Class) instanceFactory)) {
							throw new Exception(
											"instanceFactory "
											+ instanceFactory.getClass().getName()
											+ " could not instanciate objects assignable to "
											+ castClass.getName());
						}
						this.castClass=castClass;
					}
					instanceClass = (Class) instanceFactory;
				} else {
					newInstanceMethod = instanceFactory.getClass()
						.getDeclaredMethod("newInstance", null);
					Class returnType = newInstanceMethod.getReturnType();
					if (castClass == null) {
						this.castClass = returnType;
					} else if (!castClass.isAssignableFrom(returnType)) {
						throw new Exception(
								"instanceFactory "
								+ instanceFactory.getClass().getName()
								+ " could not instanciate objects assignable to "
								+ castClass.getName());
					} else 
						this.castClass=castClass;
					instanceClass = returnType;
				}
			} catch (NoSuchMethodException e) {
				throw new Exception(
						"instanceFactory " + instanceFactory.getClass().getName()
						+ " should have a newInstance method");
			}
			
			// TODO check method
			this.defaultAddMethod = defaultAddMethod;
            if (this.defaultAddMethod != null)
            {
                this.defaultAddMethod.setAccessible(true);
            }
		}
		
		public String toString(){
			StringBuffer sb=new StringBuffer();
			sb.append("[");
			if(instanceFactory instanceof Class)
				sb.append("instanceFactory=").append(((Class)instanceFactory).getName());
			else
				sb.append("instanceFactory=").append(instanceFactory.getClass().getName());
			sb.append(",instanceClass=").append(instanceClass.getName());
			sb.append(",castClass=").append(castClass.getName());
			sb.append(",defaultAddMethod=");
			if(defaultAddMethod==null) sb.append(""); else sb.append(defaultAddMethod.getName());
			sb.append("]");
			return sb.toString();
		}
	}
	
	public XmlCommonHandler() {
		elementStack = new Stack();
		pis = new HashMap();
		missingPIExceptionFlag = false;
		types = new HashMap();
		context = new HashMap();
		context.put(METADATAPARSER_PIS, pis);
		context.put(METADATAPARSER_TYPES, types);
	}

	public void addPI(String piname, Class clazz) {
		pis.put(piname, clazz);
	}

	/**
	 * set the missing PI exception flag. If during parsing, the flag is true
	 * and the processing instruction is unknown, then the parser throws a
	 * exception
	 * 
	 * @param flag
	 */
	public void setMissingPIExceptionFlag(boolean flag) {
		missingPIExceptionFlag = flag;
	}
	
	public void addType(String qname, Object instanceFactory, Class castClass, Method defaultAddMethod)
	throws Exception {

		TypeEntry typeEntry;
		try {
			typeEntry = new TypeEntry(
					instanceFactory,
					castClass,
					defaultAddMethod
				);
		} catch (Exception e) {
			throw new Exception(lineNumber + "," + columnNumber + ":" + qname + " : " + e.getMessage());
		}
		types.put(qname,typeEntry);		
		trace("element "
				+ qname
				+ " : " + typeEntry.toString());
	}
		
	public void setDefaultType(Object instanceFactory, Class castClass, Method defaultAddMethod)
			throws Exception {
		TypeEntry typeEntry;
		try {
			typeEntry = new TypeEntry(
					instanceFactory,
					castClass,
					defaultAddMethod
				);
		} catch (Exception e) {
			throw new Exception(lineNumber + "," + columnNumber + ": default element : " + e.getMessage());
		}
		defaultType=typeEntry;		
		trace("default element "
				+ " : " + typeEntry.toString());
	}

	public void setContext(Map context) {
		this.context = context;
	}

	public Map getContext() {
		return context;
	}

	public Object getRoot() {
		return root.object;
	}

	/* for PCDATA */
	public void characters(char[] ch, int offset, int length) throws Exception {
		if (currentText != null)
			currentText.append(ch, offset, length);
	}

	private String adderOf(Class clazz) {
		return "add"
				+ ClassUtility
						.capitalize(ClassUtility.classOf(clazz.getName()));
	}

	private String adderOf(String key) {
		return "add" + ClassUtility.capitalize(key);
	}

	private String setterOf(Class clazz) {
		return "set"
				+ ClassUtility
						.capitalize(ClassUtility.classOf(clazz.getName()));
	}

	private String setterOf(String key) {
		return "set" + ClassUtility.capitalize(key);
	}

	/**
	 * set the parser context in a object
	 */
	private void setObjectContext(Object object)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		Method method = null;
		try {
			// TODO setContext from castClass or object.getClass() ?
			method = object.getClass().getDeclaredMethod("setContext",
					new Class[] { Map.class });
		} catch (NoSuchMethodException e) {
			// do nothing
		}
		if (method != null) {
			trace(method.getName());
			try {
				method.invoke(object, new Object[] { context });
			} catch (InvocationTargetException e) {
				e.getTargetException().printStackTrace(System.err);
				throw e;
			}
		}
	}

	/**
	 * set the parser context in a object
	 * 
	 * @throws Throwable
	 */
	private void invokeProcess(Object object) throws Throwable {
		Method method = null;
		try {
			// TODO process from castClass or object.getClass() ?
			method = object.getClass().getDeclaredMethod("process", null);
		} catch (NoSuchMethodException e) {
			// do nothing
		}
		if (method != null) {
			trace(method.getName());
			try {
				method.invoke(object, null);
			} catch (InvocationTargetException e) {
				// e.getTargetException().printStackTrace(System.err);
				throw e.getTargetException();
			}

		}
	}

	/**
	 * set the parent in a object
	 */
	private void setObjectParent(Object object, Object parent)
			throws InvocationTargetException, IllegalArgumentException,
			IllegalAccessException {
		Method method = null;
		try {
			// TODO setParent from castClass or object.getClass() ?
			method = object.getClass().getDeclaredMethod("setParent",
					new Class[] { parent.getClass() });
		} catch (NoSuchMethodException e) {
			// do nothing
		}
		if (method != null) {
			trace(method.getName());
			try {
				method.invoke(object, new Object[] { parent });
			} catch (InvocationTargetException e) {
				e.getTargetException().printStackTrace(System.err);
				throw e;
			}
		}
	}

	/**
	 * Method called when a tag opens
	 * 
	 * @param uri
	 * @param localName
	 * @param qName
	 * @param attrib
	 * @exception SAXException
	 */
	public void startElement(String uri, String localName, String qName,
			Properties attrib) throws Exception {

		trace("START (" + lineNumber + "," + columnNumber + "):" + uri + ":"
				+ qName);

		// TODO: should add uri in the qname in the future
		TypeEntry type=(TypeEntry) types.get(qName);
		if(type==null) {
			type=defaultType;
		}

		Object obj = null;
		if (type != null) {

			try {
				// enables to access to "unmuttable" method
				type.newInstanceMethod.setAccessible(true);
				obj = type.newInstanceMethod.invoke(type.instanceFactory, null);
			} catch (Exception e) {
				// do nothing
			}

			// set parent
			if (!elementStack.isEmpty()) {
				XmlStackElement parent = (XmlStackElement) elementStack.peek();
				setObjectParent(obj, parent.object);
			}

			// set the parser context
			setObjectContext(obj);

			// set the attributes
			Set keyset = attrib.keySet();
			Iterator iter = keyset.iterator();
			while (iter.hasNext()) {
				String key = (String) iter.next();

				// substitute ${property} sbustrings by context' properties
				// values
				String value = ReplaceUtility.replace((String) attrib.get(key),
						context);

				// Firstly, test if the getter or the adder exists

				Method method = null;
				if (!(obj instanceof String)) {
					try {
						// method = castClass.getDeclaredMethod(setterOf(key),new
						// Class[] { String.class });
						method = type.instanceClass.getDeclaredMethod(setterOf(key),
								new Class[] { String.class });
					} catch (NoSuchMethodException e) {
						// do nothing
					}
					if (method == null)
						try {
							method = type.instanceClass.getDeclaredMethod(adderOf(key),
									new Class[] { String.class });

						} catch (NoSuchMethodException e) {
							/*
							 * throw new Exception(lineNumber + "," +
							 * columnNumber + ":" + "element " + qName + " does
							 * not support the attribute " + key);
							 */
						}

				}
				
				if (method != null) {
					trace(method.getName());
					try {
						method.invoke(obj, new String[] { value });
					} catch (InvocationTargetException e) {
						e.getTargetException().printStackTrace(System.err);
						throw e;
					}
				} else {
					
					if (obj instanceof String) {
						if (key.equals(VALUE)) {
							obj = value;
						} else {
							throw new Exception(lineNumber + "," + columnNumber
									+ ":" + "String element " + qName
									+ " cannot have other attribute than value");
						}
					} else {
						if (type.defaultAddMethod != null) {
							Class[] parameterTypes=type.defaultAddMethod.getParameterTypes();
							if(parameterTypes.length==2
								&& parameterTypes[0].isAssignableFrom(String.class)
								&& parameterTypes[1].isAssignableFrom(String.class)
							){
								type.defaultAddMethod.invoke(obj,new String[]{key, value});
							} else if(parameterTypes.length==1
										&& parameterTypes[0].isAssignableFrom(String.class)
									){
										type.defaultAddMethod.invoke(obj,new String[]{value});
							} else 
								throw new Exception(lineNumber + "," + columnNumber
										+ ":" + "class "
										+ type.instanceFactory.getClass().getName()
										+ " for element " + qName
										+ " does not support the attribute " + key
										);							
						} else {
							throw new Exception(lineNumber + "," + columnNumber
								+ ":" + "class "
								+ type.instanceFactory.getClass().getName()
								+ " for element " + qName
								+ " does not support the attribute " + key
								);
						}

					}
				}

			}

		} else {
			throw new Exception(lineNumber + "," + columnNumber + ":"
					+ "this element " + qName + " has not corresponding class");
		}
		XmlStackElement element=new XmlStackElement(qName,obj);
		if (root == null)
			root = element;
		elementStack.push(element);
		currentText = new StringBuffer();

		trace("START/ (" + lineNumber + "," + columnNumber + "):" + uri + ":"
				+ qName);
	}

	/**
	 * Method called when a tag closes
	 * 
	 * @param uri
	 * @param localName
	 * @param qName
	 * @exception SAXException
	 */
	public void endElement(java.lang.String uri, java.lang.String localName,
			java.lang.String qName) throws Exception {

		trace("END (" + lineNumber + "," + columnNumber + "):" + uri + ":"
				+ qName);

		XmlStackElement element = (XmlStackElement) elementStack.pop();
		TypeEntry elementType=(TypeEntry) types.get(element.qname);
		if(elementType==null) {
			elementType=defaultType;
		}		
		
		if (currentText != null && currentText.length() != 0) {

			String currentStr = ReplaceUtility.replace(currentText.toString(),
					context).trim();
			// TODO: trim may be not the right choice
			trace("current text:" + currentStr);

			Method method = null;
			try {
				method = elementType.castClass.getDeclaredMethod("addText",
						new Class[] { String.class });
			} catch (NoSuchMethodException e) {
				try {
					method = elementType.castClass.getDeclaredMethod("setText",
							new Class[] { String.class });
				} catch (NoSuchMethodException e2) {
					// do nothing
				}
			}
			if (method != null) {
				trace(method.getName());
				try {
					method.invoke(element.object, new String[] { currentStr });
				} catch (InvocationTargetException e) {
					e.getTargetException().printStackTrace(System.err);
					throw e;
				}
			} else {
				if (String.class.isAssignableFrom(elementType.castClass)) {
					String str = (String) element.object;
					if (str.length() != 0) {
						throw new Exception(
								lineNumber
										+ ","
										+ columnNumber
										+ ":"
										+ "String element "
										+ qName
										+ " cannot have both PCDATA and an attribute value");
					} else {
						element.object = currentStr;
					}
				}
			}

		}

		currentText = null;

		if (!elementStack.isEmpty()) {

			XmlStackElement parent = (XmlStackElement) elementStack.peek();
			TypeEntry parentType = (TypeEntry) types.get(parent.qname);
			if(parentType==null) {
				parentType=defaultType;
			}		

			String capqName=ClassUtility.capitalize(qName);
			Method method = null;
			try {
// TODO: OBR PARSER: We should also check for instance class as a parameter.
				method = parentType.instanceClass.getDeclaredMethod(
						adderOf(capqName),
						new Class[] { elementType.castClass });  // instanceClass
			} catch (NoSuchMethodException e) {
				trace("NoSuchMethodException: "
						+ adderOf(capqName) + "("+elementType.castClass.getName()+")");
				// do nothing
			}
			if (method == null)
                try {
					method = parentType.instanceClass.getDeclaredMethod(
						setterOf(capqName),
						new Class[] { elementType.castClass });
				} catch (NoSuchMethodException e) {
					trace("NoSuchMethodException: "
							+ setterOf(capqName) + "("+elementType.castClass.getName()+")");
					// do nothing
				}
			/*if (method == null)
				try {
					method = parentType.castClass.getDeclaredMethod(
							adderOf(type.castClass),
							new Class[] { type.castClass });
				} catch (NoSuchMethodException e) {
					trace("NoSuchMethodException: " + adderOf(type.castClass)+ "("+type.castClass.getName()+")");
					// do nothing
				}
			if (method == null)
				try {
					method = parentType.castClass.getDeclaredMethod(
							setterOf(type.castClass),
							new Class[] { type.castClass });
				} catch (NoSuchMethodException e) {
					trace("NoSuchMethodException: " + setterOf(type.castClass)+ "("+type.castClass.getName()+")");
					// do nothing
				}
				*/
			if (method != null) {
				trace(method.getName());
				try {
                    method.setAccessible(true);
					method.invoke(parent.object, new Object[] { element.object });
				} catch (InvocationTargetException e) {
					e.getTargetException().printStackTrace(System.err);
					throw e;
				}
			} else {
				if (parentType.defaultAddMethod != null) {
					Class[] parameterTypes=parentType.defaultAddMethod.getParameterTypes();
					if(parameterTypes.length==2
						&& parameterTypes[0].isAssignableFrom(String.class)
						&& parameterTypes[1].isAssignableFrom(elementType.castClass)
					){
						parentType.defaultAddMethod.invoke(parent.object,new Object[]{qName, element.object});
					} else 	if(parameterTypes.length==1
							&& parameterTypes[0].isAssignableFrom(elementType.castClass)
						){
							parentType.defaultAddMethod.invoke(parent.object,new Object[]{element.object});
					} else {
						throw new Exception(lineNumber + "," + columnNumber + ":"
								+ " element " + parent.qname
								+ " cannot have an attribute " + qName
								+ " of type " + elementType.castClass);						
					}
				} else {
					throw new Exception(lineNumber + "," + columnNumber + ":"
							+ " element " + parent.qname
							+ " cannot have an attribute " + qName
							+ " of type " + elementType.castClass);
				}
			}

		}

		// invoke the process method
		try {
			invokeProcess(element);
		} catch (Throwable e) {
			e.printStackTrace();
			throw new Exception(e);
		}

		trace("END/ (" + lineNumber + "," + columnNumber + "):" + uri + ":"
				+ qName);

	}

	public void setTrace(boolean trace) {
		this.traceFlag = trace;
	}

	private void trace(String msg) {
		if (traceFlag)
			System.err.println(msg);
	}

	/**
	 * @see kxml.sax.KXmlSAXHandler#setLineNumber(int)
	 */
	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	/**
	 * @see kxml.sax.KXmlSAXHandler#setColumnNumber(int)
	 */
	public void setColumnNumber(int columnNumber) {
		this.columnNumber = columnNumber;

	}

	/**
	 * @see kxml.sax.KXmlSAXHandler#processingInstruction(java.lang.String,
	 *      java.lang.String)
	 */

	public void processingInstruction(String target, String data)
			throws Exception {
		trace("PI:" + target + ";" + data);
		trace("ignore PI : "+data);
/*		// reuse the kXML parser methods to parser the PI data
		Reader reader = new StringReader(data);
		XmlParser parser = new XmlParser(reader);
		parser.parsePIData();

		target = parser.getTarget();
		Map attributes = parser.getAttributes();

		// get the class
		Class clazz = (Class) pis.get(target);
		if (clazz == null) {
			if (missingPIExceptionFlag)
				throw new Exception(lineNumber + "," + columnNumber + ":"
						+ "Unknown processing instruction");
			else {
				trace(lineNumber + "," + columnNumber + ":"
						+ "No class for PI " + target);
				return;
			}
		}

		// instanciate a object
		Object object;
		Constructor ctor = null;
		try {
			ctor = clazz.getConstructor(new Class[] { XmlCommonHandler.class });
		} catch (NoSuchMethodException e) {
			// do nothing
			trace("no constructor with XmlCommonHandler parameter");
		}
		try {
			if (ctor == null) {
				object = clazz.newInstance();
			} else {
				object = ctor.newInstance(new Object[] { this });
			}
		} catch (InstantiationException e) {
			throw new Exception(
					lineNumber
							+ ","
							+ columnNumber
							+ ":"
							+ "class "
							+ clazz.getName()
							+ " for PI "
							+ target
							+ " should have an empty constructor or a constructor with XmlCommonHandler parameter");
		} catch (IllegalAccessException e) {
			throw new Exception(lineNumber + "," + columnNumber + ":"
					+ "illegal access on the constructor " + clazz.getName()
					+ " for PI " + target);
		}

		// set the context
		setObjectContext(object);

		// TODO: set the parent

		// invoke setter
		Iterator iter = attributes.keySet().iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			String value = ReplaceUtility.replace((String) attributes.get(key),
					context);
			Method method = null;
			try {
				method = clazz.getDeclaredMethod(setterOf(key),
						new Class[] { String.class });
			} catch (NoSuchMethodException e) {
				// do nothing
			}
			if (method != null) {
				trace(method.getName());
				try {
					method.invoke(object, new String[] { value });
				} catch (InvocationTargetException e) {
					e.getTargetException().printStackTrace(System.err);
					throw e;
				}
			}

		}

		// invoke process
		try {
			invokeProcess(object);
		} catch (Throwable e) {
			e.printStackTrace();
			throw new Exception(e);
		}
*/	}

	public void processingInstructionForMapping(String target, String data)
			throws Exception {
		
		
		if (target == null) { // TODO kXML
			if (!data.startsWith(PI_MAPPING))
				return;
		} else if (!target.equals(PI_MAPPING))
			return;

		// defaultclass attribute
		String datt = "defaultclass=\"";
		int dstart = data.indexOf(datt);
		if (dstart != -1) {
			int dend = data.indexOf("\"", dstart + datt.length());
			if (dend == -1)
				throw new Exception(
						lineNumber
								+ ","
								+ columnNumber
								+ ":"
								+ " \"defaultclass\" attribute in \"mapping\" PI is not quoted");

			String classname = data.substring(dstart + datt.length(), dend);
			Class clazz = null;
			try {
				clazz = getClass().getClassLoader().loadClass(classname);
			} catch (ClassNotFoundException e) {
				throw new Exception(lineNumber + "," + columnNumber + ":"
						+ " cannot found class " + classname
						+ " for \"mapping\" PI");
			}

			//			 TODO Add method
			Method defaultdefaultAddMethod=null;
			setDefaultType(clazz, null,defaultdefaultAddMethod);
			return;
		}

		// element attribute
		String eatt = "element=\"";
		int estart = data.indexOf(eatt);
		if (estart == -1)
			throw new Exception(lineNumber + "," + columnNumber + ":"
					+ " missing \"element\" attribute in \"mapping\" PI");
		int eend = data.indexOf("\"", estart + eatt.length());
		if (eend == -1)
			throw new Exception(lineNumber + "," + columnNumber + ":"
					+ " \"element\" attribute in \"mapping\" PI is not quoted");

		String element = data.substring(estart + eatt.length(), eend);

		// element class
		String catt = "class=\"";
		int cstart = data.indexOf(catt);
		if (cstart == -1)
			throw new Exception(lineNumber + "," + columnNumber + ":"
					+ " missing \"class\" attribute in \"mapping\" PI");
		int cend = data.indexOf("\"", cstart + catt.length());
		if (cend == -1)
			throw new Exception(lineNumber + "," + columnNumber + ":"
					+ " \"class\" attribute in \"mapping\" PI is not quoted");

		String classname = data.substring(cstart + catt.length(), cend);

		// element cast (optional)
		String castname = null;
		String castatt = "cast=\"";
		int caststart = data.indexOf(castatt);
		if (caststart != -1) {
			int castend = data.indexOf("\"", cstart + castatt.length());
			if (castend == -1)
				throw new Exception(lineNumber + "," + columnNumber + ":"
						+ " \"cast\" attribute in \"mapping\" PI is not quoted");

			castname = data.substring(caststart + castatt.length(), castend);
		}

		Class clazz = null;
		try {
			clazz = getClass().getClassLoader().loadClass(classname);
		} catch (ClassNotFoundException e) {
			throw new Exception(lineNumber + "," + columnNumber + ":"
					+ " cannot found class " + classname
					+ " for \"mapping\" PI");
		}

		Class castClazz = null;
		if (castname != null)
			try {
				clazz = getClass().getClassLoader().loadClass(castname);
			} catch (ClassNotFoundException e) {
				throw new Exception(lineNumber + "," + columnNumber + ":"
						+ " cannot found cast class " + classname
						+ " for \"mapping\" PI");
			}

		// TODO Add method
		Method defaultAddMethod=null;
			
		addType(element, clazz, castClazz, defaultAddMethod);
	}
}