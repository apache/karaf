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
package org.apache.felix.moduleloader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

public interface IContent
{

    /**
     * <p>
     * This method must be called before using any other methods on this
     * interface. If the content is already opened, then subsequent calls
     * should have no effect. This method is intended to allow the content
     * to initialize any necessary resources.
     * </p>
    **/
    public void open();

    /**
     * <p>
     * This method must be called when the content is no longer needed so
     * that any resourses being used (e.g., open files) can be closed. Once
     * this method is called, the content is no longer usable. If the content
     * is already closed, then calls on this method should have no effect.
     * </p>
    **/
    public void close();

    /**
     * <p>
     * This method determines if the specified named entry is contained in
     * the associated content. The entry name is a relative path with '/'
     * separators.
     * </p>
     * @param name The name of the entry to find.
     * @return <tt>true</tt> if a corresponding entry was found, <tt>false</tt>
     *         otherwise.
    **/
    public boolean hasEntry(String name);

    /**
     * <p>
     * Returns an enumeration of entry names as <tt>String</tt> objects.
     * An entry name is a path constructed with '/' as path element
     * separators and is relative to the root of the content. Entry names
     * for entries that represent directories should end with the '/'
     * character.
     * </p>
     * @returns An enumeration of entry names or <tt>null</tt>.
    **/
    public Enumeration getEntries();

    /**
     * <p>
     * This method returns the named entry as an array of bytes.
     * </p>
     * @param name The name of the entry to retrieve as a byte array.
     * @return An array of bytes if the corresponding entry was found, <tt>null</tt>
     *         otherwise.
    **/
    public byte[] getEntryAsBytes(String name);

    /**
     * <p>
     * This method returns the named entry as an input stream.
     * </p>
     * @param name The name of the entry to retrieve as an input stream.
     * @return An input stream if the corresponding entry was found, <tt>null</tt>
     *         otherwise.
     * @throws <tt>java.io.IOException</tt> if any error occurs.
    **/
    public InputStream getEntryAsStream(String name)
        throws IOException;

    /**
     * <p>
     * This method returns the named entry as an <tt>IContent</tt> Typically,
     * this method only makes sense for entries that correspond to some form
     * of aggregated resource (e.g., an embedded JAR file or directory), but
     * implementations are free to interpret this however makes sense. This method
     * should return a new <tt>IContent</tt> instance for every invocation and
     * the caller is responsible for opening and closing the returned content
     * object.
     * </p>
     * @param name The name of the entry to retrieve as an <tt>IContent</tt>.
     * @return An <tt>IContent</tt> instance if a corresponding entry was found,
     *         <tt>null</tt> otherwise.
    **/
    public IContent getEntryAsContent(String name);

    /**
     * <p>
     * This method returns the named entry as a file in the file system for
     * use as a native library. It may not be possible for all content
     * implementations (e.g., memory only) to implement this method, in which
     * case it is acceptable to return <tt>null</tt>.
     * </p>
     * @param name The name of the entry to retrieve as a file.
     * @return A string corresponding to the absolute path of the file if a
     *         corresponding entry was found, <tt>null</tt> otherwise.
    **/
// TODO: CACHE - This method needs to be rethought once we start allowing
//               native libs in fragments to support multi-host attachement.
//               For now, our implementations of this interface will not
//               return a new file for every invocation.
    public String getEntryAsNativeLibrary(String name);
}