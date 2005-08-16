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
package org.apache.osgi.moduleloader;

import java.net.URL;

/**
 * <p>
 * This class implements a simple <tt>URLPolicy</tt> that the <tt>ModuleManager</tt>
 * uses if the application does not specify one. This implementation always returns
 * <tt>null</tt> for <tt>CodeSource</tt> <tt>URL</tt>s, which means that security
 * is simply ignored. For resource <tt>URL</tt>s, it returns an <tt>URL</tt> in the
 * form of:
 * </p>
 * <pre>
 *     module://&lt;module-id&gt;/&lt;resource-path&gt;
 * </pre>
 * <p>
 * In order to properly handle the "<tt>module:</tt>" protocol, this policy
 * also defines a custom <tt>java.net.URLStreamHandler</tt> that it assigns
 * to each <tt>URL</tt> as it is created. This custom handler is used to
 * return a custom <tt>java.net.URLConnection</tt> that will correctly parse
 * the above <tt>URL</tt> and retrieve the associated resource bytes using
 * methods from <tt>ModuleManager</tt> and <tt>Module</tt>.
 * </p>
 * @see org.apache.osgi.moduleloader.ModuleManager
 * @see org.apache.osgi.moduleloader.Module
 * @see org.apache.osgi.moduleloader.URLPolicy
**/
public class DefaultURLPolicy implements URLPolicy
{
    private ModuleURLStreamHandler m_handler = null;

    /**
     * <p>
     * This method is a stub and always returns <tt>null</tt>.
     * </p>
     * @param mgr the <tt>ModuleManager</tt> of the module.
     * @param module the module for which the <tt>URL</tt> is to be created.
     * @return <tt>null</tt>.
    **/
    public URL createCodeSourceURL(ModuleManager mgr, Module module)
    {
        return null;
    }

    /**
     * <p>
     * This method returns a <tt>URL</tt> that is suitable
     * for accessing the bytes of the specified resource.
     * </p>
     * @param mgr the <tt>ModuleManager</tt> of the module.
     * @param module the module for which the resource is being loaded.
     * @param rsIdx the index of the <tt>ResourceSource</tt> containing the resource.
     * @param name the name of the resource being loaded.
     * @return an <tt>URL</tt> for retrieving the resource.
    **/
    public URL createResourceURL(ModuleManager mgr, Module module, int rsIdx, String name)
    {
        if (m_handler == null)
        {
            m_handler = new ModuleURLStreamHandler(mgr);
        }

        // Add a slash if there is one already, otherwise
        // the is no slash separating the host from the file
        // in the resulting URL.
        if (!name.startsWith("/"))
        {
            name = "/" + name;
        }

        try
        {
            return new URL("module", module.getId(), -1, "/" + rsIdx + name, m_handler);
        }
        catch (Exception ex)
        {
            System.err.println("DefaultResourceURLPolicy: " + ex);
            return null;
        }
    }
}