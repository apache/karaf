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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import jline.Terminal;
import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CommandException;
import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.apache.felix.gogo.runtime.CommandNotFoundException;
import org.apache.felix.gogo.runtime.CommandProcessorImpl;
import org.apache.felix.gogo.runtime.threadio.ThreadIOImpl;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.apache.felix.service.threadio.ThreadIO;
import org.apache.karaf.shell.console.jline.Console;
import org.apache.karaf.shell.console.jline.TerminalFactory;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

public class Main {
    private String application = System.getProperty("karaf.name", "root");
    private String user = "karaf";

    public static void main(String args[]) throws Exception {
        Main main = new Main();
        main.run(args);
    }

    /**
     * Use this method when the shell is being executed as a top level shell.
     *
     * @param args
     * @throws Exception
     */
    public void run(String args[]) throws Exception {

        ThreadIOImpl threadio = new ThreadIOImpl();
        threadio.start();

        CommandProcessorImpl commandProcessor = new CommandProcessorImpl(threadio);

        InputStream in = unwrap(System.in);
        PrintStream out = wrap(unwrap(System.out));
        PrintStream err = wrap(unwrap(System.err));
        run(commandProcessor, threadio, args, in, out, err);

        // TODO: do we need to stop the threadio that was started?
        // threadio.stop();
    }

    /**
     * Use this method when the shell is being executed as a command
     * of another shell.
     *
     * @param parent
     * @param args
     * @throws Exception
     */
    public void run(CommandSession parent, ThreadIO threadIO, String args[]) throws Exception {

        // TODO: find out what the down side of not using a real ThreadIO implementation is.
        CommandProcessorImpl commandProcessor = new CommandProcessorImpl(new ThreadIO() {
            public void setStreams(InputStream in, PrintStream out, PrintStream err) {
            }
            public void close() {
            }
        });

        InputStream in = parent.getKeyboard();
        PrintStream out = parent.getConsole();
        PrintStream err = parent.getConsole();
        run(commandProcessor, threadIO, args, in, out, err);
    }

    private void run(CommandProcessorImpl commandProcessor, ThreadIO threadIO, String[] args, InputStream in, PrintStream out, PrintStream err) throws Exception {
        StringBuilder sb = new StringBuilder();
        String classpath = null;
        boolean batch = false;
        String file  = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--classpath=")) {
                classpath = arg.substring("--classpath=".length());
            } else if (arg.startsWith("-c=")) {
                classpath = arg.substring("-c=".length());
            } else if (arg.equals("--classpath") || arg.equals("-c")) {
                classpath = args[++i];
            } else if (arg.equals("-b") || arg.equals("--batch")) {
                batch = true;
            } else if (arg.startsWith("--file=")) {
                file = arg.substring("--file=".length());
            } else if (arg.startsWith("-f=")) {
                file = arg.substring("-f=".length());
            } else if (arg.equals("--file") || arg.equals("-f")) {
                file = args[++i];
            } else {
                sb.append(arg);
                sb.append(' ');
            }
        }

        if (file != null) {
            Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            try {
                sb.setLength(0);
                for (int c = reader.read(); c >= 0; c = reader.read()) {
                    sb.append((char) c);
                }
            } finally {
                reader.close();
            }
        } else if (batch) {
            // read all stdin
            Reader reader = new BufferedReader(new InputStreamReader(System.in));
            sb.setLength(0);
            for (int c = reader.read(); c >= 0; c = reader.read()) {
                sb.append((char) c);
            }
        }

        ClassLoader cl = Main.class.getClassLoader();
        if (classpath != null) {
            List<URL> urls = getFiles(new File(classpath));
            cl = new URLClassLoader(urls.toArray(new URL[urls.size()]), cl);
        }

        discoverCommands(commandProcessor, cl);

        run(commandProcessor, threadIO, sb.toString(), in, out, err);
    }

    private void run(final CommandProcessorImpl commandProcessor, ThreadIO threadIO, String command, final InputStream in, final PrintStream out, final PrintStream err) throws Exception {

        if (command.length() > 0) {

            // Shell is directly executing a sub/command, we don't setup a terminal and console
            // in this case, this avoids us reading from stdin un-necessarily.
            CommandSession session = commandProcessor.createSession(in, out, err);
            session.put("USER", user);
            session.put("APPLICATION", application);
            session.put(NameScoping.MULTI_SCOPE_MODE_KEY, Boolean.toString(isMultiScopeMode()));

            try {
                session.execute(command);
            } catch (Throwable t) {
                if (t instanceof CommandNotFoundException) {
                    String str = Ansi.ansi()
                        .fg(Ansi.Color.RED)
                        .a("Command not found: ")
                        .a(Ansi.Attribute.INTENSITY_BOLD)
                        .a(((CommandNotFoundException) t).getCommand())
                        .a(Ansi.Attribute.INTENSITY_BOLD_OFF)
                        .fg(Ansi.Color.DEFAULT).toString();
                    session.getConsole().println(str);
                } else if (t instanceof CommandException) {
                    session.getConsole().println(((CommandException) t).getNiceHelp());
                } else {
                    session.getConsole().print(Ansi.ansi().fg(Ansi.Color.RED).toString());
                    t.printStackTrace(session.getConsole());
                    session.getConsole().print(Ansi.ansi().fg(Ansi.Color.DEFAULT).toString());
                }
            }
        } else {
            // We are going into full blown interactive shell mode.

            final TerminalFactory terminalFactory = new TerminalFactory();
            final Terminal terminal = terminalFactory.getTerminal();
            Console console = createConsole(commandProcessor, threadIO, in, out, err, terminal);
            CommandSession session = console.getSession();
            for (Object o : System.getProperties().keySet()) {
                String key = o.toString();
                session.put(key, System.getProperty(key));
            }
            session.put("USER", user);
            session.put("APPLICATION", application);
            session.put(NameScoping.MULTI_SCOPE_MODE_KEY, Boolean.toString(isMultiScopeMode()));
            session.put("#LINES", new Function() {
                public Object execute(CommandSession session, List<Object> arguments) throws Exception {
                    return Integer.toString(terminal.getHeight());
                }
            });
            session.put("#COLUMNS", new Function() {
                public Object execute(CommandSession session, List<Object> arguments) throws Exception {
                    return Integer.toString(terminal.getWidth());
                }
            });
            session.put(".jline.terminal", terminal);

            console.run();

            terminalFactory.destroy();
        }

    }

    /**
     * Allow sub classes of main to change the Console implementation used.
     * 
     * @param commandProcessor
     * @param in
     * @param out
     * @param err
     * @param terminal
     * @return
     * @throws Exception
     */
    protected Console createConsole(CommandProcessorImpl commandProcessor, ThreadIO threadIO, InputStream in, PrintStream out, PrintStream err, Terminal terminal) throws Exception {
        return new Console(commandProcessor, threadIO, in, out, err, terminal, null, null, null);
    }

    /**
     * Sub classes can override so that their registered commands do not conflict with the default shell
     * implementation.
     *
     * @return
     */
    public String getDiscoveryResource() {
        return "META-INF/services/org/apache/karaf/shell/commands";
    }

    private void discoverCommands(CommandProcessorImpl commandProcessor, ClassLoader cl) throws IOException, ClassNotFoundException {
        Enumeration<URL> urls = cl.getResources(getDiscoveryResource());
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = r.readLine();
            while (line != null) {
                line = line.trim();
                if (line.length() > 0 && line.charAt(0) != '#') {
                    final Class<Action> actionClass = (Class<Action>) cl.loadClass(line);
                    Command cmd = actionClass.getAnnotation(Command.class);
                    Function function = new AbstractCommand() {
                        @Override
                        public Action createNewAction() {
                            try {
                                return ((Class<? extends Action>) actionClass).newInstance();
                            } catch (InstantiationException e) {
                                throw new RuntimeException(e);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    };
                    addCommand(cmd, function, commandProcessor);
                }
                line = r.readLine();
            }
            r.close();
        }
    }

    protected void addCommand(Command cmd, Function function, CommandProcessorImpl commandProcessor) {
        try {
            commandProcessor.addCommand(cmd.scope(), function, cmd.name());
        } catch (Exception e) {
        }
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

    /**
     * Returns whether or not we are in multi-scope mode.
     *
     * The default mode is multi-scoped where we prefix commands by their scope. If we are in single
     * scoped mode then we don't use scope prefixes when registering or tab completing commands.
     */
    public boolean isMultiScopeMode() {
        return true;
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
