package org.apache.felix.karaf.gshell.console.jline;

import java.lang.reflect.Method;

import jline.Terminal;
import jline.WindowsTerminal;
import jline.UnixTerminal;
import jline.UnsupportedTerminal;

public class TerminalFactory {

    private Terminal term;
    private Thread hook;

    public Terminal getTerminal() throws Exception {
        init();
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
                UnixTerminal t = new UnixTerminal();
                Method mth = UnixTerminal.class.getDeclaredMethod("stty", String.class);
                mth.setAccessible(true);
                mth.invoke(null, "intr undef");
                t.initializeTerminal();
                hook = new Thread() {
                    public void run() {
                        try {
                            Method mth = UnixTerminal.class.getDeclaredMethod("stty", String.class);
                            mth.setAccessible(true);
                            mth.invoke(null, "intr ^C");
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                };
                Runtime.getRuntime().addShutdownHook(hook);
                term = t;
            }
        } catch (Throwable e) {
            term = new UnsupportedTerminal();
        }
    }

    public synchronized void destroy() throws Exception {
        if (hook != null) {
            hook.run();
            Runtime.getRuntime().removeShutdownHook(hook);
            hook = null;
        }
    }

}
