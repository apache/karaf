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
package org.apache.felix.ipojo.manipulation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * iPOJO Byte code Manipulator.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 * 
 */
public class Manipulator {
    /**
     * Store the visited fields : [name of the field, type of the field].
     */
    private Map m_fields;

    /**
     * Store the interface implemented by the class.
     */
    private List m_interfaces;

    /**
     * Store the methods list.
     */
    private List m_methods;
    
    /**
     * Pojo super class.
     */
    private String m_superClass;

    /**
     * Manipulate the given byte array.
     * @param origin : original class.
     * @return the manipulated class.
     * @throws IOException : if an error occurs during the manipulation.
     */
    public byte[] manipulate(byte[] origin) throws IOException {
        InputStream is1 = new ByteArrayInputStream(origin);

        // First check if the class is already manipulated :
        ClassReader ckReader = new ClassReader(is1);
        ClassChecker ck = new ClassChecker();
        ckReader.accept(ck, ClassReader.SKIP_FRAMES);
        is1.close();

        m_fields = ck.getFields(); // Get visited fields (contains only POJO fields)

        // Get interfaces and super class.
        m_interfaces = ck.getInterfaces();
        m_superClass = ck.getSuperClass();

        // Get the methods list
        m_methods = ck.getMethods();

        ClassWriter finalWriter = null;
        if (!ck.isalreadyManipulated()) {
            // Manipulation ->
            // Add the _setComponentManager method
            // Instrument all fields
            InputStream is2 = new ByteArrayInputStream(origin);
            ClassReader cr0 = new ClassReader(is2);
            ClassWriter cw0 = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            //CheckClassAdapter ch = new CheckClassAdapter(cw0);
            MethodCreator preprocess = new MethodCreator(cw0, m_fields);
            cr0.accept(preprocess, ClassReader.SKIP_FRAMES);
            is2.close();
            finalWriter = cw0;
        }
        // The file is in the bundle
        if (ck.isalreadyManipulated()) {
            return new byte[0];
        } else {
            return finalWriter.toByteArray();
        }
    }

    /**
     * Compute component type manipulation metadata.
     * @return the manipulation metadata of the class.
     */
    public Element getManipulationMetadata() {
        Element elem = new Element("Manipulation", "");
        
        if (m_superClass != null) {
            elem.addAttribute(new Attribute("super", m_superClass));
        }
        
        for (int j = 0; j < m_interfaces.size(); j++) {
            Element itf = new Element("Interface", "");
            Attribute att = new Attribute("name", m_interfaces.get(j).toString());
            itf.addAttribute(att);
            elem.addElement(itf);
        }
        
        for (Iterator it = m_fields.keySet().iterator(); it.hasNext();) {
            Element field = new Element("Field", "");
            String name = (String) it.next();
            String type = (String) m_fields.get(name);
            Attribute attName = new Attribute("name", name);
            Attribute attType = new Attribute("type", type);
            field.addAttribute(attName);
            field.addAttribute(attType);
            elem.addElement(field);
        }

        for (int j = 0; j < m_methods.size(); j++) {
            MethodDescriptor method = (MethodDescriptor) m_methods.get(j);
            elem.addElement(method.getElement());
        }

        return elem;
    }

}
