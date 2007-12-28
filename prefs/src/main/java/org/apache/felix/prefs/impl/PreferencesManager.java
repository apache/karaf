/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.prefs.impl;

import java.util.*;

import org.apache.felix.prefs.*;
import org.osgi.framework.*;
import org.osgi.service.log.LogService;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.PreferencesService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This activator registers itself as a service factory for the
 * preferences service.
 *
 */
public class PreferencesManager
    implements BundleActivator,
               BundleListener,
               ServiceFactory,
               BackingStoreManager {

    /** The map of already created services. For each client bundle
     * a new service is created.
     */
    protected final Map services = new HashMap();

    /** The bundle context. */
    protected BundleContext context;

    /** The backing store service tracker. */
    protected ServiceTracker storeTracker;

    /** The service tracker for the log service. */
    protected ServiceTracker logTracker;

    /** The default store which is used if no service can be found. */
    protected BackingStore defaultStore;

    /** Tracking count for the store tracker to detect changes. */
    protected int storeTrackingCount = -1;

    /**
     * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
     */
    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.UNINSTALLED) {
            final Long bundleId = new Long(event.getBundle().getBundleId());
            synchronized ( this.services ) {
                try {
                    this.getStore().remove(bundleId);
                } catch (BackingStoreException ignore) {
                    // we ignore this for now
                }
                this.services.remove(bundleId);
            }
        }
    }

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) throws Exception {
        this.context = context;

        // track the log service using a ServiceTracker
        this.logTracker = new ServiceTracker( context, LogService.class.getName(), null );
        this.logTracker.open();

        // create the tracker for our backing store
        this.storeTracker = new ServiceTracker( context, BackingStore.class.getName(), null);
        this.storeTracker.open();

        // register this activator as a bundle lister
        context.addBundleListener(this);

        // finally register the service factory for the preferences service
        context.registerService(PreferencesService.class.getName(), this, null);
    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) throws Exception {
        // if we get stopped, we should save all in memory representations
        synchronized (this.services) {
            final Iterator i = this.services.values().iterator();
            while ( i.hasNext() ) {
                final PreferencesServiceImpl service = (PreferencesServiceImpl)i.next();
                this.save(service);
            }
            this.services.clear();
        }
        // stop tracking store service
        if ( this.storeTracker != null ) {
            this.storeTracker.close();
            this.storeTracker = null;
        }
        this.defaultStore = null;

        // stop tracking log service
        if ( this.logTracker != null ) {
            this.logTracker.close();
            this.logTracker = null;
        }

        this.context = null;
    }

    /**
     * @see org.osgi.framework.ServiceFactory#getService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration)
     */
    public Object getService(Bundle bundle, ServiceRegistration reg) {
        final Long bundleId = new Long(bundle.getBundleId());

        synchronized (this.services) {
            PreferencesServiceImpl service;
            // do we already have created a service for this bundle?
            service = (PreferencesServiceImpl) this.services.get(bundleId);

            if (service == null) {
                // create a new service instance
                service = new PreferencesServiceImpl(bundleId, this);
                this.services.put(bundleId, service);
            }
            return service;
        }
    }

    /**
     * @see org.osgi.framework.ServiceFactory#ungetService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration, java.lang.Object)
     */
    public void ungetService(Bundle bundle, ServiceRegistration reg, Object s) {
        final Long bundleId = new Long(bundle.getBundleId());
        // we save all the preferences
        synchronized ( this.services ) {
            final PreferencesServiceImpl service = (PreferencesServiceImpl) this.services.get(bundleId);
            if ( service != null ) {
                this.save(service);
            }
        }
    }

    /**
     * Save all preferences for this service.
     * @param service
     */
    protected void save(PreferencesServiceImpl service) {
        final Iterator i = service.getAllPreferences().iterator();
        while ( i.hasNext() ) {
            final PreferencesImpl prefs = (PreferencesImpl)i.next();
            try {
                this.getStore().store(prefs);
            } catch (BackingStoreException ignore) {
                // we ignore this
            }
        }
    }

    protected void log( int level, String message, Throwable t ) {
        final LogService log = ( LogService ) this.logTracker.getService();
        if ( log != null ) {
            log.log( level, message, t );
            return;
        }
    }

    /**
     * @see org.apache.felix.prefs.BackingStoreManager#getStore()
     */
    public BackingStore getStore() {
        // has the service changed?
        int currentCount = this.storeTracker.getTrackingCount();
        BackingStore service = (BackingStore) this.storeTracker.getService();
        if ( service != null && this.storeTrackingCount < currentCount ) {
            this.storeTrackingCount = currentCount;
            this.cleanupStore(service);
        }
        if ( service == null ) {
            // no service available use default store
            if ( this.defaultStore == null ) {
                synchronized ( this ) {
                    if ( this.defaultStore == null ) {
                        this.defaultStore = new DataFileBackingStoreImpl(this.context);
                        this.cleanupStore(this.defaultStore);
                    }
                }
            }
            service = this.defaultStore;
        }

        return service;
    }

    /**
     * Clean up the store and remove preferences for deleted bundles.
     * @param store
     */
    protected void cleanupStore(BackingStore store) {
        // check which bundles are available
        final Long[] availableBundleIds = store.availableBundles();

        // now check the bundles, for which we have preferences, if they are still
        // in service and delete the preferences where the bundles are out of service.
        // we synchronize on services in order to get not disturbed by a bundle event
        synchronized ( this.services ) {
            for(int i=0; i<availableBundleIds.length; i++) {
                final Long bundleId = availableBundleIds[i];
                final Bundle bundle = this.context.getBundle(bundleId.longValue());
                if (bundle == null || bundle.getState() == Bundle.UNINSTALLED) {
                    try {
                        store.remove(bundleId);
                    } catch (BackingStoreException ignore) {
                        // we ignore this for now
                    }
                }
            }
        }
    }
}
