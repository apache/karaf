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
package org.apache.felix.moduleloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public interface IContentLoader
{
    public void open();
    public void close();

    public IContent getContent();

    public void setSearchPolicy(ISearchPolicy searchPolicy);
    public ISearchPolicy getSearchPolicy();

    public void setURLPolicy(IURLPolicy urlPolicy);
    public IURLPolicy getURLPolicy();

    public Class getClass(String name);
    public URL getResource(String name);

    public boolean hasInputStream(String urlPath)
        throws IOException;
    public InputStream getInputStream(String urlPath)
        throws IOException;
}