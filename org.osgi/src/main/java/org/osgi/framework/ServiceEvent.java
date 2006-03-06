/*
 * $Header: /cvshome/build/org.osgi.framework/src/org/osgi/framework/ServiceEvent.java,v 1.9 2005/05/13 20:32:56 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2000, 2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.osgi.framework;

import java.util.Dictionary;
import java.util.EventObject;

/**
 * A service lifecycle change event.
 * <p>
 * <code>ServiceEvent</code> objects are delivered to a <code>ServiceListener</code>
 * objects when a change occurs in this service's lifecycle. A type code is used
 * to identify the event type for future extendability.
 * 
 * <p>
 * OSGi Alliance reserves the right to extend the set of types.
 * 
 * @version $Revision: 1.9 $
 * @see ServiceListener
 */

public class ServiceEvent extends EventObject {
	static final long			serialVersionUID	= 8792901483909409299L;
	/**
	 * Reference to the service that had a change occur in its lifecycle.
	 */
	private ServiceReference	reference;

	/**
	 * Type of service lifecycle change.
	 */
	private int					type;

	/**
	 * This service has been registered.
	 * <p>
	 * This event is synchronously delivered <strong>after </strong> the service
	 * has been registered with the Framework.
	 * 
	 * <p>
	 * The value of <code>REGISTERED</code> is 0x00000001.
	 * 
	 * @see BundleContext#registerService(String[],Object,Dictionary)
	 */
	public final static int		REGISTERED			= 0x00000001;

	/**
	 * The properties of a registered service have been modified.
	 * <p>
	 * This event is synchronously delivered <strong>after </strong> the service
	 * properties have been modified.
	 * 
	 * <p>
	 * The value of <code>MODIFIED</code> is 0x00000002.
	 * 
	 * @see ServiceRegistration#setProperties
	 */
	public final static int		MODIFIED			= 0x00000002;

	/**
	 * This service is in the process of being unregistered.
	 * <p>
	 * This event is synchronously delivered <strong>before </strong> the
	 * service has completed unregistering.
	 * 
	 * <p>
	 * If a bundle is using a service that is <code>UNREGISTERING</code>, the
	 * bundle should release its use of the service when it receives this event.
	 * If the bundle does not release its use of the service when it receives
	 * this event, the Framework will automatically release the bundle's use of
	 * the service while completing the service unregistration operation.
	 * 
	 * <p>
	 * The value of UNREGISTERING is 0x00000004.
	 * 
	 * @see ServiceRegistration#unregister
	 * @see BundleContext#ungetService
	 */
	public final static int		UNREGISTERING		= 0x00000004;

	/**
	 * Creates a new service event object.
	 * 
	 * @param type The event type.
	 * @param reference A <code>ServiceReference</code> object to the service that
	 *        had a lifecycle change.
	 */
	public ServiceEvent(int type, ServiceReference reference) {
		super(reference);
		this.reference = reference;
		this.type = type;
	}

	/**
	 * Returns a reference to the service that had a change occur in its
	 * lifecycle.
	 * <p>
	 * This reference is the source of the event.
	 * 
	 * @return Reference to the service that had a lifecycle change.
	 */
	public ServiceReference getServiceReference() {
		return (reference);
	}

	/**
	 * Returns the type of event. The event type values are:
	 * <ul>
	 * <li>{@link #REGISTERED}
	 * <li>{@link #MODIFIED}
	 * <li>{@link #UNREGISTERING}
	 * </ul>
	 * 
	 * @return Type of service lifecycle change.
	 */

	public int getType() {
		return (type);
	}
}

