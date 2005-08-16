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

/**
 * <p>
 * This interface represents a source for obtaining native libraries for a
 * given module via the module's class loader. The main goal of a library
 * source is to map a library name to a path in the file system.
 * </p>
 * <p>
 * All library sources are initialized before first usage via a call
 * to the <a href="#open()"><tt>LibrarySource.open()</tt></a> method and
 * are also deinitialized via a call to
 * <a href="#open()"><tt>LibrarySource.close()</tt></a>. Library sources
 * should be implemented such that they can be opened, closed, and then
 * re-opened.
 * </p>
 * @see org.apache.osgi.moduleloader.Module
 * @see org.apache.osgi.moduleloader.ModuleClassLoader
**/
public interface LibrarySource
{
    /**
     * <p>
     * This method initializes the library source. It is called when
     * the associated module is added to the <tt>ModuleManager</tt>. It
     * is acceptable for implementations to ignore duplicate calls to this
     * method if the library source is already opened.
     * </p>
    **/
    public void open();

    /**
     * <p>
     * This method de-initializes the library source. It is called when
     * the associated module is removed from the <tt>ModuleManager</tt> or
     * when the module is reset by the <tt>ModuleManager</tt>.
     * </p>
    **/
    public void close();

    /**
     * <p>
     * Returns a file system path to the specified library.
     * </p>
     * @param name the name of the library that is being requested.
     * @return a file system path to the specified library.
     * @throws java.lang.IllegalStateException if the resource source has not
     *         been opened.
    **/
    public String getPath(String name) throws IllegalStateException;
}