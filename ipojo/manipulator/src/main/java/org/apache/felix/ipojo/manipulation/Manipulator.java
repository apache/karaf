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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
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
    private Map m_fields = new HashMap();

    /**
     * Store the interface implemented by the class.
     */
    private String[] m_interfaces = new String[0];

    /**
     * Store the methods list.
     */
    private List m_methods = new ArrayList();

    /**
     * Manipulate the class.
     * 
     * @param name : The name of the class
     * @param outputDirectory : output directory where the class if stored.
     * @return true if the class is correctly manipulated.
     * @throws Exception : occurs if the manipulation failed.
     */
    public boolean manipulate(String name, File outputDirectory) throws Exception {

        // Initialize fields, interfaces and methods
        m_fields = new HashMap();
        m_interfaces = new String[0];
        m_methods = new ArrayList();

        // gets an input stream to read the byte code of the class
        String path = outputDirectory + "/" + name.replace('.', '/') + ".class";
        File clazz = new File(path);

        if (!clazz.exists()) {
            return false;
        }

        URL url = clazz.toURL();

        // if (url == null) { throw new ClassNotFoundException(name); }
        ManipulationProperty.getLogger().log(ManipulationProperty.INFO, "Manipulate the class file : " + clazz.getAbsolutePath());

        InputStream is1 = url.openStream();

        // First check if the class is already manipulated :
        ClassReader ckReader = new ClassReader(is1);
        ClassChecker ck = new ClassChecker();
        ckReader.accept(ck, ClassReader.SKIP_FRAMES);
        is1.close();

        m_fields = ck.getFields();

        // Get interface and remove POJO interface is presents
        String[] its = ck.getInterfaces();
        List l = new ArrayList();
        for (int i = 0; i < its.length; i++) {
            l.add(its[i]);
        }
        l.remove("org/apache/felix/ipojo/Pojo");

        m_interfaces = new String[l.size()];
        for (int i = 0; i < m_interfaces.length; i++) {
            m_interfaces[i] = ((String) l.get(i)).replace('/', '.');
        }

        // Get the method list
        // Remove iPOJO methods
        for (int i = 0; i < ck.getMethods().size(); i++) {
            MethodDescriptor method = (MethodDescriptor) ck.getMethods().get(i);
            if (!(method.getName().startsWith("_get") || // Avoid getter method
                    method.getName().startsWith("_set") || // Avoid setter method
                    method.getName().equals("_setComponentManager") || // Avoid the set method
                    method.getName().equals("getComponentInstance"))) { // Avoid the getComponentInstance method
                System.err.println(" Add the method : " + method);
                m_methods.add(method);
            }
        }

        if (!ck.isalreadyManipulated()) {

            // Manipulation ->
            // Add the _setComponentManager method
            // Instrument all fields
            InputStream is2 = url.openStream();
            ClassReader cr0 = new ClassReader(is2);
            ClassWriter cw0 = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            PojoAdapter preprocess = new PojoAdapter(cw0);
            cr0.accept(preprocess, ClassReader.SKIP_FRAMES);
            is2.close();

            try {
                FileOutputStream fos = new FileOutputStream(clazz);

                fos.write(cw0.toByteArray());

                fos.close();
                ManipulationProperty.getLogger().log(ManipulationProperty.INFO, "Put the file " + clazz.getAbsolutePath() + " in the jar file");
            } catch (Exception e) {
                System.err.println("Problem to write the adapted class on the file system " + " [ " + clazz.getAbsolutePath() + " ] " + e.getMessage());
                e.printStackTrace();
            }
        }
        // The file is in the bundle
        return true;
    }

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

        m_fields = ck.getFields();

        // Get interface and remove POJO interface is presents
        String[] its = ck.getInterfaces();
        List l = new ArrayList();
        for (int i = 0; i < its.length; i++) {
            l.add(its[i]);
        }
        l.remove("org/apache/felix/ipojo/Pojo");

        m_interfaces = new String[l.size()];
        for (int i = 0; i < m_interfaces.length; i++) {
            m_interfaces[i] = ((String) l.get(i)).replace('/', '.');
        }

        for (int i = 0; i < ck.getMethods().size(); i++) {
            MethodDescriptor method = (MethodDescriptor) ck.getMethods().get(i);
            if (!(method.getName().startsWith("_get") || // Avoid getter method
                    method.getName().startsWith("_set") || // Avoid setter method
                    method.getName().equals("_setComponentManager") || // Avoid the set method
                    method.getName().equals("getComponentInstance"))) { // Avoid the getComponentInstance method
                m_methods.add(method);
            }
        }

        ClassWriter finalWriter = null;
        if (!ck.isalreadyManipulated()) {
            // Manipulation ->
            // Add the _setComponentManager method
            // Instrument all fields
            InputStream is2 = new ByteArrayInputStream(origin);
            ClassReader cr0 = new ClassReader(is2);
            ClassWriter cw0 = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            PojoAdapter preprocess = new PojoAdapter(cw0);
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
        for (int j = 0; j < m_interfaces.length; j++) {
            Element itf = new Element("Interface", "");
            Attribute att = new Attribute("name", m_interfaces[j]);
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
