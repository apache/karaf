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
package org.apache.felix.framework.util;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.Logger;
import org.osgi.framework.*;

public class EventDispatcher
{
    private static final int LISTENER_BUNDLE_OFFSET = 0;
    private static final int LISTENER_CLASS_OFFSET = 1;
    private static final int LISTENER_OBJECT_OFFSET = 2;
    private static final int LISTENER_FILTER_OFFSET = 3;
    private static final int LISTENER_SECURITY_OFFSET = 4;
    private static final int LISTENER_ARRAY_INCREMENT = 5;

    // Framework instance for this dispatcher.
    private Logger m_logger = null;

    // Representation of an empty listener list.
    private static final Object[] m_emptyList = new Object[0];

    private Object[] m_frameworkListeners = m_emptyList;
    private Object[] m_bundleListeners = m_emptyList;
    private Object[] m_syncBundleListeners = m_emptyList;
    private Object[] m_serviceListeners = m_emptyList;

    // A single thread is used to deliver events for all dispatchers.
    private static Thread m_thread = null;
    private static String m_threadLock = "thread lock";
    private static int m_references = 0;
    private static boolean m_stopping = false;
    private static boolean m_stopped = false;
    // List of requests.
    private static final ArrayList m_requestList = new ArrayList();
    // Pooled requests to avoid memory allocation.
    private static final ArrayList m_requestPool = new ArrayList();

    private EventDispatcher(Logger logger)
    {
        m_logger = logger;
    }

    public static EventDispatcher start(Logger logger)
    {
        EventDispatcher eventDispatcher = new EventDispatcher(logger);

        synchronized (m_threadLock)
        {
            // Start event dispatching thread if necessary.
            if (m_thread == null)
            {
                m_thread = new Thread(new Runnable() {
                    public void run()
                    {
                        EventDispatcher.run();
                    }
                }, "FelixDispatchQueue");
                m_thread.start();
            }

            // reference counting and flags
            m_references++;
            m_stopping = false;
            m_stopped = false;
        }

        return eventDispatcher;
    }
    
    public static void shutdown()
    {
        synchronized (m_threadLock)
        {
            // Return if already stopped.
            if (m_stopped)
            {
                return;
            }

            // decrement use counter, don't continue if there are users
            m_references--;
            if (m_references > 0)
            {
                return;
            }
            
            // Signal dispatch thread.
            m_stopping = true;
            synchronized (m_requestList)
            {
                m_requestList.notify();
            }

            // Wait for dispatch thread to stop.
            while (!m_stopped)
            {
                try
                {
                    m_threadLock.wait();
                }
                catch (InterruptedException ex)
                {
                }
            }
            
            // remove the thread reference
            m_thread = null;
        }
    }

    public void addListener(Bundle bundle, Class clazz, EventListener l, Filter filter)
    {
        // Verify the listener.
        if (l == null)
        {
            throw new IllegalArgumentException("Listener is null");
        }
        else if (!clazz.isInstance(l))
        {
            throw new IllegalArgumentException(
                "Listener not of type " + clazz.getName());
        }

        // See if we can simply update the listener, if so then
        // return immediately.
        if (updateListener(bundle, clazz, l, filter))
        {
            return;
        }

        // Lock the object to add the listener.
        synchronized (this)
        {
            Object[] listeners = null;
            Object acc = null;

            if (clazz == FrameworkListener.class)
            {
                listeners = m_frameworkListeners;
            }
            else if (clazz == BundleListener.class)
            {
                if (SynchronousBundleListener.class.isInstance(l))
                {
                    listeners = m_syncBundleListeners;
                }
                else
                {
                    listeners = m_bundleListeners;
                }
            }
            else if (clazz == ServiceListener.class)
            {
                // Remember security context for filtering service events.
                Object sm = System.getSecurityManager();
                if (sm != null)
                {
                    acc = ((SecurityManager) sm).getSecurityContext();
                }

                listeners = m_serviceListeners;
            }
            else
            {
                throw new IllegalArgumentException("Unknown listener: " + l.getClass());
            }

            // If we have no listeners, then just add the new listener.
            if (listeners == m_emptyList)
            {
                listeners = new Object[LISTENER_ARRAY_INCREMENT];
                listeners[LISTENER_BUNDLE_OFFSET] = bundle;
                listeners[LISTENER_CLASS_OFFSET] = clazz;
                listeners[LISTENER_OBJECT_OFFSET] = l;
                listeners[LISTENER_FILTER_OFFSET] = filter;
                listeners[LISTENER_SECURITY_OFFSET] = acc;
            }
            // Otherwise, we need to do some array copying.
            // Notice, the old array is always valid, so if
            // the dispatch thread is in the middle of a dispatch,
            // then it has a reference to the old listener array
            // and is not affected by the new value.
            else
            {
                Object[] newList = new Object[listeners.length + LISTENER_ARRAY_INCREMENT];
                System.arraycopy(listeners, 0, newList, 0, listeners.length);
                newList[listeners.length + LISTENER_BUNDLE_OFFSET] = bundle;
                newList[listeners.length + LISTENER_CLASS_OFFSET] = clazz;
                newList[listeners.length + LISTENER_OBJECT_OFFSET] = l;
                newList[listeners.length + LISTENER_FILTER_OFFSET] = filter;
                newList[listeners.length + LISTENER_SECURITY_OFFSET] = acc;
                listeners = newList;
            }

            if (clazz == FrameworkListener.class)
            {
                m_frameworkListeners = listeners;
            }
            else if (clazz == BundleListener.class)
            {
                if (SynchronousBundleListener.class.isInstance(l))
                {
                    m_syncBundleListeners = listeners;
                }
                else
                {
                    m_bundleListeners = listeners;
                }
            }
            else if (clazz == ServiceListener.class)
            {
                m_serviceListeners = listeners;
            }
        }
    }

    public void removeListener(Bundle bundle, Class clazz, EventListener l)
    {
        // Verify listener.
        if (l == null)
        {
            throw new IllegalArgumentException("Listener is null");
        }
        else if (!clazz.isInstance(l))
        {
            throw new IllegalArgumentException(
                "Listener not of type " + clazz.getName());
        }

        // Lock the object to remove the listener.
        synchronized (this)
        {
            Object[] listeners = null;

            if (clazz == FrameworkListener.class)
            {
                listeners = m_frameworkListeners;
            }
            else if (clazz == BundleListener.class)
            {
                if (SynchronousBundleListener.class.isInstance(l))
                {
                    listeners = m_syncBundleListeners;
                }
                else
                {
                    listeners = m_bundleListeners;
                }
            }
            else if (clazz == ServiceListener.class)
            {
                listeners = m_serviceListeners;
            }
            else
            {
                throw new IllegalArgumentException("Unknown listener: " + l.getClass());
            }

            // Try to find the instance in our list.
            int idx = -1;
            for (int i = 0; i < listeners.length; i += LISTENER_ARRAY_INCREMENT)
            {
                if (listeners[i + LISTENER_BUNDLE_OFFSET].equals(bundle) &&
                    (listeners[i + LISTENER_CLASS_OFFSET] == clazz) &&
                    (listeners[i + LISTENER_OBJECT_OFFSET] == l))
                {
                    idx = i;
                    break;
                }
            }

            // If we have the instance, then remove it.
            if (idx >= 0)
            {
                // If this is the last listener, then point to empty list.
                if ((listeners.length - LISTENER_ARRAY_INCREMENT) == 0)
                {
                    listeners = m_emptyList;
                }
                // Otherwise, we need to do some array copying.
                // Notice, the old array is always valid, so if
                // the dispatch thread is in the middle of a dispatch,
                // then it has a reference to the old listener array
                // and is not affected by the new value.
                else
                {
                    Object[] newList  = new Object[listeners.length - LISTENER_ARRAY_INCREMENT];
                    System.arraycopy(listeners, 0, newList, 0, idx);
                    if (idx < newList.length)
                    {
                        System.arraycopy(
                            listeners, idx + LISTENER_ARRAY_INCREMENT,
                            newList, idx, newList.length - idx);
                    }
                    listeners = newList;
                }
            }

            if (clazz == FrameworkListener.class)
            {
                m_frameworkListeners = listeners;
            }
            else if (clazz == BundleListener.class)
            {
                if (SynchronousBundleListener.class.isInstance(l))
                {
                    m_syncBundleListeners = listeners;
                }
                else
                {
                    m_bundleListeners = listeners;
                }
            }
            else if (clazz == ServiceListener.class)
            {
                m_serviceListeners = listeners;
            }
        }
    }

    public void removeListeners(Bundle bundle)
    {
        if (bundle == null)
        {
            return;
        }

        synchronized (this)
        {
            // Remove all framework listeners associated with the specified bundle.
            Object[] listeners = m_frameworkListeners;
            for (int i = listeners.length - LISTENER_ARRAY_INCREMENT;
                i >= 0;
                i -= LISTENER_ARRAY_INCREMENT)
            {
                // Check if the bundle associated with the current listener
                // is the same as the specified bundle, if so remove the listener.
                Bundle registeredBundle = (Bundle) listeners[i + LISTENER_BUNDLE_OFFSET];
                if (bundle.equals(registeredBundle))
                {
                    Class clazz = (Class) listeners[i + LISTENER_CLASS_OFFSET];
                    EventListener l = (EventListener) listeners[i + LISTENER_OBJECT_OFFSET];
                    removeListener(bundle, clazz, l);
                }
            }

            // Remove all bundle listeners associated with the specified bundle.
            listeners = m_bundleListeners;
            for (int i = listeners.length - LISTENER_ARRAY_INCREMENT;
                i >= 0;
                i -= LISTENER_ARRAY_INCREMENT)
            {
                // Check if the bundle associated with the current listener
                // is the same as the specified bundle, if so remove the listener.
                Bundle registeredBundle = (Bundle) listeners[i + LISTENER_BUNDLE_OFFSET];
                if (bundle.equals(registeredBundle))
                {
                    Class clazz = (Class) listeners[i + LISTENER_CLASS_OFFSET];
                    EventListener l = (EventListener) listeners[i + LISTENER_OBJECT_OFFSET];
                    removeListener(bundle, clazz, l);
                }
            }

            // Remove all synchronous bundle listeners associated with
            // the specified bundle.
            listeners = m_syncBundleListeners;
            for (int i = listeners.length - LISTENER_ARRAY_INCREMENT;
                i >= 0;
                i -= LISTENER_ARRAY_INCREMENT)
            {
                // Check if the bundle associated with the current listener
                // is the same as the specified bundle, if so remove the listener.
                Bundle registeredBundle = (Bundle) listeners[i + LISTENER_BUNDLE_OFFSET];
                if (bundle.equals(registeredBundle))
                {
                    Class clazz = (Class) listeners[i + LISTENER_CLASS_OFFSET];
                    EventListener l = (EventListener) listeners[i + LISTENER_OBJECT_OFFSET];
                    removeListener(bundle, clazz, l);
                }
            }

            // Remove all service listeners associated with the specified bundle.
            listeners = m_serviceListeners;
            for (int i = listeners.length - LISTENER_ARRAY_INCREMENT;
                i >= 0;
                i -= LISTENER_ARRAY_INCREMENT)
            {
                // Check if the bundle associated with the current listener
                // is the same as the specified bundle, if so remove the listener.
                Bundle registeredBundle = (Bundle) listeners[i + LISTENER_BUNDLE_OFFSET];
                if (bundle.equals(registeredBundle))
                {
                    Class clazz = (Class) listeners[i + LISTENER_CLASS_OFFSET];
                    EventListener l = (EventListener) listeners[i + LISTENER_OBJECT_OFFSET];
                    removeListener(bundle, clazz, l);
                }
            }
        }
    }

    public boolean updateListener(Bundle bundle, Class clazz, EventListener l, Filter filter)
    {
        synchronized (this)
        {
            Object[] listeners = null;

            if (clazz == FrameworkListener.class)
            {
                listeners = m_frameworkListeners;
            }
            else if (clazz == BundleListener.class)
            {
                if (SynchronousBundleListener.class.isInstance(l))
                {
                    listeners = m_syncBundleListeners;
                }
                else
                {
                    listeners = m_bundleListeners;
                }
            }
            else if (clazz == ServiceListener.class)
            {
                listeners = m_serviceListeners;
            }
    
            // See if the listener is already registered, if so then
            // handle it according to the spec.
            for (int i = 0; i < listeners.length; i += LISTENER_ARRAY_INCREMENT)
            {
                if (listeners[i + LISTENER_BUNDLE_OFFSET].equals(bundle) &&
                    (listeners[i + LISTENER_CLASS_OFFSET] == clazz) &&
                    (listeners[i + LISTENER_OBJECT_OFFSET] == l))
                {
                    if (clazz == FrameworkListener.class)
                    {
                        // The spec says to ignore this case.
                    }
                    else if (clazz == BundleListener.class)
                    {
                        // The spec says to ignore this case.
                    }
                    else if (clazz == ServiceListener.class)
                    {
                        // The spec says to update the filter in this case.
                        listeners[i + LISTENER_FILTER_OFFSET] = filter;
                    }
                    return true;
                }
            }
        }

        return false;
    }

    public void fireFrameworkEvent(FrameworkEvent event)
    {
        // Take a snapshot of the listener array.
        Object[] listeners = null;
        synchronized (this)
        {
            listeners = m_frameworkListeners;
        }

        // Fire all framework listeners on a separate thread.
        fireEventAsynchronously(m_logger, Request.FRAMEWORK_EVENT, listeners, event);
    }

    public void fireBundleEvent(BundleEvent event)
    {
        // Take a snapshot of the listener array.
        Object[] listeners = null;
        Object[] syncListeners = null;
        synchronized (this)
        {
            listeners = m_bundleListeners;
            syncListeners = m_syncBundleListeners;
        }

        // Fire synchronous bundle listeners immediately on the calling thread.
        fireEventImmediately(m_logger, Request.BUNDLE_EVENT, syncListeners, event);

        // The spec says that asynchronous bundle listeners do not get events
        // of types STARTING or STOPPING.
        if ((event.getType() != BundleEvent.STARTING) &&
            (event.getType() != BundleEvent.STOPPING))
        {
            // Fire asynchronous bundle listeners on a separate thread.
            fireEventAsynchronously(m_logger, Request.BUNDLE_EVENT, listeners, event);
        }
    }

    public void fireServiceEvent(ServiceEvent event)
    {
        // Take a snapshot of the listener array.
        Object[] listeners = null;
        synchronized (this)
        {
            listeners = m_serviceListeners;
        }

        // Fire all service events immediately on the calling thread.
        fireEventImmediately(m_logger, Request.SERVICE_EVENT, listeners, event);
    }

    private void fireEventAsynchronously(
        Logger logger, int type, Object[] listeners, EventObject event)
    {
        // If dispatch thread is stopped, then ignore dispatch request.
        if (m_stopped || m_stopping)
        {
            return;
        }

        // First get a request from the pool or create one if necessary.
        Request req = null;
        synchronized (m_requestPool)
        {
            if (m_requestPool.size() > 0)
            {
                req = (Request) m_requestPool.remove(0);
            }
            else
            {
                req = new Request();
            }
        }

        // Initialize dispatch request.
        req.m_logger = logger;
        req.m_type = type;
        req.m_listeners = listeners;
        req.m_event = event;

        // Lock the request list.
        synchronized (m_requestList)
        {
            // Add our request to the list.
            m_requestList.add(req);
            // Notify the dispatch thread that there is work to do.
            m_requestList.notify();
        }
    }

    private static void fireEventImmediately(
        Logger logger, int type, Object[] listeners, EventObject event)
    {
        if (listeners.length > 0)
        {
            // Notify appropriate listeners.
            for (int i = listeners.length - LISTENER_ARRAY_INCREMENT;
                i >= 0;
                i -= LISTENER_ARRAY_INCREMENT)
            {
                Bundle bundle = (Bundle) listeners[i + LISTENER_BUNDLE_OFFSET];
                EventListener l = (EventListener) listeners[i + LISTENER_OBJECT_OFFSET];
                Filter filter = (Filter) listeners[i + LISTENER_FILTER_OFFSET];
                Object acc = listeners[i + LISTENER_SECURITY_OFFSET];
                try
                {
                    if (type == Request.FRAMEWORK_EVENT)
                    {
                        invokeFrameworkListenerCallback(bundle, l, event);
                    }
                    else if (type == Request.BUNDLE_EVENT)
                    {
                        invokeBundleListenerCallback(bundle, l, event);
                    }
                    else if (type == Request.SERVICE_EVENT)
                    {
                        invokeServiceListenerCallback(bundle, l, filter, acc, event);
                    }
                }
                catch (Throwable th)
                {
                    logger.log(
                        Logger.LOG_ERROR,
                        "EventDispatcher: Error during dispatch.", th);
                }
            }
        }
    }

    private static void invokeFrameworkListenerCallback(
        Bundle bundle, final EventListener l, final EventObject event)
    {
        // The spec says only active bundles receive asynchronous events,
        // but we will include starting bundles too otherwise
        // it is impossible to see everything.
        if ((bundle.getState() == Bundle.STARTING) ||
            (bundle.getState() == Bundle.ACTIVE))
        {
            if (System.getSecurityManager() != null)
            {
                AccessController.doPrivileged(new PrivilegedAction() {
                    public Object run()
                    {
                        ((FrameworkListener) l).frameworkEvent((FrameworkEvent) event);
                        return null;
                    }
                });
            }
            else
            {
                ((FrameworkListener) l).frameworkEvent((FrameworkEvent) event);
            }
        }
    }

    private static void invokeBundleListenerCallback(
        Bundle bundle, final EventListener l, final EventObject event)
    {
        // A bundle listener is either synchronous or asynchronous.
        // If the bundle listener is synchronous, then deliver the
        // event to bundles with a state of STARTING, STOPPING, or
        // ACTIVE. If the listener is asynchronous, then deliver the
        // event only to bundles that are STARTING or ACTIVE.
        if (((SynchronousBundleListener.class.isAssignableFrom(l.getClass())) &&
            ((bundle.getState() == Bundle.STARTING) ||
            (bundle.getState() == Bundle.STOPPING) ||
            (bundle.getState() == Bundle.ACTIVE)))
            ||
            ((bundle.getState() == Bundle.STARTING) ||
            (bundle.getState() == Bundle.ACTIVE)))
        {
            if (System.getSecurityManager() != null)
            {
                AccessController.doPrivileged(new PrivilegedAction() {
                    public Object run()
                    {
                        ((BundleListener) l).bundleChanged((BundleEvent) event);
                        return null;
                    }
                });
            }
            else
            {
                ((BundleListener) l).bundleChanged((BundleEvent) event);
            }
        }
    }

    private static void invokeServiceListenerCallback(
        Bundle bundle, final EventListener l, Filter filter, Object acc, final EventObject event)
    {
        // Service events should be delivered to STARTING,
        // STOPPING, and ACTIVE bundles.
        if ((bundle.getState() != Bundle.STARTING) &&
            (bundle.getState() != Bundle.STOPPING) &&
            (bundle.getState() != Bundle.ACTIVE))
        {
            return;
        }

        // Check that the bundle has permission to get at least
        // one of the service interfaces; the objectClass property
        // of the service stores its service interfaces.
        ServiceReference ref = ((ServiceEvent) event).getServiceReference();
        String[] objectClass = (String[]) ref.getProperty(Constants.OBJECTCLASS);

        // On the safe side, if there is no objectClass property
        // then ignore event altogether.
        if (objectClass != null)
        {
            boolean hasPermission = false;
            
            Object sm = System.getSecurityManager();
            if ((acc != null) && (sm != null))
            {
                for (int i = 0;
                    !hasPermission && (i < objectClass.length);
                    i++)
                {
                    try 
                    {
                        ServicePermission perm =
                            new ServicePermission(
                                objectClass[i], ServicePermission.GET);
                        ((SecurityManager) sm).checkPermission(perm, acc);
                        hasPermission = true;
                    } 
                    catch (Exception ex) 
                    {
                    }
                }
            }
            else
            {
                hasPermission = true;
            }

            if (hasPermission)
            {
                // Dispatch according to the filter.
                if ((filter == null) || filter.match(((ServiceEvent) event).getServiceReference()))
                {
                    if ((l instanceof AllServiceListener) ||
                        Util.isServiceAssignable(bundle, ((ServiceEvent) event).getServiceReference()))
                    {
                        if (System.getSecurityManager() != null)
                        {
                            AccessController.doPrivileged(new PrivilegedAction() {
                                public Object run()
                                {
                                    ((ServiceListener) l).serviceChanged((ServiceEvent) event);
                                    return null;
                                }
                            });
                        }
                        else
                        {
                            {
                                ((ServiceListener) l).serviceChanged((ServiceEvent) event);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * This is the dispatching thread's main loop.
    **/
    private static void run()
    {
        Request req = null;
        while (true)
        {
            // Lock the request list so we can try to get a
            // dispatch request from it.
            synchronized (m_requestList)
            {
                // Wait while there are no requests to dispatch. If the
                // dispatcher thread is supposed to stop, then let the
                // dispatcher thread exit the loop and stop.
                while ((m_requestList.size() == 0) && !m_stopping)
                {
                    // Wait until some signals us for work.
                    try
                    {
                        m_requestList.wait();
                    }
                    catch (InterruptedException ex)
                    {
                        // Not much we can do here except for keep waiting.
                    }
                }

                // If there are no events to dispatch and shutdown
                // has been called then exit, otherwise dispatch event.
                if ((m_requestList.size() == 0) && (m_stopping))
                {
                    synchronized (m_threadLock)
                    {
                        m_stopped = true;
                        m_threadLock.notifyAll();
                    }
                    return;
                }

                // Get the dispatch request.
                req = (Request) m_requestList.remove(0);
            }

            // Deliver event outside of synchronized block
            // so that we don't block other requests from being
            // queued during event processing.
            fireEventImmediately(req.m_logger, req.m_type, req.m_listeners, req.m_event);

            // Put dispatch request in cache.
            synchronized (m_requestPool)
            {
                req.m_logger = null;
                req.m_type = -1;
                req.m_listeners = null;
                req.m_event = null;
                m_requestPool.add(req);
            }
        }
    }

    private static class Request
    {
        public static final int FRAMEWORK_EVENT = 0;
        public static final int BUNDLE_EVENT = 1;
        public static final int SERVICE_EVENT = 2;

        public Logger m_logger = null;
        public int m_type = -1;
        public Object[] m_listeners = null;
        public EventObject m_event = null;
    }
}
