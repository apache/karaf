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
package org.apache.karaf.shell.impl.console;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.gogo.api.Job;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.apache.felix.service.command.Function;
import org.apache.felix.service.threadio.ThreadIO;
import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.History;
import org.apache.karaf.shell.api.console.Registry;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.shell.api.console.Terminal;
import org.apache.karaf.shell.impl.console.parsing.CommandLineParser;
import org.apache.karaf.shell.impl.console.parsing.KarafParser;
import org.apache.karaf.shell.support.ShellUtil;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.apache.karaf.shell.support.completers.FileOrUriCompleter;
import org.apache.karaf.shell.support.completers.UriCompleter;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.reader.impl.history.history.FileHistory;
import org.jline.reader.impl.history.history.MemoryHistory;
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.impl.DumbTerminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleSessionImpl implements Session {

    public static final String SHELL_INIT_SCRIPT = "karaf.shell.init.script";
    public static final String SHELL_HISTORY_MAXSIZE = "karaf.shell.history.maxSize";
    public static final String PROMPT = "PROMPT";
    public static final String DEFAULT_PROMPT = "\u001B[1m${USER}\u001B[0m@${APPLICATION}(${SUBSHELL})> ";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleSessionImpl.class);

    // Input stream
    volatile boolean running;

    final SessionFactory factory;
    final ThreadIO threadIO;
    final InputStream in;
    final PrintStream out;
    final PrintStream err;
    private Runnable closeCallback;

    final CommandSession session;
    final Registry registry;
    final Terminal terminal;
    final org.jline.terminal.Terminal jlineTerminal;
    final History history;
    final LineReaderImpl reader;

    private Thread thread;

    public ConsoleSessionImpl(SessionFactory factory,
                              CommandProcessor processor,
                              ThreadIO threadIO,
                              InputStream in,
                              PrintStream out,
                              PrintStream err,
                              Terminal term,
                              String encoding,
                              Runnable closeCallback) {
        // Arguments
        this.factory = factory;
        this.threadIO = threadIO;
        this.in = in;
        this.out = out;
        this.err = err;
        this.closeCallback = closeCallback;

        // Terminal
        if (term instanceof org.jline.terminal.Terminal) {
            terminal = term;
            jlineTerminal = (org.jline.terminal.Terminal) term;
        } else if (term != null) {
            terminal = term;
//            jlineTerminal = new KarafTerminal(term);
            // TODO:JLINE
            throw new UnsupportedOperationException();
        } else {
            try {
                jlineTerminal = new DumbTerminal(in, out);
                terminal = new JLineTerminal(jlineTerminal);
            } catch (IOException e) {
                throw new RuntimeException("Unable to create terminal", e);
            }
        }

        // Console reader
        try {
            reader = new LineReaderImpl(
                    jlineTerminal,
                    "karaf");
        } catch (IOException e) {
            throw new RuntimeException("Error opening console reader", e);
        }

        // History
        final File file = getHistoryFile();
        try {
            file.getParentFile().mkdirs();
            reader.setHistory(new FileHistory(file));
        } catch (Exception e) {
            LOGGER.error("Can not read history from file " + file + ". Using in memory history", e);
        }
        if (reader.getHistory() instanceof MemoryHistory) {
            String maxSizeStr = System.getProperty(SHELL_HISTORY_MAXSIZE);
            if (maxSizeStr != null) {
                ((MemoryHistory) this.reader.getHistory()).setMaxSize(Integer.parseInt(maxSizeStr));
            }
        }
        history = new HistoryWrapper(reader.getHistory());

        // Registry
        registry = new RegistryImpl(factory.getRegistry());
        registry.register(factory);
        registry.register(this);
        registry.register(registry);
        registry.register(terminal);
        registry.register(history);

        // Completers
        CommandsCompleter completer = new CommandsCompleter(factory, this);
        reader.setCompleter(completer);
        registry.register(completer);
        registry.register(new CommandNamesCompleter());
        registry.register(new FileCompleter());
        registry.register(new UriCompleter());
        registry.register(new FileOrUriCompleter());

        // Session
        session = processor.createSession(jlineTerminal.input(),
                                          jlineTerminal.output(),
                                          jlineTerminal.output());
        Properties sysProps = System.getProperties();
        for (Object key : sysProps.keySet()) {
            session.put(key.toString(), sysProps.get(key));
        }
        session.put(".session", this);
        session.put(".commandSession", session);
        session.put(".jline.reader", reader);
        session.put(".jline.terminal", reader.getTerminal());
        session.put(".jline.history", reader.getHistory());
        session.put(Session.SCOPE, "shell:bundle:*");
        session.put(Session.SUBSHELL, "");
        session.put(Session.COMPLETION_MODE, loadCompletionMode());
        session.put("USER", ShellUtil.getCurrentUserName());
        session.put("TERM", terminal.getType());
        session.put("APPLICATION", System.getProperty("karaf.name", "root"));
        session.put("#LINES", (Function) (session, arguments) -> Integer.toString(terminal.getHeight()));
        session.put("#COLUMNS", (Function) (session, arguments) -> Integer.toString(terminal.getWidth()));
        session.put("pid", getPid());
        session.currentDir(null);

        reader.setHighlighter(new org.apache.felix.gogo.jline.Highlighter(session));
        reader.setParser(new KarafParser(this));

    }

    /**
     * Subclasses can override to use a different history file.
     *
     * @return the history file
     */
    protected File getHistoryFile() {
        String defaultHistoryPath = new File(System.getProperty("user.home"), ".karaf/karaf.history").toString();
        return new File(System.getProperty("karaf.history", defaultHistoryPath));
    }

    @Override
    public Terminal getTerminal() {
        return terminal;
    }

    public History getHistory() {
        return history;
    }

    @Override
    public Registry getRegistry() {
        return registry;
    }

    @Override
    public SessionFactory getFactory() {
        return factory;
    }

    public void close() {
        if (!running) {
            return;
        }
//        out.println();
        try {
            reader.getHistory().flush();
        } catch (IOException e) {
            // ignore
        }

        running = false;
        if (thread != Thread.currentThread()) {
            thread.interrupt();
        }
        if (closeCallback != null) {
            closeCallback.run();
        }
        if (terminal instanceof AutoCloseable) {
            try {
                ((AutoCloseable) terminal).close();
            } catch (Exception e) {
                // Ignore
            }
        }
        if (session != null)
            session.close();
    }

    public void run() {
        try {
            threadIO.setStreams(session.getKeyboard(), out, err);
            thread = Thread.currentThread();
            running = true;
            Properties brandingProps = Branding.loadBrandingProperties(terminal);
            welcome(brandingProps);
            setSessionProperties(brandingProps);
            jlineTerminal.handle(Signal.INT, s -> {
                Job current = session.foregroundJob();
                if (current != null) {
                    current.interrupt();
                }
            });

            String scriptFileName = System.getProperty(SHELL_INIT_SCRIPT);
            executeScript(scriptFileName);
            while (running) {
                try {
                    String command = reader.readLine(getPrompt());
                    Object result = session.execute(command);
                    if (result != null) {
                        session.getConsole().println(session.format(result, Converter.INSPECT));
                    }
                } catch (UserInterruptException e) {
                    // Ignore, loop again
                } catch (EndOfFileException e) {
                    break;
                } catch (Throwable t) {
                    ShellUtil.logException(this, t);
                }
            }
            close();
        } finally {
            try {
                threadIO.close();
            } catch (Throwable t) {
                // Ignore
            }
        }
    }

    @Override
    public Object execute(CharSequence commandline) throws Exception {
        String command = CommandLineParser.parse(this, commandline.toString());
        return session.execute(command);
    }

    @Override
    public Object get(String name) {
        return session.get(name);
    }

    @Override
    public void put(String name, Object value) {
        session.put(name, value);
    }

    @Override
    public InputStream getKeyboard() {
        return session.getKeyboard();
    }

    @Override
    public PrintStream getConsole() {
        return session.getConsole();
    }

    @Override
    public String resolveCommand(String name) {
        // TODO: optimize
        if (!name.contains(":")) {
            String[] scopes = ((String) get(Session.SCOPE)).split(":");
            List<Command> commands = registry.getCommands();
            for (String scope : scopes) {
                for (Command command : commands) {
                    if ((Session.SCOPE_GLOBAL.equals(scope) || command.getScope().equals(scope)) && command.getName().equals(name)) {
                        return command.getScope() + ":" + name;
                    }
                }
            }
        }
        return name;
    }

    @Override
    public String readLine(String prompt, Character mask) throws IOException {
        return reader.readLine(prompt, mask);
    }

    private String loadCompletionMode() {
        String mode;
        try {
            File shellCfg = new File(System.getProperty("karaf.etc"), "/org.apache.karaf.shell.cfg");
            Properties properties = new Properties();
            properties.load(new FileInputStream(shellCfg));
            mode = (String) properties.get("completionMode");
            if (mode == null) {
                LOGGER.debug("completionMode property is not defined in etc/org.apache.karaf.shell.cfg file. Using default completion mode.");
                mode = Session.COMPLETION_MODE_GLOBAL;
            }
        } catch (Exception e) {
            LOGGER.warn("Can't read {}/org.apache.karaf.shell.cfg file. The completion is set to default.", System.getProperty("karaf.etc"));
            mode = Session.COMPLETION_MODE_GLOBAL;
        }
        return mode;
    }

    private void executeScript(String scriptFileName) {
        if (scriptFileName != null) {
            try {
                String script = String.join("\n",
                        Files.readAllLines(Paths.get(scriptFileName)));
                session.execute(script);
            } catch (Exception e) {
                LOGGER.debug("Error in initialization script", e);
                System.err.println("Error in initialization script: " + e.getMessage());
            }
        }
    }

    protected void welcome(Properties brandingProps) {
        String welcome = brandingProps.getProperty("welcome");
        if (welcome != null && welcome.length() > 0) {
            session.getConsole().println(welcome);
        }
    }

    protected void setSessionProperties(Properties brandingProps) {
        for (Map.Entry<Object, Object> entry : brandingProps.entrySet()) {
            String key = (String) entry.getKey();
            if (key.startsWith("session.")) {
                session.put(key.substring("session.".length()), entry.getValue());
            }
        }
    }

    protected String getPrompt() {
        try {
            String prompt;
            try {
                Object p = session.get(PROMPT);
                if (p != null) {
                    prompt = p.toString();
                } else {
                    Properties properties = Branding.loadBrandingProperties(terminal);
                    if (properties.getProperty("prompt") != null) {
                        prompt = properties.getProperty("prompt");
                        // we put the PROMPT in ConsoleSession to avoid to read
                        // the properties file each time.
                        session.put(PROMPT, prompt);
                    } else {
                        prompt = DEFAULT_PROMPT;
                    }
                }
            } catch (Throwable t) {
                prompt = DEFAULT_PROMPT;
            }
            Matcher matcher = Pattern.compile("\\$\\{([^}]+)\\}").matcher(prompt);
            while (matcher.find()) {
                Object rep = session.get(matcher.group(1));
                if (rep != null) {
                    prompt = prompt.replace(matcher.group(0), rep.toString());
                    matcher.reset(prompt);
                }
            }
            return prompt;
        } catch (Throwable t) {
            return "$ ";
        }
    }

    private String getPid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        String[] parts = name.split("@");
        return parts[0];
    }

}
