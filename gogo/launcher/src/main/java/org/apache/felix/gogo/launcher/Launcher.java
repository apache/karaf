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
package org.apache.felix.gogo.launcher;

import org.apache.felix.gogo.runtime.osgi.OSGiShell;
import org.apache.felix.gogo.runtime.threadio.ThreadIOImpl;
import org.apache.felix.gogo.console.stdio.Console;
import org.osgi.framework.Bundle;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.launch.Framework;
import org.osgi.service.command.CommandSession;

import java.io.*;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class Launcher
{
    static List<URL> classpath = new ArrayList<URL>();
    static File cwd = new File("").getAbsoluteFile();

    public static void main(String args[]) throws Exception
    {
        StringBuffer sb = new StringBuffer();
        String fwkClassName = null;
        PrintStream out = System.out;
        InputStream in = System.in;
        boolean console = false;

        for (int i = 0; i < args.length; i++)
        {
            String arg = args[i];
            if (arg.equals("-f"))
            {
                fwkClassName = args[++i];
            }
            else
            {
                if (arg.equals("-cp") || arg.equals("-classpath"))
                {
                    classpath(args[++i]);
                }
                else
                {
                    if (arg.equals("-console"))
                    {
                        console = true;
                    }
                    else
                    {
                        if (arg.equals("-i"))
                        {
                            in = new FileInputStream(args[++i]);
                        }
                        else
                        {
                            if (arg.equals("-o"))
                            {
                                out = new PrintStream(new FileOutputStream(args[++i]));
                            }
                            else
                            {
                                sb.append(' ');
                                sb.append(arg);
                            }
                        }
                    }
                }
            }
        }

        URL[] urls = classpath.toArray(new URL[classpath.size()]);
        URLClassLoader cl = new URLClassLoader(urls, Launcher.class.getClassLoader());

        Properties p = new Properties(System.getProperties());

        Framework framework;
        if (fwkClassName == null)
        {
            framework = getFrameworkFactory(cl).newFramework(p);
        }
        else
        {
            Class<?> fw = cl.loadClass(fwkClassName);
            Constructor<?> c = fw.getConstructor(Map.class, List.class);
            framework = (Framework) c.newInstance(p);
        }

        ThreadIOImpl threadio = new ThreadIOImpl();
        threadio.start();

        OSGiShell shell = new OSGiShell();
        shell.setThreadio(threadio);
        shell.setBundle(framework);
        shell.start();

        CommandSession session = shell.createSession(in, out, System.err);
        session.put("shell", shell);
        session.put("threadio", threadio);

        session.execute(sb);
        out.flush();

        if (framework.getState() == Bundle.ACTIVE)
        {
        }
        if (console)
        {
            Console cons = new Console();
            cons.setSession(session);
            cons.run();
        }
    }

    /**
     * Simple method to search for META-INF/services for a FrameworkFactory
     * provider. It simply looks for the first non-commented line and assumes
     * it is the name of the provider class.
     * @return a <tt>FrameworkFactory</tt> instance.
     * @throws Exception if anything goes wrong.
    **/
    private static FrameworkFactory getFrameworkFactory(ClassLoader cl)
        throws Exception
    {
        URL url = cl.getResource(
            "META-INF/services/org.osgi.framework.launch.FrameworkFactory");
        if (url != null)
        {
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            try
            {
                for (String s = br.readLine(); s != null; s = br.readLine())
                {
                    s = s.trim();
                    // Try to load first non-empty, non-commented line.
                    if ((s.length() > 0) && (s.charAt(0) != '#'))
                    {
                        return (FrameworkFactory) cl.loadClass(s).newInstance();
                    }
                }
            }
            finally
            {
                if (br != null) br.close();
            }
        }

        throw new Exception("Could not find framework factory.");
    }

    private static void classpath(String string) throws MalformedURLException
    {
        StringTokenizer st = new StringTokenizer(string, File.pathSeparator);
        while (st.hasMoreTokens())
        {
            String part = st.nextToken();
            if (part.equals("."))
            {
                classpath.add(cwd.toURL());
            }

            File f = new File(part);
            if (!f.isAbsolute())
            {
                f = new File(cwd, part);
            }
            if (f.exists())
            {
                classpath.add(f.toURL());
            }
            else
            {
                System.err.println("Can not find " + part);
            }
        }
    }
}