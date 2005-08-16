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
package org.apache.osgi.framework.util;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.osgi.framework.*;

public class FrameworkListenerWrapper extends ListenerWrapper implements FrameworkListener
{
    public FrameworkListenerWrapper(Bundle bundle, FrameworkListener l)
    {
        super(bundle, FrameworkListener.class, l);
    }

    public void frameworkEvent(final FrameworkEvent event)
    {
        // The spec says only active bundles receive asynchronous events,
        // but we will include starting bundles too otherwise
        // it is impossible to see everything.
        if ((getBundle().getState() == Bundle.STARTING) ||
            (getBundle().getState() == Bundle.ACTIVE))
        {
            if (System.getSecurityManager() != null)
            {
                AccessController.doPrivileged(new PrivilegedAction() {
                    public Object run()
                    {
                        ((FrameworkListener) getListener()).frameworkEvent(event);
                        return null;
                    }
                });
            }
            else
            {
                ((FrameworkListener) getListener()).frameworkEvent(event);
            }
        }
    }
}
