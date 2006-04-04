/*
 *   Copyright 2006 The Apache Software Foundation
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
package org.apache.felix.bundlerepository.metadataparser;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import org.apache.felix.bundlerepository.metadataparser.kxmlsax.KXml2SAXHandler;
import org.xml.sax.SAXException;


/**
 * SAX handler for the XML file
 * 
 * @author Didier Donsez (didier.donsez@imag.fr)
 */
public class XmlCommonHandler implements KXml2SAXHandler {

	private static final String PI_MAPPING = "mapping";

	public static final String METADATAPARSER_PIS = "METADATAPARSER_PIS";

	public static final String METADATAPARSER_INSTANCEFACTORY = "METADATAPARSER_INSTANCEFACTORY";

	private int columnNumber;

	private int lineNumber;

	private boolean traceFlag = false;

	private static String VALUE = "value";

	//
	// Data
	//

	private Object root;

	private Stack objectStack;

	private Stack qnameStack;

	private Map pis;

	private boolean missingPIExceptionFlag;

	private Map instanceFactories;

	private Map instanceClasses;

	private Map castClasses;

	private Map context;

	private Object defaultInstanceFactory;

	private Class defaultInstanceClass;

	private Class defaultCastClass;

	private StringBuffer currentText;

	public XmlCommonHandler() {
		objectStack = new Stack();
		qnameStack = new Stack();
		pis = new HashMap();
		missingPIExceptionFlag = false;
		instanceFactories = new HashMap();
		instanceClasses = new HashMap();
		castClasses = new HashMap();
		context = new HashMap();
		context.put(METADATAPARSER_PIS, pis);
		context.put(METADATAPARSER_INSTANCEFACTORY, instanceFactories);
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

	private Class checkAndGetInstanceClass(String qname,
			Object instanceFactory, Class castClass) throws Exception {
		String typeMsg = (qname == null) ? " for default type "
				: (" for type " + qname);
		try {
			if (instanceFactory instanceof Class) {
				if (castClass == null) {
					castClass = (Class) instanceFactory;
				} else {
					if (!castClass.isAssignableFrom((Class) instanceFactory)) {
						throw new Exception(
								lineNumber
										+ ","
										+ columnNumber
										+ ":"
										+ "instanceFactory "
										+ instanceFactory.getClass().getName()
										+ typeMsg
										+ " could not instanciate objects assignable to "
										+ castClass.getName());
					}
				}
				return (Class) instanceFactory;
			} else {
				Method newInstanceMethod = instanceFactory.getClass()
						.getDeclaredMethod("newInstance", null);
				Class returnType = newInstanceMethod.getReturnType();
				if (castClass == null) {
					castClass = returnType;
				} else if (!castClass.isAssignableFrom(returnType)) {
					throw new Exception(lineNumber + "," + columnNumber + ":"
							+ "instanceFactory "
							+ instanceFactory.getClass().getName() + typeMsg
							+ " could not instanciate objects assignable to "
							+ castClass.getName());
				}
				return returnType;
			}
		} catch (NoSuchMethodException e) {
			throw new Exception(lineNumber + "," + columnNumber + ":"
					+ "instanceFactory " + instanceFactory.getClass().getName()
					+ " for type " + qname
					+ " should have a newInstance method");
		}
	}

	public void addType(String qname, Object instanceFactory, Class castClass)
			throws Exception {
		Class instanceClass = checkAndGetInstanceClass(qname, instanceFactory,
				castClass);
		instanceClasses.put(qname, instanceClass);
		instanceFactories.put(qname, instanceFactory);
		castClasses.put(qname, castClass != null ? castClass : instanceClass);
		trace("element "
				+ qname
				+ " : instanceFactory="
				+ (instanceFactory instanceof Class ? ((Class) instanceFactory)
						.getName() : instanceFactory.getClass().getName())
				+ " castClass="
				+ (castClass != null ? castClass : instanceClass).getName());
	}

	public void setDefaultType(Object instanceFactory, Class castClass)
			throws Exception {
		defaultInstanceClass = checkAndGetInstanceClass(null, instanceFactory,
				castClass);
		defaultInstanceFactory = instanceFactory;
		defaultCastClass = castClass != null ? castClass : defaultInstanceClass;
		trace("defaut type : instanceFactory="
				+ (instanceFactory instanceof Class ? ((Class) instanceFactory)
						.getName() : instanceFactory.getClass().getName())
				+ " castClass=" + defaultCastClass.getName());
	}

	public void setContext(Map context) {
		this.context = context;
	}

	public Map getContext() {
		return context;
	}

	public Object getRoot() {
		return root;
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
			method = object.getClass().getMethod("setContext",
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
			method = object.getClass().getMethod("process", null);
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
			method = object.getClass().getMethod("setParent",
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
		Object instanceFactory = instanceFactories.get(qName); // instanceFactory
		// can be a
		// java.lang.Class
		// or a object
		// with a
		// newInstance()
		// method
		Class castClass = null;
		Class instanceClass = null;

		if (instanceFactory == null) {
			if (defaultInstanceFactory != null) {
				instanceFactory = defaultInstanceFactory;
				castClass = defaultCastClass;
				instanceClass = defaultInstanceClass;
			}
		} else {
			castClass = (Class) castClasses.get(qName);
			instanceClass = (Class) instanceClasses.get(qName);
		}

		Object obj = null;
		if (instanceFactory != null) {
			Method newInstanceMethod = null;
			try {
				newInstanceMethod = instanceFactory.getClass().getMethod(
						"newInstance", null);
			} catch (NoSuchMethodException e) {
				// never catch since checked by addType and setDefaultType
				throw new Exception(lineNumber + "," + columnNumber + ":"
						+ "instanceFactory "
						+ instanceFactory.getClass().getName()
						+ " for element " + qName
						+ " should have a newInstance method");
			}

			try {
                newInstanceMethod.setAccessible(true);
				obj = newInstanceMethod.invoke(instanceFactory, null);
			} catch (Exception e) {
				// do nothing
			}

			// set parent
			if (!objectStack.isEmpty()) {
				Object parent = objectStack.peek();
				setObjectParent(obj, parent);
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
						// method = castClass.getMethod(setterOf(key),new
						// Class[] { String.class });
						method = instanceClass.getMethod(setterOf(key),
								new Class[] { String.class });
					} catch (NoSuchMethodException e) {
						// do nothing
					}
					if (method == null)
						try {
							method = instanceClass.getMethod(adderOf(key),
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
					// Secondly, test if object if a map, a dictionary, a
					// collection, or a string

					if (obj instanceof Map) {
						((Map) obj).put(key, value);
					} else if (obj instanceof Dictionary) {
						((Dictionary) obj).put(key, value);
					} else if (obj instanceof Collection) {
						throw new Exception(lineNumber + "," + columnNumber
								+ ":" + "List element " + qName
								+ " cannot have any attribute");
					} else if (obj instanceof String) {
						if (key.equals("value")) {
							obj = value;
						} else {
							throw new Exception(lineNumber + "," + columnNumber
									+ ":" + "String element " + qName
									+ " cannot have other attribute than value");
						}
					} else {
						throw new Exception(lineNumber + "," + columnNumber
								+ ":" + "class "
								+ instanceFactory.getClass().getName()
								+ " for element " + qName
								+ " does not support the attribute " + key
								+ "or List.add or Map.put");

					}
				}

			}

		} else {
			throw new Exception(lineNumber + "," + columnNumber + ":"
					+ "this element " + qName + " has not corresponding class");
		}

		if (root == null)
			root = obj;
		objectStack.push(obj);
		qnameStack.push(qName);
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

		Object obj = objectStack.pop();
		Class objClass = obj.getClass();

		if (currentText != null && currentText.length() != 0) {

			String currentStr = ReplaceUtility.replace(currentText.toString(),
					context).trim();
			// TODO: trim may be not the right choice
			trace("current text:" + currentStr);

			Method method = null;
			try {
				method = objClass.getMethod("addText",
						new Class[] { String.class });
			} catch (NoSuchMethodException e) {
				try {
					method = objClass.getMethod("setText",
							new Class[] { String.class });
				} catch (NoSuchMethodException e2) {
					// do nothing
				}
			}
			if (method != null) {
				trace(method.getName());
				try {
					method.invoke(obj, new String[] { currentStr });
				} catch (InvocationTargetException e) {
					e.getTargetException().printStackTrace(System.err);
					throw e;
				}
			} else {
				if (Map.class.isAssignableFrom(objClass)) {
					((Map) obj).put(qName, currentStr);
				} else if (Dictionary.class.isAssignableFrom(objClass)) {
					((Dictionary) obj).put(qName, currentStr);
				} else if (Collection.class.isAssignableFrom(objClass)) {
					throw new Exception(lineNumber + "," + columnNumber + ":"
							+ "List element " + qName + " cannot have PCDATAs");
				} else if (String.class.isAssignableFrom(objClass)) {
					String str = (String) obj;
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
						obj = currentStr;
					}
				}
			}

		}

		currentText = null;

		if (!objectStack.isEmpty()) {

			Object parent = objectStack.peek();
			String parentName = (String) qnameStack.peek();
			// TODO Class parentClass = (castClasses(parentName)).getClass() ????
			Class parentClass = parent.getClass();

			Method method = null;
			try {
				method = parentClass.getMethod(adderOf(ClassUtility
						.capitalize(qName)), new Class[] { objClass });  // instanceClass
			} catch (NoSuchMethodException e) {
				trace("NoSuchMethodException: "
						+ adderOf(ClassUtility.capitalize(qName)));
				// do nothing
			}
			if (method == null)
				try {
					method = parent.getClass().getMethod(
							setterOf(ClassUtility.capitalize(qName)),
							new Class[] { objClass });
				} catch (NoSuchMethodException e) {
					trace("NoSuchMethodException: "
							+ setterOf(ClassUtility.capitalize(qName)));
					// do nothing
				}
			if (method == null)
				try {
					method = parent.getClass().getMethod(adderOf(objClass),
							new Class[] { objClass });
				} catch (NoSuchMethodException e) {
					trace("NoSuchMethodException: " + adderOf(objClass));
					// do nothing
				}
			if (method == null)
				try {
					method = parent.getClass().getMethod(setterOf(objClass),
							new Class[] { objClass });
				} catch (NoSuchMethodException e) {
					trace("NoSuchMethodException: " + setterOf(objClass));
					// do nothing
				}

			if (method != null) {
				trace(method.getName());
				try {
					method.invoke(parent, new Object[] { obj });
				} catch (InvocationTargetException e) {
					e.getTargetException().printStackTrace(System.err);
					throw e;
				}
			} else {
				if (Map.class.isAssignableFrom(parentClass)) {
					((Map) parent).put(qName, obj);
				} else if (Dictionary.class.isAssignableFrom(parentClass)) {
					((Dictionary) parent).put(qName, obj);
				} else if (Collection.class.isAssignableFrom(parentClass)) {
					((Collection) parent).add(obj);
				} else {
					throw new Exception(lineNumber + "," + columnNumber + ":"
							+ " element " + parentName
							+ " cannot have an attribute " + qName
							+ " of type " + objClass);
				}
			}

		}

		// invoke the process method
		try {
			invokeProcess(obj);
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
				method = clazz.getMethod(setterOf(key),
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
			setDefaultType(clazz, null);
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

		addType(element, clazz, castClazz);
	}
}