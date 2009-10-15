/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.karaf.shell.osgi;

import java.util.Map;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.osgi.context.event.OsgiBundleApplicationContextListener;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEvent;
import org.springframework.osgi.context.event.OsgiBundleContextFailedEvent;
import org.springframework.osgi.context.event.OsgiBundleContextRefreshedEvent;
import org.springframework.osgi.extender.event.BootstrappingDependencyEvent;
import org.springframework.osgi.service.importer.event.OsgiServiceDependencyEvent;
import org.springframework.osgi.service.importer.event.OsgiServiceDependencyWaitStartingEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SpringStateListenerFactory implements BundleStateListener.Factory {

    private BundleContext bundleContext;
    private BundleStateListener listener;

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void init() {
        createListener();
    }

    public void destroy() throws Exception {
        if (listener instanceof Destroyable) {
            ((Destroyable) listener).destroy();
        }
    }

    public BundleStateListener getListener() {
        if (listener == null) {
            listener = createListener();
        }
        return listener;
    }

    private BundleStateListener createListener() {
        try {
            return new SpringApplicationListener(bundleContext);
        } catch (Throwable t) {
            return null;
        }
    }

    public static interface Destroyable {

        public void destroy() throws Exception;

    }

    public static class SpringApplicationListener implements OsgiBundleApplicationContextListener,
            BundleListener, Destroyable, BundleStateListener {

        public static enum SpringState {
            Unknown,
            Waiting,
            Started,
            Failed,
        }

        private static final Logger LOG = LoggerFactory.getLogger(BlueprintListener.class);

        private final Map<Long, SpringState> states;
        private BundleContext bundleContext;
        private ServiceRegistration registration;

        public SpringApplicationListener(BundleContext bundleContext) {
            this.states = new ConcurrentHashMap<Long, SpringState>();
            this.bundleContext = bundleContext;
            this.bundleContext.addBundleListener(this);
            this.registration = this.bundleContext.registerService(OsgiBundleApplicationContextListener.class.getName(), this, new Hashtable());
        }

        public void destroy() throws Exception {
            bundleContext.removeBundleListener(this);
            registration.unregister();
        }

        public String getName() {
            return "Spring ";
        }

        public String getState(Bundle bundle) {
            SpringState state = states.get(bundle.getBundleId());
            if (state == null || bundle.getState() != Bundle.ACTIVE || state == SpringState.Unknown) {
                return null;
            }
            return state.toString();
        }

        public SpringState getSpringState(Bundle bundle) {
            SpringState state = states.get(bundle.getBundleId());
            if (state == null || bundle.getState() != Bundle.ACTIVE) {
                state = SpringState.Unknown;
            }
            return state;
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

}
