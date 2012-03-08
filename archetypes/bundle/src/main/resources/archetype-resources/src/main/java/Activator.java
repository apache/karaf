package ${package};

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

    public void start(BundleContext context) {
        System.out.println("Starting the bundle");
    }

    public void stop(BundleContext context) {
        System.out.println("Stopping the bundle");
    }

}