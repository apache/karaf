/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.framework.util;

import java.util.EventListener;
import java.util.EventObject;

import org.apache.felix.framework.LogWrapper;
import org.osgi.framework.*;

/**
 * This is a subclass of <tt>DispatchQueue</tt> that specifically adds support
 * for <tt>SynchronousBundleListener</tt>s; the OSGi specification
 * says that synchronous bundle listeners should receive their events
 * immediately, i.e., they should not be delivered on a separate thread
 * like is the case with the <tt>DispatchQueue</tt>. To achieve this
 * functionality, this class overrides the dispatch method to automatically
 * fire any bundle events to synchronous bundle listeners using the
 * calling thread. In order to ensure that synchronous bundle listeners
 * do not receive an event twice, it wraps the passed in <tt>Dipatcher</tt>
 * instance so that it filters synchronous bundle listeners.
**/
public class FelixDispatchQueue extends DispatchQueue
{
    public FelixDispatchQueue(LogWrapper logger)
    {
        super(logger);
    }

    /**
     * Dispatches an event to a set of event listeners using a specified
     * dispatcher object. This overrides the definition of the super class
     * to deliver events to <tt>ServiceListener</tt>s and
     * <tt>SynchronousBundleListener</tt>s using
     * the calling thread instead of the event dispatching thread. All
     * other events are still delivered asynchronously.
     *
     * @param dispatcher the dispatcher used to actually dispatch the event; this
     *          varies according to the type of event listener.
     * @param clazz the class associated with the target event listener type;
     *              only event listeners of this type will receive the event.
     * @param eventObj the actual event object to dispatch.
    **/
    public void dispatch(Dispatcher dispatcher, Class clazz, EventObject eventObj)
    {
        Object[] listeners = getListeners();

        // If this is an event for service listeners, then dispatch it
        // immediately since service events are never asynchronous.
        if ((clazz == ServiceListener.class) && (listeners.length > 0))
        {
            // Notify appropriate listeners.
            for (int i = listeners.length - 2; i >= 0; i -= 2)
            {
                // If the original listener is a synchronous bundle listener
                // or a service listener, then dispatch event immediately
                // per the specification.
                ListenerWrapper lw = (ListenerWrapper) listeners[i + 1];
                if (lw.getListenerClass() == ServiceListener.class)
                {
                    try {
                        dispatcher.dispatch(
                            (EventListener) lw, eventObj);
                    } catch (Throwable th) {
                        getLogger().log(
                            LogWrapper.LOG_ERROR,
                            "FelixDispatchQueue: Error during dispatch.", th);
                    }
                }
            }
        }
        // Dispatch bundle events to synchronous bundle listeners immediately,
        // but deliver to standard bundle listeners asynchronously.
        else if ((clazz == BundleListener.class) && (listeners.length > 0))
        {
            // Notify appropriate listeners.
            for (int i = listeners.length - 2; i >= 0; i -= 2)
            {
                // If the original listener is a synchronous bundle listener,
                // then dispatch event immediately per the specification.
                ListenerWrapper lw = (ListenerWrapper) listeners[i + 1];
                if (lw.getListenerClass() == SynchronousBundleListener.class)
                {
                    try {
                        dispatcher.dispatch(
                            (EventListener) lw, eventObj);
                    } catch (Throwable th) {
                        getLogger().log(
                            LogWrapper.LOG_ERROR,
                            "FelixDispatchQueue: Error during dispatch.", th);
                    }
                }
            }

            // Wrap the dispatcher so that it ignores synchronous
            // bundle listeners since they have already been dispatched.
            IgnoreSynchronousDispatcher ignoreDispatcher = new IgnoreSynchronousDispatcher();
            ignoreDispatcher.setDispatcher(dispatcher);

            // Dispatch the bundle listener asynchronously.
            dispatch(listeners, ignoreDispatcher, clazz, eventObj);
        }
        // All other events are dispatched asynchronously.
        else
        {
            dispatch(listeners, dispatcher, clazz, eventObj);
        }
    }

    private static class IgnoreSynchronousDispatcher implements Dispatcher
    {
        private Dispatcher m_dispatcher = null;

        public void setDispatcher(Dispatcher dispatcher)
        {
            m_dispatcher = dispatcher;
        }

        public void dispatch(EventListener l, EventObject eventObj)
        {
            if (l instanceof ListenerWrapper)
            {
                ListenerWrapper lw = (ListenerWrapper) l;
                // Do not dispatch events to synchronous listeners,
                // since they are dispatched immediately above.
                if (!(lw.getListenerClass() == SynchronousBundleListener.class))
                {
                    m_dispatcher.dispatch(l, eventObj);
                }
            }
        }
    }
}