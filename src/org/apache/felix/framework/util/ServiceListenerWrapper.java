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

import java.security.*;

import org.osgi.framework.*;

public class ServiceListenerWrapper extends ListenerWrapper implements ServiceListener
{
    // LDAP query filter.
    private Filter m_filter = null;
    // Remember the security context.
    private AccessControlContext m_acc = null;

    public ServiceListenerWrapper(Bundle bundle, ServiceListener l, Filter filter)
    {
        super(bundle, ServiceListener.class, l);
        m_filter = filter;

        // Remember security context for filtering
        // events based on security.
        if (System.getSecurityManager() != null)
        {
            m_acc = AccessController.getContext();
        }
    }

    public void setFilter(Filter filter)
    {
        m_filter = filter;
    }
    
    public void serviceChanged(final ServiceEvent event)
    {
        // Service events should be delivered to STARTING,
        // STOPPING, and ACTIVE bundles.
        if ((getBundle().getState() != Bundle.STARTING) &&
            (getBundle().getState() != Bundle.STOPPING) &&
            (getBundle().getState() != Bundle.ACTIVE))
        {
            return;
        }

        // Check that the bundle has permission to get at least
        // one of the service interfaces; the objectClass property
        // of the service stores its service interfaces.
        ServiceReference ref = event.getServiceReference();
        String[] objectClass = (String[]) ref.getProperty(Constants.OBJECTCLASS);

        // On the safe side, if there is no objectClass property
        // then ignore event altogether.
        if (objectClass != null)
        {
            boolean hasPermission = false;
            if (m_acc != null)
            {
                for (int i = 0;
                    !hasPermission && (i < objectClass.length);
                    i++)
                {
                    try {
                        ServicePermission perm =
                            new ServicePermission(
                                objectClass[i], ServicePermission.GET);
                        m_acc.checkPermission(perm);
                        hasPermission = true;
                    } catch (Exception ex) {
                    }
                }
            }
            else
            {
                hasPermission = true;
            }

            if (hasPermission)
            {
                // Dispatch according to the filter.
                if ((m_filter == null) || m_filter.match(event.getServiceReference()))
                {
                    if (System.getSecurityManager() != null)
                    {
                        AccessController.doPrivileged(new PrivilegedAction() {
                            public Object run()
                            {
                                ((ServiceListener) getListener()).serviceChanged(event);
                                return null;
                            }
                        });
                    }
                    else
                    {
                        ((ServiceListener) getListener()).serviceChanged(event);
                    }
                }
            }
        }
    }
}