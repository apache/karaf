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
package org.apache.karaf.shell.console;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import jline.Terminal;
import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.apache.felix.gogo.runtime.lang.Support;
import org.apache.felix.gogo.runtime.shell.CommandShellImpl;
import org.apache.felix.gogo.runtime.threadio.ThreadIOImpl;
import org.apache.karaf.shell.console.jline.Console;
import org.apache.karaf.shell.console.jline.TerminalFactory;
import org.fusesource.jansi.AnsiConsole;
import org.osgi.service.command.CommandSession;
import org.osgi.service.command.Function;

public class Main {
    private String application = System.getProperty("karaf.name", "root");
    private String user = "karaf";

    public static void main(String args[]) throws Exception {
        Main main = new Main();
        main.run(args);
    }

    public void run(String args[]) throws Exception {
        ThreadIOImpl threadio = new ThreadIOImpl();
        threadio.start();

        CommandShellImpl commandProcessor = new CommandShellImpl();
        commandProcessor.setThreadio(threadio);
        commandProcessor.setConverter(new Support());

        ClassLoader cl = Main.class.getClassLoader();
        if (args.length > 0 && args[0].startsWith("--classpath=")) {
            String base = args[0].substring("--classpath=".length());
            List<URL> urls = getFiles(new File(base));
            cl = new URLClassLoader(urls.toArray(new URL[urls.size()]), cl);
            String[] a = new String[args.length - 1];
            System.arraycopy(args, 1, a, 0, a.length);
            args = a;
        }

        Enumeration<URL> urls = cl.getResources("META-INF/services/org/apache/karaf/shell/commands");
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = r.readLine();
            while (line != null) {
                line = line.trim();
                if (line.length() > 0 && line.charAt(0) != '#') {
                        final Class<Action> actionClass = (Class<Action>) cl.loadClass(line);
                        try {
                            Command cmd = actionClass.getAnnotation(Command.class);
                            Function function = new AbstractCommand() {
                                @Override
                                protected Action createNewAction() throws Exception {
                                    return actionClass.newInstance();
                                }
                            };
                            commandProcessor.addCommand(cmd.scope(), function, cmd.name());
                        } catch (Exception e) {
                        }
                }
                line = r.readLine();
            }
            r.close();
        }

        TerminalFactory terminalFactory = new TerminalFactory();

        InputStream in = unwrap(System.in);
        PrintStream out = unwrap(System.out);
        PrintStream err = unwrap(System.err);
        Terminal terminal = terminalFactory.getTerminal();
        Console console = new Console(commandProcessor,
                                   in,
                                   wrap(out),
                                   wrap(err),
                                   terminal,
                                   null,
                                   null) {
            @Override
            protected Properties loadBrandingProperties() {
                return super.loadBrandingProperties();
            }
        };
        CommandSession session = console.getSession();
        session.put("USER", user);
        session.put("APPLICATION", application);
        session.put("LINES", Integer.toString(terminal.getTerminalHeight()));
        session.put("COLUMNS", Integer.toString(terminal.getTerminalWidth()));
        session.put(".jline.terminal", terminal);

        if (args.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    sb.append(" ");
                }
                sb.append(args[i]);
            }
            session.execute(sb);
        } else {
            console.run();
        }

        terminalFactory.destroy();
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    private static PrintStream wrap(PrintStream stream) {
        OutputStream o = AnsiConsole.wrapOutputStream(stream);
        if (o instanceof PrintStream) {
            return ((PrintStream) o);
        } else {
            return new PrintStream(o);
        }
    }

    private static <T> T unwrap(T stream) {
        try {
            Method mth = stream.getClass().getMethod("getRoot");
            return (T) mth.invoke(stream);
        } catch (Throwable t) {
            return stream;
        }
    }

    private static List<URL> getFiles(File base) throws MalformedURLException {
        List<URL> urls = new ArrayList<URL>();
        getFiles(base, urls);
        return urls;
    }

    private static void getFiles(File base, List<URL> urls) throws MalformedURLException {
        for (File f : base.listFiles()) {
            if (f.isDirectory()) {
                getFiles(f, urls);
            } else if (f.getName().endsWith(".jar")) {
                urls.add(f.toURI().toURL());
            }
        }
    }
}
