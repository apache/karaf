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
package org.apache.karaf.bundle.state.blueprint.internal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.karaf.bundle.core.BundleState;
import org.apache.karaf.bundle.core.BundleStateService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: use event instance to receive WAIT topics notifications from blueprint
 * extender
 */
public class BlueprintStateService implements org.osgi.service.blueprint.container.BlueprintListener, BundleListener,
    BundleStateService {

    private static final Logger LOG = LoggerFactory.getLogger(BlueprintStateService.class);

    private final Map<Long, BlueprintEvent> states;

    public BlueprintStateService() {
        states = new ConcurrentHashMap<>();
    }

    @Override
    public String getName() {
        return BundleStateService.NAME_BLUEPRINT;
    }

    @Override
    public String getDiag(Bundle bundle) {
        BlueprintEvent event = states.get(bundle.getBundleId());
        if (event == null) {
            return null;
        }
        if (event.getType() != BlueprintEvent.FAILURE && event.getType() != BlueprintEvent.GRACE_PERIOD
            && event.getType() != BlueprintEvent.WAITING) {
            return null;
        }
        StringBuilder message = new StringBuilder();
        Date date = new Date(event.getTimestamp());
        SimpleDateFormat df = new SimpleDateFormat();
        message.append(df.format(date)).append("\n");
        if (event.getCause() != null) {
            message.append("Exception: \n");
            addMessages(message, event.getCause());
        }
        if (event.getDependencies() != null) {
            message.append("Missing dependencies: \n");
            for (String dep : event.getDependencies()) {
                message.append(dep).append(" ");
            }
            message.append("\n");
        }
        return message.toString();
    }

    public void addMessages(StringBuilder message, Throwable ex) {
        message.append(ex.getMessage());
        message.append("\n");
        StringWriter errorWriter = new StringWriter();
        ex.printStackTrace(new PrintWriter(errorWriter));
        message.append(errorWriter.toString());
        message.append("\n");
    }

    @Override
    public BundleState getState(Bundle bundle) {
        BlueprintEvent event = states.get(bundle.getBundleId());
        BundleState state = getState(event);
        return (bundle.getState() != Bundle.ACTIVE) ? BundleState.Unknown : state;
    }

    @Override
    public void blueprintEvent(BlueprintEvent blueprintEvent) {
        if (LOG.isDebugEnabled()) {
            BundleState state = getState(blueprintEvent);
            LOG.debug("Blueprint app state changed to " + state + " for bundle "
                      + blueprintEvent.getBundle().getBundleId());
        }
        // DESTROYED blueprint containers should be removed from the map to avoid memory "leak/high consumption"
        if (blueprintEvent.getType() == BlueprintEvent.DESTROYED) {
            states.remove(blueprintEvent.getBundle().getBundleId());
        } else {
            states.put(blueprintEvent.getBundle().getBundleId(), blueprintEvent);
        }
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.UNINSTALLED) {
            states.remove(event.getBundle().getBundleId());
        }
    }

    private BundleState getState(BlueprintEvent blueprintEvent) {
        if (blueprintEvent == null) {
            return BundleState.Unknown;
        }
        switch (blueprintEvent.getType()) {
        case BlueprintEvent.CREATING:
            return BundleState.Starting;
        case BlueprintEvent.CREATED:
            return BundleState.Active;
        case BlueprintEvent.DESTROYING:
            return BundleState.Stopping;
        case BlueprintEvent.DESTROYED:
            return BundleState.Resolved;
        case BlueprintEvent.FAILURE:
            return BundleState.Failure;
        case BlueprintEvent.GRACE_PERIOD:
            return BundleState.GracePeriod;
        case BlueprintEvent.WAITING:
            return BundleState.Waiting;
        default:
            return BundleState.Unknown;
        }
    }

}
