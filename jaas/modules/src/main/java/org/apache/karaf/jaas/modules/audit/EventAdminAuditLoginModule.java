/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.apache.karaf.jaas.modules.audit;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventAdminAuditLoginModule extends AbstractAuditLoginModule {

    public static final String TOPIC_EVENTS = "org/apache/karaf/login/";

    private static final Logger LOGGER = LoggerFactory.getLogger(EventAdminAuditLoginModule.class);
    private static boolean errorLogged;

    private BundleContext bundleContext;
    private String topic;

    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map sharedState, Map options) {
        super.initialize(subject, callbackHandler, sharedState, options);
        bundleContext = (BundleContext) options.get(BundleContext.class.getName());
        topic = (String) options.get("topic");
        if (topic == null) {
            topic = TOPIC_EVENTS;
        } else if (!topic.endsWith("/")) {
            topic += "/";
        }
    }

    @Override
    protected void audit(Action action, String user) {
        try {
            EventAdminAuditor.audit(bundleContext, topic + action.toString().toUpperCase(), user, subject);
        } catch (Throwable t) {
            if (!errorLogged) {
                errorLogged = true;
                LOGGER.warn("Unable to send security auditing EventAdmin events: " + t);
            }
        }
    }

    static class EventAdminAuditor {
        public static void audit(BundleContext bundleContext, String topic, String username, Subject subject) {
            ServiceReference<EventAdmin> ref = bundleContext.getServiceReference(EventAdmin.class);
            if (ref != null) {
                EventAdmin eventAdmin = bundleContext.getService(ref);
                try {
                    Map<String, Object> props = new HashMap<String, Object>();
                    props.put("type", topic.substring(topic.lastIndexOf("/") + 1).toLowerCase());
                    props.put("timestamp", System.currentTimeMillis());
                    props.put("username", username);
                    props.put("subject", subject);
                    eventAdmin.postEvent(new Event(topic, props));
                } finally {
                    bundleContext.ungetService(ref);
                }
            }
        }
    }
}
