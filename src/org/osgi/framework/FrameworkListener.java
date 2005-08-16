/*
 * $Header: /cvshome/build/org.osgi.framework/src/org/osgi/framework/FrameworkListener.java,v 1.6 2005/05/13 20:32:55 hargrave Exp $
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
 * A <code>FrameworkEvent</code> listener.
 * 
 * <p>
 * <code>FrameworkListener</code> is a listener interface that may be implemented
 * by a bundle developer. A <code>FrameworkListener</code> object is registered
 * with the Framework using the {@link BundleContext#addFrameworkListener}
 * method. <code>FrameworkListener</code> objects are called with a
 * <code>FrameworkEvent</code> objects when the Framework starts and when
 * asynchronous errors occur.
 * 
 * @version $Revision: 1.6 $
 * @see FrameworkEvent
 */

public abstract interface FrameworkListener extends EventListener {

	/**
	 * Receives notification of a general <code>FrameworkEvent</code> object.
	 * 
	 * @param event The <code>FrameworkEvent</code> object.
	 */
	public abstract void frameworkEvent(FrameworkEvent event);
}

