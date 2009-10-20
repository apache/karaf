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
package org.apache.felix.karaf.shell.console.jline;

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
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jline.AnsiWindowsTerminal;
import jline.ConsoleReader;
import jline.Terminal;
import jline.UnsupportedTerminal;
import org.apache.felix.karaf.shell.console.Completer;
import org.apache.felix.karaf.shell.console.completer.AggregateCompleter;
import org.apache.felix.karaf.shell.console.completer.SessionScopeCompleter;
import org.osgi.service.command.CommandProcessor;
import org.osgi.service.command.CommandSession;
import org.osgi.service.command.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Console implements Runnable
{

    public static final String SHELL_INIT_SCRIPT = "karaf.shell.init.script";
    public static final String PROMPT = "PROMPT";
    public static final String DEFAULT_PROMPT = "\u001B[1m${USER}\u001B[0m@${APPLICATION}> ";
    public static final String PRINT_STACK_TRACES = "karaf.printStackTraces";

    private static final Logger LOGGER = LoggerFactory.getLogger(Console.class);

    private CommandSession session;
    private ConsoleReader reader;
    private BlockingQueue<Integer> queue;
    private boolean interrupt;
    private Thread pipe;
    private boolean running;
    private Runnable closeCallback;
    private Terminal terminal;
    private InputStream consoleInput;
    private InputStream in;
    private PrintStream out;
    private PrintStream err;
    private Callable<Boolean> printStackTraces;

    public Console(CommandProcessor processor,
                   InputStream in,
                   PrintStream out,
                   PrintStream err,
                   Terminal term,
                   Completer completer,
                   Runnable closeCallback,
                   Callable<Boolean> printStackTraces) throws Exception
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
        this.printStackTraces = printStackTraces;

        reader = new ConsoleReader(this.consoleInput,
                                   new PrintWriter(this.out),
                                   getClass().getResourceAsStream("keybinding.properties"),
                                   this.terminal);

        File file = new File(System.getProperty("user.home"), ".karaf/karaf.history");
        file.getParentFile().mkdirs();
        reader.getHistory().setHistoryFile(file);
        if (completer != null) {
            reader.addCompletor(
                new CompleterAsCompletor(
                    new AggregateCompleter(
                        Arrays.asList(
                            completer,
                            new SessionScopeCompleter( session, completer )
                        )
                    )
                )
            );
        }
        if (Boolean.getBoolean("jline.nobell")) {
            reader.setBellEnabled(false);
        }
        pipe = new Thread(new Pipe());
        pipe.setName("gogo shell pipe thread");
        pipe.setDaemon(true);
    }

    public CommandSession getSession() {
        return session;
    }

    public void close() {
        //System.err.println("Closing");
        running = false;
        pipe.interrupt();
        Thread.interrupted();
    }

    public void run()
    {
        running = true;
        pipe.start();
        welcome();
        String scriptFileName = System.getProperty(SHELL_INIT_SCRIPT);
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
        while (running) {
            try {
                String line = reader.readLine(getPrompt());
                if (line == null)
                {
                    break;
                }
                //session.getConsole().println("Executing: " + line);
                Object result = session.execute(line);
                if (result != null)
                {
                    session.getConsole().println(session.format(result, Converter.INSPECT));
                }
            }
            catch (InterruptedIOException e)
            {
                //System.err.println("^C");
                // TODO: interrupt current thread
            }
            catch (Throwable t)
            {
                try {
                    if ( printStackTraces.call()) {
                        t.printStackTrace(session.getConsole());
                    }
                    else {
                        session.getConsole().println(t.getMessage());
                    }
                } catch (Exception ignore) {
                        // ignore
                }
            }
        }
        //System.err.println("Exiting console...");
        if (closeCallback != null)
        {
            closeCallback.run();
        }
    }

    protected void welcome() {
        Properties props = new Properties();
        loadProps(props, "org/apache/felix/karaf/shell/console/branding.properties");
        loadProps(props, "org/apache/felix/karaf/branding/branding.properties");
        String welcome = props.getProperty("welcome");
        if (welcome != null && welcome.length() > 0) {
            session.getConsole().println(welcome);
        }
    }

    private void loadProps(Properties props, String resource) {
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
                prompt = p != null ? p.toString() : DEFAULT_PROMPT;
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
        if (interrupt) {
            interrupt = false;
            throw new InterruptedIOException("Keyboard interruption");
        }
    }

    private void interrupt() {
        interrupt = true;
    }

    private class ConsoleInputStream extends InputStream
    {
        private int read(boolean wait) throws IOException
        {
            if (!running) {
                return -1;
            }
            checkInterrupt();
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
                        int c;
                        if (terminal instanceof AnsiWindowsTerminal) {
                            c = ((AnsiWindowsTerminal) terminal).readDirectChar(in);
                        } else {
                            c = terminal.readCharacter(in);
                        }
                        if (c == -1)
                        {
                            queue.put(c);
                            return;
                        }
                        else if (c == 4)
                        {
                            err.println("^D");
                        }
                        else if (c == 3)
                        {
                            err.println("^C");
                            reader.getCursorBuffer().clearBuffer();
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
                close();
            }
        }
    }

}
