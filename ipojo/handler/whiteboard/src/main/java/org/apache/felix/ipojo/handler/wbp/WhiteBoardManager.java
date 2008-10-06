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
package org.apache.felix.ipojo.handler.wbp;

import java.lang.reflect.InvocationTargetException;

import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.util.Callback;
import org.apache.felix.ipojo.util.Tracker;
import org.apache.felix.ipojo.util.TrackerCustomizer;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

/**
 * Manage a white board pattern.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class WhiteBoardManager implements TrackerCustomizer {
    
    /**
     * The monitored filter. 
     */
    private Filter m_filter;
    
    /**
     * The onArrival method. 
     */
    private Callback m_onArrival;
    
    /**
     * The onDeparture method. 
     */
    private Callback m_onDeparture;
    
    /**
     * The onModify method. 
     */
    private Callback m_onModification;
    
    /**
     * The service tracker. 
     */
    private Tracker m_tracker;
    
    /**
     * The attached handler. 
     */
    private PrimitiveHandler m_handler;
    
    /**
     * Constructor.
     * @param handler the attached handler
     * @param filter the monitored filter
     * @param bind the onArrival method
     * @param unbind the onDeparture method
     * @param modification the onModify method
     */
    public WhiteBoardManager(WhiteBoardPatternHandler handler, Filter filter, String bind, String unbind, String modification) {
        m_handler = handler;
        m_onArrival = new Callback(bind, new Class[] {ServiceReference.class}, false, m_handler.getInstanceManager());
        m_onDeparture = new Callback(unbind, new Class[] {ServiceReference.class}, false, m_handler.getInstanceManager());
        if (modification != null) {
            m_onModification = new Callback(modification, new Class[] {ServiceReference.class}, false, m_handler.getInstanceManager());
        }
        m_filter = filter;
        m_tracker = new Tracker(handler.getInstanceManager().getContext(), m_filter, this);
    }
    
    /**
     * Opens the tracker.
     */
    public void start() {
        m_tracker.open();
    }
    
    /**
     * Closes the tracker.
     */
    public void stop() {
        m_tracker.close();
    }

    /**
     * A new service was added to the tracker.
     * @param arg0 the service reference.
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#addedService(org.osgi.framework.ServiceReference)
     */
    public void addedService(ServiceReference arg0) {
        try {
            m_onArrival.call(new Object[] {arg0});
        } catch (NoSuchMethodException e) {
            m_handler.error("The onArrival method " + m_onArrival.getMethod() + " does not exist in the class", e);
            m_handler.getInstanceManager().stop();
        } catch (IllegalAccessException e) {
            m_handler.error("The onArrival method " + m_onArrival.getMethod() + " cannot be invoked", e);
            m_handler.getInstanceManager().stop();
        } catch (InvocationTargetException e) {
            m_handler.error("The onArrival method " + m_onArrival.getMethod() + " has thrown an exception", e.getTargetException());
            m_handler.getInstanceManager().stop();
        }
    }

    /**
     * A new service is detected.
     * @param arg0 the service reference
     * @return {@code true} to add the service.
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
     */
    public boolean addingService(ServiceReference arg0) {
        return true;
    }

    /**
     * An existing service was modified.
     * @param arg0 the service reference
     * @param arg1 the service object (if already get)
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    public void modifiedService(ServiceReference arg0, Object arg1) {
        if (m_onModification != null) {
            try {
                m_onModification.call(new Object[] {arg0});
            } catch (NoSuchMethodException e) {
                m_handler.error("The onModification method " + m_onModification.getMethod() + " does not exist in the class", e);
                m_handler.getInstanceManager().stop();
            } catch (IllegalAccessException e) {
                m_handler.error("The onModification method " + m_onModification.getMethod() + " cannot be invoked", e);
                m_handler.getInstanceManager().stop();
            } catch (InvocationTargetException e) {
                m_handler.error("The onModification method " + m_onModification.getMethod() + " has thrown an exception", e.getTargetException());
                m_handler.getInstanceManager().stop();
            }
        }
    }

    /**
     * A service disappears.
     * @param arg0 the service reference
     * @param arg1 the service object (if already get)
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    public void removedService(ServiceReference arg0, Object arg1) {
        try {
            m_onDeparture.call(new Object[] {arg0});
        } catch (NoSuchMethodException e) {
            m_handler.error("The onDeparture method " + m_onDeparture.getMethod() + " does not exist in the class", e);
            m_handler.getInstanceManager().stop();
        } catch (IllegalAccessException e) {
            m_handler.error("The onDeparture method " + m_onDeparture.getMethod() + " cannot be invoked", e);
            m_handler.getInstanceManager().stop();
        } catch (InvocationTargetException e) {
            m_handler.error("The onDeparture method " + m_onDeparture.getMethod() + " has thrown an exception", e.getTargetException());
            m_handler.getInstanceManager().stop();
        }
    }

}
