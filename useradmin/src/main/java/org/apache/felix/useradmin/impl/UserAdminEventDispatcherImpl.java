/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.felix.useradmin.impl;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.felix.useradmin.UserAdminEventDispatcher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminListener;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Dispatching UserAdmin events.
 * @see org.apache.felix.useradmin.UserAdminEventDispatcher
 * 
 * @version $Rev$ $Date$
 */
public class UserAdminEventDispatcherImpl extends Thread implements UserAdminEventDispatcher
{
    private Vector queue;
    private ServiceTracker userAdminTrackerListener;
    private ServiceTracker eventAdminTracker;
    private static final String userAdminTopic = "org/osgi/service/useradmin/UserAdmin/";
    private volatile boolean running;

    /**
     * This constructor is used to create UserAdmin event dispatcher.
     * It creating and opening two trackers for UserAdminListener and EventAdmin service.
     * Setting thread as a daemon.
     * @param context bundle context
     */
    public UserAdminEventDispatcherImpl(BundleContext context)
    {
        super();
        this.queue = new Vector();
        this.userAdminTrackerListener = new ServiceTracker(context, UserAdminListener.class.getName(), null);
        this.userAdminTrackerListener.open();
        this.eventAdminTracker = new ServiceTracker(context, EventAdmin.class.getName(), null);
        this.eventAdminTracker.open();
        this.running = true;
        setDaemon(true);
        setName("UserAdminEventDispatcher-Thread");
    }

    /**
     * @see org.apache.felix.useradmin.impl.UserAdminEventDispatcher#start()
     */
    public void start()
    {
        super.start();
    }

    /**
     * @see org.apache.felix.useradmin.impl.UserAdminEventDispatcher#run()
     */
    public void run()
    {
        while (running)
        {
            UserAdminEvent event = take();
            if (event != null)
            {
                notifyListeners(event);
            }

        }
    }

    /**
     * Notifying UserAdminListeners about change made to roles.
     * 
     * @param event @see org.osgi.service.useradmin.UserAdminEvent
     */
    private void notifyListeners(UserAdminEvent event)
    {
        Object[] services = userAdminTrackerListener.getServices();
        if (services != null)
        {
            for (int i = 0; i < services.length; i++)
            {
                UserAdminListener listener = ((UserAdminListener) services[i]);
                listener.roleChanged(event);
            }
        }
    }

    /**
     * @see
     * org.apache.felix.useradmin.impl.UserAdminEventDispatcher#dispatchEventAsynchronusly(org.osgi.service.useradmin
     * .UserAdminEvent)
     */
    public synchronized void dispatchEventAsynchronusly(UserAdminEvent userAdminEvent)
    {
        EventAdmin eventAdmin = (EventAdmin) eventAdminTracker.getService();
        if (eventAdmin != null)
        {
            Event event = createEvent(userAdminEvent);
            eventAdmin.postEvent(event);
        }
        queue.add(userAdminEvent);
        notifyAll();
    }

    /**
     * This method is consuming event from the queue if queue is empty and dispatcher is running
     * waiting for events. 
     * @return UserAdmin event
     */
    private synchronized UserAdminEvent take()
    {

        while (running && queue.isEmpty())
        {
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
            }
        }

        if (running)
        {
            UserAdminEvent event = (UserAdminEvent) queue.get(0);
            queue.removeElementAt(0);
            return event;
        }
        else
        {
            return null;
        }

    }

    /**
     * Closing UserAdminTrackers and putting running state to false.
     * @see org.apache.felix.useradmin.impl.UserAdminEventDispatcher#close()
     */
    public void close()
    {
        userAdminTrackerListener.close();
        running = true;
    }

    /**
     * This method is creating OSGi event from UserAdminEvent.
     * @param userAdminEvent event.
     * @return OSGi event converted from UserAdmin event.
     */
    private Event createEvent(UserAdminEvent userAdminEvent)
    {
        String topic = getEventAdminTopic(userAdminEvent.getType());
        Dictionary eventProperties = new Hashtable();

        eventProperties.put(EventConstants.EVENT_TOPIC, topic);
        eventProperties.put(EventConstants.EVENT, userAdminEvent);
        eventProperties.put(EventConstants.TIMESTAMP, new Long(System.currentTimeMillis()));
        eventProperties.put("role", userAdminEvent.getRole());
        eventProperties.put("role.name", userAdminEvent.getRole().getName());
        eventProperties.put("role.type", new Integer(userAdminEvent.getRole().getType()));
        eventProperties.put(EventConstants.SERVICE, userAdminEvent.getServiceReference());
        eventProperties.put(EventConstants.SERVICE_ID, userAdminEvent.getServiceReference().getProperty(
            Constants.SERVICE_ID));
        eventProperties.put(EventConstants.SERVICE_OBJECTCLASS, userAdminEvent.getServiceReference().getProperty(
            Constants.OBJECTCLASS));
        eventProperties.put(EventConstants.SERVICE_PID, userAdminEvent.getServiceReference().getProperty(
            Constants.SERVICE_PID));

        return new Event(topic, eventProperties);

    }

    /**
     * This method is getting topic for specific event type. 
     * @param type role type.
     * @return OSGi topic specific for UserAdminEvent.
     */
    private String getEventAdminTopic(int type)
    {
        String evtType = "?";

        switch (type)
        {
            case UserAdminEvent.ROLE_CREATED:
                evtType = "ROLE_CREATED";
                break;
            case UserAdminEvent.ROLE_CHANGED:
                evtType = "ROLE_CHANGED";
                break;
            case UserAdminEvent.ROLE_REMOVED:
                evtType = "ROLE_REMOVED";
                break;
        }
        return userAdminTopic + evtType;
    }
}
