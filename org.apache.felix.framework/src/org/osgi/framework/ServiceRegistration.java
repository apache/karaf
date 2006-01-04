/*
 * $Header: /cvshome/build/org.osgi.framework/src/org/osgi/framework/ServiceRegistration.java,v 1.8 2005/05/13 20:32:55 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2000, 2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.osgi.framework;

import java.util.Dictionary;

/**
 * A registered service.
 * 
 * <p>
 * The Framework returns a <code>ServiceRegistration</code> object when a
 * <code>BundleContext.registerService</code> method invocation is successful.
 * The <code>ServiceRegistration</code> object is for the private use of the
 * registering bundle and should not be shared with other bundles.
 * <p>
 * The <code>ServiceRegistration</code> object may be used to update the
 * properties of the service or to unregister the service.
 * 
 * @version $Revision: 1.8 $
 * @see BundleContext#registerService(String[],Object,Dictionary)
 */

public abstract interface ServiceRegistration {
	/**
	 * Returns a <code>ServiceReference</code> object for a service being
	 * registered.
	 * <p>
	 * The <code>ServiceReference</code> object may be shared with other bundles.
	 * 
	 * @exception java.lang.IllegalStateException If this
	 *            <code>ServiceRegistration</code> object has already been
	 *            unregistered.
	 * @return <code>ServiceReference</code> object.
	 */
	public abstract ServiceReference getReference();

	/**
	 * Updates the properties associated with a service.
	 * 
	 * <p>
	 * The {@link Constants#OBJECTCLASS} and {@link Constants#SERVICE_ID} keys
	 * cannot be modified by this method. These values are set by the Framework
	 * when the service is registered in the OSGi environment.
	 * 
	 * <p>
	 * The following steps are required to modify service properties:
	 * <ol>
	 * <li>The service's properties are replaced with the provided properties.
	 * <li>A service event of type {@link ServiceEvent#MODIFIED} is
	 * synchronously sent.
	 * </ol>
	 * 
	 * @param properties The properties for this service. See {@link Constants}
	 *        for a list of standard service property keys. Changes should not
	 *        be made to this object after calling this method. To update the
	 *        service's properties this method should be called again.
	 * 
	 * @exception IllegalStateException If this <code>ServiceRegistration</code>
	 *            object has already been unregistered.
	 * 
	 * @exception IllegalArgumentException If <code>properties</code> contains
	 *            case variants of the same key name.
	 */
	public abstract void setProperties(Dictionary properties);

	/**
	 * Unregisters a service. Remove a <code>ServiceRegistration</code> object
	 * from the Framework service registry. All <code>ServiceReference</code>
	 * objects associated with this <code>ServiceRegistration</code> object can no
	 * longer be used to interact with the service.
	 * 
	 * <p>
	 * The following steps are required to unregister a service:
	 * <ol>
	 * <li>The service is removed from the Framework service registry so that
	 * it can no longer be used. <code>ServiceReference</code> objects for the
	 * service may no longer be used to get a service object for the service.
	 * <li>A service event of type {@link ServiceEvent#UNREGISTERING} is
	 * synchronously sent so that bundles using this service can release their
	 * use of it.
	 * <li>For each bundle whose use count for this service is greater than
	 * zero: <br>
	 * The bundle's use count for this service is set to zero. <br>
	 * If the service was registered with a {@link ServiceFactory} object, the
	 * <code>ServiceFactory.ungetService</code> method is called to release the
	 * service object for the bundle.
	 * </ol>
	 * 
	 * @exception java.lang.IllegalStateException If this
	 *            <code>ServiceRegistration</code> object has already been
	 *            unregistered.
	 * @see BundleContext#ungetService
	 * @see ServiceFactory#ungetService
	 */
	public abstract void unregister();
}

