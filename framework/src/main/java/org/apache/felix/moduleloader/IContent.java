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
    public void open();
    public void close();
    public boolean hasEntry(String name);
    public byte[] getEntry(String name);
    public InputStream getEntryAsStream(String name)
        throws IOException;

    /**
     * <p>
     * Returns an enumeration of entry names as <tt>String</tt> objects.
     * An entry name is a path constructed with '/' as path element
     * separators and is relative to the root of the content. Entry names
     * for entries that represent directories should end with the '/'
     * character.
     * </p>
     * @ returns An enumeration of entry names or <tt>null</tt>.
    **/ 
    public Enumeration getEntries();
}
