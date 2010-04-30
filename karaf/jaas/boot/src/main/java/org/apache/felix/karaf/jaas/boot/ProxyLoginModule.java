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
package org.apache.felix.karaf.jaas.boot;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * An OSGi proxy login module that should be used instead of a plain reference to
 * a given login module.  Two properties must be set, the name of the login module
 * class and the bundle to be used to load it.
 * This class must be available from all modules, so it has to be either in a fragment
 * bundle attached to the system bundle or be made available through the boot delegation
 * class path.
 */
public class ProxyLoginModule implements LoginModule {

    public static final String PROPERTY_MODULE = "org.apache.felix.karaf.jaas.module";
    public static final String PROPERTY_BUNDLE = "org.apache.felix.karaf.jaas.bundle";

    private static BundleContext bundleContext = null;
    
    private LoginModule target = null;

    public static void init(BundleContext context) {
        bundleContext = context;
    }

    /* (non-Javadoc)
     * @see javax.security.auth.spi.LoginModule#initialize(javax.security.auth.Subject, javax.security.auth.callback.CallbackHandler, java.util.Map, java.util.Map)
     */
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        if (bundleContext == null) {
            throw new IllegalStateException("ProxyLoginModule not initialized. Init must be called prior any invocation.");
        }
        Map<String,?> newOptions = new HashMap<String,Object>(options);
        String module = (String) newOptions.remove(PROPERTY_MODULE);
        if (module == null) {
            throw new IllegalStateException("Option " + PROPERTY_MODULE + " must be set to the name of the factory service");
        }
        String bundleId = (String) newOptions.remove(PROPERTY_BUNDLE);
        if (bundleId == null) {
            throw new IllegalStateException("Option " + PROPERTY_BUNDLE + " must be set to the name of the factory service");
        }
        Bundle bundle = bundleContext.getBundle(Long.parseLong(bundleId));
        if (bundle == null) {
            throw new IllegalStateException("No bundle found for id " + bundleId);
        }
        try {
            target = (LoginModule) bundle.loadClass(module).newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Can not load or create login module " + module + " for bundle " + bundleId, e);
        }
        target.initialize(subject, callbackHandler, sharedState, newOptions);
    }

    /* (non-Javadoc)
     * @see javax.security.auth.spi.LoginModule#login()
     */
    public boolean login() throws LoginException {
        return target.login();
    }

    /* (non-Javadoc)
     * @see javax.security.auth.spi.LoginModule#commit()
     */
    public boolean commit() throws LoginException {
        return target.commit();
    }

    /* (non-Javadoc)
     * @see javax.security.auth.spi.LoginModule#abort()
     */
    public boolean abort() throws LoginException {
        return target.abort();
    }

    /* (non-Javadoc)
     * @see javax.security.auth.spi.LoginModule#logout()
     */
    public boolean logout() throws LoginException {
        return target.logout();
    }

}
