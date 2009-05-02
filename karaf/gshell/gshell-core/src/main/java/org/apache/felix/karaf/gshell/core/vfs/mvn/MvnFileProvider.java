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
package org.apache.felix.karaf.gshell.core.vfs.mvn;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystem;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.provider.url.UrlFileProvider;

public class MvnFileProvider extends UrlFileProvider {

    /**
     * Locates a file object, by absolute URI.
     */
    public synchronized FileObject findFile(final FileObject baseFile,
                                            final String uri,
                                            final FileSystemOptions fileSystemOptions)
        throws FileSystemException
    {
        try
        {
            final URL url = new URL(uri);

            URL rootUrl = new URL(url, "/");
            final String key = this.getClass().getName() + rootUrl.toString();
            FileSystem fs = findFileSystem(key, fileSystemOptions);
            if (fs == null)
            {
                String extForm = rootUrl.toExternalForm();
                final FileName rootName = getContext().parseURI(extForm);
                // final FileName rootName =
                //    new BasicFileName(rootUrl, FileName.ROOT_PATH);
                fs = new MvnFileSystem(rootName, fileSystemOptions);
                addFileSystem(key, fs);
            }
            return fs.resolveFile(url.getPath());
        }
        catch (final MalformedURLException e)
        {
            throw new FileSystemException("vfs.provider.url/badly-formed-uri.error", uri, e);
        }
    }


}
