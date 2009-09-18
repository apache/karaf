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
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 *
 * TODO: use event admin to receive WAIT topics notifications from blueprint extender
 *
 */
public class BlueprintListener implements org.osgi.service.blueprint.container.BlueprintListener, BundleListener,
                                            BundleStateListener, BundleStateListener.Factory
{

    public static enum BlueprintState {
        Unknown,
        Creating,
        Created,
        Destroying,
        Destroyed,
        Failure,
        GracePeriod,
        Waiting
    }

    private static final Logger LOG = LoggerFactory.getLogger(BlueprintListener.class);

    private final Map<Long, BlueprintState> states;
    private BundleContext bundleContext;

    public BlueprintListener() {
        this.states = new ConcurrentHashMap<Long, BlueprintState>();
    }

    public String getName() {
        return "Blueprint   ";
    }

    public String getState(Bundle bundle) {
        BlueprintState state = states.get(bundle.getBundleId());
        if (state == null || bundle.getState() != Bundle.ACTIVE || state == BlueprintState.Unknown) {
            return null;
        }
        return state.toString();
    }

    public BundleStateListener getListener() {
        return this;
    }

    public BlueprintState getBlueprintState(Bundle bundle) {
        BlueprintState state = states.get(bundle.getBundleId());
        if (state == null || bundle.getState() != Bundle.ACTIVE) {
            state = BlueprintState.Unknown;
        }
        return state;
    }

    public void blueprintEvent(BlueprintEvent blueprintEvent) {
        BlueprintState state = getState(blueprintEvent);
        LOG.debug("Blueprint app state changed to " + state + " for bundle " + blueprintEvent.getBundle().getBundleId());
        states.put(blueprintEvent.getBundle().getBundleId(), state);
    }

    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.UNINSTALLED) {
            states.remove(event.getBundle().getBundleId());
        }
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void init() throws Exception {
        bundleContext.addBundleListener(this);
    }

    public void destroy() throws Exception {
        bundleContext.removeBundleListener(this);
    }

    private BlueprintState getState(BlueprintEvent blueprintEvent) {
        switch (blueprintEvent.getType()) {
            case BlueprintEvent.CREATING:
                return BlueprintState.Creating;
            case BlueprintEvent.CREATED:
                return BlueprintState.Created;
            case BlueprintEvent.DESTROYING:
                return BlueprintState.Destroying;
            case BlueprintEvent.DESTROYED:
                return BlueprintState.Destroyed;
            case BlueprintEvent.FAILURE:
                return BlueprintState.Failure;
            case BlueprintEvent.GRACE_PERIOD:
                return BlueprintState.GracePeriod;
            case BlueprintEvent.WAITING:
                return BlueprintState.Waiting;
            default:
                return BlueprintState.Unknown;
        }
    }
}
