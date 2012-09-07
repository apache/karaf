package org.apache.karaf.shell.console.jline;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.startlevel.FrameworkStartLevel;

/**
 * Delay the start of the console until the desired start level is reached or enter is pressed
 */
class DelayedStarted extends Thread implements FrameworkListener {
    private static final String SYSTEM_PROP_KARAF_CONSOLE_STARTED = "karaf.console.started";

	private final AtomicBoolean started = new AtomicBoolean(false);
    private final InputStream in;
	private final Runnable console;
	private final BundleContext bundleContext;

    DelayedStarted(Runnable console, BundleContext bundleContext, InputStream in) {
        super("Karaf Shell Console Thread");
		this.console = console;
		this.bundleContext = bundleContext;
        this.in = in;
        int defaultStartLevel = Integer.parseInt(System.getProperty(Constants.FRAMEWORK_BEGINNING_STARTLEVEL));
        int startLevel = ((FrameworkStartLevel) this.bundleContext.getBundle(0).adapt(FrameworkStartLevel.class)).getStartLevel();
        if (startLevel >= defaultStartLevel) {
            started.set(true);
        } else {
            bundleContext.addFrameworkListener(this);
            frameworkEvent(new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(), null));
        }
    }

    public void run() {
        try {
            while (!started.get()) {
                if (in.available() == 0) {
                    Thread.sleep(10);
                }
                while (in.available() > 0) {
                    char ch = (char) in.read();
                    if (ch == '\r' || ch == '\n') {
                        started.set(true);
                        break;
                    }
                }
            }
        } catch (Throwable t) {
            // Ignore
        }

        // Signal to the main module that it can stop displaying the startup progress
        System.setProperty(SYSTEM_PROP_KARAF_CONSOLE_STARTED, "true");

        System.out.println();
        this.bundleContext.removeFrameworkListener(this);
        console.run();
    }

    public void frameworkEvent(FrameworkEvent event) {
        if (event.getType() == FrameworkEvent.STARTLEVEL_CHANGED) {
            int defaultStartLevel = Integer.parseInt(System.getProperty(Constants.FRAMEWORK_BEGINNING_STARTLEVEL));
            int startLevel = ((FrameworkStartLevel) this.bundleContext.getBundle(0).adapt(FrameworkStartLevel.class)).getStartLevel();
            if (startLevel >= defaultStartLevel) {
                started.set(true);
            }
        }
    }
}
