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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.gogo.options.Option;
import org.apache.felix.gogo.options.Options;
import org.apache.felix.service.command.CommandSession;

/**
 * Posix-like utilities.
 * 
 * @see http://www.opengroup.org/onlinepubs/009695399/utilities/contents.html
 */
public class Posix
{
    static final String[] functions = { "cat", "echo", "grep" };

    public void cat(CommandSession session, String[] args) throws Exception
    {
        if (args.length == 0)
        {
            copy(System.in, System.out);
            return;
        }

        URI cwd = Shell.cwd(session);
        
        for (String arg : args)
        {
            copy(cwd.resolve(arg), System.out);
        }
    }

    public void echo(Object[] args)
    {
        StringBuilder buf = new StringBuilder();

        if (args == null)
        {
            System.out.println("Null");
            return;
        }

        for (Object arg : args)
        {
            if (buf.length() > 0)
                buf.append(' ');
            buf.append(String.valueOf(arg));
        }

        System.out.println(buf);
    }

    public boolean grep(CommandSession session, String[] argv) throws IOException
    {
        final String[] usage = {
                "grep -  search for PATTERN in each FILE or standard input.",
                "Usage: grep [OPTIONS] PATTERN [FILES]",
                "  -? --help                show help",
                "  -i --ignore-case         ignore case distinctions",
                "  -n --line-number         prefix each line with line number within its input file",
                "  -q --quiet, --silent     suppress all normal output",
                "  -v --invert-match        select non-matching lines" };

        Option opt = Options.compile(usage).parse(argv);

        if (opt.isSet("help"))
        {
            opt.usage();
            return true;
        }

        List<String> args = opt.args();

        if (args.size() == 0)
        {
            throw opt.usageError("no pattern supplied.");
        }

        String regex = args.remove(0);
        if (opt.isSet("ignore-case"))
        {
            regex = "(?i)" + regex;
        }

        if (args.isEmpty())
        {
            args.add(null);
        }

        StringBuilder buf = new StringBuilder();

        if (args.size() > 1)
        {
            buf.append("%1$s:");
        }

        if (opt.isSet("line-number"))
        {
            buf.append("%2$s:");
        }

        buf.append("%3$s");
        String format = buf.toString();

        Pattern pattern = Pattern.compile(regex);
        boolean status = true;
        boolean match = false;

        for (String arg : args)
        {
            InputStream in = null;

            try
            {
                URI cwd = Shell.cwd(session);
                in = (arg == null) ? System.in : cwd.resolve(arg).toURL().openStream();
                
                BufferedReader rdr = new BufferedReader(new InputStreamReader(in));
                int line = 0;
                String s;
                while ((s = rdr.readLine()) != null)
                {
                    line++;
                    Matcher matcher = pattern.matcher(s);
                    if (!(matcher.find() ^ !opt.isSet("invert-match")))
                    {
                        match = true;
                        if (opt.isSet("quiet"))
                            break;

                        System.out.println(String.format(format, arg, line, s));
                    }
                }

                if (match && opt.isSet("quiet"))
                {
                    break;
                }
            }
            catch (IOException e)
            {
                System.err.println("grep: " + e.getMessage());
                status = false;
            }
            finally
            {
                if (arg != null && in != null)
                {
                    in.close();
                }
            }
        }

        return match && status;
    }
    
    public static void copy(URI source, OutputStream out) throws IOException {
        InputStream in = source.toURL().openStream();
        try {
            copy(in, out);
        } finally {
            in.close();
        }
    }


    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte buf[] = new byte[10240];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.flush();
    }

}
