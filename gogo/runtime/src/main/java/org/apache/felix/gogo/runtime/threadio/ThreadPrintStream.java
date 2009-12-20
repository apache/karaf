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
import java.io.PrintStream;
import java.io.InputStream;
import java.util.Locale;

public class ThreadPrintStream extends PrintStream
{
    PrintStream dflt;
    ThreadLocal<PrintStream> map = new InheritableThreadLocal<PrintStream>();

    public ThreadPrintStream(PrintStream out)
    {
        super(out);
        dflt = out;
    }

    public PrintStream getCurrent()
    {
        PrintStream out = map.get();
        if (out != null)
        {
            return out;
        }
        return dflt;
    }

    public void setStream(PrintStream out)
    {
        if (out != dflt && out != this)
        {
            map.set(out);
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
    public PrintStream getRoot()
    {
        return dflt;
    }

    //
    // Delegate methods
    //

    public void flush()
    {
        getCurrent().flush();
    }

    public void close()
    {
        getCurrent().close();
    }

    public boolean checkError()
    {
        return getCurrent().checkError();
    }

    public void setError()
    {
        //        getCurrent().setError();
    }

    public void clearError()
    {
        //        getCurrent().clearError();
    }

    public void write(int b)
    {
        getCurrent().write(b);
    }

    public void write(byte[] buf, int off, int len)
    {
        getCurrent().write(buf, off, len);
    }

    public void print(boolean b)
    {
        getCurrent().print(b);
    }

    public void print(char c)
    {
        getCurrent().print(c);
    }

    public void print(int i)
    {
        getCurrent().print(i);
    }

    public void print(long l)
    {
        getCurrent().print(l);
    }

    public void print(float f)
    {
        getCurrent().print(f);
    }

    public void print(double d)
    {
        getCurrent().print(d);
    }

    public void print(char[] s)
    {
        getCurrent().print(s);
    }

    public void print(String s)
    {
        getCurrent().print(s);
    }

    public void print(Object obj)
    {
        getCurrent().print(obj);
    }

    public void println()
    {
        getCurrent().println();
    }

    public void println(boolean x)
    {
        getCurrent().println(x);
    }

    public void println(char x)
    {
        getCurrent().println(x);
    }

    public void println(int x)
    {
        getCurrent().println(x);
    }

    public void println(long x)
    {
        getCurrent().println(x);
    }

    public void println(float x)
    {
        getCurrent().println(x);
    }

    public void println(double x)
    {
        getCurrent().println(x);
    }

    public void println(char[] x)
    {
        getCurrent().println(x);
    }

    public void println(String x)
    {
        getCurrent().println(x);
    }

    public void println(Object x)
    {
        getCurrent().println(x);
    }

    public PrintStream printf(String format, Object... args)
    {
        return getCurrent().printf(format, args);
    }

    public PrintStream printf(Locale l, String format, Object... args)
    {
        return getCurrent().printf(l, format, args);
    }

    public PrintStream format(String format, Object... args)
    {
        return getCurrent().format(format, args);
    }

    public PrintStream format(Locale l, String format, Object... args)
    {
        return getCurrent().format(l, format, args);
    }

    public PrintStream append(CharSequence csq)
    {
        return getCurrent().append(csq);
    }

    public PrintStream append(CharSequence csq, int start, int end)
    {
        return getCurrent().append(csq, start, end);
    }

    public PrintStream append(char c)
    {
        return getCurrent().append(c);
    }

    public void write(byte[] b) throws IOException
    {
        getCurrent().write(b);
    }
}
