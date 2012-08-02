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

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.KeyPair;
import java.security.PrivilegedExceptionAction;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.security.auth.Subject;

import jline.Terminal;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.apache.karaf.jaas.modules.UserPrincipal;
import org.apache.sshd.agent.SshAgent;
import org.apache.sshd.agent.local.AgentImpl;
import org.fusesource.jansi.AnsiConsole;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleFactory.class);

    private BundleContext bundleContext;
    private CommandProcessor commandProcessor;
    private TerminalFactory terminalFactory;
    private Console console;
    private boolean start;
    private ServiceRegistration registration;
    private SshAgent local;

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public synchronized void registerCommandProcessor(CommandProcessor commandProcessor) throws Exception {
        this.commandProcessor = commandProcessor;
        start();
    }

    public synchronized void unregisterCommandProcessor(CommandProcessor commandProcessor) throws Exception {
        this.commandProcessor = null;
        stop();
    }

    public void setTerminalFactory(TerminalFactory terminalFactory) {
        this.terminalFactory = terminalFactory;
    }

    public void setStart(boolean start) {
        this.start = start;
    }

    protected void start() throws Exception {
        if (start) {
            Subject subject = new Subject();
            final String user = "karaf";
            subject.getPrincipals().add(new UserPrincipal(user));
            Subject.doAs(subject, new PrivilegedExceptionAction<Object>() {
                public Object run() throws Exception {
                    doStart(user);
                    return null;
                }
            });
        }
    }

    public static Object invokePrivateMethod(Object o, String methodName, Object[] params) throws Exception {
        final Method methods[] = o.getClass().getDeclaredMethods();
        for (int i = 0; i < methods.length; ++i) {
            if (methodName.equals(methods[i].getName())) {
                methods[i].setAccessible(true);
                return methods[i].invoke(o, params);
            }
        }
        return null;
    }
    
    private static <T> T unwrapBIS(T stream) {
        try {
             return (T) invokePrivateMethod(stream, "getInIfOpen", null);
        } catch (Throwable t) {
             return stream;
        }
    }

    protected void doStart(String user) throws Exception {
        String agentId = startAgent(user);

        final Terminal terminal = terminalFactory.getTerminal();
        // unwrap stream so it can be recognized by the terminal and wrapped to get 
        // special keys in windows
        InputStream unwrappedIn = unwrapBIS(unwrap(System.in));
        InputStream in = terminal.wrapInIfNeeded(unwrappedIn);
        PrintStream out = unwrap(System.out);
        PrintStream err = unwrap(System.err);
        Runnable callback = new Runnable() {
            public void run() {
                try {
                    bundleContext.getBundle(0).stop();
                } catch (Exception e) {
                    // Ignore
                }
            }
        };
        this.console = new Console(commandProcessor,
                                   in,
                                   wrap(out),
                                   wrap(err),
                                   terminal,
                                   callback);
        CommandSession session = console.getSession();
        session.put("USER", user);
        session.put("APPLICATION", System.getProperty("karaf.name", "root"));
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
        session.put(SshAgent.SSH_AUTHSOCKET_ENV_NAME, agentId);

        new DelayedStarted(unwrappedIn).start();
    }

    class DelayedStarted extends Thread implements FrameworkListener {
        final AtomicBoolean started = new AtomicBoolean(false);
        final InputStream in;
        DelayedStarted(InputStream in) {
            super("Karaf Shell Console Thread");
            this.in = in;
            int defaultStartLevel = Integer.parseInt(System.getProperty(Constants.FRAMEWORK_BEGINNING_STARTLEVEL));
            int startLevel = bundleContext.getBundle(0).adapt(FrameworkStartLevel.class).getStartLevel();
            if (startLevel >= defaultStartLevel) {
                started.set(true);
            } else {
                bundleContext.addFrameworkListener(this);
                frameworkEvent(new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(), null));
            }
        }

        public void run() {
            try {
                while (!started.get()) {
                    if (in.available() == 0) {
                        Thread.sleep(10);
                    }
                    while (in.available() > 0) {
                        char ch = (char) in.read();
                        if (ch == '\r' || ch == '\n') {
                            started.set(true);
                            break;
                        }
                    }
                }
            } catch (Throwable t) {
                // Ignore
            }
            System.setProperty("karaf.console.started", "true");
            System.out.println();
            bundleContext.removeFrameworkListener(this);
            console.run();
        }
        public void frameworkEvent(FrameworkEvent event) {
            if (event.getType() == FrameworkEvent.STARTLEVEL_CHANGED) {
                int defaultStartLevel = Integer.parseInt(System.getProperty(Constants.FRAMEWORK_BEGINNING_STARTLEVEL));
                int startLevel = bundleContext.getBundle(0).adapt(FrameworkStartLevel.class).getStartLevel();
                if (startLevel >= defaultStartLevel) {
                    started.set(true);
                }
            }
        }
    }

    protected String startAgent(String user) {
        try {
            local = new AgentImpl();
            URL url = bundleContext.getBundle().getResource("karaf.key");
            InputStream is = url.openStream();
            ObjectInputStream r = new ObjectInputStream(is);
            KeyPair keyPair = (KeyPair) r.readObject();
            local.addIdentity(keyPair, "karaf");
            String agentId = "local:" + user;
            Hashtable properties = new Hashtable();
            properties.put("id", agentId);
            registration = bundleContext.registerService(SshAgent.class.getName(), local, properties);
            return agentId;
        } catch (Throwable e) {
            LOGGER.warn("Error starting ssh agent for local console", e);
            return null;
        }
    }

    protected void stop() throws Exception {
        if (registration != null) {
            registration.unregister();
        }
        // The bundle is stopped
        // so close the console and remove the callback so that the
        // osgi framework isn't stopped
        if (console != null) {
            console.close(false);
        }
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
}
