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
package org.apache.karaf.shell.console.impl.jline;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jline.Terminal;
import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;
import jline.console.history.PersistentHistory;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.apache.karaf.shell.console.CloseShellException;
import org.apache.karaf.shell.console.CommandSessionHolder;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.Console;
import org.apache.karaf.shell.console.SessionProperties;
import org.apache.karaf.shell.console.completer.CommandsCompleter;
import org.apache.karaf.shell.security.impl.SecuredCommandProcessorImpl;
import org.apache.karaf.shell.util.ShellUtil;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleImpl implements Console
{
    public static final String SHELL_INIT_SCRIPT = "karaf.shell.init.script";
    public static final String PROMPT = "PROMPT";
    public static final String DEFAULT_PROMPT = "\u001B[1m${USER}\u001B[0m@${APPLICATION}(${SUBSHELL})> ";
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
    private final BundleContext bundleContext;

    public ConsoleImpl(CommandProcessor processor,
                   InputStream in,
                   PrintStream out,
                   PrintStream err,
                   Terminal term,
                   String encoding,
                   Runnable closeCallback,
                   BundleContext bc)
    {
        this.in = in;
        this.out = out;
        this.err = err;
        this.queue = new ArrayBlockingQueue<Integer>(1024);
        this.terminal = term == null ? new UnsupportedTerminal() : term;
        this.consoleInput = new ConsoleInputStream();
        this.session = new DelegateSession();

        this.session.put("SCOPE", "shell:bundle:*");
        this.session.put("SUBSHELL", "");
        this.closeCallback = closeCallback;
        this.bundleContext = bc;

        try {
            reader = new ConsoleReader(null,
                                       this.consoleInput,
                                       this.out,
                                       this.terminal,
                                       encoding);
        } catch (IOException e) {
            throw new RuntimeException("Error opening console reader", e);
        }

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
        if (closedByUser && closeCallback != null)
        {
            closeCallback.run();
        }
    }

    public void run()
    {
        SecuredCommandProcessorImpl secCP = createSecuredCommandProcessor();

        thread = Thread.currentThread();
        CommandSessionHolder.setSession(session);
        running = true;
        pipe.start();
        Properties brandingProps = Branding.loadBrandingProperties(terminal);
        welcome(brandingProps);
        setSessionProperties(brandingProps);
        String scriptFileName = System.getProperty(SHELL_INIT_SCRIPT);
        executeScript(scriptFileName);
        while (running) {
            try {
                String command = readAndParseCommand();
                if (command == null) {
                    break;
                }
                //session.getConsole().println("Executing: " + line);
                Object result = session.execute(command);
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
            catch (CloseShellException e)
            {
                break;
            }
            catch (Throwable t)
            {
                ShellUtil.logException(session, t);
            }
        }

        secCP.close();
        close(true);
    }

    SecuredCommandProcessorImpl createSecuredCommandProcessor() {
        if (!(session instanceof DelegateSession)) {
            throw new IllegalStateException("Should be an Delegate Session here, about to set the delegate");
        }
        DelegateSession is = (DelegateSession) session;

        // make it active
        SecuredCommandProcessorImpl secCP = new SecuredCommandProcessorImpl(bundleContext);
        CommandSession s = secCP.createSession(consoleInput, out, err);

        // Before the session is activated attributes may have been set on it. Pass these on to the real session now
        is.setDelegate(s);
        return secCP;
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
                // jline doesn't add blank lines to the history so we don't
                // need to replace the command in jline's console history with
                // an indented one
                if (command.length() > 0 && !" ".equals(command)) {
                    reader.getHistory().replace(command);
                }
            }
            if (command.length() > 0 && command.charAt(command.length() - 1) == '\\') {
                loop = true;
                first = false;
            } else {
                try {
                    Class<?> cl = CommandSession.class.getClassLoader().loadClass("org.apache.felix.gogo.runtime.Parser");
                    Object parser = cl.getConstructor(CharSequence.class).newInstance(command);
                    cl.getMethod("program").invoke(parser);
                    loop = false;
                } catch (Exception e) {
                    loop = true;
                    first = false;
                } catch (Throwable t) {
                    // Reflection problem ? just quit
                    loop = false;
                }
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

    protected Completer createCompleter() {
        return new CommandsCompleter(session);
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
                        else if (c == 4 && !ShellUtil.getBoolean(session, SessionProperties.IGNORE_INTERRUPTS))
                        {
                            err.println("^D");
                            return;
                        }
                        else if (c == 3 && !ShellUtil.getBoolean(session, SessionProperties.IGNORE_INTERRUPTS))
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

    static class DelegateSession implements CommandSession {
        final Map<String, Object> attrs = new HashMap<String, Object>();
        volatile CommandSession delegate;

        @Override
        public Object execute(CharSequence commandline) throws Exception {
            if (delegate != null)
                return delegate.execute(commandline);

            throw new UnsupportedOperationException();
        }

        void setDelegate(CommandSession s) {
            synchronized (this) {
                for (Map.Entry<String, Object> entry : attrs.entrySet()) {
                    s.put(entry.getKey(), entry.getValue());
                }
            }
            delegate = s;
        }

        @Override
        public void close() {
            if (delegate != null)
                delegate.close();
        }

        @Override
        public InputStream getKeyboard() {
            if (delegate != null)
                return delegate.getKeyboard();

            throw new UnsupportedOperationException();
        }

        @Override
        public PrintStream getConsole() {
            if (delegate != null)
                return delegate.getConsole();

            throw new UnsupportedOperationException();
        }

        @Override
        public Object get(String name) {
            if (delegate != null)
                return delegate.get(name);

            return attrs.get(name);
        }

        // You can put attributes on this session before it's delegate is set...
        @Override
        public void put(String name, Object value) {
            if (delegate != null) {
                delegate.put(name, value);
                return;
            }

            // There is no delegate yet, so we'll keep the attributes locally
            synchronized (this) {
                attrs.put(name, value);
            }
        }

        @Override
        public CharSequence format(Object target, int level) {
            if (delegate != null)
                return delegate.format(target, level);

            throw new UnsupportedOperationException();
        }

        @Override
        public Object convert(Class<?> type, Object instance) {
            if (delegate != null)
                return delegate.convert(type, instance);

            throw new UnsupportedOperationException();
        }
    }
}
