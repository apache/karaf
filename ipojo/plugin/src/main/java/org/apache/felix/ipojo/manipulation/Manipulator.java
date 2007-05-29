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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.apache.felix.ipojo.plugin.IPojoPluginConfiguration;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * iPOJO Bytecode Manipulator.
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
	 * Store the methods list.
	 */
	private List m_methods = new ArrayList();

	/**
	 * Return the hashmap [name of the field, type of the field].
	 * This information is found when the class is manipulated
	 * It is a clone of the original hashmap to avoid modification by handlers
	 * @return the hashmap [name of the field, type of the field].
	 */
	public HashMap getFields() {
		return (HashMap)m_fields.clone();
	}

	/**
	 * Return the hashmap [name of the field, type of the field].
	 * This information is found when the class is manipulated
	 * It is a clone of the original hashmap to avoid modification by handlers
	 * @return the hashmap [name of the field, type of the field].
	 */
	public String[] getInterfaces() { return (String[])m_interfaces.clone(); }
	
	/**
	 * @return the method list.
	 */
	public List getMethods() {
	    return m_methods; 
    }

	/**
     * Manipulate the class.
     * @param name      The name of the class
     * @param cm        The component manager attached to this manipulation
     * @throws Exception : throwed if the manipulation failed.
     */
    public boolean preProcess(String name, File outputDirectory) throws Exception {
        
        // Init field, itfs and methods
        m_fields = new HashMap();
        m_interfaces = new String[0];
        m_methods = new ArrayList();

        // gets an input stream to read the bytecode of the class
        String path = outputDirectory+"/"+name.replace('.', '/') + ".class";
        File clazz = new File(path);
        
        if(!clazz.exists()) { return false; }
        
		URL url = clazz.toURI().toURL();

        //if (url == null) { throw new ClassNotFoundException(name); }
        IPojoPluginConfiguration.getLogger().log(Level.INFO, "Manipulate the class file : " + clazz.getAbsolutePath());

        InputStream is1 = url.openStream();
        
        // First check if the class is already manipulated : 
        ClassReader ckReader = new ClassReader(is1);
        ClassChecker ck = new ClassChecker();
        ckReader.accept(ck, ckReader.SKIP_FRAMES);
        is1.close();
        
        m_fields = ck.getFields();
        
        // Get interface and remove POJO interface is presents
        String[] its = ck.getInterfaces();
        ArrayList l = new ArrayList();
        for(int i = 0; i < its.length; i++) {
        	l.add(its[i]);
        }
        l.remove("org/apache/felix/ipojo/Pojo");
        
        m_interfaces = new String[l.size()];
        for (int i = 0; i < m_interfaces.length; i++) {
        	m_interfaces[i] = ((String) l.get(i)).replace('/', '.');
        }
        
        
        // Get the method list
        // Remove iPOJO methods
        for(int i = 0; i < ck.getMethods().size(); i++) {
        	MethodDescriptor method = (MethodDescriptor) ck.getMethods().get(i);
        	if(!(method.getName().startsWith("_get") ||  //Avoid getter method 
        			method.getName().startsWith("_set") || // Avoid setter method
        			method.getName().equals("_setComponentManager") || // Avoid the set method
        			method.getName().equals("getComponentInstance"))){ // Avoid the getComponentInstance method
        		m_methods.add(method);
        	}
        }
        
        if(!ck.isalreadyManipulated()) {

        	//Manipulation  ->
        	// Add the _setComponentManager method
        	// Instrument all fields
        	InputStream is2 = url.openStream();
        	ClassReader cr0 = new ClassReader(is2);
        	ClassWriter cw0 = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        	PreprocessClassAdapter preprocess = new PreprocessClassAdapter(cw0);
        	cr0.accept(preprocess, ClassReader.SKIP_FRAMES);
        	is2.close();

        	try {
                FileOutputStream fos = new FileOutputStream(clazz);
                
                fos.write(cw0.toByteArray());
                
                fos.close();
                IPojoPluginConfiguration.getLogger().log(Level.INFO, "Put the file " + clazz.getAbsolutePath() + " in the jar file");
            } catch (Exception e) { System.err.println("Problem to write the adapted class on the file system " + " [ "+ clazz.getAbsolutePath() +" ] " + e.getMessage()); }
        }
        // The file is in the bundle
        return true;
    }


}
