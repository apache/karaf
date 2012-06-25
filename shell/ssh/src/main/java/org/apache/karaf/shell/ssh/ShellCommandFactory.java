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
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import javax.security.auth.Subject;

import org.apache.felix.gogo.commands.CommandException;
import org.apache.felix.gogo.runtime.CommandNotFoundException;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.apache.karaf.shell.console.jline.Console;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.session.ServerSession;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShellCommandFactory implements CommandFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShellCommandFactory.class);

    private CommandProcessor commandProcessor;

    public void setCommandProcessor(CommandProcessor commandProcessor) {
        this.commandProcessor = commandProcessor;
    }

    public Command createCommand(String command) {
        return new ShellCommand(command);
    }

    public class ShellCommand implements Command, SessionAware {

        private String command;
        private InputStream in;
        private OutputStream out;
        private OutputStream err;
        private ExitCallback callback;
        private ServerSession session;

        public ShellCommand(String command) {
            this.command = command;
        }

        public void setInputStream(InputStream in) {
            this.in = in;
        }

        public void setOutputStream(OutputStream out) {
            this.out = out;
        }

        public void setErrorStream(OutputStream err) {
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
                final CommandSession session = commandProcessor.createSession(in, new PrintStream(out), new PrintStream(err));
                session.put("SCOPE", "shell:osgi:*");
                session.put("APPLICATION", System.getProperty("karaf.name", "root"));
                for (Map.Entry<String,String> e : env.getEnv().entrySet()) {
                    session.put(e.getKey(), e.getValue());
                }
                try {
                    Subject subject = this.session != null ? this.session.getAttribute(KarafJaasPasswordAuthenticator.SUBJECT_ATTRIBUTE_KEY) : null;
                    Object result;
                    if (subject != null) {
                        try {
                            result = Subject.doAs(subject, new PrivilegedExceptionAction<Object>() {
                                public Object run() throws Exception {
                                    return session.execute(command);
                                }
                            });
                        } catch (PrivilegedActionException e) {
                            throw e.getException();
                        }
                    } else {
                        result = session.execute(command);
                    }
                    if (result != null)
                    {
                        session.getConsole().println(session.format(result, Converter.INSPECT));
                    }
                } catch (Throwable t) {
                    try {
                        if (t instanceof CommandNotFoundException) {
                            LOGGER.debug("Unknown command entered", t);
                        } else {
                            LOGGER.info("Exception caught while executing command", t);
                        }
                        session.put(Console.LAST_EXCEPTION, t);
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
                        if ( getBoolean(session, Console.PRINT_STACK_TRACES)) {
                            session.getConsole().print(Ansi.ansi().fg(Ansi.Color.RED).toString());
                            t.printStackTrace(session.getConsole());
                            session.getConsole().print(Ansi.ansi().fg(Ansi.Color.DEFAULT).toString());
                        }
                        else if (!(t instanceof CommandException) && !(t instanceof CommandNotFoundException)) {
                            session.getConsole().print(Ansi.ansi().fg(Ansi.Color.RED).toString());
                            session.getConsole().println("Error executing command: "
                                    + (t.getMessage() != null ? t.getMessage() : t.getClass().getName()));
                            session.getConsole().print(Ansi.ansi().fg(Ansi.Color.DEFAULT).toString());
                        }
                        session.getConsole().flush();
                    } catch (Exception ignore) {
                        // ignore
                    }
                }
            } catch (Exception e) {
                throw (IOException) new IOException("Unable to start shell").initCause(e);
            } finally {
                close(in, out, err);
                callback.onExit(0);
            }
        }

        public void destroy() {
		}

        protected boolean getBoolean(CommandSession session, String name) {
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
}
