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
package org.apache.felix.moduleloader;

import java.net.URL;

/**
 * <p>
 * This interface represents the <tt>ModuleLoader</tt>'s policy for creating
 * <tt>URL</tt> for resource loading and security purposes. Java requires the
 * use of <tt>URL</tt>s for resource loading and security. For resource loading,
 * <tt>URL</tt>s are returned for requested resources. Subsequently, the resource
 * <tt>URL</tt> is used to create an input stream for reading the resources
 * bytes. With respect to security, <tt>URL</tt>s are used when defining a
 * class in order to determine where the code came from, this concept is called
 * a <tt>CodeSource</tt>. This approach enables Java to assign permissions to
 * code that originates from particular locations.
 * </p>
 * <p>
 * The <tt>ModuleManager</tt> requires a concrete implementation of this
 * interface in order to function. Whenever the <tt>ModuleManager</tt> requires
 * a <tt>URL</tt> for either resource loading or security, it delegates to
 * the policy implementation. A default implementation is provided,
 * called <a href="DefaultURLPolicy.html"><tt>DefaultURLPolicy</tt></a>, but
 * it only supports resource loading, not security.
 * </p>
 * @see org.apache.felix.moduleloader.ModuleManager
 * @see org.apache.felix.moduleloader.DefaultURLPolicy
**/
public interface URLPolicy
{
    /**
     * <p>
     * This method should return a <tt>URL</tt> that represents the
     * location from which the module originated. This <tt>URL</tt>
     * can be used when assigning permissions to the module, such as
     * is done in the Java permissions policy file.
     * </p>
     * @param mgr the <tt>ModuleManager</tt> of the module.
     * @param module the module for which the <tt>URL</tt> is to be created.
     * @return an <tt>URL</tt> to associate with the module.
    **/
    public URL createCodeSourceURL(ModuleManager mgr, Module module);

    /**
     * <p>
     * This method should return a <tt>URL</tt> that is suitable
     * for accessing the bytes of the specified resource. It must be possible
     * open a connection to this <tt>URL</tt>, which may require that
     * the implementer of this method also introduce a custom
     * <tt>java.net.URLStreamHander</tt> when creating the <tt>URL</tt>.
     * </p>
     * @param mgr the <tt>ModuleManager</tt> of the module.
     * @param module the module for which the resource is being loaded.
     * @param rsIdx the index of the <tt>ResourceSource</tt> containing the resource.
     * @param name the name of the resource being loaded.
     * @return an <tt>URL</tt> for retrieving the resource.
    **/
    public URL createResourceURL(ModuleManager mgr, Module module, int rsIdx, String name);
}