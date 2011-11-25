package org.apache.karaf.shell.console.jline;

import org.apache.felix.service.command.CommandSession;

public class CommandSessionHolder {

    private static final ThreadLocal<CommandSession> session = new ThreadLocal<CommandSession>();

    public static CommandSession getSession() {
        return session.get();
    }

    public static void setSession(CommandSession commandSession) {
        session.set(commandSession);
    }

    public static void unset() {
        session.remove();
    }
}
