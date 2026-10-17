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
package org.apache.karaf.services.web.internal.wab;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import jakarta.servlet.ServletContext;

import org.apache.catalina.Container;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.realm.JAASRealm;
import org.apache.karaf.services.web.WebElement;
import org.apache.karaf.services.web.internal.Contexts;
import org.apache.karaf.services.web.internal.Deployment;
import org.apache.karaf.services.web.internal.WebConfig;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A web application bundle deployed in Tomcat.
 */
public class WabDeployment implements Deployment {

    private static final Logger LOGGER = LoggerFactory.getLogger(WabDeployment.class);

    static final String USER_PRINCIPAL = "org.apache.karaf.jaas.boot.principal.UserPrincipal";
    static final String ROLE_PRINCIPAL = "org.apache.karaf.jaas.boot.principal.RolePrincipal";

    private final WebApplication application;
    private final StandardHost host;
    private final WebConfig config;
    private StandardContext context;
    private ServiceRegistration<ServletContext> registration;
    private String failure;

    public WabDeployment(WebApplication application, StandardHost host, WebConfig config) {
        this.application = application;
        this.host = host;
        this.config = config;
    }

    public WebApplication getApplication() {
        return application;
    }

    @Override
    public String getPath() {
        return application.getPath();
    }

    public void deploy() {
        Bundle bundle = application.getBundle();
        BundleWiring wiring = bundle.adapt(BundleWiring.class);
        if (wiring == null || wiring.getClassLoader() == null) {
            failure = "the bundle is not resolved";
            return;
        }
        ClassLoader classLoader = wiring.getClassLoader();
        context = Contexts.create(getPath(), classLoader, bundle, config);
        context.setDisplayName(bundle.getSymbolicName());
        context.setInstanceManager(new WabInstanceManager(classLoader));
        context.addLifecycleListener(new WabContextConfig());
        context.setRealm(createRealm());
        try {
            host.addChild(context);
        } catch (RuntimeException e) {
            LOGGER.error("Unable to deploy web application bundle {} on {}", bundle.getSymbolicName(), getPath(), e);
        }
        if (!context.getState().isAvailable()) {
            failure = "the context failed to start, see the log";
            removeContext();
            return;
        }
        registerServletContext(bundle);
        LOGGER.info("Web application bundle {} deployed on {}", bundle.getSymbolicName(), getPath());
    }

    /**
     * The users of the web application are authenticated by a JAAS realm of Karaf, {@code karaf} by default.
     */
    private JAASRealm createRealm() {
        JAASRealm realm = new JAASRealm();
        realm.setAppName(config.getRealm());
        realm.setUserClassNames(USER_PRINCIPAL);
        realm.setRoleClassNames(ROLE_PRINCIPAL);
        // the login modules are loaded with the class loader of the web container, which sees the Karaf JAAS ones
        realm.setUseContextClassLoader(false);
        return realm;
    }

    /**
     * As defined by the OSGi Web Applications specification, the servlet context is registered as a service, with
     * the bundle context of the web application bundle.
     */
    private void registerServletContext(Bundle bundle) {
        BundleContext bundleContext = bundle.getBundleContext();
        if (bundleContext == null) {
            return;
        }
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("osgi.web.symbolicname", bundle.getSymbolicName());
        properties.put("osgi.web.version", bundle.getVersion().toString());
        properties.put("osgi.web.contextpath", getPath());
        try {
            registration = bundleContext.registerService(ServletContext.class, context.getServletContext(), properties);
        } catch (RuntimeException e) {
            LOGGER.warn("Unable to register the servlet context of {}", bundle.getSymbolicName(), e);
        }
    }

    @Override
    public void undeploy() {
        if (registration != null) {
            try {
                registration.unregister();
            } catch (IllegalStateException e) {
                // the bundle has stopped, the registration is already gone
            }
            registration = null;
        }
        if (context != null) {
            removeContext();
            LOGGER.info("Web application bundle {} undeployed from {}", application.getBundle().getSymbolicName(),
                    getPath());
        }
    }

    private void removeContext() {
        try {
            host.removeChild(context);
        } catch (RuntimeException e) {
            LOGGER.warn("Error while undeploying {}", getPath(), e);
        }
        context = null;
    }

    @Override
    public void describe(List<WebElement> contexts, List<WebElement> servlets, List<WebElement> filters) {
        long bundleId = application.getBundle().getBundleId();
        contexts.add(new WebElement(WebElement.Type.WAB, application.getBundle().getSymbolicName(), getPath(), null,
                null, bundleId, -1, failure == null ? WebElement.State.DEPLOYED : WebElement.State.FAILED,
                failure));
        StandardContext current = context;
        if (current == null) {
            return;
        }
        for (Container child : current.findChildren()) {
            Wrapper wrapper = (Wrapper) child;
            servlets.add(new WebElement(WebElement.Type.SERVLET, wrapper.getName(), getPath(),
                    Arrays.asList(wrapper.findMappings()), wrapper.getServletClass(), bundleId, -1,
                    wrapper.isUnavailable() ? WebElement.State.FAILED : WebElement.State.DEPLOYED, null));
        }
        for (FilterDef filterDef : current.findFilterDefs()) {
            List<String> patterns = new ArrayList<>();
            for (FilterMap filterMap : current.findFilterMaps()) {
                if (filterMap.getFilterName().equals(filterDef.getFilterName())) {
                    patterns.addAll(Arrays.asList(filterMap.getURLPatterns()));
                    for (String servletName : filterMap.getServletNames()) {
                        patterns.add("servlet:" + servletName);
                    }
                }
            }
            filters.add(new WebElement(WebElement.Type.FILTER, filterDef.getFilterName(), getPath(), patterns,
                    filterDef.getFilterClass(), bundleId, -1, WebElement.State.DEPLOYED, null));
        }
    }

    /**
     * Describes a web application bundle that could not be deployed.
     */
    public static WebElement failed(WebApplication application, String reason) {
        return new WebElement(WebElement.Type.WAB, application.getBundle().getSymbolicName(),
                application.getPath(), null, null, application.getBundle().getBundleId(), -1,
                WebElement.State.FAILED, reason);
    }

}
