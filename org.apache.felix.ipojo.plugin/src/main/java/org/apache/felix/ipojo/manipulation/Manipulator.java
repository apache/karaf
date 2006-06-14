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
package org.apache.felix.ipojo.manipulation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Level;

import org.apache.felix.ipojo.plugin.IpojoPluginConfiguration;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * iPOJO Bytecode Manipulator.
 * @author Clement Escoffier
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
     * Manipulate the class.
     * @param bundle    The component's m_bundle
     * @param name      The name of the class
     * @param cm        The component manager attached to this manipulation
     * @return          A byte array.
     * @throws Exception : throwed if the manipulation failed.
     */
    public void preProcess(String name, File outputDirectory) throws Exception {

        // gets an input stream to read the bytecode of the class
        String path = outputDirectory+"/"+name.replace('.', '/') + ".class";
        File clazz = new File(path);
		URL url = clazz.toURI().toURL();

        //if (url == null) { throw new ClassNotFoundException(name); }
        IpojoPluginConfiguration.getLogger().log(Level.INFO, "Manipulate the class file : " + clazz.getAbsolutePath());

        InputStream is = url.openStream();

        //Manipulation  ->
        // Add the _setComponentManager method
        // Instrument all fields
        ClassReader cr0 = new ClassReader(is);
        ClassWriter cw0 = new ClassWriter(true);
        PreprocessClassAdapter preprocess = new PreprocessClassAdapter(cw0);
        cr0.accept(preprocess, false);
        is.close();

        File file = null;
        try {
                file = new File(url.getFile());

                
                //file.createNewFile();
                FileOutputStream fos = new FileOutputStream(file);
                
                fos.write(cw0.toByteArray());
                
                fos.close();
                IpojoPluginConfiguration.getLogger().log(Level.INFO, "Put the file " + file.getAbsolutePath() + " in the jar file");
            } catch (Exception e) { System.err.println("Problem to write the adapted class on the file system " + " [ "+ file.getAbsolutePath() +" ] " + e.getMessage()); }

        m_fields = preprocess.getFields();
        m_interfaces = new String[preprocess.getInterfaces().length];
        for (int i = 0; i < m_interfaces.length; i++) {
        	m_interfaces[i] = preprocess.getInterfaces()[i].replace('/', '.');
        }
    }


}
