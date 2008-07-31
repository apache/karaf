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
import org.apache.felix.bundlerepository.Logger;
import org.xml.sax.SAXException;

/**
 * SAX handler for the XML file
 */
public class XmlCommonHandler implements KXml2SAXHandler
{
    private static final String PI_MAPPING = "mapping";
    public static final String METADATAPARSER_PIS = "METADATAPARSER_PIS";
    public static final String METADATAPARSER_TYPES = "METADATAPARSER_TYPES";
    private int m_columnNumber;
    private int m_lineNumber;
    private boolean m_traceFlag = false;
    private static String VALUE = "value";

    //
    // Data
    //
    private XmlStackElement m_root;
    private Stack m_elementStack;
    private Map m_pis;
    private boolean m_missingPIExceptionFlag;
    private Map m_types;
    private TypeEntry m_defaultType;
    private StringBuffer m_currentText;
    private Map m_context;
    private final Logger m_logger;

    private class XmlStackElement
    {
        public final String m_qname;
        public Object m_object;

        public XmlStackElement(String qname, Object object)
        {
            super();
            m_qname = qname;
            m_object = object;
        }
    }

    public class TypeEntry
    {
        public final Object m_instanceFactory;
        public final Class m_instanceClass;
        public final Method m_newInstanceMethod;
        public final Class m_castClass;
        public final Method m_defaultAddMethod;

        public TypeEntry(Object instanceFactory, Class castClass, Method defaultAddMethod) throws Exception
        {
            super();
            m_instanceFactory = instanceFactory;

            try
            {
                if (instanceFactory instanceof Class)
                {
                    m_newInstanceMethod = instanceFactory.getClass().getDeclaredMethod("newInstance", null);
                    if (castClass == null)
                    {
                        m_castClass = (Class) instanceFactory;
                    }
                    else
                    {
                        if (!castClass.isAssignableFrom((Class) instanceFactory))
                        {
                            throw new Exception(
                                "instanceFactory " + instanceFactory.getClass().getName() + " could not instanciate objects assignable to " + castClass.getName());
                        }
                        m_castClass = castClass;
                    }
                    m_instanceClass = (Class) instanceFactory;
                }
                else
                {
                    m_newInstanceMethod = instanceFactory.getClass().getDeclaredMethod("newInstance", null);
                    Class returnType = m_newInstanceMethod.getReturnType();
                    if (castClass == null)
                    {
                        m_castClass = returnType;
                    }
                    else if (!castClass.isAssignableFrom(returnType))
                    {
                        throw new Exception(
                            "instanceFactory " + instanceFactory.getClass().getName() + " could not instanciate objects assignable to " + castClass.getName());
                    }
                    else
                    {
                        m_castClass = castClass;
                    }
                    m_instanceClass = returnType;
                }
            }
            catch (NoSuchMethodException e)
            {
                throw new Exception(
                    "instanceFactory " + instanceFactory.getClass().getName() + " should have a newInstance method");
            }

            // TODO check method
            m_defaultAddMethod = defaultAddMethod;
            if (m_defaultAddMethod != null)
            {
                m_defaultAddMethod.setAccessible(true);
            }
        }

        public String toString()
        {
            StringBuffer sb = new StringBuffer();
            sb.append("[");
            if (m_instanceFactory instanceof Class)
            {
                sb.append("instanceFactory=").append(((Class) m_instanceFactory).getName());
            }
            else
            {
                sb.append("instanceFactory=").append(m_instanceFactory.getClass().getName());
            }
            sb.append(",instanceClass=").append(m_instanceClass.getName());
            sb.append(",castClass=").append(m_castClass.getName());
            sb.append(",defaultAddMethod=");
            if (m_defaultAddMethod == null)
            {
                sb.append("");
            }
            else
            {
                sb.append(m_defaultAddMethod.getName());
            }
            sb.append("]");
            return sb.toString();
        }
    }

    public XmlCommonHandler(Logger logger)
    {
        m_logger = logger;
        m_elementStack = new Stack();
        m_pis = new HashMap();
        m_missingPIExceptionFlag = false;
        m_types = new HashMap();
        m_context = new HashMap();
        m_context.put(METADATAPARSER_PIS, m_pis);
        m_context.put(METADATAPARSER_TYPES, m_types);
    }

    public void addPI(String piname, Class clazz)
    {
        m_pis.put(piname, clazz);
    }

    /**
     * set the missing PI exception flag. If during parsing, the flag is true
     * and the processing instruction is unknown, then the parser throws a
     * exception
     * 
     * @param flag
     */
    public void setMissingPIExceptionFlag(boolean flag)
    {
        m_missingPIExceptionFlag = flag;
    }

    public void addType(String qname, Object instanceFactory, Class castClass, Method defaultAddMethod)
        throws Exception
    {

        TypeEntry typeEntry;
        try
        {
            typeEntry = new TypeEntry(
                instanceFactory,
                castClass,
                defaultAddMethod);
        }
        catch (Exception e)
        {
            throw new Exception(m_lineNumber + "," + m_columnNumber + ":" + qname + " : " + e.getMessage());
        }
        m_types.put(qname, typeEntry);
        trace("element " + qname + " : " + typeEntry.toString());
    }

    public void setDefaultType(Object instanceFactory, Class castClass, Method defaultAddMethod)
        throws Exception
    {
        TypeEntry typeEntry;
        try
        {
            typeEntry = new TypeEntry(
                instanceFactory,
                castClass,
                defaultAddMethod);
        }
        catch (Exception e)
        {
            throw new Exception(m_lineNumber + "," + m_columnNumber + ": default element : " + e.getMessage());
        }
        m_defaultType = typeEntry;
        trace("default element " + " : " + typeEntry.toString());
    }

    public void setContext(Map context)
    {
        m_context = context;
    }

    public Map getContext()
    {
        return m_context;
    }

    public Object getRoot()
    {
        return m_root.m_object;
    }

    /* for PCDATA */
    public void characters(char[] ch, int offset, int length) throws Exception
    {
        if (m_currentText != null)
        {
            m_currentText.append(ch, offset, length);
        }
    }

    private String adderOf(Class clazz)
    {
        return "add" + ClassUtility.capitalize(ClassUtility.classOf(clazz.getName()));
    }

    private String adderOf(String key)
    {
        return "add" + ClassUtility.capitalize(key);
    }

    private String setterOf(Class clazz)
    {
        return "set" + ClassUtility.capitalize(ClassUtility.classOf(clazz.getName()));
    }

    private String setterOf(String key)
    {
        return "set" + ClassUtility.capitalize(key);
    }

    /**
     * set the parser context in a object
     */
    private void setObjectContext(Object object)
        throws IllegalArgumentException, IllegalAccessException,
        InvocationTargetException
    {
        Method method = null;
        try
        {
            // TODO setContext from castClass or object.getClass() ?
            method = object.getClass().getDeclaredMethod("setContext",
                new Class[]
                {
                    Map.class
                });
        }
        catch (NoSuchMethodException e)
        {
            // do nothing
        }
        if (method != null)
        {
            trace(method.getName());
            try
            {
                method.invoke(object, new Object[]
                    {
                        m_context
                    });
            }
            catch (InvocationTargetException e)
            {
                m_logger.log(Logger.LOG_ERROR, "Error parsing repository metadata", e.getTargetException());
                throw e;
            }
        }
    }

    /**
     * set the parser context in a object
     * 
     * @throws Throwable
     */
    private void invokeProcess(Object object) throws Throwable
    {
        Method method = null;
        try
        {
            // TODO process from castClass or object.getClass() ?
            method = object.getClass().getDeclaredMethod("process", null);
        }
        catch (NoSuchMethodException e)
        {
            // do nothing
        }
        if (method != null)
        {
            trace(method.getName());
            try
            {
                method.invoke(object, null);
            }
            catch (InvocationTargetException e)
            {
                // m_logger.log(Logger.LOG_ERROR, "Error parsing repository metadata", e.getTargetException());
                throw e.getTargetException();
            }
        }
    }

    /**
     * set the parent in a object
     */
    private void setObjectParent(Object object, Object parent)
        throws InvocationTargetException, IllegalArgumentException,
        IllegalAccessException
    {
        Method method = null;
        try
        {
            // TODO setParent from castClass or object.getClass() ?
            method = object.getClass().getDeclaredMethod("setParent",
                new Class[]
                {
                    parent.getClass()
                });
        }
        catch (NoSuchMethodException e)
        {
            // do nothing
        }
        if (method != null)
        {
            trace(method.getName());
            try
            {
                method.invoke(object, new Object[]
                    {
                        parent
                    });
            }
            catch (InvocationTargetException e)
            {
                m_logger.log(Logger.LOG_ERROR, "Error parsing repository metadata", e.getTargetException());
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
        Properties attrib) throws Exception
    {

        trace("START (" + m_lineNumber + "," + m_columnNumber + "):" + uri + ":" + qName);

        // TODO: should add uri in the qname in the future
        TypeEntry type = (TypeEntry) m_types.get(qName);
        if (type == null)
        {
            type = m_defaultType;
        }

        Object obj = null;
        if (type != null)
        {

            try
            {
                // enables to access to "unmuttable" method
                type.m_newInstanceMethod.setAccessible(true);
                obj = type.m_newInstanceMethod.invoke(type.m_instanceFactory, null);
            }
            catch (InvocationTargetException e)
            {
                m_logger.log(Logger.LOG_ERROR, "Error parsing repository metadata", e.getTargetException());
            }

            // set parent
            if (!m_elementStack.isEmpty())
            {
                XmlStackElement parent = (XmlStackElement) m_elementStack.peek();
                setObjectParent(obj, parent.m_object);
            }

            // set the parser context
            setObjectContext(obj);

            // set the attributes
            Set keyset = attrib.keySet();
            Iterator iter = keyset.iterator();
            while (iter.hasNext())
            {
                String key = (String) iter.next();

                // substitute ${property} sbustrings by context' properties
                // values
                String value = ReplaceUtility.replace((String) attrib.get(key),
                    m_context);

                // Firstly, test if the getter or the adder exists

                Method method = null;
                if (!(obj instanceof String))
                {
                    try
                    {
                        // method = castClass.getDeclaredMethod(setterOf(key),new
                        // Class[] { String.class });
                        method = type.m_instanceClass.getDeclaredMethod(setterOf(key),
                            new Class[]
                            {
                                String.class
                            });
                    }
                    catch (NoSuchMethodException e)
                    {
                        // do nothing
                    }
                    if (method == null)
                    {
                        try
                        {
                            method = type.m_instanceClass.getDeclaredMethod(adderOf(key),
                                new Class[]
                                {
                                    String.class
                                });

                        }
                        catch (NoSuchMethodException e)
                        {
                            /*
                             * throw new Exception(lineNumber + "," +
                             * columnNumber + ":" + "element " + qName + " does
                             * not support the attribute " + key);
                             */
                        }
                    }
                }

                if (method != null)
                {
                    trace(method.getName());
                    try
                    {
                        method.invoke(obj, new String[]
                            {
                                value
                            });
                    }
                    catch (InvocationTargetException e)
                    {
                        m_logger.log(Logger.LOG_ERROR, "Error parsing repository metadata", e.getTargetException());
                        throw e;
                    }
                }
                else
                {

                    if (obj instanceof String)
                    {
                        if (key.equals(VALUE))
                        {
                            obj = value;
                        }
                        else
                        {
                            throw new Exception(m_lineNumber + "," + m_columnNumber + ":" + "String element " + qName + " cannot have other attribute than value");
                        }
                    }
                    else
                    {
                        if (type.m_defaultAddMethod != null)
                        {
                            Class[] parameterTypes = type.m_defaultAddMethod.getParameterTypes();
                            if (parameterTypes.length == 2 && parameterTypes[0].isAssignableFrom(String.class) && parameterTypes[1].isAssignableFrom(String.class))
                            {
                                type.m_defaultAddMethod.invoke(obj, new String[]
                                    {
                                        key, value
                                    });
                            }
                            else if (parameterTypes.length == 1 && parameterTypes[0].isAssignableFrom(String.class))
                            {
                                type.m_defaultAddMethod.invoke(obj, new String[]
                                    {
                                        value
                                    });
                            }
                            else
                            {
                                throw new Exception(m_lineNumber + "," + m_columnNumber + ":" + "class " + type.m_instanceFactory.getClass().getName() + " for element " + qName + " does not support the attribute " + key);
                            }
                        }
                        else
                        {
                            throw new Exception(m_lineNumber + "," + m_columnNumber + ":" + "class " + type.m_instanceFactory.getClass().getName() + " for element " + qName + " does not support the attribute " + key);
                        }
                    }
                }
            }
        }
        else
        {
            throw new Exception(m_lineNumber + "," + m_columnNumber + ":" + "this element " + qName + " has not corresponding class");
        }
        XmlStackElement element = new XmlStackElement(qName, obj);
        if (m_root == null)
        {
            m_root = element;
        }
        m_elementStack.push(element);
        m_currentText = new StringBuffer();

        trace("START/ (" + m_lineNumber + "," + m_columnNumber + "):" + uri + ":" + qName);
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
        java.lang.String qName) throws Exception
    {

        trace("END (" + m_lineNumber + "," + m_columnNumber + "):" + uri + ":" + qName);

        XmlStackElement element = (XmlStackElement) m_elementStack.pop();
        TypeEntry elementType = (TypeEntry) m_types.get(element.m_qname);
        if (elementType == null)
        {
            elementType = m_defaultType;
        }

        if (m_currentText != null && m_currentText.length() != 0)
        {

            String currentStr = ReplaceUtility.replace(m_currentText.toString(),
                m_context).trim();
            // TODO: trim may be not the right choice
            trace("current text:" + currentStr);

            Method method = null;
            try
            {
                method = elementType.m_castClass.getDeclaredMethod("addText",
                    new Class[]
                    {
                        String.class
                    });
            }
            catch (NoSuchMethodException e)
            {
                try
                {
                    method = elementType.m_castClass.getDeclaredMethod("setText",
                        new Class[]
                        {
                            String.class
                        });
                }
                catch (NoSuchMethodException e2)
                {
                    // do nothing
                }
            }
            if (method != null)
            {
                trace(method.getName());
                try
                {
                    method.invoke(element.m_object, new String[]
                        {
                            currentStr
                        });
                }
                catch (InvocationTargetException e)
                {
                    m_logger.log(Logger.LOG_ERROR, "Error parsing repository metadata", e.getTargetException());
                    throw e;
                }
            }
            else
            {
                if (String.class.isAssignableFrom(elementType.m_castClass))
                {
                    String str = (String) element.m_object;
                    if (str.length() != 0)
                    {
                        throw new Exception(
                            m_lineNumber + "," + m_columnNumber + ":" + "String element " + qName + " cannot have both PCDATA and an attribute value");
                    }
                    else
                    {
                        element.m_object = currentStr;
                    }
                }
            }
        }

        m_currentText = null;

        if (!m_elementStack.isEmpty())
        {

            XmlStackElement parent = (XmlStackElement) m_elementStack.peek();
            TypeEntry parentType = (TypeEntry) m_types.get(parent.m_qname);
            if (parentType == null)
            {
                parentType = m_defaultType;
            }

            String capqName = ClassUtility.capitalize(qName);
            Method method = null;
            try
            {
// TODO: OBR PARSER: We should also check for instance class as a parameter.
                method = parentType.m_instanceClass.getDeclaredMethod(
                    adderOf(capqName),
                    new Class[]
                    {
                        elementType.m_castClass
                    
                    });  // instanceClass
            }
            catch (NoSuchMethodException e)
            {
                trace("NoSuchMethodException: " + adderOf(capqName) + "(" + elementType.m_castClass.getName() + ")");
            // do nothing
            }
            if (method == null)
            {
                try
                {
                    method = parentType.m_instanceClass.getDeclaredMethod(
                        setterOf(capqName),
                        new Class[]
                        {
                            elementType.m_castClass
                        
                        });
                }
                catch (NoSuchMethodException e)
                {
                    trace("NoSuchMethodException: " + setterOf(capqName) + "(" + elementType.m_castClass.getName() + ")");
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
            }
            if (method != null)
            {
                trace(method.getName());
                try
                {
                    method.setAccessible(true);
                    method.invoke(parent.m_object, new Object[]
                        {
                            element.m_object
                        
                        });
                }
                catch (InvocationTargetException e)
                {
                    m_logger.log(Logger.LOG_ERROR, "Error parsing repository metadata", e.getTargetException());
                    throw e;
                }
            }
            else
            {
                if (parentType.m_defaultAddMethod != null)
                {
                    Class[] parameterTypes = parentType.m_defaultAddMethod.getParameterTypes();
                    if (parameterTypes.length == 2 && parameterTypes[0].isAssignableFrom(String.class) && parameterTypes[1].isAssignableFrom(elementType.m_castClass))
                    {
                        parentType.m_defaultAddMethod.invoke(parent.m_object, new Object[]
                            {
                                qName, element.m_object
                            
                            });
                    }
                    else if (parameterTypes.length == 1 && parameterTypes[0].isAssignableFrom(elementType.m_castClass))
                    {
                        parentType.m_defaultAddMethod.invoke(parent.m_object, new Object[]
                            {
                                element.m_object
                            
                            });
                    }
                    else
                    {
                        throw new Exception(m_lineNumber + "," + m_columnNumber + ":" + " element " + parent.m_qname + " cannot have an attribute " + qName + " of type " + elementType.m_castClass);
                    }
                }
                else
                {
                    throw new Exception(m_lineNumber + "," + m_columnNumber + ":" + " element " + parent.m_qname + " cannot have an attribute " + qName + " of type " + elementType.m_castClass);
                }
            }

        }

        // invoke the process method
        try
        {
            invokeProcess(element);
        }
        catch (Throwable e)
        {
            m_logger.log(Logger.LOG_ERROR, "Error parsing repository metadata", e);
            throw new Exception(e);
        }

        trace("END/ (" + m_lineNumber + "," + m_columnNumber + "):" + uri + ":" + qName);
    }

    public void setTrace(boolean trace)
    {
        m_traceFlag = trace;
    }

    private void trace(String msg)
    {
        if (m_traceFlag)
        {
            m_logger.log(Logger.LOG_DEBUG, msg);
        }
    }

    /**
     * @see kxml.sax.KXmlSAXHandler#setLineNumber(int)
     */
    public void setLineNumber(int lineNumber)
    {
        m_lineNumber = lineNumber;
    }

    /**
     * @see kxml.sax.KXmlSAXHandler#setColumnNumber(int)
     */
    public void setColumnNumber(int columnNumber)
    {
        m_columnNumber = columnNumber;
    }

    /**
     * @see kxml.sax.KXmlSAXHandler#processingInstruction(java.lang.String,
     *      java.lang.String)
     */
    public void processingInstruction(String target, String data)
        throws Exception
    {
        trace("PI:" + target + ";" + data);
        trace("ignore PI : " + data);
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
    m_logger.log(Logger.LOG_ERROR, "Error parsing repository metadata", e.getTargetException());
    throw e;
    }
    }
    
    }
    
    // invoke process
    try {
    invokeProcess(object);
    } catch (Throwable e) {
    m_logger.log(Logger.LOG_ERROR, "Error parsing repository metadata", e);
    throw new Exception(e);
    }
     */    }

    public void processingInstructionForMapping(String target, String data)
        throws Exception
    {
        if (target == null)
        { // TODO kXML
            if (!data.startsWith(PI_MAPPING))
            {
                return;
            }
        }
        else if (!target.equals(PI_MAPPING))
        {
            return;        // defaultclass attribute
        }
        String datt = "defaultclass=\"";
        int dstart = data.indexOf(datt);
        if (dstart != -1)
        {
            int dend = data.indexOf("\"", dstart + datt.length());
            if (dend == -1)
            {
                throw new Exception(
                    m_lineNumber + "," + m_columnNumber + ":" + " \"defaultclass\" attribute in \"mapping\" PI is not quoted");
            }
            String classname = data.substring(dstart + datt.length(), dend);
            Class clazz = null;
            try
            {
                clazz = getClass().getClassLoader().loadClass(classname);
            }
            catch (ClassNotFoundException e)
            {
                throw new Exception(m_lineNumber + "," + m_columnNumber + ":" + " cannot found class " + classname + " for \"mapping\" PI");
            }

            //			 TODO Add method
            Method defaultdefaultAddMethod = null;
            setDefaultType(clazz, null, defaultdefaultAddMethod);
            return;
        }

        // element attribute
        String eatt = "element=\"";
        int estart = data.indexOf(eatt);
        if (estart == -1)
        {
            throw new Exception(m_lineNumber + "," + m_columnNumber + ":" + " missing \"element\" attribute in \"mapping\" PI");
        }
        int eend = data.indexOf("\"", estart + eatt.length());
        if (eend == -1)
        {
            throw new Exception(m_lineNumber + "," + m_columnNumber + ":" + " \"element\" attribute in \"mapping\" PI is not quoted");
        }
        String element = data.substring(estart + eatt.length(), eend);

        // element class
        String catt = "class=\"";
        int cstart = data.indexOf(catt);
        if (cstart == -1)
        {
            throw new Exception(m_lineNumber + "," + m_columnNumber + ":" + " missing \"class\" attribute in \"mapping\" PI");
        }
        int cend = data.indexOf("\"", cstart + catt.length());
        if (cend == -1)
        {
            throw new Exception(m_lineNumber + "," + m_columnNumber + ":" + " \"class\" attribute in \"mapping\" PI is not quoted");
        }
        String classname = data.substring(cstart + catt.length(), cend);

        // element cast (optional)
        String castname = null;
        String castatt = "cast=\"";
        int caststart = data.indexOf(castatt);
        if (caststart != -1)
        {
            int castend = data.indexOf("\"", cstart + castatt.length());
            if (castend == -1)
            {
                throw new Exception(m_lineNumber + "," + m_columnNumber + ":" + " \"cast\" attribute in \"mapping\" PI is not quoted");
            }
            castname = data.substring(caststart + castatt.length(), castend);
        }

        Class clazz = null;
        try
        {
            clazz = getClass().getClassLoader().loadClass(classname);
        }
        catch (ClassNotFoundException e)
        {
            throw new Exception(m_lineNumber + "," + m_columnNumber + ":" + " cannot found class " + classname + " for \"mapping\" PI");
        }

        Class castClazz = null;
        if (castname != null)
        {
            try
            {
                clazz = getClass().getClassLoader().loadClass(castname);
            }
            catch (ClassNotFoundException e)
            {
                throw new Exception(m_lineNumber + "," + m_columnNumber + ":" + " cannot found cast class " + classname + " for \"mapping\" PI");
            }        // TODO Add method
        }
        Method defaultAddMethod = null;

        addType(element, clazz, castClazz, defaultAddMethod);
    }
}