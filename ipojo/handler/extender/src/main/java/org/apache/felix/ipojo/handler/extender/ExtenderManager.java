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
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.util.Callback;
import org.osgi.framework.Bundle;

/**
 * Track and manage extensions.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ExtenderManager extends BundleTracker {
    
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
     * Set of managed bundles.
     */
    private Set m_bundles = new HashSet();
    
    /**
     * Constructor.
     * @param handler the attached handler.
     * @param extension the looked extension.
     * @param bind the onArrival method
     * @param unbind the onDeparture method.
     */
    public ExtenderManager(ExtenderModelHandler handler, String extension, String bind, String unbind) {
        super(handler.getInstanceManager().getContext());
        m_handler = handler;
        m_onArrival = new Callback(bind, new Class[] {Bundle.class, String.class}, false, m_handler.getInstanceManager());
        m_onDeparture = new Callback(unbind, new Class[] {Bundle.class}, false, m_handler.getInstanceManager());
        m_extension = extension;
    }


    /**
     * A bundle arrives.
     * Checks if the bundle match with the looked extension, if so call the arrival callback.
     * @param bundle the arriving bundle.
     * @see org.apache.felix.ipojo.handler.extender.BundleTracker#addedBundle(org.osgi.framework.Bundle)
     */
    protected void addedBundle(Bundle bundle) {
        Dictionary headers = bundle.getHeaders();
        String header = (String) headers.get(m_extension);
        if (header != null) {
            synchronized (this) {
                m_bundles.add(bundle);
            }
            try { // Call the callback outside the synchronized block.
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
     * A bundle is stopping.
     * Check if the bundle was managed, if so call the remove departure callback.
     * @param bundle the leaving bundle.
     * @see org.apache.felix.ipojo.handler.extender.BundleTracker#removedBundle(org.osgi.framework.Bundle)
     */
    protected void removedBundle(Bundle bundle) {
        boolean contained;
        synchronized (this) {
            contained = m_bundles.remove(bundle); // Stack confinement
        }
        
        if (contained) {
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
