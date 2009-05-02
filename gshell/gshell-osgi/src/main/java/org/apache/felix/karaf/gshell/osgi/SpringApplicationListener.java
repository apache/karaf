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
package org.apache.felix.karaf.gshell.osgi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEvent;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextListener;
import org.springframework.osgi.context.event.OsgiBundleContextFailedEvent;
import org.springframework.osgi.context.event.OsgiBundleContextRefreshedEvent;
import org.springframework.osgi.extender.event.BootstrappingDependencyEvent;
import org.springframework.osgi.service.importer.event.OsgiServiceDependencyEvent;
import org.springframework.osgi.service.importer.event.OsgiServiceDependencyWaitStartingEvent;

public class SpringApplicationListener implements OsgiBundleApplicationContextListener,
                                                  BundleListener, BundleContextAware,
                                                  InitializingBean, DisposableBean {

    public static enum SpringState {
        Unknown,
        Waiting,
        Started,
        Failed,
    }

    private static final Log LOG = LogFactory.getLog(SpringApplicationListener.class);

    private final Map<Long, SpringState> states;
    private BundleContext bundleContext;

    public SpringApplicationListener() {
        this.states = new ConcurrentHashMap<Long, SpringState>();
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

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void afterPropertiesSet() throws Exception {
        bundleContext.addBundleListener(this);
    }

    public void destroy() throws Exception {
        bundleContext.removeBundleListener(this);
    }
}
