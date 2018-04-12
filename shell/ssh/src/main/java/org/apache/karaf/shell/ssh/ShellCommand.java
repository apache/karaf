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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;

import javax.security.auth.Subject;

import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.shell.support.ShellUtil;
import org.apache.karaf.util.StreamUtils;
import org.apache.karaf.util.filesstream.FilesStream;
import org.apache.karaf.util.jaas.JaasHelper;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShellCommand implements Command, SessionAware {

    public static final String SHELL_INIT_SCRIPT = "karaf.shell.init.script";
    public static final String EXEC_INIT_SCRIPT = "karaf.exec.init.script";

    private static final Logger LOGGER = LoggerFactory.getLogger(ShellCommand.class);

    private static final Class[] SECURITY_BUGFIX = {
                    JaasHelper.class,
                    JaasHelper.OsgiSubjectDomainCombiner.class,
                    JaasHelper.DelegatingProtectionDomain.class,
            };

    private String command;
    private InputStream in;
    private OutputStream out;
    private OutputStream err;
    private ExitCallback callback;
    private ServerSession session;
    private SessionFactory sessionFactory;
    private Environment env;

    public ShellCommand(SessionFactory sessionFactory, String command) {
        this.sessionFactory = sessionFactory;
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
        this.env = env;
        new Thread(this::run).start();
    }

    public void run() {
        int exitStatus = 0;
        try {
            final Session session = sessionFactory.create(in, new PrintStream(out), new PrintStream(err));
            for (Map.Entry<String,String> e : env.getEnv().entrySet()) {
                session.put(e.getKey(), e.getValue());
            }
            try {
                Subject subject = this.session != null ? this.session.getAttribute(KarafJaasAuthenticator.SUBJECT_ATTRIBUTE_KEY) : null;
                Object result;
                if (subject != null) {
                    try {
                        result = JaasHelper.doAs(subject, (PrivilegedExceptionAction<Object>) () -> {
                            String scriptFileName = System.getProperty(EXEC_INIT_SCRIPT);
                            if (scriptFileName == null) {
                                scriptFileName = System.getProperty(SHELL_INIT_SCRIPT);
                            }
                            executeScript(scriptFileName, session);
                            return session.execute(command);
                        });
                    } catch (PrivilegedActionException e) {
                        throw e.getException();
                    }
                } else {
                    String scriptFileName = System.getProperty(EXEC_INIT_SCRIPT);
                    if (scriptFileName == null) {
                        scriptFileName = System.getProperty(SHELL_INIT_SCRIPT);
                    }
                    executeScript(scriptFileName, session);
                    result = session.execute(command);
                }
                if (result != null)
                {
                	if(result instanceof Integer) {
                		// if it is an integer it's interpreted as a return code
                		exitStatus = (Integer) result;
                	}

                    // TODO: print the result of the command ?
//                    session.getConsole().println(session.format(result, Converter.INSPECT));
                }
            } catch (Throwable t) {
                exitStatus = 1;
                ShellUtil.logException(session, t);
            }
        } catch (Exception e) {
            exitStatus = 1;
            LOGGER.error("Unable to start shell", e);
        } finally {
            StreamUtils.close(in, out, err);
            callback.onExit(exitStatus);
        }
    }

    public void destroy() {
	}

    private void executeScript(String names, Session session) {
        FilesStream.stream(names).forEach(p -> doExecuteScript(session, p));
    }

    private void doExecuteScript(Session session, Path scriptFileName) {
        try {
            String script = String.join("\n",
                    Files.readAllLines(scriptFileName));
            session.execute(script);
        } catch (Exception e) {
            LOGGER.debug("Error in initialization script {}", scriptFileName, e);
            System.err.println("Error in initialization script: " + scriptFileName + ": " + e.getMessage());
        }
    }

}
