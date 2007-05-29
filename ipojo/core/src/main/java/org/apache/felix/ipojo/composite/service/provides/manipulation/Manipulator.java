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
package org.apache.felix.ipojo.composite.service.provides.manipulation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * iPOJO Bytecode Manipulator.
 * 
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 * 
 */
public class Manipulator {
    /**
     * Store the visited fields : [name fo the field, type of the field].
     */
    private HashMap m_fields = new HashMap();

    /**
     * Store the interface implemented by the class.
     */
    private String[] m_interfaces = new String[0];

    /**
     * Return the hashmap [name of the field, type of the field]. This
     * information is found when the class is manipulated It is a clone of the
     * original hashmap to avoid modification by handlers
     * 
     * @return the hashmap [name of the field, type of the field].
     */
    public HashMap getFields() {
        return m_fields;
    }

    /**
     * Return the hashmap [name of the field, type of the field]. This
     * information is found when the class is manipulated It is a clone of the
     * original hashmap to avoid modification by handlers
     * 
     * @return the hashmap [name of the field, type of the field].
     */
    public String[] getInterfaces() {
        return m_interfaces;
    }

    /**
     * Manipulate the given byte array.
     * 
     * @param origin : original class.
     * @return the manipulated class.
     * @throws IOException : if an error occurs during the manipulation.
     */
    public byte[] process(byte[] origin) throws IOException {
        InputStream is1 = new ByteArrayInputStream(origin);

        // First check if the class is already manipulated :
        ClassReader ckReader = new ClassReader(is1);
        ClassChecker ck = new ClassChecker();
        ckReader.accept(ck, ClassReader.SKIP_FRAMES);
        is1.close();

        m_fields = ck.getFields();

        // Get interface and remove POJO interface is presents
        String[] its = ck.getInterfaces();
        ArrayList l = new ArrayList();
        for (int i = 0; i < its.length; i++) {
            l.add(its[i]);
        }
        l.remove("org/apache/felix/ipojo/Pojo");

        m_interfaces = new String[l.size()];
        for (int i = 0; i < m_interfaces.length; i++) {
            m_interfaces[i] = ((String) l.get(i)).replace('/', '.');
        }

        ClassWriter finalWriter = null;
        if (!ck.isalreadyManipulated()) {
            // Manipulation ->
            // Add the _setComponentManager method
            // Instrument all fields
            InputStream is2 = new ByteArrayInputStream(origin);
            ClassReader cr0 = new ClassReader(is2);
            ClassWriter cw0 = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            PreprocessClassAdapter preprocess = new PreprocessClassAdapter(cw0);
            cr0.accept(preprocess, ClassReader.SKIP_FRAMES);
            is2.close();
            finalWriter = cw0;
        }
        // The file is in the bundle
        return finalWriter.toByteArray();
    }

}
