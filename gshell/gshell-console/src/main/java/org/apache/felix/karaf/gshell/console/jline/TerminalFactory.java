package org.apache.felix.karaf.gshell.console.jline;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.io.IOException;

import jline.Terminal;
import jline.WindowsTerminal;
import jline.UnixTerminal;
import jline.UnsupportedTerminal;

public class TerminalFactory {

    private Terminal term;
    private Thread hook;

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
                WindowsTerminal t = new WindowsTerminal();
                t.setDirectConsole(true);
                t.initializeTerminal();
                term = t;
            } else {
                NoInterruptUnixTerminal t = new NoInterruptUnixTerminal();
                t.initializeTerminal();
                term = t;
            }
        } catch (Throwable e) {
            term = new UnsupportedTerminal();
        }
    }

    public synchronized void destroy() throws Exception {
        if (term instanceof UnixTerminal) {
            ((UnixTerminal) term).restoreTerminal();
        }
        term = null;
    }

    public static class NoInterruptUnixTerminal extends UnixTerminal {
        @Override
        public void initializeTerminal() throws IOException, InterruptedException {
            super.initializeTerminal();
            stty("intr undef");
        }

        @Override
        public void restoreTerminal() throws Exception {
            stty("intr ^C");
            super.restoreTerminal();
        }

        protected static String stty(final String args) throws IOException, InterruptedException {
            try {
                try {
                    Method mth = UnixTerminal.class.getDeclaredMethod("stty", String.class);
                    mth.setAccessible(true);
                    return (String) mth.invoke(null, args);
                } catch (InvocationTargetException e) {
                    throw e.getTargetException();
                }
            } catch (IOException e) {
                throw e;
            } catch (InterruptedException e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

}
