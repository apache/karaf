/*
 * $Header: /cvshome/build/org.osgi.util.tracker/src/org/osgi/util/tracker/ServiceTrackerCustomizer.java,v 1.7 2005/05/13 20:33:35 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2000, 2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.osgi.util.tracker;

import org.osgi.framework.ServiceReference;

/**
 * The <code>ServiceTrackerCustomizer</code> interface allows a
 * <code>ServiceTracker</code> object to customize the service objects that are
 * tracked. The <code>ServiceTrackerCustomizer</code> object is called when a
 * service is being added to the <code>ServiceTracker</code> object. The
 * <code>ServiceTrackerCustomizer</code> can then return an object for the tracked
 * service. The <code>ServiceTrackerCustomizer</code> object is also called when a
 * tracked service is modified or has been removed from the
 * <code>ServiceTracker</code> object.
 * 
 * <p>
 * The methods in this interface may be called as the result of a
 * <code>ServiceEvent</code> being received by a <code>ServiceTracker</code> object.
 * Since <code>ServiceEvent</code> s are synchronously delivered by the Framework,
 * it is highly recommended that implementations of these methods do not
 * register (<code>BundleContext.registerService</code>), modify (
 * <code>ServiceRegistration.setProperties</code>) or unregister (
 * <code>ServiceRegistration.unregister</code>) a service while being
 * synchronized on any object.
 * 
 * @version $Revision: 1.7 $
 */
public interface ServiceTrackerCustomizer {
	/**
	 * A service is being added to the <code>ServiceTracker</code> object.
	 * 
	 * <p>
	 * This method is called before a service which matched the search
	 * parameters of the <code>ServiceTracker</code> object is added to it. This
	 * method should return the service object to be tracked for this
	 * <code>ServiceReference</code> object. The returned service object is stored
	 * in the <code>ServiceTracker</code> object and is available from the
	 * <code>getService</code> and <code>getServices</code> methods.
	 * 
	 * @param reference Reference to service being added to the
	 *        <code>ServiceTracker</code> object.
	 * @return The service object to be tracked for the
	 *         <code>ServiceReference</code> object or <code>null</code> if the
	 *         <code>ServiceReference</code> object should not be tracked.
	 */
	public abstract Object addingService(ServiceReference reference);

	/**
	 * A service tracked by the <code>ServiceTracker</code> object has been
	 * modified.
	 * 
	 * <p>
	 * This method is called when a service being tracked by the
	 * <code>ServiceTracker</code> object has had it properties modified.
	 * 
	 * @param reference Reference to service that has been modified.
	 * @param service The service object for the modified service.
	 */
	public abstract void modifiedService(ServiceReference reference,
			Object service);

	/**
	 * A service tracked by the <code>ServiceTracker</code> object has been
	 * removed.
	 * 
	 * <p>
	 * This method is called after a service is no longer being tracked by the
	 * <code>ServiceTracker</code> object.
	 * 
	 * @param reference Reference to service that has been removed.
	 * @param service The service object for the removed service.
	 */
	public abstract void removedService(ServiceReference reference,
			Object service);
}