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
package org.apache.karaf.bundle.state.spring.internal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
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

public class SpringStateService implements OsgiBundleApplicationContextListener,
        BundleListener, BundleStateService {

    private static final Logger LOG = LoggerFactory.getLogger(SpringStateService.class);

    private final Map<Long, OsgiBundleApplicationContextEvent> states;

    public SpringStateService(BundleContext bundleContext) {
        this.states = new ConcurrentHashMap<Long, OsgiBundleApplicationContextEvent>();
    }

    public String getName() {
        return BundleStateService.NAME_SPRING_DM;
    }

    public BundleState getState(Bundle bundle) {
        OsgiBundleApplicationContextEvent event = states.get(bundle.getBundleId());
        BundleState state = mapEventToState(event);
        return (bundle.getState() != Bundle.ACTIVE) ? BundleState.Unknown : state;
    }
    
    public String getDiag(Bundle bundle) {
    	OsgiBundleApplicationContextEvent event = states.get(bundle.getBundleId());
        if (event == null) {
            return null;
        }
        
        StringBuilder message = new StringBuilder();
        Date date = new Date(event.getTimestamp());
        SimpleDateFormat df = new SimpleDateFormat();
        message.append(df.format(date) + "\n");
        Throwable ex = getException(event);
        if (ex != null) {
            message.append("Exception: \n");
            addMessages(message, ex);
        }
        return message.toString();
    }
    
    private void addMessages(StringBuilder message, Throwable ex) {
    	if (ex != null) {
    		message.append(ex.getMessage());
    		message.append("\n");
            StringWriter errorWriter = new StringWriter();
            ex.printStackTrace(new PrintWriter(errorWriter));
            message.append(errorWriter.toString());
            message.append("\n");
    	}
    }
    
    private Throwable getException(OsgiBundleApplicationContextEvent event) {
    	if (!(event instanceof OsgiBundleContextFailedEvent)) {
        	return null;
        }
    	OsgiBundleContextFailedEvent failureEvent = (OsgiBundleContextFailedEvent) event;
    	return failureEvent.getFailureCause();
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