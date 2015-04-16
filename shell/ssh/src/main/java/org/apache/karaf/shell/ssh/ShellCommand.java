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

import java.io.*;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.Subject;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.apache.karaf.jaas.modules.JaasHelper;
import org.apache.karaf.shell.util.ShellUtil;
import org.apache.karaf.util.StreamLoggerInterceptor;
import org.apache.karaf.util.StreamUtils;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShellCommand implements Command, Runnable, SessionAware {

    public static final String SHELL_INIT_SCRIPT = "karaf.shell.init.script";

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
    private CommandProcessor commandProcessor;
    private Environment env;

    private boolean consoleLogger = false;
    private String consoleLoggerName;
    private String consoleLoggerOutLevel;
    private String consoleLoggerErrLevel;

    public ShellCommand(CommandProcessor commandProcessor, String command, boolean consoleLogger, String consoleLoggerName, String consoleLoggerOutLevel, String consoleLoggerErrLevel) {
        this.commandProcessor = commandProcessor;
        this.command = command;
        this.consoleLogger = consoleLogger;
        this.consoleLoggerName = consoleLoggerName;
        this.consoleLoggerOutLevel = consoleLoggerOutLevel;
        this.consoleLoggerErrLevel = consoleLoggerErrLevel;
    }

    public void setInputStream(InputStream in) {
        this.in = in;
    }

    public void setOutputStream(OutputStream out) {
        if (consoleLogger) {
            this.out = new StreamLoggerInterceptor(out, consoleLoggerName, consoleLoggerOutLevel);
        } else {
            this.out = out;
        }
    }

    public void setErrorStream(OutputStream err) {
        if (consoleLogger) {
            this.err = new StreamLoggerInterceptor(err, consoleLoggerName, consoleLoggerErrLevel);
        } else {
            this.err = err;
        }
    }

    public void setExitCallback(ExitCallback callback) {
        this.callback = callback;
    }

    public void setSession(ServerSession session) {
        this.session = session;
    }

    public void start(final Environment env) throws IOException {
        this.env = env;
        new Thread(this).start();
    }

    public void run() {
        int exitStatus = 0;
        final CommandSession commandSession = commandProcessor.createSession(in, new PrintStream(out), new PrintStream(err));
        try {
            commandSession.put("SCOPE", "shell:osgi:*");
            commandSession.put("APPLICATION", System.getProperty("karaf.name", "root"));
            for (Map.Entry<String,String> e : env.getEnv().entrySet()) {
                commandSession.put(e.getKey(), e.getValue());
            }
            try {
                Subject subject = this.session != null ? this.session.getAttribute(KarafJaasAuthenticator.SUBJECT_ATTRIBUTE_KEY) : null;
                Object result;
                if (subject != null) {
                    try {
                        result = JaasHelper.doAs(subject, new PrivilegedExceptionAction<Object>() {
                            public Object run() throws Exception {
                                String scriptFileName = System.getProperty(SHELL_INIT_SCRIPT);
                                executeScript(scriptFileName, commandSession);
                                return commandSession.execute(command);
                            }
                        });
                    } catch (PrivilegedActionException e) {
                        throw e.getException();
                    }
                } else {
                    String scriptFileName = System.getProperty(SHELL_INIT_SCRIPT);
                    executeScript(scriptFileName, commandSession);
                    result = commandSession.execute(command);
                }
                if (result != null)
                {
                    commandSession.getConsole().println(commandSession.format(result, Converter.INSPECT));
                }
            } catch (Throwable t) {
                exitStatus = 1;
                ShellUtil.logException(commandSession, t);
            }
        } catch (Exception e) {
            exitStatus = 1;
            LOGGER.error("Unable to start shell", e);
        } finally {
            StreamUtils.close(in, out, err);
            commandSession.close();
            callback.onExit(exitStatus);
        }
    }

    public void destroy() {
	}

    private void executeScript(String scriptFileName, CommandSession session) {
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

}
