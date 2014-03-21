/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.impl.console.osgi.secured;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;

import org.apache.felix.gogo.runtime.CommandNotFoundException;
import org.apache.felix.service.command.Function;
import org.apache.felix.service.threadio.ThreadIO;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.service.guard.tools.ACLConfigurationParser;
import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.impl.console.SessionFactoryImpl;
import org.apache.karaf.util.tracker.SingleServiceTracker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecuredSessionFactoryImpl extends SessionFactoryImpl implements ConfigurationListener, SingleServiceTracker.SingleServiceListener {

    private static final String PROXY_COMMAND_ACL_PID_PREFIX = "org.apache.karaf.command.acl.";
    private static final String CONFIGURATION_FILTER =
            "(" + Constants.SERVICE_PID + "=" + PROXY_COMMAND_ACL_PID_PREFIX + "*)";

    private static final Logger LOGGER = LoggerFactory.getLogger(SecuredSessionFactoryImpl.class);

    private BundleContext bundleContext;
    private Map<String, Dictionary<String, Object>> scopes = new HashMap<String, Dictionary<String, Object>>();
    private SingleServiceTracker<ConfigurationAdmin> configAdminTracker;
    private ServiceRegistration registration;

    public SecuredSessionFactoryImpl(BundleContext bundleContext, ThreadIO threadIO) {
        super(threadIO);
        this.bundleContext = bundleContext;
        this.registration = bundleContext.registerService(ConfigurationListener.class, this, null);
        this.configAdminTracker = new SingleServiceTracker<ConfigurationAdmin>(bundleContext, ConfigurationAdmin.class, this);
        this.configAdminTracker.open();
    }

    public void stop() {
        this.registration.unregister();
        this.configAdminTracker.close();
        super.stop();
    }

    @Override
    protected Function wrap(Command command) {
        return new SecuredCommand(this, command);
    }

    @Override
    protected boolean isVisible(Object service) {
        if (service instanceof Command) {
            return isVisible((Command) service);
        } else {
            return super.isVisible(service);
        }
    }

    protected boolean isVisible(Command command) {
        Dictionary<String, Object> config = getScopeConfig(command.getScope());
        if (config != null) {
            List<String> roles = new ArrayList<String>();
            ACLConfigurationParser.getRolesForInvocation(command.getName(), null, null, config, roles);
            if (roles.isEmpty()) {
                return true;
            } else {
                for (String role : roles) {
                    if (currentUserHasRole(role)) {
                        return true;
                    }
                }
                return false;
            }
        }
        return true;
    }

    void checkSecurity(SecuredCommand command, Session session, List<Object> arguments) {
        Dictionary<String, Object> config = getScopeConfig(command.getScope());
        if (config != null) {
            if (!isVisible(command)) {
                throw new CommandNotFoundException(command.getScope() + ":" + command.getName());
            }
            List<String> roles = new ArrayList<String>();
            ACLConfigurationParser.Specificity s = ACLConfigurationParser.getRolesForInvocation(command.getName(), new Object[] { arguments.toString() }, null, config, roles);
            if (s == ACLConfigurationParser.Specificity.NO_MATCH) {
                return;
            }
            for (String role : roles) {
                if (currentUserHasRole(role)) {
                    return;
                }
            }
            throw new SecurityException("Insufficient credentials.");
        }
    }

    static boolean currentUserHasRole(String requestedRole) {
        String clazz;
        String role;
        int index = requestedRole.indexOf(':');
        if (index > 0) {
            clazz = requestedRole.substring(0, index);
            role = requestedRole.substring(index + 1);
        } else {
            clazz = RolePrincipal.class.getName();
            role = requestedRole;
        }

        AccessControlContext acc = AccessController.getContext();
        if (acc == null) {
            return false;
        }
        Subject subject = Subject.getSubject(acc);

        if (subject == null) {
            return false;
        }

        for (Principal p : subject.getPrincipals()) {
            if (clazz.equals(p.getClass().getName()) && role.equals(p.getName())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void configurationEvent(ConfigurationEvent event) {
        if (!event.getPid().startsWith(PROXY_COMMAND_ACL_PID_PREFIX))
            return;

        try {
            switch (event.getType()) {
                case ConfigurationEvent.CM_DELETED:
                    removeScopeConfig(event.getPid().substring(PROXY_COMMAND_ACL_PID_PREFIX.length()));
                    break;
                case ConfigurationEvent.CM_UPDATED:
                    ConfigurationAdmin configAdmin = bundleContext.getService(event.getReference());
                    try {
                        addScopeConfig(configAdmin.getConfiguration(event.getPid()));
                    } finally {
                        bundleContext.ungetService(event.getReference());
                    }
                    break;
            }
        } catch (Exception e) {
            LOGGER.error("Problem processing Configuration Event {}", event, e);
        }
    }

    private void addScopeConfig(Configuration config) {
        if (!config.getPid().startsWith(PROXY_COMMAND_ACL_PID_PREFIX)) {
            // not a command scope configuration file
            return;
        }
        String scope = config.getPid().substring(PROXY_COMMAND_ACL_PID_PREFIX.length());
        if (scope.indexOf('.') >= 0) {
            // scopes don't contains dots, not a command scope
            return;
        }
        scope = scope.trim();
        synchronized (scopes) {
            scopes.put(scope, config.getProperties());
        }
    }

    private void removeScopeConfig(String scope) {
        synchronized (scopes) {
            scopes.remove(scope);
        }
    }

    private Dictionary<String, Object> getScopeConfig(String scope) {
        synchronized (scopes) {
            return scopes.get(scope);
        }
    }

    @Override
    public void serviceFound() {
        try {
            ConfigurationAdmin configAdmin = this.configAdminTracker.getService();
            Configuration[] configs = configAdmin.listConfigurations(CONFIGURATION_FILTER);
            if (configs != null) {
                for (Configuration config : configs) {
                    addScopeConfig(config);
                }
            }
        } catch (Exception e) {
            // Ignore, should never happen
        }
    }

    @Override
    public void serviceLost() {
    }

    @Override
    public void serviceReplaced() {
        serviceFound();
    }
}
