package org.apache.karaf.shell.console;

import org.apache.felix.service.command.CommandSession;

public interface Console extends Runnable {
    CommandSession getSession();
    void close();
}
