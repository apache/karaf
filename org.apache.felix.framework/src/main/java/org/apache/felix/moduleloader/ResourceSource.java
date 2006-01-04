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

/**
 * <p>
 * This interface represents a source for obtaining resources for a
 * given module via the module's class loader. A resource source is used
 * for retrieving both classes and resources; at this level, classes are
 * treated in an identical manner as an ordinary resource. Resource sources
 * are completely arbitrary and implementations may load resources from a JAR
 * file, the network, a database, or anywhere.
 * </p>
 * <p>
 * All resource sources are initialized before first usage via a call
 * to the <a href="#open()"><tt>ResourceSource.open()</tt></a> method and
 * are also deinitialized via a call to
 * <a href="#open()"><tt>ResourceSource.close()</tt></a>. Resource sources
 * should be implemented such that they can be opened, closed, and then
 * re-opened.
 * </p>
 * @see org.apache.felix.moduleloader.Module
 * @see org.apache.felix.moduleloader.ModuleClassLoader
**/
public interface ResourceSource
{
    /**
     * <p>
     * This method initializes the resource source. It is called when
     * the associated module is added to the <tt>ModuleManager</tt>. It
     * is acceptable for implementations to ignore duplicate calls to this
     * method if the resource source is already opened.
     * </p>
    **/
    public void open();

    /**
     * <p>
     * This method de-initializes the resource source. It is called when
     * the associated module is removed from the <tt>ModuleManager</tt> or
     * when the module is reset by the <tt>ModuleManager</tt>.
     * </p>
    **/
    public void close();

    /**
     * <p>
     * This method returns a boolean indicating whether the resource source
     * contains the specified resource.
     * </p>
     * @param name the name of the resource whose existence is being checked.
     * @param <tt>true</tt> if the resource source has the resource, <tt>false</tt>
     *        otherwise.
     * @throws java.lang.IllegalStateException if the resource source has not
     *         been opened.
    **/
    public boolean hasResource(String name) throws IllegalStateException;

    /**
     * <p>
     * This method returns a byte array of the specified resource's contents.
     * </p>
     * @param name the name of the resource to retrieve.
     * @param a byte array of the resource's contents or <tt>null</tt>
     *        if the resource was not found.
     * @throws java.lang.IllegalStateException if the resource source has not
     *         been opened.
    **/
    public byte[] getBytes(String name) throws IllegalStateException;
}