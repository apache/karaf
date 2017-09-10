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
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.felix.gogo.jline.ParsedLineImpl;
import org.apache.felix.gogo.jline.Shell;
import org.apache.felix.gogo.runtime.CommandSessionImpl;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.apache.felix.service.command.Function;
import org.apache.felix.service.command.Job;
import org.apache.felix.service.command.Job.Status;
import org.apache.felix.service.threadio.ThreadIO;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.History;
import org.apache.karaf.shell.api.console.Registry;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.shell.api.console.Terminal;
import org.apache.karaf.shell.impl.action.command.ActionCommand;
import org.apache.karaf.shell.impl.console.parsing.CommandLineParser;
import org.apache.karaf.shell.impl.console.parsing.KarafParser;
import org.apache.karaf.shell.support.ShellUtil;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.apache.karaf.shell.support.completers.FileOrUriCompleter;
import org.apache.karaf.shell.support.completers.UriCompleter;
import org.apache.karaf.util.filesstream.FilesStream;
import org.jline.builtins.Completers;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.LineReaderCallback;
import org.jline.reader.ParsedLine;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.impl.DumbTerminal;
import org.jline.utils.AttributedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.felix.gogo.jline.Shell.VAR_SCOPE;

public class ConsoleSessionImpl implements Session {

    private static final String SUPPRESS_WELCOME = "karaf.shell.suppress.welcome";
    public static final String SHELL_INIT_SCRIPT = "karaf.shell.init.script";
    public static final String SHELL_HISTORY_MAXSIZE = "karaf.shell.history.maxSize";
    public static final String PROMPT = "PROMPT";
    public static final String DEFAULT_PROMPT = "\u001B[1m${USER}\u001B[0m@${APPLICATION}(${SUBSHELL})> ";
    public static final String RPROMPT = "RPROMPT";
    public static final String DEFAULT_RPROMPT = null;

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleSessionImpl.class);

    // Input stream
    volatile boolean running;

    private AtomicBoolean closed = new AtomicBoolean(false);

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
    final LineReader reader;

    private Thread thread;
    private Properties brandingProps;

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

        brandingProps = Branding.loadBrandingProperties(terminal);

        // Create session
        session = processor.createSession(jlineTerminal.input(),
                jlineTerminal.output(),
                jlineTerminal.output());

        // Completers
        Completer builtinCompleter = createBuiltinCompleter();
        CommandsCompleter commandsCompleter = new CommandsCompleter(factory, this);
        Completer completer =  (rdr, line, candidates) -> {
            builtinCompleter.complete(rdr, line, candidates);
            commandsCompleter.complete(rdr, line, candidates);
        };

        // Console reader
        reader = LineReaderBuilder.builder()
                    .terminal(jlineTerminal)
                    .appName("karaf")
                    .variables(((CommandSessionImpl) session).getVariables())
                    .highlighter(new org.apache.felix.gogo.jline.Highlighter(session))
                    .parser(new KarafParser(this))
                    .completer(completer)
                    .build();

        // History
        final Path file = getHistoryFile();
        reader.setVariable(LineReader.HISTORY_FILE, file);
        String maxSizeStr = System.getProperty(SHELL_HISTORY_MAXSIZE);
        if (maxSizeStr != null) {
            reader.setVariable(LineReader.HISTORY_SIZE, Integer.parseInt(maxSizeStr));
        }
        history = new HistoryWrapper(reader.getHistory());

        // Registry
        registry = new RegistryImpl(factory.getRegistry());
        registry.register(factory);
        registry.register(this);
        registry.register(registry);
        registry.register(terminal);
        registry.register(history);

        registry.register(commandsCompleter);
        registry.register(new CommandNamesCompleter());
        registry.register(new FileCompleter());
        registry.register(new UriCompleter());
        registry.register(new FileOrUriCompleter());

        // Session
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
        session.put(Shell.VAR_COMPLETIONS, new HashMap<>());
        session.put(Shell.VAR_READER, reader);
        session.put(Shell.VAR_TERMINAL, reader.getTerminal());
        session.put(CommandSession.OPTION_NO_GLOB, Boolean.TRUE);
        session.currentDir(Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize());


    }

    private Completer createBuiltinCompleter() {
        Completers.CompletionEnvironment env = new Completers.CompletionEnvironment() {
            @Override
            public Map<String, List<Completers.CompletionData>> getCompletions() {
                return Shell.getCompletions(session);
            }
            @Override
            public Set<String> getCommands() {
                return factory.getRegistry().getCommands().stream()
                        .map(c -> c.getScope() + ":" + c.getName())
                        .collect(Collectors.toSet());
            }
            @Override
            public String resolveCommand(String command) {
                String resolved = command;
                if (command.indexOf(':') < 0) {
                    Set<String> commands = getCommands();
                    Object path = session.get(VAR_SCOPE);
                    String scopePath = (null == path ? "*" : path.toString());
                    for (String scope : scopePath.split(":")) {
                        for (String entry : commands) {
                            if ("*".equals(scope) && entry.endsWith(":" + command)
                                    || entry.equals(scope + ":" + command)) {
                                resolved = entry;
                                break;
                            }
                        }
                    }
                }
                return resolved;
            }
            @Override
            public String commandName(String command) {
                int idx = command.indexOf(':');
                return idx >= 0 ? command.substring(idx + 1) : command;
            }
            @Override
            public Object evaluate(LineReader reader, ParsedLine line, String func) throws Exception {
                session.put(Shell.VAR_COMMAND_LINE, line);
                return session.execute(func);
            }
        };
        return new org.jline.builtins.Completers.Completer(env);
    }

    /**
     * Subclasses can override to use a different history file.
     *
     * @return the history file
     */
    protected Path getHistoryFile() {
        String defaultHistoryPath = new File(System.getProperty("user.home"), ".karaf/karaf41.history").toString();
        return Paths.get(System.getProperty("karaf.history", defaultHistoryPath));
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

    @Override
    public Path currentDir() {
        return session.currentDir();
    }

    @Override
    public void currentDir(Path path) {
        session.currentDir(path);
    }

    public void close() {
        if (closed.compareAndSet(false, true)) {
            if (running) {
                try {
                    reader.getHistory().save();
                } catch (IOException e) {
                    // ignore
                }
            }

            running = false;
            if (thread != Thread.currentThread() && thread != null) {
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
    }

    public void run() {
        try {
            threadIO.setStreams(session.getKeyboard(), out, err);
            thread = Thread.currentThread();
            running = true;
            welcomeBanner();

            AtomicBoolean reading = new AtomicBoolean();

            session.setJobListener((job, previous, current) -> {
                if (previous == Status.Background || current == Status.Background
                        || previous == Status.Suspended || current == Status.Suspended) {
                    int width = terminal.getWidth();
                    String status = current.name().toLowerCase();
                    jlineTerminal.writer().write(getStatusLine(job, width, status));
                    jlineTerminal.flush();
                    if (reading.get()) {
                        reader.callWidget(LineReader.REDRAW_LINE);
                        reader.callWidget(LineReader.REDISPLAY);
                    }
                }
            });
            jlineTerminal.handle(Signal.TSTP, s -> {
                Job current = session.foregroundJob();
                if (current != null) {
                    current.suspend();
                }
            });
            jlineTerminal.handle(Signal.INT, s -> {
                Job current = session.foregroundJob();
                if (current != null) {
                    current.interrupt();
                }
            });

            String scriptFileName = System.getProperty(SHELL_INIT_SCRIPT);
            executeScript(scriptFileName);
            while (running) {
                CharSequence command = readCommand(reading);
                if (command == null) {
                    break;
                }
                doExecute(command);
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

    /**
     * On the local console we only show the welcome banner once. This allows to suppress the banner
     * on refreshs of the shell core bundle. 
     * On ssh we show it every time.
     */
    private void welcomeBanner() {
        if (!isLocal() || System.getProperty(SUPPRESS_WELCOME) == null) {
            welcome(brandingProps);
            setSessionProperties(brandingProps);
            if (isLocal()) {
                System.setProperty(SUPPRESS_WELCOME, "true");
            }
        }
    }

    private boolean isLocal() {
        Boolean isLocal = (Boolean)session.get(Session.IS_LOCAL);
        return isLocal != null && isLocal;
    }

    private CharSequence readCommand(AtomicBoolean reading) throws UserInterruptException {
        CharSequence command = null;
        reading.set(true);
        try {
            // TODO build list as Commands are registered/unregisters in org.apache.karaf.shell.impl.action.command.ManagerImpl for example
            // instead of going through the whole list every time
            List<ActionCommand> censoredCommands = this.getRegistry().getCommands().stream()
                .filter(ActionCommand.class::isInstance)
                .map(ActionCommand.class::cast)
                .filter(e -> getCensoredArgument(e) != null)
                .collect(Collectors.toList());
            AggregateLineReaderCallback aggregateLineReaderCallback = new AggregateLineReaderCallback();
            for (ActionCommand censoredOptionsCommand : censoredCommands) {
                Argument censoredArgument = getCensoredArgument(censoredOptionsCommand);
                aggregateLineReaderCallback.addArgumentMask(censoredOptionsCommand.getName(), censoredArgument.index() + 1, censoredArgument.mask());
            }
            reader.readLine(getPrompt(), getRPrompt(), null, null);
            ParsedLine pl = reader.getParsedLine();
            if (pl instanceof ParsedLineImpl) {
                command = ((ParsedLineImpl) pl).program();
            } else {
                command = pl.line();
            }
        } catch (EndOfFileException e) {
            command = null;
        } catch (UserInterruptException e) {
            command = ""; // Do nothing
        } catch (Throwable t) {
            ShellUtil.logException(this, t);
        } finally {
            reading.set(false);
        }
        return command;
    }

    private Argument getCensoredArgument(ActionCommand e) {
        for (Class<?> type = e.getActionClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                Argument argument = field.getAnnotation(Argument.class);
                if (argument != null) {
                    if(argument.censor()) {
                        return argument;
                    }
                }
            }
        }
        return null;
    }

    private void doExecute(CharSequence command) {
        try {
            Object result = session.execute(command);
            if (result != null) {
                session.getConsole().println(session.format(result, Converter.INSPECT));
            }
        } catch (InterruptedException e) {
            LOGGER.debug("Console session is closed");
        } catch (Throwable t) {
            ShellUtil.logException(this, t);
        }
    }

    private String getStatusLine(Job job, int width, String status) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < width - 1; i++) {
            sb.append(' ');
        }
        sb.append('\r');
        sb.append("[").append(job.id()).append("]  ");
        sb.append(status);
        for (int i = status.length(); i < "background".length(); i++) {
            sb.append(' ');
        }
        sb.append("  ").append(job.command()).append("\n");
        return sb.toString();
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
        try {
            reader.getVariables().put(LineReader.DISABLE_HISTORY, Boolean.TRUE);
            return reader.readLine(prompt, mask);
        } finally {
            reader.getVariables().remove(LineReader.DISABLE_HISTORY);
        }
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

    private void executeScript(String names) {
        FilesStream.stream(names).forEach(this::doExecuteScript);
    }

    private void doExecuteScript(Path scriptFileName) {
        try {
            String script = String.join("\n",
                    Files.readAllLines(scriptFileName));
            session.execute(script);
        } catch (Exception e) {
            LOGGER.debug("Error in initialization script {}", scriptFileName, e);
            System.err.println("Error in initialization script: " + scriptFileName + ": " + e.getMessage());
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
        return doGetPrompt(PROMPT, DEFAULT_PROMPT);
    }

    protected String getRPrompt() {
        return doGetPrompt(RPROMPT, DEFAULT_RPROMPT);
    }

    protected String doGetPrompt(String var, String def) {
        try {
            String prompt;
            try {
                Object p = session.get(var);
                if (p != null) {
                    prompt = p.toString();
                } else {
                    var = var.toLowerCase();
                    p = session.get(var);
                    if (p != null) {
                        prompt = p.toString();
                    } else {
                        if (brandingProps.getProperty(var) != null) {
                            prompt = brandingProps.getProperty(var);
                            // we put the PROMPT in ConsoleSession to avoid to read
                            // the properties file each time.
                            session.put(var, prompt);
                        } else {
                            prompt = def;
                        }
                    }
                }
            } catch (Throwable t) {
                prompt = def;
            }
            if (prompt != null) {
                Matcher matcher = Pattern.compile("\\$\\{([^}]+)\\}").matcher(prompt);
                while (matcher.find()) {
                    Object rep = session.get(matcher.group(1));
                    if (rep != null) {
                        prompt = prompt.replace(matcher.group(0), rep.toString());
                        matcher.reset(prompt);
                    }
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
    
    private class AggregateLineReaderCallback implements LineReaderCallback{
        
        List<RegExpReplacer> regex = new ArrayList<>();
        
        public void addArgumentMask(String command, int pos, Character mask){
            regex.add(new RegExpReplacer(command, pos, mask));
        }

        @Override
        public String onDisplayLine(String line) {
            for (RegExpReplacer regExpReplacer : regex) {
                if(regExpReplacer.matches(line)) {
                    return regExpReplacer.filter(line); 
                }
            }
            return line;
        }

        @Override
        public String onAddLineToHistory(String line) {
            for (RegExpReplacer regExpReplacer : regex) {
                if(regExpReplacer.matches(line)) {
                    return regExpReplacer.filter(line);
                }
            }
            return line;
        }

        @Override
        public String onHighlightLine(String line) {
            for (RegExpReplacer regExpReplacer : regex) {
                if(regExpReplacer.matches(line)) {
                    return regExpReplacer.filter(line);
                }
            }
            return line;
        }
    }

    private static class RegExpReplacer {
        private Pattern pattern;
        private int group = 1;
        private Character replacement;

        public RegExpReplacer(String command, int group, Character replacement) {
            StringBuilder regex = new StringBuilder();
            regex.append(Pattern.quote(command));
            for (int i = 0; i < group; i++) {
                regex.append(" +([^ ]+)");
            }
            this.pattern = Pattern.compile(regex.toString());
            this.group = group;
            this.replacement = replacement;
        }
        
        public String filter(String line) {
            Matcher m = pattern.matcher(line);
            if( m.find() ) {
                StringBuilder maskedLine = new StringBuilder(line);
                for(int i = m.start(group); i < m.end(group); i++){
                    maskedLine.replace(i,i+1, String.valueOf(replacement));
                }
                return maskedLine.toString();
            } else {
                return line;
            }
        }
        
        public boolean matches(String line){
            return pattern.matcher(line).find();
        }
}
     

}
