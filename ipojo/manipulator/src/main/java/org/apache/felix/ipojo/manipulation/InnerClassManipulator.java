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
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * Manipulates inner class allowing outer class access. The manipulated class
 * has access to managed field of the outer class.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class InnerClassManipulator {

    /**
     * Component implementation class name.
     */
    private String m_outer;
    
    /**
     * Component class fields.
     */
    private Set m_fields;

    /**
     * Creates an inner class manipulator.
     * @param classname : class name
     * @param fields : fields
     */
    public InnerClassManipulator(String classname, Set fields) {
        m_outer = classname;
        m_fields = fields;
    }

    /**
     * Manipulate the inner class.
     * @param in input (i.e. original) class
     * @return manipulated class
     * @throws IOException the class cannot be read correctly
     */
    public byte[] manipulate(byte[] in) throws IOException {
        InputStream is1 = new ByteArrayInputStream(in);

        ClassReader cr = new ClassReader(is1);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        InnerClassAdapter adapter = new InnerClassAdapter(cw, m_outer, m_fields);
        cr.accept(adapter, ClassReader.SKIP_FRAMES);
        is1.close();

        return cw.toByteArray();
    }

}
