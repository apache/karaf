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
package org.apache.felix.gogo.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.felix.service.command.Converter;

public class Pipe extends Thread
{
    static final ThreadLocal<InputStream> tIn = new ThreadLocal<InputStream>();
    static final ThreadLocal<PrintStream> tOut = new ThreadLocal<PrintStream>();
    static final ThreadLocal<PrintStream> tErr = new ThreadLocal<PrintStream>();
    InputStream in;
    PrintStream out;
    PrintStream err;
    PipedOutputStream pout;
    Closure closure;
    Exception exception;
    Object result;
    List<Token> statement;

    public static Object[] mark()
    {
        Object[] mark = { tIn.get(), tOut.get(), tErr.get() };
        return mark;
    }

    public static void reset(Object[] mark)
    {
        tIn.set((InputStream) mark[0]);
        tOut.set((PrintStream) mark[1]);
        tErr.set((PrintStream) mark[2]);
    }

    public Pipe(Closure closure, List<Token> statement)
    {
        super("pipe-" + statement);
        this.closure = closure;
        this.statement = statement;

        in = tIn.get();
        out = tOut.get();
        err = tErr.get();
    }

    public String toString()
    {
        return "pipe<" + statement + "> out=" + out;
    }

    public void setIn(InputStream in)
    {
        this.in = in;
    }

    public void setOut(PrintStream out)
    {
        this.out = out;
    }

    public void setErr(PrintStream err)
    {
        this.err = err;
    }

    public Pipe connect(Pipe next) throws IOException
    {
        next.setOut(out);
        next.setErr(err);
        pout = new PipedOutputStream();
        next.setIn(new PipedInputStream(pout));
        out = new PrintStream(pout);
        return next;
    }

    public void run()
    {
        tIn.set(in);
        tOut.set(out);
        tErr.set(err);
        closure.session().threadIO().setStreams(in, out, err);

        try
        {
            result = closure.executeStatement(statement);
            if (result != null && pout != null)
            {
                if (!Boolean.FALSE.equals(closure.session().get(".FormatPipe")))
                {
                    out.println(closure.session().format(result, Converter.INSPECT));
                }
            }
        }
        catch (Exception e)
        {
            exception = e;
        }
        finally
        {
            out.flush();
            closure.session().threadIO().close();

            try
            {
                if (pout != null)
                {
                    pout.close();
                }

                if (in instanceof PipedInputStream)
                {
                    in.close();

                    // avoid writer waiting when reader has given up (FELIX-2380)
                    Method m = in.getClass().getDeclaredMethod("receivedLast",
                        (Class<?>[]) null);
                    m.setAccessible(true);
                    m.invoke(in, (Object[]) null);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}
