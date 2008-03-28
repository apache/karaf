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
package org.apache.felix.ipojo.handler.extender;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.util.Callback;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;

/**
 * Track and manage extensions.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ExtenderManager implements SynchronousBundleListener {
    
    /**
     * Looked extension.
     */
    private String m_extension;
    
    /**
     * OnArrival method. 
     */
    private Callback m_onArrival;
    
    /**
     * OnDeparture method. 
     */
    private Callback m_onDeparture;
    
    /**
     * Attached handler. 
     */
    private PrimitiveHandler m_handler;
    
    /**
     * Bundle context. 
     */
    private BundleContext m_context;
    
    /**
     * List of managed bundles. 
     */
    private List m_bundles = new ArrayList();
    
    /**
     * Constructor.
     * @param handler : attached handler.
     * @param extension : looked extension.
     * @param bind : onArrival method
     * @param unbind : onDeparture method.
     */
    public ExtenderManager(ExtenderModelHandler handler, String extension, String bind, String unbind) {
        m_handler = handler;
        m_onArrival = new Callback(bind, new Class[] {Bundle.class, String.class}, false, m_handler.getInstanceManager());
        m_onDeparture = new Callback(unbind, new Class[] {Bundle.class}, false, m_handler.getInstanceManager());
        m_extension = extension;
        m_context = handler.getInstanceManager().getContext();
    }
    
    /**
     * Start method.
     * Look for already presents bundle and register a (synchronous) bundle listener.
     */
    public void start() {
        synchronized (this) {
            // listen to any changes in bundles.
            m_context.addBundleListener(this);
            // compute already started bundles.
            for (int i = 0; i < m_context.getBundles().length; i++) {
                if (m_context.getBundles()[i].getState() == Bundle.ACTIVE) {
                    onArrival(m_context.getBundles()[i]);
                }
            }
        }
    }
    
    /**
     * Manage a bundle arrival:
     * Check the extension and manage it if present.
     * @param bundle : bundle.
     */
    private void onArrival(Bundle bundle) {
        Dictionary headers = bundle.getHeaders();
        String header = (String) headers.get(m_extension);
        if (header != null) {
            m_bundles.add(bundle);
            try {
                m_onArrival.call(new Object[] {bundle, header});
            } catch (NoSuchMethodException e) {
                m_handler.error("The onArrival method " + m_onArrival.getMethod() + " does not exist in the class", e);
                m_handler.getInstanceManager().stop();
            } catch (IllegalAccessException e) {
                m_handler.error("The onArrival method " + m_onArrival.getMethod() + " cannot be called", e);
                m_handler.getInstanceManager().stop();
            } catch (InvocationTargetException e) {
                m_handler.error("The onArrival method " + m_onArrival.getMethod() + " has thrown an exception", e.getTargetException());
                m_handler.getInstanceManager().stop();
            }
        }
    }

    /**
     * Stop method.
     * Remove the bundle listener. 
     */
    public void stop() {
        m_context.removeBundleListener(this);
        m_bundles.clear();
    }

    /**
     * Bundle listener.
     * @param event : event.
     * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
     */
    public void bundleChanged(BundleEvent event) {
        switch (event.getType()) {
            case BundleEvent.STARTED:
                onArrival(event.getBundle());
                break;
            case BundleEvent.STOPPING:
                onDeparture(event.getBundle());
                break;
            default: 
                break;
        }
        
    }

    /**
     * Manage a bundle departure.
     * If the bundle was managed, invoke the OnDeparture callback, and remove the bundle from the list.
     * @param bundle : bundle.
     */
    private void onDeparture(Bundle bundle) {
        if (m_bundles.contains(bundle)) {
            try {
                m_onDeparture.call(new Object[] {bundle});
            } catch (NoSuchMethodException e) {
                m_handler.error("The onDeparture method " + m_onDeparture.getMethod() + " does not exist in the class", e);
                m_handler.getInstanceManager().stop();
            } catch (IllegalAccessException e) {
                m_handler.error("The onDeparture method " + m_onDeparture.getMethod() + " cannot be called", e);
                m_handler.getInstanceManager().stop();
            } catch (InvocationTargetException e) {
                m_handler.error("The onDeparture method " + m_onDeparture.getMethod() + " has thrown an exception", e.getTargetException());
                m_handler.getInstanceManager().stop();
            }
        }
    }

    

}
