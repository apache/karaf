package org.apache.karaf.shell.console.impl.jline;

import java.io.PrintStream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.startlevel.FrameworkStartLevel;

public class BundleWatcher implements Runnable {

    private final BundleContext context;
    private final Runnable consoleStartCallBack;
    private final PrintStream out;
    private final int defaultStartLevel;

    public BundleWatcher(BundleContext context, int defaultStartLevel, PrintStream out, Runnable consoleStartCallBack) {
        this.context = context;
        this.defaultStartLevel = defaultStartLevel;
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
            stats.print(out);
            //out.print(Ansi.ansi().cursorUp(1).toString());
            try {
                Thread.sleep(500);
                if (System.in.available() > 0) {
                    char ch = (char) System.in.read();
                    if (ch == '\r') {
                        startConsole = true;
                    }
                }
            } catch (Exception e) {
            }
            if (stats.startLevel == defaultStartLevel) {
                startConsole = true;
            }
        }
        BundleStats stats = getBundleStats();
        stats.print(out);
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
        Bundle frameworkBundle = context.getBundle(0);
        FrameworkStartLevel fsl = frameworkBundle.adapt(FrameworkStartLevel.class);
        stats.startLevel = fsl.getStartLevel();
        return stats;
    }
    
    class BundleStats {
        int numResolved = 0;
        int numActive = 0;
        int numInstalled = 0;
        int numTotal = 0; 
        int startLevel = 0;
        
        public void print(PrintStream out) {
            out.println(String.format("Bundles - total: %d, active: %d, resolved: %d, installed: %d, startlevel: %d         ", 
                    numTotal, numActive, numResolved, numInstalled, startLevel));
        }
    }

}
