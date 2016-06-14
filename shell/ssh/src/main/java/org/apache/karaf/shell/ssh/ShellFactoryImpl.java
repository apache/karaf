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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.security.PrivilegedAction;
import java.util.Map;

import javax.security.auth.Subject;

import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.shell.api.console.Terminal;
import org.apache.karaf.shell.support.ShellUtil;
import org.apache.karaf.util.jaas.JaasHelper;
import org.apache.sshd.common.Factory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.session.ServerSession;

/**
 * SSHD {@link org.apache.sshd.server.Command} factory which provides access to
 * Shell.
 */
public class ShellFactoryImpl implements Factory<Command> {
    private SessionFactory sessionFactory;

    public ShellFactoryImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Command create() {
        return new ShellImpl();
    }

    public class ShellImpl implements Command, SessionAware {
        private InputStream in;

        private OutputStream out;

        private OutputStream err;

        private ExitCallback callback;

        private ServerSession session;

        private Session shell;

        private SshTerminal terminal;

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
            try {
                final Subject subject = ShellImpl.this.session != null ? ShellImpl.this.session
                        .getAttribute(KarafJaasAuthenticator.SUBJECT_ATTRIBUTE_KEY) : null;
                final PrintStream pout = out instanceof PrintStream ? (PrintStream) out : new PrintStream(out);
                final PrintStream perr = err instanceof PrintStream ? (PrintStream) err : out == err ? pout : new PrintStream(err);
                terminal = new SshTerminal(env, in, pout);
                String encoding = getEncoding();
                shell = sessionFactory.create(in,
                        pout, perr, terminal, encoding, this::destroy);
                for (Map.Entry<String, String> e : env.getEnv().entrySet()) {
                    shell.put(e.getKey(), e.getValue());
                }
                JaasHelper.runAs(subject, () ->
                    new Thread(shell, "Karaf ssh console user " + ShellUtil.getCurrentUserName()).start());
            } catch (Exception e) {
                throw (IOException) new IOException("Unable to start shell").initCause(e);
            }
        }

        public void destroy() {
            if (!closed) {
                closed = true;
                flush(out, err);
                close(in, out, err);
                callback.onExit(0);
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
            } catch (Exception e) {
                // Ignore
            }
        }
    }

}
