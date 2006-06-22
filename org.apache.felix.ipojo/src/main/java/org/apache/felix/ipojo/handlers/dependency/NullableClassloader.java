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
package org.apache.felix.ipojo.handlers.dependency;

import java.io.IOException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Enumeration;

import org.osgi.framework.Bundle;

/**
 * iPOJO Classloader.
 * This classloadert is used to load manipulated class.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class NullableClassloader extends ClassLoader {

	 /**
     * The owner bundle.
     * m_bundle : Bundle
     */
    private Bundle  m_bundle;

    /**
     * Constructor.
     * @param b : the owner bundle
     */
    public NullableClassloader(Bundle b) {
        m_bundle = b;
    }

    /**
     * load the class.
     * @see java.lang.ClassLoader#loadClass(java.lang.String, boolean)
     * @param name : the name of the class
     * @param resolve : should be the class resolve now ?
     * @return : the loaded class
     * @throws ClassNotFoundException : the class to load is not found
     */
    protected synchronized Class loadClass(final String name,
            final boolean resolve) throws ClassNotFoundException {

        Class clazz = null;
        //Activator.getLogger().log(Level.WARNING, "Bundle " + m_bundle.getBundleId() + " -> Try to load : " + name);

        if (m_bundle != null) { clazz = m_bundle.loadClass(name); }

        return clazz;
    }


    /**
     * Return the URL of the asked ressource.
     * @param arg : the name of the resource to find.
     * @return the URL of the resource.
     * @see java.lang.ClassLoader#getResource(java.lang.String)
     */
    public URL getResource(String arg) {
        return m_bundle.getResource(arg);
    }

    /**
     * .
     * @param arg : resource to find
     * @return : the enumeration found
     * @throws IOException : if the lookup failed.
     * @see java.lang.ClassLoader#getResources(java.lang.String)
     */
    public Enumeration getRessources(String arg) throws IOException {
        return m_bundle.getResources(arg);
    }

    /**
     * The defineClass method for GenClassLoader.
     * @param name : name of the class
     * @param b : the byte array of the class
     * @param domain : the protection domain
     * @return : the defined class.
     * @throws Exception : if a problem is detected during the loading
     */
    public Class defineClass(String name, byte[] b,
            ProtectionDomain domain) throws Exception {
    	Class clazz =  super.defineClass(name, b, 0, b.length, domain);
    	return clazz;
    }


}
