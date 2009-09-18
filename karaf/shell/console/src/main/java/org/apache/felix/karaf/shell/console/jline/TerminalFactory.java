package org.apache.felix.karaf.shell.console.jline;

import jline.Terminal;
import jline.UnsupportedTerminal;
import jline.AnsiWindowsTerminal;
import jline.NoInterruptUnixTerminal;

public class TerminalFactory {

    private Terminal term;

    public Terminal getTerminal() throws Exception {
        if (term == null) {
            init();
        }
        return term;
    }

    public synchronized void init() throws Exception {
        boolean windows = System.getProperty("os.name").toLowerCase().contains("windows");
        try {
            if (windows) {
                AnsiWindowsTerminal t = new AnsiWindowsTerminal();
                t.setDirectConsole(true);
                t.initializeTerminal();
                term = t;
            } else {
                NoInterruptUnixTerminal t = new NoInterruptUnixTerminal();
                t.initializeTerminal();
                term = t;
            }
        } catch (Throwable e) {
            System.out.println("Using an unsupported terminal: " + e.toString());
            term = new UnsupportedTerminal();
        }
    }

    public synchronized void destroy() throws Exception {
        term.restoreTerminal();
        term = null;
    }

}
