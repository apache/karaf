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
package org.apache.karaf.shell.console.jline;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jline.Terminal;
import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;
import jline.console.history.FileHistory;
import jline.console.history.PersistentHistory;
import org.apache.felix.gogo.commands.CommandException;
import org.apache.felix.gogo.runtime.CommandNotFoundException;
import org.apache.felix.gogo.runtime.Parser;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.apache.karaf.shell.console.CloseShellException;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.CommandsCompleter;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Console implements Runnable
{

    public static final String SHELL_INIT_SCRIPT = "karaf.shell.init.script";
    public static final String PROMPT = "PROMPT";
    public static final String DEFAULT_PROMPT = "\u001B[1m${USER}\u001B[0m@${APPLICATION}> ";
    public static final String PRINT_STACK_TRACES = "karaf.printStackTraces";
    public static final String LAST_EXCEPTION = "karaf.lastException";
    public static final String IGNORE_INTERRUPTS = "karaf.ignoreInterrupts";

    private static final Logger LOGGER = LoggerFactory.getLogger(Console.class);

    protected CommandSession session;
    private ConsoleReader reader;
    private BlockingQueue<Integer> queue;
    private boolean interrupt;
    private Thread pipe;
    volatile private boolean running;
    volatile private boolean eof;
    private Runnable closeCallback;
    private Terminal terminal;
    private InputStream consoleInput;
    private InputStream in;
    private PrintStream out;
    private PrintStream err;
    private Thread thread;

    public Console(CommandProcessor processor,
                   InputStream in,
                   PrintStream out,
                   PrintStream err,
                   Terminal term,
                   Runnable closeCallback) throws Exception
    {
        this.in = in;
        this.out = out;
        this.err = err;
        this.queue = new ArrayBlockingQueue<Integer>(1024);
        this.terminal = term == null ? new UnsupportedTerminal() : term;
        this.consoleInput = new ConsoleInputStream();
        this.session = processor.createSession(this.consoleInput, this.out, this.err);
        this.session.put("SCOPE", "shell:osgi:*");
        this.closeCallback = closeCallback;

        reader = new ConsoleReader(this.consoleInput,
                                   this.out,
                                   this.terminal);

        final File file = getHistoryFile();
        try {
            file.getParentFile().mkdirs();
        	reader.setHistory(new KarafFileHistory(file));
		} catch (Exception e) {
			LOGGER.error("Can not read history from file " + file + ". Using in memory history", e);
		}
        session.put(".jline.reader", reader);
        session.put(".jline.history", reader.getHistory());
        Completer completer = createCompleter();
        if (completer != null) {
            reader.addCompleter(new CompleterAsCompletor(completer));
        }
        pipe = new Thread(new Pipe());
        pipe.setName("gogo shell pipe thread");
        pipe.setDaemon(true);
    }

    /**
     * Subclasses can override to use a different history file.
     * @return
     */
    protected File getHistoryFile() {
        String defaultHistoryPath = new File(System.getProperty("user.home"), ".karaf/karaf.history").toString();
        return new File(System.getProperty("karaf.history", defaultHistoryPath));
    }

    public CommandSession getSession() {
        return session;
    }

    public void close(boolean closedByUser) {
        //System.err.println("Closing");
        if (!running) {
            return;
        }
        if (reader.getHistory() instanceof PersistentHistory) {
            try {
                ((PersistentHistory) reader.getHistory()).flush();
            } catch (IOException e) {
                // ignore
            }
        }
        running = false;
        CommandSessionHolder.unset();
        pipe.interrupt();
        if (closedByUser && closeCallback != null) {
            closeCallback.run();
        }
    }

    public void run()
    {
        ThreadLocal<CommandSessionHolder> consoleState = new ThreadLocal<CommandSessionHolder>();
        thread = Thread.currentThread();
        CommandSessionHolder.setSession(session);
        running = true;
        pipe.start();
        welcome();
        setSessionProperties();
        String scriptFileName = System.getProperty(SHELL_INIT_SCRIPT);
        executeScript(scriptFileName);
        while (running) {
            try {
                String command = readAndParseCommand();
                if (command == null) {
                    break;
                }
                Object result = session.execute(command);
                if (result != null) {
                    session.getConsole().println(session.format(result, Converter.INSPECT));
                }
            }
            catch (InterruptedIOException e) {
                // System.err.println("^C");
                // TODO: interrupt current thread
            }
            catch (CloseShellException e) {
                break;
            }
            catch (Throwable t) {
                logException(t);
            }
        }
        close(true);
        //System.err.println("Exiting console...");
        if (closeCallback != null)
        {
            closeCallback.run();
        }
    }

    private void logException(Throwable t) {
        try {
            if (t instanceof CommandNotFoundException) {
                LOGGER.debug("Unknown command entered", t);
            } else {
                LOGGER.info("Exception caught while executing command", t);
            }
            session.put(LAST_EXCEPTION, t);
            if (t instanceof CommandException) {
                session.getConsole().println(((CommandException) t).getNiceHelp());
            } else if (t instanceof CommandNotFoundException) {
                String str = Ansi.ansi()
                        .fg(Ansi.Color.RED)
                        .a("Command not found: ")
                        .a(Ansi.Attribute.INTENSITY_BOLD)
                        .a(((CommandNotFoundException) t).getCommand())
                        .a(Ansi.Attribute.INTENSITY_BOLD_OFF)
                        .fg(Ansi.Color.DEFAULT).toString();
                session.getConsole().println(str);
            }
            if (getBoolean(PRINT_STACK_TRACES)) {
                session.getConsole().print(Ansi.ansi().fg(Ansi.Color.RED).toString());
                t.printStackTrace(session.getConsole());
                session.getConsole().print(Ansi.ansi().fg(Ansi.Color.DEFAULT).toString());
            } else if (!(t instanceof CommandException) && !(t instanceof CommandNotFoundException)) {
                session.getConsole().print(Ansi.ansi().fg(Ansi.Color.RED).toString());
                session.getConsole().println("Error executing command: "
                    + (t.getMessage() != null ? t.getMessage() : t.getClass().getName()));
                session.getConsole().print(Ansi.ansi().fg(Ansi.Color.DEFAULT).toString());
            }
        } catch (Exception ignore) {
            // ignore
        }
    }

    private String readAndParseCommand() throws IOException {
        String command = null;
        boolean loop = true;
        boolean first = true;
        while (loop) {
            checkInterrupt();
            String line = reader.readLine(first ? getPrompt() : "> ");
            if (line == null)
            {
                break;
            }
            if (command == null) {
                command = line;
            } else {
                command += " " + line;
            }
            if (reader.getHistory().size()==0) {
                reader.getHistory().add(command);
            } else {
                reader.getHistory().replace(command);
            }
            try {
                new Parser(command).program();
                loop = false;
            } catch (Exception e) {
                loop = true;
                first = false;
            }
        }
        return command;
    }

    private void executeScript(String scriptFileName) {
        if (scriptFileName != null) {
            Reader r = null;
            try {
                File scriptFile = new File(scriptFileName);
                r = new InputStreamReader(new FileInputStream(scriptFile));
                CharArrayWriter w = new CharArrayWriter();
                int n;
                char[] buf = new char[8192];
                while ((n = r.read(buf)) > 0) {
                    w.write(buf, 0, n);
                }
                session.execute(new String(w.toCharArray()));
            } catch (Exception e) {
                LOGGER.debug("Error in initialization script", e);
                System.err.println("Error in initialization script: " + e.getMessage());
            } finally {
                if (r != null) {
                    try {
                        r.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }
        }
    }

    protected boolean getBoolean(String name) {
        Object s = session.get(name);
        if (s == null) {
            s = System.getProperty(name);
        }
        if (s == null) {
            return false;
        }
        if (s instanceof Boolean) {
            return (Boolean) s;
        }
        return Boolean.parseBoolean(s.toString());
    }

    protected void welcome() {
        Properties props = loadBrandingProperties();
        String welcome = props.getProperty("welcome");
        if (welcome != null && welcome.length() > 0) {
            session.getConsole().println(welcome);
        }
    }

    protected void setSessionProperties() {
        Properties props = loadBrandingProperties();
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String key = (String) entry.getKey();
            if (key.startsWith("session.")) {
                session.put(key.substring("session.".length()), entry.getValue());
            }
        }
    }

    protected Completer createCompleter() {
        return new CommandsCompleter(session);
    }

    protected Properties loadBrandingProperties() {
        Properties props = new Properties();
        loadProps(props, "org/apache/karaf/shell/console/branding.properties");
        loadProps(props, "org/apache/karaf/branding/branding.properties");
        return props;
    }

    protected void loadProps(Properties props, String resource) {
        InputStream is = null;
        try {
            is = getClass().getClassLoader().getResourceAsStream(resource);
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            // ignore
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Ignore
                }
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
                    Properties properties = loadBrandingProperties();
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

    private void checkInterrupt() throws IOException {
        if (Thread.interrupted() || interrupt) {
            interrupt = false;
            throw new InterruptedIOException("Keyboard interruption");
        }
    }

    private void interrupt() {
        interrupt = true;
        thread.interrupt();
    }

    private class ConsoleInputStream extends InputStream
    {
        private int read(boolean wait) throws IOException
        {
            if (!running) {
                return -1;
            }
            checkInterrupt();
            if (eof && queue.isEmpty()) {
                return -1;
            }
            Integer i;
            if (wait) {
                try {
                    i = queue.take();
                } catch (InterruptedException e) {
                    throw new InterruptedIOException();
                }
                checkInterrupt();
            } else {
                i = queue.poll();
            }
            if (i == null) {
                return -1;
            }
            return i;
        }

        @Override
        public int read() throws IOException
        {
            return read(true);
        }

        @Override
        public int read(byte b[], int off, int len) throws IOException
        {
            if (b == null) {
                throw new NullPointerException();
            } else if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            }

            int nb = 1;
            int i = read(true);
            if (i < 0) {
                return -1;
            }
            b[off++] = (byte) i;
            while (nb < len) {
                i = read(false);
                if (i < 0) {
                    return nb;
                }
                b[off++] = (byte) i;
                nb++;
            }
            return nb;
        }

        @Override
        public int available() throws IOException {
            return queue.size();
        }
    }

    private class Pipe implements Runnable
    {
        public void run()
        {
            try {
                while (running)
                {
                    try
                    {
                        int c = in.read();
                        if (c == -1)
                        {
                            return;
                        }
                        else if (c == 4 && !getBoolean(IGNORE_INTERRUPTS))
                        {
                            err.println("^D");
                            return;
                        }
                        else if (c == 3 && !getBoolean(IGNORE_INTERRUPTS))
                        {
                            err.println("^C");
                            reader.getCursorBuffer().clear();
                            interrupt();
                        }
                        queue.put(c);
                    }
                    catch (Throwable t) {
                        return;
                    }
                }
            }
            finally
            {
                eof = true;
                try
                {
                    queue.put(-1);
                }
                catch (InterruptedException e)
                {
                }
            }
        }
    }

}
