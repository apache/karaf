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
package org.apache.felix.gogo.shell;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URLConnection;
import java.nio.CharBuffer;
import java.util.List;
import java.util.Set;

import org.apache.felix.gogo.options.Option;
import org.apache.felix.gogo.options.Options;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class Shell
{
    static final String[] functions = { "gosh", "sh", "source" };

    private final static URI CWD = new File(".").toURI();

    private final URI baseURI;
    private final BundleContext context;
    private final CommandProcessor processor;

    public Shell(BundleContext context, CommandProcessor processor)
    {
        this.context = context;
        this.processor = processor;
        String baseDir = System.getProperty("gosh.home", System.getProperty("user.dir"));
        baseURI = new File(baseDir).toURI();
    }

    public Object gosh(final CommandSession session, String[] argv) throws Exception
    {
        final String[] usage = {
                "gosh - execute script with arguments in a new session",
                "  args are available as session variables $1..$9 and $args.",
                "Usage: gosh [OPTIONS] [script-file [args..]]",
                "  -c --command             pass all remaining args to sub-shell",
                "     --login               login shell (same session, reads etc/gosh_profile)",
                "  -s --noshutdown          don't shutdown framework when script completes",
                "  -x --xtrace              echo commands before execution",
                "  -? --help                show help",
                "If no script-file, an interactive shell is started, type $D to exit." };

        Option opt = Options.compile(usage).setOptionsFirst(true).parse(argv);
        List<String> args = opt.args();

        boolean login = opt.isSet("login");

        if (opt.isSet("help"))
        {
            opt.usage();
            if (login && !opt.isSet("noshutdown"))
            {
                shutdown();
            }
            return null;
        }

        if (opt.isSet("command") && args.isEmpty())
        {
            throw opt.usageError("option --command requires argument(s)");
        }

        CommandSession newSession = (login ? session : processor.createSession(
            session.getKeyboard(), session.getConsole(), System.err));

        if (opt.isSet("xtrace"))
        {
            newSession.put("echo", true);
        }

        if (login)
        {
            URI uri = baseURI.resolve("etc/gosh_profile");
            if (!new File(uri).exists())
            {
                uri = getClass().getResource("/gosh_profile").toURI();
            }
            if (uri != null)
            {
                source(session, uri.toString());
            }
        }

        // export variables starting with upper-case to newSession
        for (String key : getVariables(session))
        {
            if (key.matches("[.]?[A-Z].*"))
            {
                newSession.put(key, session.get(key));
            }
        }

        Object result;

        if (args.isEmpty())
        {
            result = console(newSession);
        }
        else
        {
            CharSequence program;

            if (opt.isSet("command"))
            {
                StringBuilder buf = new StringBuilder();
                for (String arg : args)
                {
                    if (buf.length() > 0)
                    {
                        buf.append(' ');
                    }
                    buf.append(arg);
                }
                program = buf;
            }
            else
            {
                URI script = cwd(session).resolve(args.remove(0));

                // set script arguments
                newSession.put("0", script);
                newSession.put("args", args);

                for (int i = 0; i < args.size(); ++i)
                {
                    newSession.put(String.valueOf(i + 1), args.get(i));
                }

                program = readScript(script);
            }

            result = newSession.execute(program);
        }

        if (login && !opt.isSet("noshutdown"))
        {
            System.out.println("gosh: stopping framework");
            shutdown();
        }

        return result;
    }

    public Object sh(final CommandSession session, String[] argv) throws Exception
    {
        return gosh(session, argv);
    }

    private void shutdown() throws BundleException
    {
        context.getBundle(0).stop();
    }

    public Object source(CommandSession session, String script) throws Exception
    {
        URI uri = cwd(session).resolve(script);
        session.put("0", uri);
        try
        {
            return session.execute(readScript(uri));
        }
        finally
        {
            session.put("0", null); // API doesn't support remove
        }
    }

    private Object console(CommandSession session)
    {
        Console console = new Console(session);
        console.run();
        return null;
    }

    private CharSequence readScript(URI script) throws Exception
    {
        URLConnection conn = script.toURL().openConnection();
        int length = conn.getContentLength();

        if (length == -1)
        {
            System.err.println("eek! unknown Contentlength for: " + script);
            length = 10240;
        }

        InputStream in = conn.getInputStream();
        CharBuffer cbuf = CharBuffer.allocate(length);
        Reader reader = new InputStreamReader(in);
        reader.read(cbuf);
        in.close();
        cbuf.rewind();

        return cbuf;
    }

    @SuppressWarnings("unchecked")
    static Set<String> getVariables(CommandSession session)
    {
        return (Set<String>) session.get(".variables");
    }

    static URI cwd(CommandSession session)
    {
        Object cwd = session.get("_cwd"); // _cwd is set by felixcommands:cd

        if (cwd instanceof URI)
        {
            return (URI) cwd;
        }
        else if (cwd instanceof File)
        {
            return ((File) cwd).toURI();
        }
        else
        {
            return CWD;
        }
    }
}
