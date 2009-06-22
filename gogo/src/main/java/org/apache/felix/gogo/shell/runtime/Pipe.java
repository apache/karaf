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
// DWB16: redirect System.err when creating pipe
package org.apache.felix.gogo.shell.runtime;

import org.osgi.service.command.Converter;

import java.io.*;
import java.util.List;

public class Pipe extends Thread
{
    InputStream in;
    PrintStream out;
    PrintStream err;    // derek
    PipedOutputStream pout;
    Closure closure;
    Exception exception;
    Object result;
    List<List<CharSequence>> statements;

    public Pipe(Closure closure, List<List<CharSequence>> statements)
    {
        super("pipe-" + statements);
        this.closure = closure;
        this.statements = statements;
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
        pout = new PipedOutputStream();
        next.setIn(new PipedInputStream(pout));
        out = new PrintStream(pout);
        return next;

    }

    public void run()
    {
        //closure.session.service.threadIO.setStreams(in, out, System.err);
        closure.session.service.threadIO.setStreams(in, out, err);    // derek
        try
        {
            for (List<CharSequence> statement : statements)
            {
                result = closure.executeStatement(statement);
                if (result != null && pout != null)
                {
                    out.println(closure.session.format(result, Converter.INSPECT));
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
            closure.session.service.threadIO.close();
            try
            {
                if (in instanceof PipedInputStream)
                {
                    in.close();
                }
                if (pout != null)
                {
                    pout.close();
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
