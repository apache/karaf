/*
 * $Header: /cvshome/build/org.osgi.framework/src/org/osgi/framework/ServiceListener.java,v 1.8 2005/05/13 20:32:55 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2000, 2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.osgi.framework;

import java.util.EventListener;

/**
 * A <code>ServiceEvent</code> listener.
 *
 * <p><code>ServiceListener</code> is a listener interface that may be implemented by a bundle
 * developer.
 * <p>A <code>ServiceListener</code> object is registered with the Framework using the
 * <code>BundleContext.addServiceListener</code> method.
 * <code>ServiceListener</code> objects are called with a <code>ServiceEvent</code> object when
 * a service is registered, modified, or is in the process of unregistering.
 *
 * <p><code>ServiceEvent</code> object delivery to <code>ServiceListener</code> objects is filtered by the
 * filter specified when the listener was registered. If the Java Runtime Environment
 * supports permissions, then additional filtering is done.
 * <code>ServiceEvent</code> objects are only delivered to the listener if the bundle which defines
 * the listener object's class has the appropriate <code>ServicePermission</code> to get the service
 * using at least one of the named classes the service was registered under.
 *
 * <p><code>ServiceEvent</code> object delivery to <code>ServiceListener</code> objects is
 * further filtered according to package sources as defined in
 * {@link ServiceReference#isAssignableTo(Bundle, String)}.
 * 
 * @version $Revision: 1.8 $
 * @see ServiceEvent
 * @see ServicePermission
 */

public abstract interface ServiceListener extends EventListener
{
    /**
     * Receives notification that a service has had a lifecycle change.
     *
     * @param event The <code>ServiceEvent</code> object.
     */
    public abstract void serviceChanged(ServiceEvent event);
}


