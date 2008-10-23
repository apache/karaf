/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.servicemix.kernel.gshell.core.vfs.mvn;

import java.net.URL;
import java.net.MalformedURLException;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.provider.url.UrlFileObject;
import org.apache.commons.vfs.provider.URLFileName;

public class MvnFileObject extends UrlFileObject {

    public MvnFileObject(MvnFileSystem fs, FileName fileName) {
        super(fs, fileName);
    }

    protected URL createURL(final FileName name) throws MalformedURLException, FileSystemException, URIException
    {
        String url;
        if (name instanceof URLFileName)
        {
            URLFileName urlName = (URLFileName) getName();

            // TODO: charset
            url = urlName.getURIEncoded(null);
        }
        else
        {
            url = getName().getURI();
        }
        if (url.startsWith("mvn:///")) {
            url = "mvn:" + url.substring("mvn:///".length());
        }
        return new URL(url);
    }

}
