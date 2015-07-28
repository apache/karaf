/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.ssh;

import java.io.Closeable;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;
import javax.security.auth.Subject;

import jline.Terminal;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.apache.felix.service.threadio.ThreadIO;
import org.apache.karaf.jaas.modules.JaasHelper;
import org.apache.karaf.shell.console.jline.Console;
import org.apache.sshd.common.Factory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.session.ServerSession;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.ReifiedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SSHD {@link org.apache.sshd.server.Command} factory which provides access to Shell.
 */
public class ShellFactoryImpl implements Factory<Command> {

    private static final Class[] SECURITY_BUGFIX = {
            JaasHelper.class,
            JaasHelper.OsgiSubjectDomainCombiner.class,
            JaasHelper.DelegatingProtectionDomain.class,
    };

    private static final Logger LOGGER = LoggerFactory.getLogger(ShellFactoryImpl.class);

    private CommandProcessor commandProcessor;
    private ThreadIO threadIO;
    private BundleContext bundleContext;

    public void setCommandProcessor(CommandProcessor commandProcessor) {
        this.commandProcessor = commandProcessor;
    }

    public void setThreadIO(ThreadIO threadIO) {
        this.threadIO = threadIO;
    }
    
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public Command create() {
        return new ShellImpl();
    }

    public class ShellImpl implements Command, SessionAware
    {
        private InputStream in;

        private OutputStream out;

        private OutputStream err;

        private ExitCallback callback;

        private ServerSession session;

        private boolean closed;

        public void setInputStream(final InputStream in) {
            this.in = in;
        }

        public void setOutputStream(final OutputStream out) {
            this.out = out;
        }

        public void setErrorStream(final OutputStream err) {
            this.err = err;
        }

        public void setExitCallback(ExitCallback callback) {
            this.callback = callback;
        }

        public void setSession(ServerSession session) {
            this.session = session;
        }

        public void start(final Environment env) throws IOException {
            new Thread() {
                @Override
                public void run() {
                    Subject subject = ShellImpl.this.session != null ? ShellImpl.this.session.getAttribute(KarafJaasAuthenticator.SUBJECT_ATTRIBUTE_KEY) : null;
                    if (subject != null) {
                        JaasHelper.doAs(subject, new PrivilegedAction<Object>() {
                            public Object run() {
                                runConsole();
                                return null;
                            }
                        });
                    } else {
                        runConsole();
                    }
                }
                protected void runConsole() {
                    try {
                        final Terminal terminal = new SshTerminal(env);
                        String encoding = getEncoding();
                        final Console console = new Console(commandProcessor,
                                threadIO,
                                in,
                                new PrintStream(new LfToCrLfFilterOutputStream(out), true),
                                new PrintStream(new LfToCrLfFilterOutputStream(err), true),
                                terminal,
                                encoding,
                                new Runnable() {
                                    public void run() {
                                        ShellImpl.this.destroy();
                                    }
                                },
                                bundleContext);
                        final CommandSession session = console.getSession();
                        for (Object o : System.getProperties().keySet()) {
                            String key = o.toString();
                            session.put(key, System.getProperty(key));
                        }
                        session.put("APPLICATION", System.getProperty("karaf.name", "root"));
                        for (Map.Entry<String,String> e : env.getEnv().entrySet()) {
                            session.put(e.getKey(), e.getValue());
                        }
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
                    } catch (Exception e) {
                        LOGGER.warn("Unable to start shell", e);
                        //close the session to notify the ssh client if something wrong
                        //during starting shell
                        try {
                            out.write(("unable to start shell because " + e.getMessage() + "\n").getBytes());
                            out.flush();
                        } catch (IOException e1) {
                            LOGGER.warn("Unable to send back error message", e1);
                        }
                        ShellImpl.this.session.close(true);
                    }
                }
            }.start();
        }

        public void destroy() {
            if (!closed) {
                closed = true;
                ShellFactoryImpl.flush(out, err);
                ShellFactoryImpl.close(in, out, err);
                callback.onExit(0);
            }
        }

    }

    private static void flush(OutputStream... streams) {
        for (OutputStream s : streams) {
            try {
                s.flush();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private static void close(Closeable... closeables) {
        for (Closeable c : closeables) {
            try {
                c.close();
            } catch (IOException e) {
                // Ignore
            }
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

    public static Converter getConverter() {
        return new Converter();
    }

    public static class Converter implements org.osgi.service.blueprint.container.Converter {

        public boolean canConvert(Object sourceObject, ReifiedType targetType) {
            return ShellFactoryImpl.class.isAssignableFrom(sourceObject.getClass())
                    && Factory.class.equals(targetType.getRawClass())
                    && Command.class.equals(targetType.getActualTypeArgument(0).getRawClass());
        }

        public Object convert(Object sourceObject, ReifiedType targetType) throws Exception {
            return sourceObject;
        }
    }

    // TODO: remove this class when sshd use lf->crlf conversion by default
    public class LfToCrLfFilterOutputStream extends FilterOutputStream {

        private boolean lastWasCr;

        public LfToCrLfFilterOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            if (!lastWasCr && b == '\n') {
                out.write('\r');
                out.write('\n');
            } else {
                out.write(b);
            }
            lastWasCr = b == '\r';
        }

    }


}
