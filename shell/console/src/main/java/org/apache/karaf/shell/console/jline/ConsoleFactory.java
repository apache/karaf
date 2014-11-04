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
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.KeyPair;
import java.nio.charset.Charset;
import java.security.PrivilegedExceptionAction;
import java.util.List;

import javax.security.auth.Subject;

import jline.Terminal;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.apache.felix.service.threadio.ThreadIO;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.sshd.agent.SshAgent;
import org.apache.sshd.agent.local.AgentImpl;
import org.apache.karaf.jaas.modules.JaasHelper;
import org.fusesource.jansi.AnsiConsole;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleFactory.class);

    private static final Class[] SECURITY_BUGFIX = {
            JaasHelper.class,
            JaasHelper.OsgiSubjectDomainCombiner.class,
            JaasHelper.DelegatingProtectionDomain.class,
    };

    private BundleContext bundleContext;
    private CommandProcessor commandProcessor;
    private ThreadIO threadIO;
    private TerminalFactory terminalFactory;
    Console console;
    private boolean start;
    private ServiceRegistration registration;


    
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void setCommandProcessor(CommandProcessor commandProcessor) {
        this.commandProcessor = commandProcessor;
    }

    public void setThreadIO(ThreadIO threadIO) {
        this.threadIO = threadIO;
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

    public void start() throws Exception {
        if (start) {
            Subject subject = new Subject();
            String userName = System.getProperty("karaf.local.user");
            if (userName == null) {
                userName = "karaf";
            }
            final String user = userName;
            subject.getPrincipals().add(new UserPrincipal(user));
            String roles = System.getProperty("karaf.local.roles");
            if (roles != null) {
                for (String role : roles.split("[,]")) {
                    subject.getPrincipals().add(new RolePrincipal(role.trim()));
                }
            }
            
            JaasHelper.doAs(subject, new PrivilegedExceptionAction<Object>() {
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
        String encoding = getEncoding();
        this.console = new Console(commandProcessor,
                                   threadIO,
                                   in,
                                   wrap(out),
                                   wrap(err),
                                   terminal,
                                   encoding,
                                   callback,
                                   bundleContext);
        CommandSession session = console.getSession();
        for (Object o : System.getProperties().keySet()) {
            String key = o.toString();
            session.put(key, System.getProperty(key));
        }
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
        if (encoding != null) {
            session.put("LC_CTYPE", encoding);
        }
        session.put(".jline.terminal", terminal);
        session.put("pid", getPid());

        registration = bundleContext.registerService(CommandSession.class, session, null);

        boolean delayconsole = Boolean.parseBoolean(System.getProperty("karaf.delay.console"));
        if (delayconsole) {
            new DelayedStarted(this.console, bundleContext, unwrappedIn).start();
        } else {
            new Thread(this.console, "Karaf Shell Console Thread").start();
        }
    }

    /**
     * Get the default encoding.  Will first look at the LC_CTYPE environment variable, then the input.encoding
     * system property, then the default charset according to the JVM.
     *
     * @return The default encoding to use when none is specified.
     */
    public static String getEncoding() {
        // LC_CTYPE is usually in the form en_US.UTF-8
        String envEncoding = extractEncodingFromCtype(System.getenv("LC_CTYPE"));
        if (envEncoding != null) {
            return envEncoding;
        }
        return System.getProperty("input.encoding", Charset.defaultCharset().name());
    }

    /**
     * Parses the LC_CTYPE value to extract the encoding according to the POSIX standard, which says that the LC_CTYPE
     * environment variable may be of the format <code>[language[_territory][.codeset][@modifier]]</code>
     *
     * @param ctype The ctype to parse, may be null
     * @return The encoding, if one was present, otherwise null
     */
    static String extractEncodingFromCtype(String ctype) {
        if (ctype != null && ctype.indexOf('.') > 0) {
            String encodingAndModifier = ctype.substring(ctype.indexOf('.') + 1);
            if (encodingAndModifier.indexOf('@') > 0) {
                return encodingAndModifier.substring(0, encodingAndModifier.indexOf('@'));
            } else {
                return encodingAndModifier;
            }
        }
        return null;
    }
    
    private String getPid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        String[] parts = name.split("@");
        return parts[0];
    }

    public void stop() throws Exception {
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

    static class SshAgentLoader {
        static SshAgent load(BundleContext bundleContext) {
            try {
                SshAgent agent = new AgentImpl();
                URL url = bundleContext.getBundle().getResource("karaf.key");
                InputStream is = url.openStream();
                ObjectInputStream r = new ObjectInputStream(is);
                KeyPair keyPair = (KeyPair) r.readObject();
                agent.addIdentity(keyPair, "karaf");
                return agent;
            } catch (Throwable e) {
                LOGGER.warn("Error starting ssh agent for local console", e);
                return null;
            }
        }
    }

}
