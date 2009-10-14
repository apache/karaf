/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.fileinstall.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.osgi.service.url.AbstractURLStreamHandlerService;

/**
 * A URL handler that can jar a directory on the fly
 */
public class JarDirUrlHandler extends AbstractURLStreamHandlerService
{

    public static final String PROTOCOL = "jardir";

    private static final String SYNTAX = PROTOCOL + ": file";

    /**
     * Open the connection for the given URL.
     *
     * @param url the url from which to open a connection.
     * @return a connection on the specified URL.
     * @throws java.io.IOException if an error occurs or if the URL is malformed.
     */
	public URLConnection openConnection(URL url) throws IOException
    {
		if (url.getPath() == null || url.getPath().trim().length() == 0)
        {
			throw new MalformedURLException("Path can not be null or empty. Syntax: " + SYNTAX );
		}
		return new Connection(url);
	}

    public class Connection extends URLConnection
    {

        public Connection(URL url)
        {
            super(url);
        }

        public void connect() throws IOException
        {
        }

        public InputStream getInputStream() throws IOException
        {
            try
            {
                final PipedOutputStream pos = new PipedOutputStream();
                final PipedInputStream pis = new PipedInputStream(pos);
                new Thread()
                {
                    public void run()
                    {
                        try
                        {
                            Util.jarDir(new File(getURL().getPath()), pos);
                        }
                        catch (IOException e)
                        {
                            try
                            {
                                pos.close();
                            }
                            catch (IOException e2)
                            {
                                // Ignore
                            }
                        }
                    }
                }.start();
                return pis;
            }
            catch (Exception e)
            {
                throw (IOException) new IOException("Error opening spring xml url").initCause(e);
            }
        }
    }

}
