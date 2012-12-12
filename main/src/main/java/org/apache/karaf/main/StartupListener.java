package org.apache.karaf.main;

import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.startlevel.FrameworkStartLevel;

/**
 * Watches the startup of the framework and displays a progress bar of the
 * number of bundles started / total. The listener will remove itself after the
 * desired start level is reached or the system property karaf.console.started
 * is set to true.
 */
class StartupListener implements FrameworkListener, SynchronousBundleListener {
    private Logger log;
    private static final String SYSTEM_PROP_KARAF_CONSOLE_STARTED = "karaf.console.started";
    private long startTime;
    private int currentPercentage;

    private final BundleContext context;

    StartupListener(Logger log, BundleContext context) {
        this.log = log;
        this.context = context;
        this.currentPercentage = 0;
        this.startTime = System.currentTimeMillis();
        context.addBundleListener(this);
        context.addFrameworkListener(this);
    }
    
    public BundleStats getBundleStats() {
        Bundle[] bundles = context.getBundles();
        int numActive = 0;
        int numBundles = bundles.length;
        for (Bundle bundle : bundles) {
            if (bundle.getHeaders().get(Constants.FRAGMENT_HOST) != null) {
                numBundles--;
            } else if (bundle.getState() == Bundle.ACTIVE) {
                numActive++;
            }
        }
        BundleStats stats = new BundleStats();
        stats.numActive = numActive;
        stats.numTotal = numBundles;
        return stats;
    }

    public synchronized void bundleChanged(BundleEvent bundleEvent) {
        BundleStats stats = getBundleStats();
        if (!isConsoleStarted()) {
            showProgressBar(stats.numActive, stats.numTotal);
        }
    }

    private boolean isConsoleStarted() {
        return Boolean.parseBoolean(System.getProperty(SYSTEM_PROP_KARAF_CONSOLE_STARTED, "false"));
    }

    public synchronized void frameworkEvent(FrameworkEvent frameworkEvent) {
        if (frameworkEvent.getType() == FrameworkEvent.STARTLEVEL_CHANGED) {
            int defStartLevel = Integer.parseInt(System
                    .getProperty(Constants.FRAMEWORK_BEGINNING_STARTLEVEL));
            int startLevel = context.getBundle(0)
                    .adapt(FrameworkStartLevel.class).getStartLevel();
            if (startLevel >= defStartLevel) {
                context.removeBundleListener(this);
                context.removeFrameworkListener(this);
                long startTimeSeconds = (System.currentTimeMillis() - this.startTime) / 1000;
                BundleStats stats = getBundleStats();
                String message = "Karaf started in " + startTimeSeconds + "s. Bundle stats: " + stats.numActive 
                        + " active, " + stats.numTotal + " total";
                log.info(message);
                if (!isConsoleStarted()) {
                    showProgressBar(100, 100);
                    System.out.println(message);
                }

            }
        }
    }

    public void showProgressBar(int done, int total) {
        int percent = (done * 100) / total;
        // progress bar can only have 72 characters so that 80 char wide terminal will display properly
        int scaledPercent = (int) (72.0 * (percent / 100.0));
        // Make sure we do not go backwards with percentage
        if (percent > currentPercentage) {
            currentPercentage = percent;
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("\r%3d%% [", percent));
            for (int i = 0; i < 72; i++) {
                if (i < scaledPercent) {
                    sb.append('=');
                } else if (i == scaledPercent) {
                    sb.append('>');
                } else {
                    sb.append(' ');
                }
            }
            sb.append(']');
            System.out.print(sb.toString());
            System.out.flush();
        }
        if (done == total) {
            System.out.println();
        }
    }
    
    class BundleStats {
        int numActive;
        int numTotal;
    }
}
