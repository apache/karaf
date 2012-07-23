package org.apache.karaf.shell.console.impl.jline;

import java.io.IOException;
import java.io.PrintStream;

import org.fusesource.jansi.Ansi;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class BundleWatcher implements Runnable {

    private final BundleContext context;
    private final Runnable consoleStartCallBack;
    private final PrintStream out;

    public BundleWatcher(BundleContext context, PrintStream out, Runnable consoleStartCallBack) {
        this.context = context;
        this.out = out;
        this.consoleStartCallBack = consoleStartCallBack;
    }

    @Override
    public void run() {
        boolean startConsole = false;
        out.println("Apache Karaf starting up. Press Enter to start the shell now ...");
        out.println();
        while (!startConsole) {
            BundleStats stats = getBundleStats();
            //out.print(Ansi.ansi().cursorUp(1).toString());
            out.println(String.format("Bundles - total: %d, active: %d, resolved: %d, installed: %d         ", 
                    stats.numTotal, stats.numActive, stats.numResolved, stats.numInstalled));
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
            try {
                if (System.in.available() > 0) {
                    char ch = (char) System.in.read();
                    if (ch == '\r') {
                        startConsole = true;
                    }
                }
            } catch (IOException e) {
            }
            if (stats.numActive + stats.numResolved == stats.numTotal) {
                startConsole = true;
            }
        }
        consoleStartCallBack.run();
    }

    private BundleStats getBundleStats() {
        Bundle[] bundles = context.getBundles();
        BundleStats stats = new BundleStats();
        stats.numTotal = bundles.length;
        for (Bundle bundle : bundles) {
            if (bundle.getState() == Bundle.ACTIVE) {
                stats.numActive ++;
            }
            if (bundle.getState() == Bundle.RESOLVED) {
                stats.numResolved ++;
            }
            if (bundle.getState() == Bundle.INSTALLED) {
                stats.numInstalled ++;
            }
        }
        return stats;
    }
    
    class BundleStats {
        int numResolved = 0;
        int numActive = 0;
        int numInstalled = 0;
        int numTotal = 0; 
    }

}
