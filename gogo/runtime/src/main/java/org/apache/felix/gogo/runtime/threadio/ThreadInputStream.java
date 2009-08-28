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
package org.apache.felix.gogo.runtime.threadio;

import java.io.IOException;
import java.io.InputStream;

public class ThreadInputStream extends InputStream
{
    ThreadLocal<InputStream> map = new InheritableThreadLocal<InputStream>();
    InputStream dflt;

    public ThreadInputStream(InputStream in)
    {
        dflt = in;
    }

    private InputStream getCurrent()
    {
        InputStream in = map.get();
        if (in != null)
        {
            return in;
        }
        return dflt;
    }

    public void setStream(InputStream in)
    {
        if (in != dflt && in != this)
        {
            map.set(in);
        }
        else
        {
            map.remove();
        }
    }

    public void end()
    {
        map.remove();
    }

    /**
     * Access to the root stream through reflection
     *
     * @return
     */
    public InputStream getRoot()
    {
        return dflt;
    }

    //
    // Delegate methods
    //

    public int read() throws IOException
    {
        return getCurrent().read();
    }

    public int read(byte[] b) throws IOException
    {
        return getCurrent().read(b);
    }

    public int read(byte[] b, int off, int len) throws IOException
    {
        return getCurrent().read(b, off, len);
    }

    public long skip(long n) throws IOException
    {
        return getCurrent().skip(n);
    }

    public int available() throws IOException
    {
        return getCurrent().available();
    }

    public void close() throws IOException
    {
        getCurrent().close();
    }

    public void mark(int readlimit)
    {
        getCurrent().mark(readlimit);
    }

    public void reset() throws IOException
    {
        getCurrent().reset();
    }

    public boolean markSupported()
    {
        return getCurrent().markSupported();
    }
}
