/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.framework.util;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.osgi.framework.*;

public class BundleListenerWrapper extends ListenerWrapper implements BundleListener
{
    public BundleListenerWrapper(Bundle bundle, BundleListener l)
    {
        super(bundle,
            (l instanceof SynchronousBundleListener)
                ? SynchronousBundleListener.class : BundleListener.class,
            l);
    }

    public void bundleChanged(final BundleEvent event)
    {
        // A bundle listener is either synchronous or asynchronous.
        // If the bundle listener is synchronous, then deliver the
        // event to bundles with a state of STARTING, STOPPING, or
        // ACTIVE. If the listener is asynchronous, then deliver the
        // event only to bundles that are STARTING or ACTIVE.
        if (((getListenerClass() == SynchronousBundleListener.class) &&
            ((getBundle().getState() == Bundle.STARTING) ||
            (getBundle().getState() == Bundle.STOPPING) ||
            (getBundle().getState() == Bundle.ACTIVE)))
            ||
            ((getBundle().getState() == Bundle.STARTING) ||
            (getBundle().getState() == Bundle.ACTIVE)))
        {
            if (System.getSecurityManager() != null)
            {
                AccessController.doPrivileged(new PrivilegedAction() {
                    public Object run()
                    {
                        ((BundleListener) getListener()).bundleChanged(event);
                        return null;
                    }
                });
            }
            else
            {
                ((BundleListener) getListener()).bundleChanged(event);
            }
        }
    }
}
