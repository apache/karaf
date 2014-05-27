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
package org.apache.karaf.jaas.modules.eventadmin;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class EventAdminLoginModule implements LoginModule {

    public static final String TOPIC_EVENTS = "org/apache/karaf/jaas";
    public static final String TOPIC_LOGIN = TOPIC_EVENTS + "/LOGIN";
    public static final String TOPIC_SUCCESS = TOPIC_EVENTS + "/SUCCESS";
    public static final String TOPIC_FAILURE = TOPIC_EVENTS + "/FAILURE";
    public static final String TOPIC_LOGOUT = TOPIC_EVENTS + "/LOGOUT";

    private Subject subject;
    private CallbackHandler handler;
    private Map<String, ?> options;
    private String username;
    private BundleContext bundleContext;

    @Override
    public void initialize(Subject subject, CallbackHandler handler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.handler = handler;
        this.options = options;
        this.bundleContext = (BundleContext) options.get(BundleContext.class.getName());
    }

    @Override
    public boolean login() throws LoginException {
        NameCallback user = new NameCallback("User name:");
        Callback[] callbacks = new Callback[]{user};
        try {
            handler.handle(callbacks);
        } catch (Exception e) {
            throw (LoginException) new LoginException("Unable to process callback: " + e.getMessage()).initCause(e);
        }
        if (callbacks.length != 1) {
            throw new IllegalStateException("Number of callbacks changed by server!");
        }
        user = (NameCallback) callbacks[0];
        username = user.getName();
        sendEvent(TOPIC_LOGIN);
        return false;
    }

    @Override
    public boolean commit() throws LoginException {
        if (username != null) {
            sendEvent(TOPIC_SUCCESS);
        }
        return false;
    }

    @Override
    public boolean abort() throws LoginException {
        if (username != null) { //work around initial "fake" login
            sendEvent(TOPIC_FAILURE);
            username = null;
        }
        return false;
    }

    @Override
    public boolean logout() throws LoginException {
        if (username != null) {
            sendEvent(TOPIC_LOGOUT);
            username = null;
        }
        return false;
    }

    private void sendEvent(String topic) {
        if (Boolean.parseBoolean((String) options.get("eventadmin.enabled"))) {
            Dictionary<String, Object> props = new Hashtable<>();
            props.put("type", topic.substring(topic.lastIndexOf("/") + 1).toLowerCase());
            props.put("timestamp", System.currentTimeMillis());
            props.put("username", username);
            props.put("subject", subject);

            try {
                Inner.send(bundleContext, topic, props);
            } catch (Throwable t) {
                // Ignore
            }
        }
    }

    static class Inner {

        public static void send(BundleContext bundleContext, String topic, Dictionary<String, Object> props) {
            ServiceReference<EventAdmin> ref = bundleContext.getServiceReference(EventAdmin.class);
            if (ref != null) {
                EventAdmin admin = bundleContext.getService(ref);
                try {
                    admin.sendEvent(new Event(topic, props));
                } finally {
                    bundleContext.ungetService(ref);
                }
            }
        }
    }


}
