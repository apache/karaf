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
package org.apache.felix.dependencymanager;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * TODO copied this from the OSGi specification, but it's not clear if that
 * is allowed or not, for now I modified as little as possible but I might
 * integrate only the parts I want as soon as this code is finished. Perhaps
 * it would be better to borrow the Knopflerfish implementation here.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceTracker implements ServiceTrackerCustomizer
{
    /**
	 * Bundle context this <tt>ServiceTracker</tt> object is tracking against.
	 */
    protected final BundleContext context;

    /**
	 * Filter specifying search criteria for the services to track.
	 * @since 1.1
	 */
    protected final Filter filter;

    /**
	 * Tracked services: <tt>ServiceReference</tt> object -> customized Object
	 * and <tt>ServiceListener</tt> object
	 */
    private Tracked tracked;

    /** <tt>ServiceTrackerCustomizer</tt> object for this tracker. */
    private ServiceTrackerCustomizer customizer;

    /**
	 * Create a <tt>ServiceTracker</tt> object on the specified <tt>ServiceReference</tt> object.
	 *
	 * <p>The service referenced by the specified <tt>ServiceReference</tt> object
	 * will be tracked by this <tt>ServiceTracker</tt> object.
	 *
	 * @param context   <tt>BundleContext</tt> object against which the tracking is done.
	 * @param reference <tt>ServiceReference</tt> object for the service to be tracked.
	 * @param customizer The customizer object to call when services are
	 * added, modified, or removed in this <tt>ServiceTracker</tt> object.
	 * If customizer is <tt>null</tt>, then this <tt>ServiceTracker</tt> object will be used
	 * as the <tt>ServiceTrackerCustomizer</tt> object and the <tt>ServiceTracker</tt>
	 * object will call the <tt>ServiceTrackerCustomizer</tt> methods on itself.
	 */
    public ServiceTracker(BundleContext context, ServiceReference reference,
                          ServiceTrackerCustomizer customizer)
    {
        this.context = context;
        this.customizer = (customizer == null) ? this : customizer;

        try
        {
            this.filter = context.createFilter("("+Constants.SERVICE_ID+"="+reference.getProperty(Constants.SERVICE_ID).toString()+")");
        }
        catch (InvalidSyntaxException e)
        {
            throw new RuntimeException("unexpected InvalidSyntaxException: "+e.getMessage());
        }
    }

    /**
	 * Create a <tt>ServiceTracker</tt> object on the specified class name.
	 *
	 * <p>Services registered under the specified class name will be tracked
	 * by this <tt>ServiceTracker</tt> object.
	 *
	 * @param context   <tt>BundleContext</tt> object against which the tracking is done.
	 * @param clazz     Class name of the services to be tracked.
	 * @param customizer The customizer object to call when services are
	 * added, modified, or removed in this <tt>ServiceTracker</tt> object.
	 * If customizer is <tt>null</tt>, then this <tt>ServiceTracker</tt> object will be used
	 * as the <tt>ServiceTrackerCustomizer</tt> object and the <tt>ServiceTracker</tt> object
	 * will call the <tt>ServiceTrackerCustomizer</tt> methods on itself.
	 */
    public ServiceTracker(BundleContext context, String clazz,
                          ServiceTrackerCustomizer customizer)
    {
        this.context = context;
        this.customizer = (customizer == null) ? this : customizer;

        try
        {
            this.filter = context.createFilter("("+Constants.OBJECTCLASS+"="+clazz+")");
        }
        catch (InvalidSyntaxException e)
        {
            throw new RuntimeException("unexpected InvalidSyntaxException: "+e.getMessage());
        }

        if (clazz == null)
        {
            throw new NullPointerException();
        }
    }

    /**
	 * Create a <tt>ServiceTracker</tt> object on the specified <tt>Filter</tt> object.
	 *
	 * <p>Services which match the specified <tt>Filter</tt> object will be tracked
	 * by this <tt>ServiceTracker</tt> object.
	 *
	 * @param context   <tt>BundleContext</tt> object against which the tracking is done.
	 * @param filter    <tt>Filter</tt> object to select the services to be tracked.
	 * @param customizer The customizer object to call when services are
	 * added, modified, or removed in this <tt>ServiceTracker</tt> object.
	 * If customizer is null, then this <tt>ServiceTracker</tt> object will be used
	 * as the <tt>ServiceTrackerCustomizer</tt> object and the <tt>ServiceTracker</tt>
	 * object will call the <tt>ServiceTrackerCustomizer</tt> methods on itself.
	 * @since 1.1
	 */
    public ServiceTracker(BundleContext context, Filter filter,
                          ServiceTrackerCustomizer customizer)
    {
        this.context = context;
        this.filter = filter;
        this.customizer = (customizer == null) ? this : customizer;

        if ((context == null) || (filter == null))
        {
            throw new NullPointerException();
        }
    }

    /**
	 * Open this <tt>ServiceTracker</tt> object and begin tracking services.
	 *
	 * <p>Services which match the search criteria specified when
	 * this <tt>ServiceTracker</tt> object was created are now tracked
	 * by this <tt>ServiceTracker</tt> object.
	 *
	 * @throws java.lang.IllegalStateException if the <tt>BundleContext</tt>
	 * object with which this <tt>ServiceTracker</tt> object was created is no longer valid.
	 */
    public synchronized void open()
    {
        if (tracked == null)
        {
            tracked = new Tracked(customizer, filter);

            ServiceReference[] references;

            synchronized (tracked)
            {
                context.addServiceListener(tracked);

                try
                {
                    references = context.getServiceReferences(null, filter.toString());
                }
                catch (InvalidSyntaxException e)
                {
                    throw new RuntimeException("unexpected InvalidSyntaxException");
                }
            }

            /* Call tracked outside of synchronized region */
            if (references != null)
            {
                int size = references.length;

                for (int i=0; i < size; i++)
                {
                    ServiceReference reference = references[i];

                    /* if the service is still registered */
                    if (reference.getBundle() != null)
                    {
                        tracked.track(reference);
                    }
                }
            }
        }
    }

    /**
	 * Close this <tt>ServiceTracker</tt> object.
	 *
	 * <p>This method should be called when this <tt>ServiceTracker</tt> object
	 * should end the tracking of services.
	 */

    public synchronized void close()
    {
        if (tracked != null)
        {
            tracked.close();

            ServiceReference references[] = getServiceReferences();

            Tracked outgoing = tracked;
            tracked = null;

            try
            {
                context.removeServiceListener(outgoing);
            }
            catch (IllegalStateException e)
            {
                /* In case the context was stopped. */
            }

            if (references != null)
            {
                for (int i = 0; i < references.length; i++)
                {
                    outgoing.untrack(references[i]);
                }
            }
        }
    }

    /**
	 * Properly close this <tt>ServiceTracker</tt> object when finalized.
	 * This method calls the <tt>close</tt> method to close this <tt>ServiceTracker</tt> object
	 * if it has not already been closed.
	 *
	 */
    protected void finalize() throws Throwable
    {
        close();
    }

    /**
	 * Default implementation of the <tt>ServiceTrackerCustomizer.addingService</tt> method.
	 *
	 * <p>This method is only called when this <tt>ServiceTracker</tt> object
	 * has been constructed with a <tt>null ServiceTrackerCustomizer</tt> argument.
	 *
	 * The default implementation returns the result of
	 * calling <tt>getService</tt>, on the
	 * <tt>BundleContext</tt> object with which this <tt>ServiceTracker</tt> object was created,
	 * passing the specified <tt>ServiceReference</tt> object.
	 * <p>This method can be overridden in a subclass to customize
	 * the service object to be tracked for the service
	 * being added. In that case, take care not
	 * to rely on the default implementation of removedService that will unget the service.
	 *
	 * @param reference Reference to service being added to this
	 * <tt>ServiceTracker</tt> object.
	 * @return The service object to be tracked for the service
	 * added to this <tt>ServiceTracker</tt> object.
	 * @see ServiceTrackerCustomizer
	 */
    public Object addingService(ServiceReference reference)
    {
        return context.getService(reference);
    }
    public void addedService(ServiceReference ref, Object service) {
        // do nothing
    }
    
    /**
	 * Default implementation of the <tt>ServiceTrackerCustomizer.modifiedService</tt> method.
	 *
	 * <p>This method is only called when this <tt>ServiceTracker</tt> object
	 * has been constructed with a <tt>null ServiceTrackerCustomizer</tt> argument.
	 *
	 * The default implementation does nothing.
	 *
	 * @param reference Reference to modified service.
	 * @param service The service object for the modified service.
	 * @see ServiceTrackerCustomizer
	 */
    public void modifiedService(ServiceReference reference, Object service)
    {
    }

    /**
	 * Default implementation of the <tt>ServiceTrackerCustomizer.removedService</tt> method.
	 *
	 * <p>This method is only called when this <tt>ServiceTracker</tt> object
	 * has been constructed with a <tt>null ServiceTrackerCustomizer</tt> argument.
	 *
	 * The default implementation
	 * calls <tt>ungetService</tt>, on the
	 * <tt>BundleContext</tt> object with which this <tt>ServiceTracker</tt> object was created,
	 * passing the specified <tt>ServiceReference</tt> object.
	 * <p>This method can be overridden in a subclass. If the default
	 * implementation of <tt>addingService</tt> method was used, this method must unget the service.
	 *
	 * @param reference Reference to removed service.
	 * @param object The service object for the removed service.
	 * @see ServiceTrackerCustomizer
	 */
    public void removedService(ServiceReference reference, Object object)
    {
        context.ungetService(reference);
    }

    /**
	 * Wait for at least one service to be tracked by this <tt>ServiceTracker</tt> object.
	 * <p>It is strongly recommended that <tt>waitForService</tt> is not used
	 * during the calling of the <tt>BundleActivator</tt> methods. <tt>BundleActivator</tt> methods are
	 * expected to complete in a short period of time.
	 *
	 * @param timeout time interval in milliseconds to wait.  If zero,
	 * the method will wait indefinately.
	 * @return Returns the result of <tt>getService()</tt>.
	 * @throws IllegalArgumentException If the value of timeout is
	 * negative.
	 */
    public Object waitForService(long timeout) throws InterruptedException
    {
        if (timeout < 0)
        {
            throw new IllegalArgumentException("timeout value is negative");
        }

        Object object = getService();

        while (object == null)
        {
            Tracked tracked = this.tracked;     /* use local var since we are not synchronized */

            if (tracked == null)    /* if ServiceTracker is not open */
            {
                return null;
            }

            synchronized (tracked)
            {
                if (tracked.size() == 0)
                {
                    tracked.wait(timeout);
                }
            }

            object = getService();

            if (timeout > 0)
            {
                return object;
            }
        }

        return object;
    }

    /**
	 * Return an array of <tt>ServiceReference</tt> objects for all services
	 * being tracked by this <tt>ServiceTracker</tt> object.
	 *
	 * @return Array of <tt>ServiceReference</tt> objects or <tt>null</tt> if no service
	 * are being tracked.
	 */
    public ServiceReference[] getServiceReferences()
    {
        Tracked tracked = this.tracked;     /* use local var since we are not synchronized */

        if (tracked == null)    /* if ServiceTracker is not open */
        {
            return null;
        }

        synchronized (tracked)
        {
            int size = tracked.size();

            if (size == 0)
            {
                return null;
            }

            ServiceReference references[] = new ServiceReference[size];

            Enumeration trackedServiceRefs = tracked.keys();

            for (int i = 0; i < size; i++)
            {
                references[i] = (ServiceReference)trackedServiceRefs.nextElement();
            }

            return references;
        }
    }

    /**
	 * Return an array of service objects for all services
	 * being tracked by this <tt>ServiceTracker</tt> object.
	 *
	 * @return Array of service objects or <tt>null</tt> if no service
	 * are being tracked.
	 */
    public Object[] getServices()
    {
        Tracked tracked = this.tracked;     /* use local var since we are not synchronized */

        if (tracked == null)    /* if ServiceTracker is not open */
        {
            return null;
        }

        synchronized (tracked)
        {
            int size = tracked.size();

            if (size == 0)
            {
                return null;
            }

            Object objects[] = new Object[size];

            Enumeration trackedServices = tracked.elements();

            for (int i = 0; i < size; i++)
            {
                objects[i] = trackedServices.nextElement();
            }

            return objects;
        }
    }

    /**
	 * Returns a <tt>ServiceReference</tt> object for one of the services
	 * being tracked by this <tt>ServiceTracker</tt> object.
	 *
	 * <p>If multiple services are being tracked, the service
	 * with the highest ranking (as specified in its <tt>service.ranking</tt> property) is
	 * returned.
	 *
	 * <p>If there is a tie in ranking, the service with the lowest
	 * service ID (as specified in its <tt>service.id</tt> property); that is,
	 * the service that was registered first is returned.
	 * <p>This is the same algorithm used by <tt>BundleContext.getServiceReference</tt>.
	 *
	 * @return <tt>ServiceReference</tt> object or <tt>null</tt> if no service is being tracked.
	 * @since 1.1
	 */
    public ServiceReference getServiceReference()
    {
        ServiceReference[] references = getServiceReferences();

        int length = (references == null) ? 0 : references.length;

        if (length > 0)         /* if a service is being tracked */
        {
            int index = 0;

            if (length > 1)     /* if more than one service, select highest ranking */
            {
                int rankings[] = new int[length];
                int count = 0;
                int maxRanking = Integer.MIN_VALUE;

                for (int i = 0 ; i < length; i++)
                {
                    Object property = references[i].getProperty(Constants.SERVICE_RANKING);

                    int ranking = (property instanceof Integer)
                                    ? ((Integer)property).intValue() : 0;

                    rankings[i] = ranking;

                    if (ranking > maxRanking)
                    {
                        index = i;
                        maxRanking = ranking;
                        count = 1;
                    }
                    else
                    {
                        if (ranking == maxRanking)
                        {
                            count++;
                        }
                    }
                }

                if (count > 1)  /* if still more than one service, select lowest id */
                {
                    long minId = Long.MAX_VALUE;

                    for (int i = 0 ; i < length; i++)
                    {
                        if (rankings[i] == maxRanking)
                        {
                            long id = ((Long)(references[i].getProperty(Constants.SERVICE_ID))).longValue();

                            if (id < minId)
                            {
                                index = i;
                                minId = id;
                            }
                        }
                    }
                }
            }

            return references[index];
        }

        return null;
    }

    /**
	 * Returns the service object for the specified <tt>ServiceReference</tt> object
	 * if the referenced service is
	 * being tracked by this <tt>ServiceTracker</tt> object.
	 *
	 * @param reference Reference to the desired service.
	 * @return Service object or <tt>null</tt> if the service referenced by the
	 * specified <tt>ServiceReference</tt> object is not being tracked.
	 */
    public Object getService(ServiceReference reference)
    {
        Tracked tracked = this.tracked;     /* use local var since we are not synchronized */

        if (tracked == null)    /* if ServiceTracker is not open */
        {
            return null;
        }

        return tracked.get(reference);
    }

    /**
	 * Returns a service object for one of the services
	 * being tracked by this <tt>ServiceTracker</tt> object.
	 *
	 * <p>If any services are being tracked, this method returns the result
	 * of calling <tt>getService(getServiceReference())</tt>.
	 *
	 * @return Service object or <tt>null</tt> if no service is being tracked.
	 */
    public Object getService()
    {
        ServiceReference reference = getServiceReference();

        if (reference != null)
        {
            return getService(reference);
        }

        return null;
    }

    /**
	 * Remove a service from this <tt>ServiceTracker</tt> object.
	 *
	 * The specified service will be removed from this
	 * <tt>ServiceTracker</tt> object.
	 * If the specified service was being tracked then the
	 * <tt>ServiceTrackerCustomizer.removedService</tt> method will be
	 * called for that service.
	 *
	 * @param reference Reference to the service to be removed.
	 */
    public void remove(ServiceReference reference)
    {
        Tracked tracked = this.tracked;     /* use local var since we are not synchronized */

        if (tracked == null)    /* if ServiceTracker is not open */
        {
            return;
        }

        tracked.untrack(reference);
    }

    /**
	 * Return the number of services being tracked by this <tt>ServiceTracker</tt> object.
	 *
	 * @return Number of services being tracked.
	 */

    public int size()
    {
        Tracked tracked = this.tracked;     /* use local var since we are not synchronized */

        if (tracked == null)    /* if ServiceTracker is not open */
        {
            return 0;
        }

        return tracked.size();
    }

    /**
	 * Returns the tracking count for this <tt>ServiceTracker</tt> object.
	 *
	 * The tracking count is initialized to 0 when this
	 * <tt>ServiceTracker</tt> object is opened. Every time a service is
	 * added or removed from this <tt>ServiceTracker</tt> object
	 * the tracking count is incremented.
	 *
	 * <p>The tracking count can
	 * be used to determine if this <tt>ServiceTracker</tt> object
	 * has added or removed a service by comparing a tracking count value
	 * previously collected with the current tracking count value. If the value
	 * has not changed, then no service has been added or removed from
	 * this <tt>ServiceTracker</tt> object
	 * since the previous tracking count was collected.
	 *
	 * @since 1.2
	 * @return The tracking count for this <tt>ServiceTracker</tt> object
	 * or -1 if this <tt>ServiceTracker</tt> object is not open.
	 */
    public int getTrackingCount()
    {
        Tracked tracked = this.tracked;     /* use local var since we are not synchronized */

        if (tracked == null)    /* if ServiceTracker is not open */
        {
            return -1;
        }

        return tracked.getTrackingCount();
    }

    /**
	 * Inner class to track the services.
	 * This class is a hashtable mapping <tt>ServiceReference</tt> object -> customized Object.
	 * This class also implements the <tt>ServiceListener</tt> interface for the tracker.
	 * This is not a public class. It is only for use by the implementation
	 * of the <tt>ServiceTracker</tt> class.
	 *
	 */
    static class Tracked extends Hashtable implements ServiceListener {
        private ServiceTrackerCustomizer customizer;    /** ServiceTrackerCustomizer object for this tracker. */
        private Filter filter;      /** The filter used to track */
        private Vector adding;      /** list of ServiceReferences currently being added */
        private boolean closed;     /** true if the tracked object is closed */
        private int trackingCount;  /** modification count */


        /**
		 * Tracked constructor.
		 *
		 * @param customizer Customizer object from parent <tt>ServiceTracker</tt> object.
		 * @param filter <tt>Filter</tt> object from parent <tt>ServiceTracker</tt> object.
		 */
        protected Tracked(ServiceTrackerCustomizer customizer, Filter filter)
        {
            super();
            this.customizer = customizer;
            this.filter = filter;
            closed = false;
            trackingCount = 0;
            adding = new Vector(10, 10);
        }

        /**
		 * Called by the parent <tt>ServiceTracker</tt> object when it is closed.
		 */
        protected void close()
        {
            closed = true;
        }

        /**
		 * Called by the parent <tt>ServiceTracker</tt> object to get
		 * the modification count.
		 *
		 * @since 1.2
		 * @return modification count.
		 */
        protected int getTrackingCount()
        {
            return trackingCount;
        }

        /**
		 * <tt>ServiceListener</tt> method for the <tt>ServiceTracker</tt> class.
		 * This method must NOT be synchronized to avoid deadlock potential.
		 *
		 * @param event <tt>ServiceEvent</tt> object from the framework.
		 */
        public void serviceChanged(ServiceEvent event)
        {
            /* Check if we had a delayed call (which could happen when we close). */
            if (closed)
            {
                return;
            }

            ServiceReference reference = event.getServiceReference();

            switch (event.getType())
            {
                case ServiceEvent.REGISTERED:
                case ServiceEvent.MODIFIED:
                    if (filter.match(reference))
                    {
                        track(reference);
                        /* If the customizer throws an unchecked exception, it is safe to let it propagate */
                    }
                    else
                    {
                        untrack(reference);
                        /* If the customizer throws an unchecked exception, it is safe to let it propagate */
                    }

                    break;

                case ServiceEvent.UNREGISTERING:
                    untrack(reference);
                    /* If the customizer throws an unchecked exception, it is safe to let it propagate */

                    break;
            }
        }

        /**
		 * Begin to track the referenced service.
		 *
		 * @param reference Reference to a service to be tracked.
		 */
        protected void track(ServiceReference reference)
        {
            Object object = get(reference);

            if (object != null)     /* we are already tracking the service */
            {
                /* Call customizer outside of synchronized region */
                customizer.modifiedService(reference, object);
                /* If the customizer throws an unchecked exception, it is safe to let it propagate */

                return;
            }

            synchronized (this)
            {
                if (adding.indexOf(reference, 0) != -1) /* if this service is already
                                                         * in the process of being added. */
                {
                    return;
                }

                adding.addElement(reference);   /* mark this service is being added */
            }

            boolean becameUntracked = false;

            /* Call customizer outside of synchronized region */
            try
            {
                object = customizer.addingService(reference);
                /* If the customizer throws an unchecked exception, it will propagate after the finally */
            }
            finally
            {
                boolean needToCallback = false;
                synchronized (this)
                {
                    if (adding.removeElement(reference))    /* if the service was not untracked
                                                             * during the customizer callback */
                    {
                        if (object != null)
                        {
                            put(reference, object);

                            trackingCount++;            /* increment modification count */

                            notifyAll();
                            
                            // Marrs: extra callback added, will be invoked after the synchronized block
                            needToCallback = true;
                        }
                    }
                    else
                    {
                        becameUntracked = true;
                    }
                }
                if (needToCallback) {
                    customizer.addedService(reference, object);
                }
            }

            /* The service became untracked during
			 * the customizer callback.
			 */
            if (becameUntracked)
            {
                /* Call customizer outside of synchronized region */
                customizer.removedService(reference, object);
                /* If the customizer throws an unchecked exception, it is safe to let it propagate */
            }
        }

        /**
		 * Discontinue tracking the referenced service.
		 *
		 * @param reference Reference to the tracked service.
		 */
        protected void untrack(ServiceReference reference)
        {
            Object object;

            synchronized (this)
            {
                if (adding.removeElement(reference)) /* if the service is in the process
                                                      * of being added */
                {
                    return;                          /* in case the service is untracked
                                                      * while in the process of adding */
                }

                object = this.remove(reference);     /* must remove from tracker before calling
                                                      * customizer callback */

                if (object == null)             /* are we actually tracking the service */
                {
                    return;
                }

                trackingCount++;                /* increment modification count */
            }

            /* Call customizer outside of synchronized region */
            customizer.removedService(reference, object);
            /* If the customizer throws an unchecked exception, it is safe to let it propagate */
        }
    }
}
