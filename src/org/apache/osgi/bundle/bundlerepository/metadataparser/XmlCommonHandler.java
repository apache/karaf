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
package org.apache.osgi.bundle.bundlerepository.metadataparser;

import java.lang.reflect.Method;
import java.util.*;

import org.apache.osgi.bundle.bundlerepository.kxmlsax.KXmlSAXHandler;
import org.xml.sax.SAXException;

/**
 * SAX handler for the XML OBR file
 *
 * @author Didier Donsez (didier.donsez@imag.fr)
 */
public class XmlCommonHandler implements KXmlSAXHandler {

    private static final String PI_MAPPING="mapping";

    private int columnNumber;

    private int lineNumber;

    //
    // Data
    //

    private Object root;

    private Stack objectStack;
    private Stack qnameStack;

    private Map types;
    private Class defaultType;

    private StringBuffer currentText;

    public XmlCommonHandler() {
        objectStack = new Stack();
        qnameStack = new Stack();
        types = new HashMap();
    }

    public void addType(String qname, Class clazz) {
        types.put(qname, clazz);
    }

    public void setDefaultType(Class clazz) {
        defaultType=clazz;
    }

    public Object getRoot() {
        return root;
    }

    /* for PCDATA */
    public void characters(char[] ch, int offset, int length)
        throws Exception {
        if (currentText != null)
            currentText.append(ch, offset, length);
    }

    private String adderOf(Class clazz) {
        return "add"
            + ClassUtility.capitalize(ClassUtility.classOf(clazz.getName()));
    }

    private String adderOf(String key) {
        return "add" + ClassUtility.capitalize(key);
    }

    private String setterOf(Class clazz) {
        return "set"
            + ClassUtility.capitalize(ClassUtility.classOf(clazz.getName()));
    }

    private String setterOf(String key) {
        return "set" + ClassUtility.capitalize(key);
    }

    /**
    * Method called when a tag opens
    *
    * @param   uri
    * @param   localName
    * @param   qName
    * @param   attrib
    * @exception   SAXException
    **/
    public void startElement(
        String uri,
        String localName,
        String qName,
        Properties attrib)
        throws Exception {

        trace("START ("+lineNumber+","+columnNumber+"):" + uri + ":" + qName);

        Class clazz = (Class) types.get(qName);
        // TODO: should add uri in the future

        if(clazz==null && defaultType!=null)
            clazz=defaultType;

        Object obj;
        if (clazz != null) {

            try {
                obj = clazz.newInstance();
            } catch (InstantiationException e) {
                throw new Exception(lineNumber+","+columnNumber+":"+
                    "class "+clazz.getName()+" for element " + qName + " should have an empty constructor");
            } catch (IllegalAccessException e) {
                throw new Exception(lineNumber+","+columnNumber+":"+
                    "illegal access on the empty constructor of class "+clazz.getName()+" for element " + qName);
            }

            Set keyset = attrib.keySet();
            Iterator iter = keyset.iterator();
            while (iter.hasNext()) {
                String key = (String) iter.next();

                if (obj instanceof Map) {
                    ((Map) obj).put(key, attrib.get(key));
                } else if (obj instanceof List) {
                    throw new Exception(lineNumber+","+columnNumber+":"+
                        "List element " + qName + " cannot have any attribute");
                } else if (obj instanceof String) {
                    if(key.equals("value")){
                        obj=(String)attrib.get(key);
                    } else {
                        throw new Exception(lineNumber+","+columnNumber+":"+
                            "String element " + qName + " cannot have other attribute than value");
                    }
                } else {
                    Method method = null;
                    try {
                        method =
                            clazz.getMethod(
                                setterOf(key),
                                new Class[] { String.class });
                    } catch (NoSuchMethodException e) {
                        // do nothing
                    }
                    if (method == null)
                        try {
                            method =
                                clazz.getMethod(
                                    adderOf(key),
                                    new Class[] { String.class });

                        } catch (NoSuchMethodException e) {
                            throw new Exception(lineNumber+","+columnNumber+":"+
                                "element "
                                    + qName
                                    + " does not support the attribute "
                                    + key);
                        }
                    if (method != null)
                        method.invoke(
                            obj,
                            new String[] {(String) attrib.get(key)});
                }

            }

        } else {
            throw new Exception(lineNumber+","+columnNumber+":"+
                "this element " + qName + " has not corresponding class");
        }

        if (root == null)
            root = obj;
        objectStack.push(obj);
        qnameStack.push(qName);
        currentText = new StringBuffer();

        trace("START/ ("+lineNumber+","+columnNumber+"):" + uri + ":" + qName);
    }

    /**
    * Method called when a tag closes
    *
    * @param   uri
    * @param   localName
    * @param   qName
    * @exception   SAXException
    */
    public void endElement(
        java.lang.String uri,
        java.lang.String localName,
        java.lang.String qName)
        throws Exception {

        trace("END ("+lineNumber+","+columnNumber+"):" + uri + ":" + qName);

        Object obj = objectStack.pop();

        if (currentText != null && currentText.length() != 0) {
            if (obj instanceof Map) {
                ((Map) obj).put(qName, currentText.toString().trim());
            } else if (obj instanceof List) {
                throw new Exception(lineNumber+","+columnNumber+":"+
                    "List element " + qName + " cannot have PCDATAs");
            } else if (obj instanceof String) {
                String str=(String)obj;
                if(str.length()!=0){
                    throw new Exception(lineNumber+","+columnNumber+":"+
                        "String element " + qName + " cannot have both PCDATA and an attribute value");
                } else {
                    obj=currentText.toString().trim();
                }
            } else {
                Method method = null;
                try {
                    method =
                        obj.getClass().getMethod(
                            "addText",
                            new Class[] { String.class });
                } catch (NoSuchMethodException e) {
                    // do nothing
                }
                if (method != null) {
                    method.invoke(obj, new String[] { currentText.toString().trim()});
                }
            }
        }

        currentText = null;

        if (!objectStack.isEmpty()) {

            Object parent = objectStack.peek();
            String parentName = (String) qnameStack.peek();

            if (parent instanceof Map) {
                ((Map) parent).put(qName, obj);
            } else if (parent instanceof List) {
                ((List) parent).add(obj);
            } else {
                Method method = null;
                try {
                    method =
                        parent.getClass().getMethod(
                            adderOf(ClassUtility.capitalize(qName)),
                            new Class[] { obj.getClass()});
                } catch (NoSuchMethodException e) {
                    trace(
                        "NoSuchMethodException: "
                            + adderOf(ClassUtility.capitalize(qName)));
                    // do nothing
                }
                if (method == null)
                    try {
                        method =
                            parent.getClass().getMethod(
                                setterOf(ClassUtility.capitalize(qName)),
                                new Class[] { obj.getClass()});
                    } catch (NoSuchMethodException e) {
                        trace(
                            "NoSuchMethodException: "
                                + setterOf(ClassUtility.capitalize(qName)));
                        // do nothing
                    }
                if (method == null)
                    try {
                        method =
                            parent.getClass().getMethod(
                                adderOf(obj.getClass()),
                                new Class[] { obj.getClass()});
                    } catch (NoSuchMethodException e) {
                        trace(
                            "NoSuchMethodException: "
                                + adderOf(obj.getClass()));
                        // do nothing
                    }
                if (method == null)
                    try {
                        method =
                            parent.getClass().getMethod(
                                setterOf(obj.getClass()),
                                new Class[] { obj.getClass()});
                    } catch (NoSuchMethodException e) {
                        trace(
                            "NoSuchMethodException: "
                                + setterOf(obj.getClass()));
                        // do nothing
                    }

                if (method != null) {
                    trace(method.getName());
                    method.invoke(parent, new Object[] { obj });
                } else {
                    throw new Exception(lineNumber+","+columnNumber+":"+
                        " element " + parentName + " cannot have an attribute " + qName + " of type " + obj.getClass());
                }
            }

        }

        trace("END/ ("+lineNumber+","+columnNumber+"):" + uri + ":" + qName);

    }

    private void trace(String msg) {
        if (false)
            System.err.println(msg);
    }

    /**
     * @see kxml.sax.KXmlSAXHandler#setLineNumber(int)
     */
    public void setLineNumber(int lineNumber) {
        this.lineNumber=lineNumber;
    }

    /**
     * @see kxml.sax.KXmlSAXHandler#setColumnNumber(int)
     */
    public void setColumnNumber(int columnNumber) {
        this.columnNumber=columnNumber;

    }

    /**
     * @see kxml.sax.KXmlSAXHandler#processingInstruction(java.lang.String, java.lang.String)
     */
    public void processingInstruction(String target, String data) throws Exception {
        trace("pi:"+target+";"+data);
        if(target==null){ // TODO kXML
            if(!data.startsWith(PI_MAPPING)) return;
        } else if(!target.equals(PI_MAPPING))return;


        // defaultclass attribute
        String datt="defaultclass=\"";
        int dstart=data.indexOf(datt);
        if(dstart!=-1) {
            int dend=data.indexOf("\"",dstart+datt.length());
            if(dend==-1)
                throw new Exception(lineNumber+","+columnNumber+":"+
                    " \"defaultclass\" attribute in \"mapping\" PI is not quoted");

            String classname=data.substring(dstart+datt.length(),dend);
            Class clazz=null;
            try {
                clazz=getClass().getClassLoader().loadClass(classname);
            } catch (ClassNotFoundException e) {
                throw new Exception(lineNumber+","+columnNumber+":"+
                    " cannot found class "+ classname+" for \"mapping\" PI");
            }
            setDefaultType(clazz);
            return;
        }

        // element attribute
        String eatt="element=\"";
        int estart=data.indexOf(eatt);
        if(estart==-1)
            throw new Exception(lineNumber+","+columnNumber+":"+
                " missing \"element\" attribute in \"mapping\" PI");
        int eend=data.indexOf("\"",estart+eatt.length());
        if(eend==-1)
        throw new Exception(lineNumber+","+columnNumber+":"+
            " \"element\" attribute in \"mapping\" PI is not quoted");

        String element=data.substring(estart+eatt.length(),eend);

        // element class
        String catt="class=\"";
        int cstart=data.indexOf(catt);
        if(cstart==-1)
            throw new Exception(lineNumber+","+columnNumber+":"+
                " missing \"class\" attribute in \"mapping\" PI");
        int cend=data.indexOf("\"",cstart+catt.length());
        if(cend==-1)
        throw new Exception(lineNumber+","+columnNumber+":"+
            " \"class\" attribute in \"mapping\" PI is not quoted");

        String classname=data.substring(cstart+catt.length(),cend);

        Class clazz=null;
        try {
            clazz=getClass().getClassLoader().loadClass(classname);
        } catch (ClassNotFoundException e) {
            throw new Exception(lineNumber+","+columnNumber+":"+
                " cannot found class "+ classname+" for \"mapping\" PI");
        }
        addType(element,clazz);
    }
}
