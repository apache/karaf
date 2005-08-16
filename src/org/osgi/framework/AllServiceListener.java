/*
 * $Header: /cvshome/build/org.osgi.framework/src/org/osgi/framework/AllServiceListener.java,v 1.5 2005/05/13 20:32:55 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.osgi.framework;

/**
 * A <code>ServiceEvent</code> listener.
 *
 * <p><code>AllServiceListener</code> is a listener interface that may be implemented by a bundle
 * developer.
 * <p>An <code>AllServiceListener</code> object is registered with the Framework using the
 * <code>BundleContext.addServiceListener</code> method.
 * <code>AllServiceListener</code> objects are called with a <code>ServiceEvent</code> object when
 * a service is registered, modified, or is in the process of unregistering.
 *
 * <p><code>ServiceEvent</code> object delivery to <code>AllServiceListener</code> objects is filtered by the
 * filter specified when the listener was registered. If the Java Runtime Environment
 * supports permissions, then additional filtering is done.
 * <code>ServiceEvent</code> objects are only delivered to the listener if the bundle which defines
 * the listener object's class has the appropriate <code>ServicePermission</code> to get the service
 * using at least one of the named classes the service was registered under.
 * 
 * <p>
 * Unlike normal <code>ServiceListener</code> objects,
 * <code>AllServiceListener</code> objects receive all ServiceEvent objects regardless of the
 * whether the package source of the listening bundle is equal to the package source of
 * the bundle that registered the service. This means that the listener may not be able to
 * cast the service object to any of its corresponding service interfaces if the service
 * object is retrieved.
 * 
 * @version $Revision: 1.5 $
 * @see ServiceEvent
 * @see ServicePermission
 */

public abstract interface AllServiceListener extends ServiceListener
{
	//This is a marker interface
}


