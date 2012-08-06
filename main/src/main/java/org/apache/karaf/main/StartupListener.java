package org.apache.karaf.main;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.startlevel.FrameworkStartLevel;

/**
 * Watches the startup of the framework and displays a progress bar of the number of bundles started / total.
 * The listener will remove itself after the desired start level is reached or the system property karaf.console.started is set to 
 * true. 
 */
class StartupListener implements FrameworkListener, SynchronousBundleListener {
    private static final String SYSTEM_PROP_KARAF_CONSOLE_STARTED = "karaf.console.started";

	private final BundleContext context;
    StartupListener(BundleContext context) {
        this.context = context;
        context.addBundleListener(this);
        context.addFrameworkListener(this);
    }
    public synchronized void bundleChanged(BundleEvent bundleEvent) {
        Bundle[] bundles = context.getBundles();
        int numActive = 0;
        int numBundles = bundles.length;
        for (Bundle bundle : bundles) {
            if (bundle.getHeaders().get(Constants.FRAGMENT_HOST) != null) {
                numBundles--;
            } else if (bundle.getState() == Bundle.ACTIVE) {
                numActive ++;
            }
        }
        boolean started = Boolean.parseBoolean(System.getProperty(SYSTEM_PROP_KARAF_CONSOLE_STARTED, "false"));
        if (!started) {
            showProgressBar(numActive, numBundles);
        }
    }
    public synchronized void frameworkEvent(FrameworkEvent frameworkEvent) {
        if (frameworkEvent.getType() == FrameworkEvent.STARTLEVEL_CHANGED) {
            int defStartLevel = Integer.parseInt(System.getProperty(Constants.FRAMEWORK_BEGINNING_STARTLEVEL));
            int startLevel = context.getBundle(0).adapt(FrameworkStartLevel.class).getStartLevel();
            if (startLevel >= defStartLevel) {
                context.removeBundleListener(this);
                context.removeFrameworkListener(this);
            }
        }
    }
    public void showProgressBar(int done, int total) {
        int percent = (done * 100) / total;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\r%3d%% [", percent));
        for (int i = 0; i < 100; i++) {
            if (i < percent) {
                sb.append('=');
            } else if (i == percent) {
                sb.append('>');
            } else {
                sb.append(' ');
            }
        }
        sb.append(']');
        System.out.print(sb.toString());
        System.out.flush();
        if (done == total) {
            System.out.println();
        }
    }
}