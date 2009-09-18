package org.apache.felix.karaf.shell.console.completer;

import java.util.List;

import org.apache.felix.karaf.shell.console.Completer;

public class NullCompleter implements Completer {

    public static final NullCompleter INSTANCE = new NullCompleter();

    public int complete(String buffer, int cursor, List<String> candidates) {
        return -1;
    }
}
