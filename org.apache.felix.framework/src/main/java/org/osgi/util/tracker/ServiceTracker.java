/*
 * $Header: /cvshome/build/org.osgi.util.tracker/src/org/osgi/util/tracker/ServiceTracker.java,v 1.13 2005/05/13 20:33:35 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2000, 2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.osgi.util.tracker;

import java.util.*;

import org.osgi.framework.*;

/**
 * The <code>ServiceTracker</code> class simplifies using services from the
 * Framework's service registry.
 * <p>
 * A <code>ServiceTracker</code> object is constructed with search criteria
 * and a <code>ServiceTrackerCustomizer</code> object. A
 * <code>ServiceTracker</code> object can use the
 * <code>ServiceTrackerCustomizer</code> object to customize the service
 * objects to be tracked. The <code>ServiceTracker</code> object can then be
 * opened to begin tracking all services in the Framework's service registry
 * that match the specified search criteria. The <code>ServiceTracker</code>
 * object correctly handles all of the details of listening to
 * <code>ServiceEvent</code> objects and getting and ungetting services.
 * <p>
 * The <code>getServiceReferences</code> method can be called to get
 * references to the services being tracked. The <code>getService</code> and
 * <code>getServices</code> methods can be called to get the service objects
 * for the tracked service.
 * 
 * @version $Revision: 1.13 $
 */
public class ServiceTracker implements ServiceTrackerCustomizer {
	/* set this to true to compile in debug messages */
	static final boolean					DEBUG			= false;
	/**
	 * Bundle context this <code>ServiceTracker</code> object is tracking
	 * against.
	 */
	protected final BundleContext			context;
	/**
	 * Filter specifying search criteria for the services to track.
	 * 
	 * @since 1.1
	 */
	protected final Filter					filter;
	/**
	 * <code>ServiceTrackerCustomizer</code> object for this tracker.
	 */
	private final ServiceTrackerCustomizer	customizer;
	/**
	 * Filter string for use when adding the ServiceListener. If this field is
	 * set, then certain optimizations can be taken since we don't have a user
	 * supplied filter.
	 */
	private final String					listenerFilter;
	/**
	 * Class name to be tracked. If this field is set, then we are tracking by
	 * class name.
	 */
	private final String					trackClass;
	/**
	 * Reference to be tracked. If this field is set, then we are tracking a
	 * single ServiceReference.
	 */
	private final ServiceReference			trackReference;
	/**
	 * Tracked services: <code>ServiceReference</code> object -> customized
	 * Object and <code>ServiceListener</code> object
	 */
	private Tracked							tracked;
	/**
	 * Modification count. This field is initialized to zero by open, set to -1
	 * by close and incremented by modified. This field is volatile since it is
	 * accessed by multiple threads.
	 */
	private volatile int					trackingCount	= -1;
	/**
	 * Cached ServiceReference for getServiceReference. This field is volatile
	 * since it is accessed by multiple threads.
	 */
	private volatile ServiceReference		cachedReference;
	/**
	 * Cached service object for getService. This field is volatile since it is
	 * accessed by multiple threads.
	 */
	private volatile Object					cachedService;

	/**
	 * Create a <code>ServiceTracker</code> object on the specified
	 * <code>ServiceReference</code> object.
	 * 
	 * <p>
	 * The service referenced by the specified <code>ServiceReference</code>
	 * object will be tracked by this <code>ServiceTracker</code> object.
	 * 
	 * @param context <code>BundleContext</code> object against which the
	 *        tracking is done.
	 * @param reference <code>ServiceReference</code> object for the service
	 *        to be tracked.
	 * @param customizer The customizer object to call when services are added,
	 *        modified, or removed in this <code>ServiceTracker</code> object.
	 *        If customizer is <code>null</code>, then this
	 *        <code>ServiceTracker</code> object will be used as the
	 *        <code>ServiceTrackerCustomizer</code> object and the
	 *        <code>ServiceTracker</code> object will call the
	 *        <code>ServiceTrackerCustomizer</code> methods on itself.
	 */
	public ServiceTracker(BundleContext context, ServiceReference reference,
			ServiceTrackerCustomizer customizer) {
		this.context = context;
		this.trackReference = reference;
		this.trackClass = null;
		this.customizer = (customizer == null) ? this : customizer;
		this.listenerFilter = "(" + Constants.SERVICE_ID + "=" + reference.getProperty(Constants.SERVICE_ID).toString() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		try {
			this.filter = context.createFilter(listenerFilter);
		}
		catch (InvalidSyntaxException e) { // we could only get this exception
			// if the ServiceReference was
			// invalid
			throw new IllegalArgumentException(
					"unexpected InvalidSyntaxException: " + e.getMessage()); //$NON-NLS-1$
		}
	}

	/**
	 * Create a <code>ServiceTracker</code> object on the specified class
	 * name.
	 * 
	 * <p>
	 * Services registered under the specified class name will be tracked by
	 * this <code>ServiceTracker</code> object.
	 * 
	 * @param context <code>BundleContext</code> object against which the
	 *        tracking is done.
	 * @param clazz Class name of the services to be tracked.
	 * @param customizer The customizer object to call when services are added,
	 *        modified, or removed in this <code>ServiceTracker</code> object.
	 *        If customizer is <code>null</code>, then this
	 *        <code>ServiceTracker</code> object will be used as the
	 *        <code>ServiceTrackerCustomizer</code> object and the
	 *        <code>ServiceTracker</code> object will call the
	 *        <code>ServiceTrackerCustomizer</code> methods on itself.
	 */
	public ServiceTracker(BundleContext context, String clazz,
			ServiceTrackerCustomizer customizer) {
		this.context = context;
		this.trackReference = null;
		this.trackClass = clazz;
		this.customizer = (customizer == null) ? this : customizer;
		this.listenerFilter = "(" + Constants.OBJECTCLASS + "=" + clazz.toString() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		try {
			this.filter = context.createFilter(listenerFilter);
		}
		catch (InvalidSyntaxException e) { // we could only get this exception
			// if the clazz argument was
			// malformed
			throw new IllegalArgumentException(
					"unexpected InvalidSyntaxException: " + e.getMessage()); //$NON-NLS-1$
		}
	}

	/**
	 * Create a <code>ServiceTracker</code> object on the specified
	 * <code>Filter</code> object.
	 * 
	 * <p>
	 * Services which match the specified <code>Filter</code> object will be
	 * tracked by this <code>ServiceTracker</code> object.
	 * 
	 * @param context <code>BundleContext</code> object against which the
	 *        tracking is done.
	 * @param filter <code>Filter</code> object to select the services to be
	 *        tracked.
	 * @param customizer The customizer object to call when services are added,
	 *        modified, or removed in this <code>ServiceTracker</code> object.
	 *        If customizer is null, then this <code>ServiceTracker</code>
	 *        object will be used as the <code>ServiceTrackerCustomizer</code>
	 *        object and the <code>ServiceTracker</code> object will call the
	 *        <code>ServiceTrackerCustomizer</code> methods on itself.
	 * @since 1.1
	 */
	public ServiceTracker(BundleContext context, Filter filter,
			ServiceTrackerCustomizer customizer) {
		this.context = context;
		this.trackReference = null;
		this.trackClass = null;
		this.listenerFilter = null;
		this.filter = filter;
		this.customizer = (customizer == null) ? this : customizer;
		if ((context == null) || (filter == null)) { // we throw a NPE here
			// to
			// be consistent with the
			// other constructors
			throw new NullPointerException();
		}
	}

	/**
	 * Open this <code>ServiceTracker</code> object and begin tracking
	 * services.
	 * 
	 * <p>
	 * This method calls <code>open(false)</code>.
	 * 
	 * @throws java.lang.IllegalStateException if the <code>BundleContext</code>
	 *         object with which this <code>ServiceTracker</code> object was
	 *         created is no longer valid.
	 * @see #open(boolean)
	 */
	public void open() {
		open(false);
	}

	/**
	 * Open this <code>ServiceTracker</code> object and begin tracking
	 * services.
	 * 
	 * <p>
	 * Services which match the search criteria specified when this
	 * <code>ServiceTracker</code> object was created are now tracked by this
	 * <code>ServiceTracker</code> object.
	 * 
	 * @param trackAllServices If <code>true</code>, then this
	 *        <code>ServiceTracker</code> will track all matching services
	 *        regardless of class loader accessibility. If <code>false</code>,
	 *        then this <code>ServiceTracker</code> will only track matching
	 *        services which are class loader accessibile to the bundle whose
	 *        <code>BundleContext</code> is used by this
	 *        <code>ServiceTracker</code>.
	 * @throws java.lang.IllegalStateException if the <code>BundleContext</code>
	 *         object with which this <code>ServiceTracker</code> object was
	 *         created is no longer valid.
	 * @since 1.3
	 */
	public synchronized void open(boolean trackAllServices) {
		if (tracked != null) {
			return;
		}
		if (DEBUG) {
			System.out.println("ServiceTracker.open: " + filter); //$NON-NLS-1$
		}
		tracked = trackAllServices ? new AllTracked() : new Tracked();
		trackingCount = 0;
		ServiceReference[] references;
		synchronized (tracked) {
			try {
				context.addServiceListener(tracked, listenerFilter);
				if (listenerFilter == null) { // user supplied filter
					references = getInitialReferences(trackAllServices, null,
							filter.toString());
				}
				else { // constructor supplied filter
					if (trackClass == null) {
						references = new ServiceReference[] {trackReference};
					}
					else {
						references = getInitialReferences(trackAllServices,
								trackClass, null);
					}
				}
			}
			catch (InvalidSyntaxException e) {
				throw new RuntimeException(
						"unexpected InvalidSyntaxException: " + e.getMessage()); //$NON-NLS-1$
			}
		}
		/* Call tracked outside of synchronized region */
		if (references != null) {
			int length = references.length;
			for (int i = 0; i < length; i++) {
				ServiceReference reference = references[i];
				/* if the service is still registered */
				if (reference.getBundle() != null) {
					tracked.track(reference);
				}
			}
		}
	}

	/**
	 * Returns the list of initial <code>ServiceReference</code> objects that
	 * will be tracked by this <code>ServiceTracker</code> object.
	 * 
	 * @param trackAllServices If true, use getAllServiceReferences.
	 * @param trackClass the class name with which the service was registered,
	 *        or null for all services.
	 * @param filterString the filter criteria or null for all services.
	 * @return the list of initial <code>ServiceReference</code> objects.
	 * @throws InvalidSyntaxException if the filter uses an invalid syntax.
	 */
	private ServiceReference[] getInitialReferences(boolean trackAllServices,
			String trackClass, String filterString)
			throws InvalidSyntaxException {
		if (trackAllServices) {
			return context.getAllServiceReferences(trackClass, filterString);
		}
		else {
			return context.getServiceReferences(trackClass, filterString);
		}
	}

	/**
	 * Close this <code>ServiceTracker</code> object.
	 * 
	 * <p>
	 * This method should be called when this <code>ServiceTracker</code>
	 * object should end the tracking of services.
	 */
	public synchronized void close() {
		if (tracked == null) {
			return;
		}
		if (DEBUG) {
			System.out.println("ServiceTracker.close: " + filter); //$NON-NLS-1$
		}
		tracked.close();
		ServiceReference[] references = getServiceReferences();
		Tracked outgoing = tracked;
		tracked = null;
		try {
			context.removeServiceListener(outgoing);
		}
		catch (IllegalStateException e) {
			/* In case the context was stopped. */
		}
		if (references != null) {
			for (int i = 0; i < references.length; i++) {
				outgoing.untrack(references[i]);
			}
		}
		trackingCount = -1;
		if (DEBUG) {
			if ((cachedReference == null) && (cachedService == null)) {
				System.out
						.println("ServiceTracker.close[cached cleared]: " + filter); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Default implementation of the
	 * <code>ServiceTrackerCustomizer.addingService</code> method.
	 * 
	 * <p>
	 * This method is only called when this <code>ServiceTracker</code> object
	 * has been constructed with a <code>null ServiceTrackerCustomizer</code>
	 * argument.
	 * 
	 * The default implementation returns the result of calling
	 * <code>getService</code>, on the <code>BundleContext</code> object
	 * with which this <code>ServiceTracker</code> object was created, passing
	 * the specified <code>ServiceReference</code> object.
	 * <p>
	 * This method can be overridden in a subclass to customize the service
	 * object to be tracked for the service being added. In that case, take care
	 * not to rely on the default implementation of removedService that will
	 * unget the service.
	 * 
	 * @param reference Reference to service being added to this
	 *        <code>ServiceTracker</code> object.
	 * @return The service object to be tracked for the service added to this
	 *         <code>ServiceTracker</code> object.
	 * @see ServiceTrackerCustomizer
	 */
	public Object addingService(ServiceReference reference) {
		return context.getService(reference);
	}

	/**
	 * Default implementation of the
	 * <code>ServiceTrackerCustomizer.modifiedService</code> method.
	 * 
	 * <p>
	 * This method is only called when this <code>ServiceTracker</code> object
	 * has been constructed with a <code>null ServiceTrackerCustomizer</code>
	 * argument.
	 * 
	 * The default implementation does nothing.
	 * 
	 * @param reference Reference to modified service.
	 * @param service The service object for the modified service.
	 * @see ServiceTrackerCustomizer
	 */
	public void modifiedService(ServiceReference reference, Object service) {
	}

	/**
	 * Default implementation of the
	 * <code>ServiceTrackerCustomizer.removedService</code> method.
	 * 
	 * <p>
	 * This method is only called when this <code>ServiceTracker</code> object
	 * has been constructed with a <code>null ServiceTrackerCustomizer</code>
	 * argument.
	 * 
	 * The default implementation calls <code>ungetService</code>, on the
	 * <code>BundleContext</code> object with which this
	 * <code>ServiceTracker</code> object was created, passing the specified
	 * <code>ServiceReference</code> object.
	 * <p>
	 * This method can be overridden in a subclass. If the default
	 * implementation of <code>addingService</code> method was used, this
	 * method must unget the service.
	 * 
	 * @param reference Reference to removed service.
	 * @param service The service object for the removed service.
	 * @see ServiceTrackerCustomizer
	 */
	public void removedService(ServiceReference reference, Object service) {
		context.ungetService(reference);
	}

	/**
	 * Wait for at least one service to be tracked by this
	 * <code>ServiceTracker</code> object.
	 * <p>
	 * It is strongly recommended that <code>waitForService</code> is not used
	 * during the calling of the <code>BundleActivator</code> methods.
	 * <code>BundleActivator</code> methods are expected to complete in a
	 * short period of time.
	 * 
	 * @param timeout time interval in milliseconds to wait. If zero, the method
	 *        will wait indefinately.
	 * @return Returns the result of <code>getService()</code>.
	 * @throws IllegalArgumentException If the value of timeout is negative.
	 */
	public Object waitForService(long timeout) throws InterruptedException {
		if (timeout < 0) {
			throw new IllegalArgumentException("timeout value is negative"); //$NON-NLS-1$
		}
		Object object = getService();
		while (object == null) {
			Tracked tracked = this.tracked; /*
											 * use local var since we are not
											 * synchronized
											 */
			if (tracked == null) /* if ServiceTracker is not open */
			{
				return null;
			}
			synchronized (tracked) {
				if (tracked.size() == 0) {
					tracked.wait(timeout);
				}
			}
			object = getService();
			if (timeout > 0) {
				return object;
			}
		}
		return object;
	}

	/**
	 * Return an array of <code>ServiceReference</code> objects for all
	 * services being tracked by this <code>ServiceTracker</code> object.
	 * 
	 * @return Array of <code>ServiceReference</code> objects or
	 *         <code>null</code> if no service are being tracked.
	 */
	public ServiceReference[] getServiceReferences() {
		Tracked tracked = this.tracked; /*
										 * use local var since we are not
										 * synchronized
										 */
		if (tracked == null) /* if ServiceTracker is not open */
		{
			return null;
		}
		synchronized (tracked) {
			int length = tracked.size();
			if (length == 0) {
				return null;
			}
			ServiceReference[] references = new ServiceReference[length];
			Enumeration keys = tracked.keys();
			for (int i = 0; i < length; i++) {
				references[i] = (ServiceReference) keys.nextElement();
			}
			return references;
		}
	}

	/**
	 * Returns a <code>ServiceReference</code> object for one of the services
	 * being tracked by this <code>ServiceTracker</code> object.
	 * 
	 * <p>
	 * If multiple services are being tracked, the service with the highest
	 * ranking (as specified in its <code>service.ranking</code> property) is
	 * returned.
	 * 
	 * <p>
	 * If there is a tie in ranking, the service with the lowest service ID (as
	 * specified in its <code>service.id</code> property); that is, the
	 * service that was registered first is returned.
	 * <p>
	 * This is the same algorithm used by
	 * <code>BundleContext.getServiceReference</code>.
	 * 
	 * @return <code>ServiceReference</code> object or <code>null</code> if
	 *         no service is being tracked.
	 * @since 1.1
	 */
	public ServiceReference getServiceReference() {
		ServiceReference reference = cachedReference;
		if (reference != null) {
			if (DEBUG) {
				System.out
						.println("ServiceTracker.getServiceReference[cached]: " + filter); //$NON-NLS-1$
			}
			return reference;
		}
		if (DEBUG) {
			System.out.println("ServiceTracker.getServiceReference: " + filter); //$NON-NLS-1$
		}
		ServiceReference[] references = getServiceReferences();
		int length = (references == null) ? 0 : references.length;
		if (length == 0) /* if no service is being tracked */
		{
			return null;
		}
		int index = 0;
		if (length > 1) /* if more than one service, select highest ranking */
		{
			int rankings[] = new int[length];
			int count = 0;
			int maxRanking = Integer.MIN_VALUE;
			for (int i = 0; i < length; i++) {
				Object property = references[i]
						.getProperty(Constants.SERVICE_RANKING);
				int ranking = (property instanceof Integer) ? ((Integer) property)
						.intValue()
						: 0;
				rankings[i] = ranking;
				if (ranking > maxRanking) {
					index = i;
					maxRanking = ranking;
					count = 1;
				}
				else {
					if (ranking == maxRanking) {
						count++;
					}
				}
			}
			if (count > 1) /* if still more than one service, select lowest id */
			{
				long minId = Long.MAX_VALUE;
				for (int i = 0; i < length; i++) {
					if (rankings[i] == maxRanking) {
						long id = ((Long) (references[i]
								.getProperty(Constants.SERVICE_ID)))
								.longValue();
						if (id < minId) {
							index = i;
							minId = id;
						}
					}
				}
			}
		}
		return cachedReference = references[index];
	}

	/**
	 * Returns the service object for the specified
	 * <code>ServiceReference</code> object if the referenced service is being
	 * tracked by this <code>ServiceTracker</code> object.
	 * 
	 * @param reference Reference to the desired service.
	 * @return Service object or <code>null</code> if the service referenced
	 *         by the specified <code>ServiceReference</code> object is not
	 *         being tracked.
	 */
	public Object getService(ServiceReference reference) {
		Tracked tracked = this.tracked; /*
										 * use local var since we are not
										 * synchronized
										 */
		if (tracked == null) /* if ServiceTracker is not open */
		{
			return null;
		}
		synchronized (tracked) {
			return tracked.get(reference);
		}
	}

	/**
	 * Return an array of service objects for all services being tracked by this
	 * <code>ServiceTracker</code> object.
	 * 
	 * @return Array of service objects or <code>null</code> if no service are
	 *         being tracked.
	 */
	public Object[] getServices() {
		Tracked tracked = this.tracked; /*
										 * use local var since we are not
										 * synchronized
										 */
		if (tracked == null) /* if ServiceTracker is not open */
		{
			return null;
		}
		synchronized (tracked) {
			ServiceReference[] references = getServiceReferences();
			int length = (references == null) ? 0 : references.length;
			if (length == 0) {
				return null;
			}
			Object[] objects = new Object[length];
			for (int i = 0; i < length; i++) {
				objects[i] = getService(references[i]);
			}
			return objects;
		}
	}

	/**
	 * Returns a service object for one of the services being tracked by this
	 * <code>ServiceTracker</code> object.
	 * 
	 * <p>
	 * If any services are being tracked, this method returns the result of
	 * calling <code>getService(getServiceReference())</code>.
	 * 
	 * @return Service object or <code>null</code> if no service is being
	 *         tracked.
	 */
	public Object getService() {
		Object service = cachedService;
		if (service != null) {
			if (DEBUG) {
				System.out
						.println("ServiceTracker.getService[cached]: " + filter); //$NON-NLS-1$
			}
			return service;
		}
		if (DEBUG) {
			System.out.println("ServiceTracker.getService: " + filter); //$NON-NLS-1$
		}
		ServiceReference reference = getServiceReference();
		if (reference == null) {
			return null;
		}
		return cachedService = getService(reference);
	}

	/**
	 * Remove a service from this <code>ServiceTracker</code> object.
	 * 
	 * The specified service will be removed from this
	 * <code>ServiceTracker</code> object. If the specified service was being
	 * tracked then the <code>ServiceTrackerCustomizer.removedService</code>
	 * method will be called for that service.
	 * 
	 * @param reference Reference to the service to be removed.
	 */
	public void remove(ServiceReference reference) {
		Tracked tracked = this.tracked; /*
										 * use local var since we are not
										 * synchronized
										 */
		if (tracked == null) /* if ServiceTracker is not open */
		{
			return;
		}
		tracked.untrack(reference);
	}

	/**
	 * Return the number of services being tracked by this
	 * <code>ServiceTracker</code> object.
	 * 
	 * @return Number of services being tracked.
	 */
	public int size() {
		Tracked tracked = this.tracked; /*
										 * use local var since we are not
										 * synchronized
										 */
		if (tracked == null) /* if ServiceTracker is not open */
		{
			return 0;
		}
		return tracked.size();
	}

	/**
	 * Returns the tracking count for this <code>ServiceTracker</code> object.
	 * 
	 * The tracking count is initialized to 0 when this
	 * <code>ServiceTracker</code> object is opened. Every time a service is
	 * added or removed from this <code>ServiceTracker</code> object the
	 * tracking count is incremented.
	 * 
	 * <p>
	 * The tracking count can be used to determine if this
	 * <code>ServiceTracker</code> object has added or removed a service by
	 * comparing a tracking count value previously collected with the current
	 * tracking count value. If the value has not changed, then no service has
	 * been added or removed from this <code>ServiceTracker</code> object
	 * since the previous tracking count was collected.
	 * 
	 * @since 1.2
	 * @return The tracking count for this <code>ServiceTracker</code> object
	 *         or -1 if this <code>ServiceTracker</code> object is not open.
	 */
	public int getTrackingCount() {
		return trackingCount;
	}

	/**
	 * Called by the Tracked object whenever the set of tracked services is
	 * modified. Increments the tracking count and clears the cache.
	 */
	/*
	 * This method must not be synchronized since it is called by Tracked while
	 * Tracked is synchronized. We don't want synchronization interactions
	 * between the ServiceListener thread and the user thread.
	 */
	private void modified() {
		trackingCount++; /* increment modification count */
		cachedReference = null; /* clear cached value */
		cachedService = null; /* clear cached value */
		if (DEBUG) {
			System.out.println("ServiceTracker.modified: " + filter); //$NON-NLS-1$
		}
	}

	/**
	 * Inner class to track services. If a <code>ServiceTracker</code> object
	 * is reused (closed then reopened), then a new Tracked object is used. This
	 * class is a hashtable mapping <code>ServiceReference</code> object ->
	 * customized Object. This class is the <code>ServiceListener</code>
	 * object for the tracker. This class is used to synchronize access to the
	 * tracked services. This is not a public class. It is only for use by the
	 * implementation of the <code>ServiceTracker</code> class.
	 * 
	 */
	class Tracked extends Hashtable implements ServiceListener {
		static final long			serialVersionUID	= -7420065199791006079L;
		/**
		 * List of ServiceReferences in the process of being added.
		 */
		private ArrayList			adding;
		/**
		 * true if the tracked object is closed. This field is volatile because
		 * it is set by one thread and read by another.
		 */
		private volatile boolean	closed;

		/**
		 * Tracked constructor.
		 */
		protected Tracked() {
			super();
			closed = false;
			adding = new ArrayList(6);
		}

		/**
		 * Called by the owning <code>ServiceTracker</code> object when it is
		 * closed.
		 */
		protected void close() {
			closed = true;
		}

		/**
		 * <code>ServiceListener</code> method for the
		 * <code>ServiceTracker</code> class. This method must NOT be
		 * synchronized to avoid deadlock potential.
		 * 
		 * @param event <code>ServiceEvent</code> object from the framework.
		 */
		public void serviceChanged(ServiceEvent event) {
			/*
			 * Check if we had a delayed call (which could happen when we
			 * close).
			 */
			if (closed) {
				return;
			}
			ServiceReference reference = event.getServiceReference();
			switch (event.getType()) {
				case ServiceEvent.REGISTERED :
				case ServiceEvent.MODIFIED :
					if (listenerFilter != null) { // constructor supplied
						// filter
						track(reference);
						/*
						 * If the customizer throws an unchecked exception, it
						 * is safe to let it propagate
						 */
					}
					else { // user supplied filter
						if (filter.match(reference)) {
							track(reference);
							/*
							 * If the customizer throws an unchecked exception,
							 * it is safe to let it propagate
							 */
						}
						else {
							untrack(reference);
							/*
							 * If the customizer throws an unchecked exception,
							 * it is safe to let it propagate
							 */
						}
					}
					break;
				case ServiceEvent.UNREGISTERING :
					untrack(reference);
					/*
					 * If the customizer throws an unchecked exception, it is
					 * safe to let it propagate
					 */
					break;
			}
		}

		/**
		 * Begin to track the referenced service.
		 * 
		 * @param reference Reference to a service to be tracked.
		 */
		protected void track(ServiceReference reference) {
			Object object;
			synchronized (this) {
				object = this.get(reference);
			}
			if (object != null) /* we are already tracking the service */
			{
				if (DEBUG) {
					System.out
							.println("ServiceTracker.Tracked.track[modified]: " + reference); //$NON-NLS-1$
				}
				/* Call customizer outside of synchronized region */
				customizer.modifiedService(reference, object);
				/*
				 * If the customizer throws an unchecked exception, it is safe
				 * to let it propagate
				 */
				return;
			}
			synchronized (this) {
				if (adding.contains(reference)) /*
												 * if this service is already in
												 * the process of being added.
												 */
				{
					if (DEBUG) {
						System.out
								.println("ServiceTracker.Tracked.track[already adding]: " + reference); //$NON-NLS-1$
					}
					return;
				}
				adding.add(reference); /* mark this service is being added */
			}
			if (DEBUG) {
				System.out
						.println("ServiceTracker.Tracked.track[adding]: " + reference); //$NON-NLS-1$
			}
			boolean becameUntracked = false;
			/* Call customizer outside of synchronized region */
			try {
				object = customizer.addingService(reference);
				/*
				 * If the customizer throws an unchecked exception, it will
				 * propagate after the finally
				 */
			}
			finally {
				synchronized (this) {
					if (adding.remove(reference)) /*
													 * if the service was not
													 * untracked during the
													 * customizer callback
													 */
					{
						if (object != null) {
							this.put(reference, object);
							modified(); /* increment modification count */
							notifyAll();
						}
					}
					else {
						becameUntracked = true;
					}
				}
			}
			/*
			 * The service became untracked during the customizer callback.
			 */
			if (becameUntracked) {
				if (DEBUG) {
					System.out
							.println("ServiceTracker.Tracked.track[removed]: " + reference); //$NON-NLS-1$
				}
				/* Call customizer outside of synchronized region */
				customizer.removedService(reference, object);
				/*
				 * If the customizer throws an unchecked exception, it is safe
				 * to let it propagate
				 */
			}
		}

		/**
		 * Discontinue tracking the referenced service.
		 * 
		 * @param reference Reference to the tracked service.
		 */
		protected void untrack(ServiceReference reference) {
			Object object;
			synchronized (this) {
				if (adding.remove(reference)) /*
												 * if the service is in the
												 * process of being added
												 */
				{
					if (DEBUG) {
						System.out
								.println("ServiceTracker.Tracked.untrack[being added]: " + reference); //$NON-NLS-1$
					}
					return; /*
							 * in case the service is untracked while in the
							 * process of adding
							 */
				}
				object = this.remove(reference); /*
													 * must remove from tracker
													 * before calling customizer
													 * callback
													 */
				if (object == null) /* are we actually tracking the service */
				{
					return;
				}
				modified(); /* increment modification count */
			}
			if (DEBUG) {
				System.out
						.println("ServiceTracker.Tracked.untrack[removed]: " + reference); //$NON-NLS-1$
			}
			/* Call customizer outside of synchronized region */
			customizer.removedService(reference, object);
			/*
			 * If the customizer throws an unchecked exception, it is safe to
			 * let it propagate
			 */
		}
	}

	/**
	 * Subclass of Tracked which implements the AllServiceListener interface.
	 * This class is used by the ServiceTracker if isAllServiceTracker returns
	 * true.
	 * 
	 * @since 1.3
	 */
	class AllTracked extends Tracked implements AllServiceListener {
		static final long	serialVersionUID	= 4050764875305137716L;

		/**
		 * Tracked constructor.
		 */
		protected AllTracked() {
			super();
		}
	}
}