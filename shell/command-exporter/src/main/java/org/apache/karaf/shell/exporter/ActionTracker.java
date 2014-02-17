package org.apache.karaf.shell.exporter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.gogo.commands.Action;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks services that implement {@link org.apache.felix.gogo.commands.Action}, 
 * wraps each into an ActionCommand 
 * and exports the command as a service in the name of the bundle exporting the Action
 */
@SuppressWarnings("deprecation")
final class ActionTracker extends ServiceTracker<Action, Action> {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @SuppressWarnings("rawtypes")
    private Map<ServiceReference, ServiceRegistration> registrations = 
        new ConcurrentHashMap<ServiceReference, ServiceRegistration>();

    ActionTracker(BundleContext context, Class<Action> clazz,
                         ServiceTrackerCustomizer<Action, Action> customizer) {
        super(context, clazz, customizer);
    }

    @Override
    public Action addingService(ServiceReference<Action> reference) {
        if (!registrations.containsKey(reference)) {
            Bundle userBundle = reference.getBundle();
            try {
                BundleContext context = userBundle.getBundleContext();
                ActionCommand command = new ActionCommand(context.getService(reference));
                registrations.put(reference, command.registerService(context));
            } catch (Exception e) {
                logger.warn("Error exporting action as command from service of bundle " 
                    + userBundle.getSymbolicName()
                    + "[" + userBundle.getBundleId() + "]", e);
            }
        }
        return super.addingService(reference);
    }

    @Override
    public void removedService(ServiceReference<Action> reference, Action service) {
        if (registrations.containsKey(reference)) {
            try {
                registrations.remove(reference).unregister();
            } catch (Exception e) {
                // Ignore .. might already be unregistered if exporting bundle stopped
            }
        }
        super.removedService(reference, service);
    }

    @Override
    public void close() {
        for (ServiceRegistration<?> reg : registrations.values()) {
            try {
                reg.unregister();
            } catch (Exception e) {
                // Ignore .. might already be unregistered if exporting bundle stopped
            }
        }
        registrations.clear();
        super.close();
    }
}