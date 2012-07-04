package org.apache.karaf.shell.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;

import javax.security.auth.Subject;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.apache.karaf.shell.util.ShellUtil;
import org.apache.karaf.util.StreamUtils;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.session.ServerSession;

public class ShellCommand implements Command, SessionAware {

    private String command;
    private InputStream in;
    private OutputStream out;
    private OutputStream err;
    private ExitCallback callback;
    private ServerSession session;
    private CommandProcessor commandProcessor;

    public ShellCommand(CommandProcessor commandProcessor, String command) {
        this.commandProcessor = commandProcessor;
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
                Subject subject = this.session != null ? this.session.getAttribute(KarafJaasAuthenticator.SUBJECT_ATTRIBUTE_KEY) : null;
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
                ShellUtil.logException(session, t);
            }
        } catch (Exception e) {
            throw (IOException) new IOException("Unable to start shell").initCause(e);
        } finally {
            StreamUtils.close(in, out, err);
            callback.onExit(0);
        }
    }

    public void destroy() {
	}

}