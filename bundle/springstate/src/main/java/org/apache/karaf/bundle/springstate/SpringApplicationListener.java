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
package org.apache.karaf.bundle.springstate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.karaf.bundle.core.BundleState;
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

public class SpringApplicationListener implements OsgiBundleApplicationContextListener,
        BundleListener, BundleStateService {

    private static final Logger LOG = LoggerFactory.getLogger(SpringApplicationListener.class);

    private final Map<Long, OsgiBundleApplicationContextEvent> states;

    public SpringApplicationListener(BundleContext bundleContext) {
        this.states = new ConcurrentHashMap<Long, OsgiBundleApplicationContextEvent>();
    }

    public String getName() {
        return "Spring";
    }

    public BundleState getState(Bundle bundle) {
        OsgiBundleApplicationContextEvent event = states.get(bundle.getBundleId());
        BundleState state = mapEventToState(event);
        return (bundle.getState() != Bundle.ACTIVE) ? BundleState.Unknown : state;
    }
    
    public String getDiag(Bundle bundle) {
        return null;
    }

    public void onOsgiApplicationEvent(OsgiBundleApplicationContextEvent event) {
        if (LOG.isDebugEnabled()) {
            BundleState state = mapEventToState(event);
            LOG.debug("Spring app state changed to " + state + " for bundle " + event.getBundle().getBundleId());
        }
        states.put(event.getBundle().getBundleId(), event);
    }

    private BundleState mapEventToState(OsgiBundleApplicationContextEvent event) {
        if (event == null) {
            return BundleState.Unknown;
        } else if (event instanceof BootstrappingDependencyEvent) {
            return BundleState.Waiting;
        } else if (event instanceof OsgiBundleContextFailedEvent) {
            return BundleState.Failure;
        } else if (event instanceof OsgiBundleContextRefreshedEvent) {
            return BundleState.Active;
        } else {
            return BundleState.Unknown;
        }
    }

    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.UNINSTALLED) {
            states.remove(event.getBundle().getBundleId());
        }
    }

}