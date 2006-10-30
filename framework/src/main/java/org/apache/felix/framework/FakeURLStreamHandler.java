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
package org.apache.felix.framework;

import java.io.IOException;
import java.net.*;

/**
 * This class implements a fake stream handler. This class is necessary in
 * some cases when assigning <tt>CodeSource</tt>s to classes in
 * <tt>BundleClassLoader</tt>. In general, the bundle location is an URL
 * and this URL is used as the code source for the bundle's associated
 * classes. The OSGi specification does not require that the bundle
 * location be an URL, though, so in that case we try to generate a
 * fake URL for the code source of the bundle, which is just the location
 * string prepended with the "location:" protocol, by default. We need
 * this fake handler to avoid an unknown protocol exception.
**/
class FakeURLStreamHandler extends URLStreamHandler
{
    protected URLConnection openConnection(URL url) throws IOException
    {
        throw new IOException("FakeURLStreamHandler can not be used!");
    }
}
