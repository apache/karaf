package org.apache.karaf.bundle.springstate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.karaf.bundle.core.BundleStateService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEvent;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextListener;
import org.springframework.osgi.context.event.OsgiBundleContextFailedEvent;
import org.springframework.osgi.context.event.OsgiBundleContextRefreshedEvent;
import org.springframework.osgi.extender.event.BootstrappingDependencyEvent;
import org.springframework.osgi.service.importer.event.OsgiServiceDependencyEvent;
import org.springframework.osgi.service.importer.event.OsgiServiceDependencyWaitStartingEvent;

public class SpringApplicationListener implements OsgiBundleApplicationContextListener,
        BundleListener, BundleStateService {

    public static enum SpringState {
        Unknown,
        Waiting,
        Started,
        Failed,
    }

    private static final Logger LOG = LoggerFactory.getLogger(SpringApplicationListener.class);

    private final Map<Long, SpringApplicationListener.SpringState> states;

    public SpringApplicationListener(BundleContext bundleContext) {
        this.states = new ConcurrentHashMap<Long, SpringApplicationListener.SpringState>();
    }

    public String getName() {
        return "Spring";
    }

    public String getState(Bundle bundle) {
        SpringState state = states.get(bundle.getBundleId());
        if (state == null || bundle.getState() != Bundle.ACTIVE || state == SpringState.Unknown) {
            return null;
        }
        return state.toString();
    }

    public void onOsgiApplicationEvent(OsgiBundleApplicationContextEvent event) {
        SpringState state = null;
        if (event instanceof BootstrappingDependencyEvent) {
            OsgiServiceDependencyEvent de = ((BootstrappingDependencyEvent) event).getDependencyEvent();
            if (de instanceof OsgiServiceDependencyWaitStartingEvent) {
                state = SpringState.Waiting;
            }
        } else if (event instanceof OsgiBundleContextFailedEvent) {
            state = SpringState.Failed;
        } else if (event instanceof OsgiBundleContextRefreshedEvent) {
            state = SpringState.Started;
        }
        if (state != null) {
            LOG.debug("Spring app state changed to " + state + " for bundle " + event.getBundle().getBundleId());
            states.put(event.getBundle().getBundleId(), state);
        }
    }

    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.UNINSTALLED) {
            states.remove(event.getBundle().getBundleId());
        }
    }

}