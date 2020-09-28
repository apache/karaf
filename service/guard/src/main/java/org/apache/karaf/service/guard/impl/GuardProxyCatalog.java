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
package org.apache.karaf.service.guard.impl;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

import org.apache.aries.proxy.InvocationListener;
import org.apache.aries.proxy.ProxyManager;
import org.apache.aries.proxy.UnableToProxyException;
import org.apache.karaf.service.guard.tools.ACLConfigurationParser;
import org.apache.karaf.service.guard.tools.ACLConfigurationParser.Specificity;
import org.apache.karaf.util.jaas.JaasHelper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuardProxyCatalog implements ServiceListener {
    public static final String KARAF_SECURED_SERVICES_SYSPROP = "karaf.secured.services";
    public static final String SERVICE_GUARD_ROLES_PROPERTY = "org.apache.karaf.service.guard.roles";
    public static final String KARAF_SECURED_COMMAND_COMPULSORY_ROLES_PROPERTY = "karaf.secured.command.compulsory.roles";

    static final String PROXY_CREATOR_THREAD_NAME = "Secure OSGi Service Proxy Creator";
    static final String PROXY_SERVICE_KEY = "." + GuardProxyCatalog.class.getName(); // The only currently used value is Boolean.TRUE
    static final String SERVICE_ACL_PREFIX = "org.apache.karaf.service.acl.";
    static final String SERVICE_GUARD_KEY = "service.guard";
    static final Logger LOG = LoggerFactory.getLogger(GuardProxyCatalog.class);

    private static final Pattern JAVA_METHOD_NAME_PATTERN = Pattern.compile("[a-zA-Z_$][a-zA-Z0-9_$]*");
    private static final String ROLE_WILDCARD = "*";

    private final BundleContext myBundleContext;
    private final Map<String, Filter> filters = new ConcurrentHashMap<>();

    final ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> configAdminTracker;
    final ServiceTracker<ProxyManager, ProxyManager> proxyManagerTracker;
    final ConcurrentMap<Long, ServiceRegistrationHolder> proxyMap = new ConcurrentHashMap<>();
    final BlockingQueue<CreateProxyRunnable> createProxyQueue = new LinkedBlockingQueue<>();
    final String compulsoryRoles;

    // These two variables control the proxy creator thread, which is started as soon as a ProxyManager Service
    // becomes available.
    volatile boolean runProxyCreator = true;
    volatile Thread proxyCreatorThread = null;
    
    

    GuardProxyCatalog(BundleContext bc) throws Exception {
        LOG.trace("Starting GuardProxyCatalog");
        myBundleContext = bc;
        
        compulsoryRoles = System.getProperty(GuardProxyCatalog.KARAF_SECURED_COMMAND_COMPULSORY_ROLES_PROPERTY);
        if (compulsoryRoles == null) {
            //default behavior as before, no compulsory roles for a karaf command without the ACL
            LOG.info("No compulsory roles for a karaf command without the ACL as its system property is not set: {}", GuardProxyCatalog.KARAF_SECURED_COMMAND_COMPULSORY_ROLES_PROPERTY);
        } 

        // The service listener is used to update/unregister proxies if the backing service changes/goes away
        bc.addServiceListener(this);

        Filter caFilter = getNonProxyFilter(bc, ConfigurationAdmin.class);
        LOG.trace("Creating Config Admin Tracker using filter {}", caFilter);
        configAdminTracker = new ServiceTracker<>(bc, caFilter, null);
        configAdminTracker.open();

        Filter pmFilter = getNonProxyFilter(bc, ProxyManager.class);
        LOG.trace("Creating Proxy Manager Tracker using filter {}", pmFilter);
        proxyManagerTracker = new ServiceTracker<>(bc, pmFilter, new ServiceProxyCreatorCustomizer());
        proxyManagerTracker.open();
    }

    static Filter getNonProxyFilter(BundleContext bc, Class<?> clazz) throws InvalidSyntaxException {
        Filter caFilter = bc.createFilter(
                "(&(" + Constants.OBJECTCLASS + "=" + clazz.getName() +
                        ")(!(" + PROXY_SERVICE_KEY + "=*)))");
        return caFilter;
    }

    void close() {
        LOG.trace("Stopping GuardProxyCatalog");
        stopProxyCreator();
        proxyManagerTracker.close();
        configAdminTracker.close();

        myBundleContext.removeServiceListener(this);

        // Remove all proxy registrations
        for (ServiceRegistrationHolder holder : proxyMap.values()) {
            ServiceRegistration<?> reg = holder.registration;
            if (reg != null) {
                LOG.info("Unregistering proxy service of {} with properties {}",
                        reg.getReference().getProperty(Constants.OBJECTCLASS), copyProperties(reg.getReference()));
                reg.unregister();
            }
        }
        proxyMap.clear();
    }

    @Override
    public void serviceChanged(ServiceEvent event) {
        // This method is to ensure that proxied services follow the original service. I.e. if the original service
        // goes away the proxies should go away too. If the original service is modified, the proxies should be
        // modified accordingly

        ServiceReference<?> sr = event.getServiceReference();
        if (event.getType() == ServiceEvent.REGISTERED) {
            // Nothing to do for new services
            return;
        }

        if (isProxy(sr)) {
            // Ignore proxies, we only react to real service changes
            return;
        }

        Long orgServiceID = (Long) sr.getProperty(Constants.SERVICE_ID);
        if (event.getType() == ServiceEvent.UNREGISTERING) {
            handleOriginalServiceUnregistering(orgServiceID);
        }

        if ((event.getType() & (ServiceEvent.MODIFIED | ServiceEvent.MODIFIED_ENDMATCH)) > 0) {
            handleOriginalServiceModifed(orgServiceID, sr);
        }
    }

    private void handleOriginalServiceUnregistering(Long orgServiceID) {
        // If the service queued up to be proxied, remove it.
        createProxyQueue.removeIf(cpr -> orgServiceID.equals(cpr.getOriginalServiceID()));

        ServiceRegistrationHolder holder = proxyMap.remove(orgServiceID);
        if (holder != null) {
            if (holder.registration != null) {
                holder.registration.unregister();
            }
        }
    }

    private void handleOriginalServiceModifed(Long orgServiceID, ServiceReference<?> orgServiceRef) {
        // We don't need to do anything for services that are queued up to be proxied, as the
        // properties are only taken at the point of proxyfication...

        ServiceRegistrationHolder holder = proxyMap.get(orgServiceID);
        if (holder != null) {
            ServiceRegistration<?> reg = holder.registration;
            if (reg != null) {
                // Preserve the roles as they are expensive to compute
                Object roles = reg.getReference().getProperty(SERVICE_GUARD_ROLES_PROPERTY);
                Dictionary<String, Object> newProxyProps = proxyProperties(orgServiceRef);
                if (roles != null) {
                    newProxyProps.put(SERVICE_GUARD_ROLES_PROPERTY, roles);
                } else {
                    newProxyProps.remove(SERVICE_GUARD_ROLES_PROPERTY);
                }
                reg.setProperties(newProxyProps);
            }
        }
    }

    boolean isProxy(ServiceReference<?> sr) {
        return sr.getProperty(PROXY_SERVICE_KEY) != null;
    }

    // Called by hooks to find out whether the service should be hidden.
    // Also handles the proxy creation of services if applicable.
    // Return true if the hook should hide the service for the bundle
    boolean handleProxificationForHook(ServiceReference<?> sr) {
        // Note that when running under an OSGi R6 framework the number of proxies created
        // can be limited by looking at the new 'service.scope' property. If the value is
        // 'singleton' then the same proxy can be shared across all clients.
        // Pre OSGi R6 it is not possible to find out whether a service is backed by a
        // Service Factory, so we assume that every service is.

        if (isProxy(sr)) {
            return false;
        }

        proxyIfNotAlreadyProxied(sr); // Note does most of the work async
        return true;
    }

    void proxyIfNotAlreadyProxied(final ServiceReference<?> originalRef)  {
        final long orgServiceID = (Long) originalRef.getProperty(Constants.SERVICE_ID);

        // make sure it's on the map before the proxy is registered, as that can trigger
        // another call into this method, and we need to make sure that it doesn't proxy
        // the service again.
        final ServiceRegistrationHolder registrationHolder = new ServiceRegistrationHolder();
        ServiceRegistrationHolder previousHolder = proxyMap.putIfAbsent(orgServiceID, registrationHolder);
        if (previousHolder != null) {
            // There is already a proxy for this service
            return;
        }

        LOG.trace("Will create proxy of service {}({})", originalRef.getProperty(Constants.OBJECTCLASS), orgServiceID);

        // Instead of immediately creating the proxy, we add the code that creates the proxy to the proxyQueue.
        // This means that we can better react to the fact that the ProxyManager service might arrive
        // later. As soon as the Proxy Manager is available, the queue is emptied and the proxies created.
        CreateProxyRunnable cpr = new CreateProxyRunnable() {
            @Override
            public long getOriginalServiceID() {
                return orgServiceID;
            }

            @Override
            public void run(final ProxyManager pm) throws Exception {
                String[] objectClassProperty = (String[]) originalRef.getProperty(Constants.OBJECTCLASS);
                ServiceFactory<Object> sf = new ProxyServiceFactory(pm, originalRef);
                registrationHolder.registration = originalRef.getBundle().getBundleContext().registerService(
                        objectClassProperty, sf, proxyPropertiesRoles());

                Dictionary<String, Object> actualProxyProps = copyProperties(registrationHolder.registration.getReference());
                LOG.debug("Created proxy of service {} under {} with properties {}",
                        orgServiceID, actualProxyProps.get(Constants.OBJECTCLASS), actualProxyProps);
            }

            private Dictionary<String, Object> proxyPropertiesRoles() throws Exception {
                Dictionary<String, Object> p = proxyProperties(originalRef);

                Set<String> roles = getServiceInvocationRoles(originalRef);
                if (roles != null) {
                    roles.remove(ROLE_WILDCARD); // we don't expose that on the service property
                    p.put(SERVICE_GUARD_ROLES_PROPERTY, roles);
                } else {
                    // In this case there are no roles defined for the service so anyone can invoke it
                    p.remove(SERVICE_GUARD_ROLES_PROPERTY);
                }
                return p;
            }
        };

        try {
            createProxyQueue.put(cpr);
        } catch (InterruptedException e) {
            LOG.warn("Problem scheduling a proxy creator for service {}({})",
                    originalRef.getProperty(Constants.OBJECTCLASS), orgServiceID, e);
            e.printStackTrace();
        }
    }

    private static Dictionary<String, Object> proxyProperties(ServiceReference<?> sr) {
        Dictionary<String, Object> p = copyProperties(sr);
        p.put(PROXY_SERVICE_KEY, Boolean.TRUE);
        return p;
    }

    private static Dictionary<String, Object> copyProperties(ServiceReference<?> sr) {
        Dictionary<String, Object> p = new Hashtable<>();

        for (String key : sr.getPropertyKeys()) {
            p.put(key, sr.getProperty(key));
        }
        return p;
    }

    // Returns what roles can possibly ever invoke this service. Note that not every invocation may be successful
    // as there can be different roles for different methods and also roles based on arguments passed in.
    Set<String> getServiceInvocationRoles(ServiceReference<?> serviceReference) throws Exception {
        boolean definitionFound = false;
        Set<String> allRoles = new HashSet<>();

        // This can probably be optimized. Maybe we can cache the config object relevant instead of
        // walking through all of the ones that have 'service.guard'.
        for (Configuration config : getServiceGuardConfigs()) {
            Dictionary<String, Object> properties = config.getProcessedProperties(null);
            Object guardFilter = properties.get(SERVICE_GUARD_KEY);
            if (guardFilter instanceof String) {
                Filter filter = getFilter((String) guardFilter);
                if (filter.match(serviceReference)) {
                    definitionFound = true;
                    for (Enumeration<String> e = properties.keys(); e.hasMoreElements(); ) {
                        String key = e.nextElement();
                        String bareKey = key;
                        int idx = bareKey.indexOf('(');
                        if (idx >= 0) {
                            bareKey = bareKey.substring(0, idx);
                        }
                        int idx1 = bareKey.indexOf('[');
                        if (idx1 >= 0) {
                            bareKey = bareKey.substring(0, idx1);
                        }
                        int idx2 = bareKey.indexOf('*');
                        if (idx2 >= 0) {
                            bareKey = bareKey.substring(0, idx2);
                        }
                        if (!isValidMethodName(bareKey)) {
                            continue;
                        }
                        Object value = properties.get(key);
                        if (value instanceof String) {
                            allRoles.addAll(ACLConfigurationParser.parseRoles((String) value));
                        }
                    }
                }
            }
        }
        return definitionFound ? allRoles : null;
    }

    private Filter getFilter(String string) throws InvalidSyntaxException {
        Filter filter = filters.get(string);
        if (filter == null) {
            filter = myBundleContext.createFilter(string);
            filters.put(string, filter);
        }
        return filter;
    }

    // Ensures that it never returns null
    private Configuration[] getServiceGuardConfigs() throws IOException, InvalidSyntaxException {
        ConfigurationAdmin ca = null;
        try {
            ca = configAdminTracker.waitForService(5000);
        } catch (InterruptedException e) {
        }
        if (ca == null) {
            throw new IllegalStateException("Role based access for services requires the OSGi Configuration Admin Service to be present");
        }

        Configuration[] configs = ca.listConfigurations(
                "(&(" + Constants.SERVICE_PID  + "=" + SERVICE_ACL_PREFIX + "*)(" + SERVICE_GUARD_KEY + "=*))");
        if (configs == null) {
            return new Configuration [] {};
        }
        return configs;
    }

    private boolean isValidMethodName(String name) {
        return JAVA_METHOD_NAME_PATTERN.matcher(name).matches();
    }

    void stopProxyCreator() {
        runProxyCreator = false; // Will end the proxy creation thread
        if (proxyCreatorThread != null) {
            proxyCreatorThread.interrupt();
        }
    }

    static boolean currentUserHasRole(String reqRole) {
        if (ROLE_WILDCARD.equals(reqRole)) {
            return true;
        }
        return JaasHelper.currentUserHasRole(reqRole);
    }

    static class ServiceRegistrationHolder {
        volatile ServiceRegistration<?> registration;
    }

    class ProxyServiceFactory implements ServiceFactory<Object> {
        private final ProxyManager pm;
        private final ServiceReference<?> originalRef;

        ProxyServiceFactory(ProxyManager pm, ServiceReference<?> originalRef) {
            this.pm = pm;
            this.originalRef = originalRef;
        }

        @Override
        public Object getService(Bundle bundle, ServiceRegistration<Object> registration) {
            Set<Class<?>> allClasses = new HashSet<>();

            // This needs to be done on the Client BundleContext since the bundle might be backed by a Service Factory
            // in which case it needs to be given a chance to produce the right service for this client.
            Object svc = bundle.getBundleContext().getService(originalRef);
            Class<?> curClass = svc.getClass();
            while (!Object.class.equals(curClass)) {
                allClasses.add(curClass);
                allClasses.addAll(Arrays.asList(curClass.getInterfaces()));
                curClass = curClass.getSuperclass(); // Collect super types too
            }

            for (Iterator<Class<?>> i = allClasses.iterator(); i.hasNext(); ) {
                Class<?> cls = i.next();
                if (((cls.getModifiers() & (Modifier.PUBLIC | Modifier.PROTECTED)) == 0) ||
                        ((cls.getModifiers() & Modifier.FINAL) > 0) ||
                        cls.isAnonymousClass()  || cls.isLocalClass()) {
                    // Do not attempt to proxy private, package-default, final,  anonymous or local classes
                    i.remove();
                } else {
                    for (Method m : cls.getDeclaredMethods()) {
                        if ((m.getModifiers() & Modifier.FINAL) > 0) {
                            // If the class contains final methods, don't attempt to proxy it
                            i.remove();
                            break;
                        }
                    }
                }
            }

            InvocationListener il = new ProxyInvocationListener(originalRef);
            try {
                return pm.createInterceptingProxy(originalRef.getBundle(), allClasses, svc, il);
            } catch (UnableToProxyException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void ungetService(Bundle bundle, ServiceRegistration<Object> registration, Object service) {
            bundle.getBundleContext().ungetService(originalRef);
        }
    }

    class ProxyInvocationListener implements InvocationListener {
        private final ServiceReference<?> serviceReference;

        ProxyInvocationListener(ServiceReference<?> sr) {
            this.serviceReference = sr;
        }

        @Override
        public Object preInvoke(Object proxy, Method m, Object[] args) throws Throwable {
            String[] sig = new String[m.getParameterTypes().length];
            for (int i = 0; i < m.getParameterTypes().length; i++) {
                sig[i] = m.getParameterTypes()[i].getName();
            }

            // The ordering of the keys is important because the first value when iterating has the highest specificity
            TreeMap<Specificity, List<String>> roleMappings = new TreeMap<>();
            boolean foundMatchingConfig = false;

            // This can probably be optimized. Maybe we can cache the config object relevant instead of
            // walking through all of the ones that have 'service.guard'.
            Object guardFilter = null;
            for (Configuration config : getServiceGuardConfigs()) {
                guardFilter = config.getProperties().get(SERVICE_GUARD_KEY);
                if (guardFilter instanceof String) {
                    Filter filter = myBundleContext.createFilter((String) guardFilter);
                    if (filter.match(serviceReference)) {
                        foundMatchingConfig = true;
                        List<String> roles = new ArrayList<>();
                        Specificity s = ACLConfigurationParser.
                                getRolesForInvocation(m.getName(), args, sig, config.getProperties(), roles);
                        if (s != Specificity.NO_MATCH) {
                            roleMappings.put(s, roles);
                            if (s == Specificity.ARGUMENT_MATCH) {
                                // No more specific mapping can be found
                                break;
                            }
                        }
                    }
                }
            }

            if (!foundMatchingConfig) {
                if (compulsoryRoles != null && (guardFilter instanceof String) 
                    && ((String)guardFilter).indexOf("osgi.command.scope") > 0 
                    && ((String)guardFilter).indexOf("osgi.command.functio") > 0) {
                    //use compulsoryRoles roles for those karaf command without any ACL
                    roleMappings.put(Specificity.NAME_MATCH, ACLConfigurationParser.parseRoles(compulsoryRoles));
                } else {
                    // No mappings for this service, anyone can invoke
                    return null;
                }
            }

            if (roleMappings.size() == 0) {
                LOG.info("Service {} has role mapping, but assigned no roles to method {}", serviceReference, m);
                throw new SecurityException("Insufficient credentials.");
            }

            // The first entry on the map has the highest significance because the keys are sorted in the order of
            // the Specificity enum.
            List<String> allowedRoles = roleMappings.values().iterator().next();
            for (String role : allowedRoles) {
                if (currentUserHasRole(role)) {
                    LOG.trace("Allow user with role {} to invoke service {} method {}", role, serviceReference, m);
                    return null;
                }
            }

            // The current user does not have the required roles to invoke the service.
            LOG.info("Current user does not have required roles ({}) for service {} method {} and/or arguments",
                    allowedRoles, serviceReference, m);
            throw new SecurityException("Insufficient credentials.");
        }


        @Override
        public void postInvokeExceptionalReturn(Object token, Object proxy, Method m, Throwable exception) throws Throwable {
        }

        @Override
        public void postInvoke(Object token, Object proxy, Method m, Object returnValue) throws Throwable {
        }
    }

    // This customizer comes into action as the ProxyManager service arrives.
    class ServiceProxyCreatorCustomizer implements ServiceTrackerCustomizer<ProxyManager, ProxyManager> {
        @Override
        public ProxyManager addingService(ServiceReference<ProxyManager> reference) {
            runProxyCreator = true;
            final ProxyManager svc = myBundleContext.getService(reference);
            if (proxyCreatorThread == null && svc != null) {
                proxyCreatorThread = newProxyProducingThread(svc);
            }
            return svc;
        }

        private Thread newProxyProducingThread(final ProxyManager proxyManager) {
            Thread t = new Thread(() -> {
                while (runProxyCreator) {
                    CreateProxyRunnable proxyCreator = null;
                    try {
                        proxyCreator = createProxyQueue.take(); // take waits until there is something on the queue
                    } catch (InterruptedException ie) {
                        // part of normal behaviour
                    }

                    if (proxyCreator != null) {
                        try {
                            proxyCreator.run(proxyManager);
                        } catch (Exception e) {
                            LOG.warn("Problem creating secured service proxy", e);
                        }
                    }
                }
                // finished running
                proxyCreatorThread = null;
            });
            t.setName(PROXY_CREATOR_THREAD_NAME);
            t.setDaemon(true);
            t.start();

            return t;
        }

        @Override
        public void modifiedService(ServiceReference<ProxyManager> reference, ProxyManager service) {
            // no need to react
        }

        @Override
        public void removedService(ServiceReference<ProxyManager> reference, ProxyManager service) {
            stopProxyCreator();
        }
    }

    interface CreateProxyRunnable {
        long getOriginalServiceID();
        void run(ProxyManager pm) throws Exception;
    }
}
