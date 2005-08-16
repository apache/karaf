/*
 * $Header: /cvshome/build/org.osgi.framework/src/org/osgi/framework/Configurable.java,v 1.7 2005/05/13 20:32:55 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2000, 2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.osgi.framework;

/**
 * Supports a configuration object.
 *
 * <p><code>Configurable</code> is an interface that should be used by a bundle developer in support
 * of a configurable service.
 * Bundles that need to configure a service may test to determine
 * if the service object is an <code>instanceof Configurable</code>.
 *
 * @version $Revision: 1.7 $
 * @deprecated Please use the Configuration Admin
 */
public abstract interface Configurable
{
    /**
     * Returns this service's configuration object.
     *
     * <p>Services implementing <code>Configurable</code> should take care when returning a
     * service configuration object since this object is probably sensitive.
     * <p>If the Java Runtime Environment supports permissions, it is recommended that
     * the caller is checked for the appropriate permission before returning the configuration object.
     * It is recommended that callers possessing the appropriate
     * {@link AdminPermission} always be allowed to get the configuration object.
     *
     * @return The configuration object for this service.
     * @exception java.lang.SecurityException If the caller does not have
     * an appropriate permission and the Java Runtime Environment supports permissions.
     * @deprecated Please use the Configuration Admin
     */
    public abstract Object getConfigurationObject();
}


