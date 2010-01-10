/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.framework.security.condpermadmin;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.DomainCombiner;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.framework.BundleProtectionDomain;

/**
 * This class is a hack to get all BundleProtectionDomains currently on the
 * security stack. This way we don't need to have our own security manager set.
 */
final class DomainGripper implements DomainCombiner, PrivilegedAction
{
    private static final ProtectionDomain[] ALL_PERMISSION_PD = new ProtectionDomain[] { new ProtectionDomain(
        null, null)
    {
        public boolean implies(Permission perm)
        {
            return true;
        }
    } };

    // A per thread cache of DomainGripper objects. We might want to wrap them
    // in a softreference eventually
    private static final ThreadLocal m_cache = new ThreadLocal();

    private static final Permission ALL_PERMISSION = new AllPermission();

    private final List m_domains = new ArrayList();

    private AccessControlContext m_system = null;

    /**
     * Get all bundle protection domains and add them to the m_domains. Then
     * return the ALL_PERMISSION_PD.
     */
    public ProtectionDomain[] combine(ProtectionDomain[] current,
        ProtectionDomain[] assigned)
    {
        filter(current, m_domains);
        filter(assigned, m_domains);

        return ALL_PERMISSION_PD;
    }

    private void filter(ProtectionDomain[] assigned, List domains)
    {
        if (assigned != null)
        {
            for (int i = 0; i < assigned.length; i++)
            {
                if ((assigned[i].getClass() == BundleProtectionDomain.class)
                    && !domains.contains(assigned[i]))
                {
                    domains.add(assigned[i]);
                }
            }
        }
    }

    /**
     * Get the current bundle protection domains on the stack up to the last
     * privileged call.
     */
    public static List grab()
    {
        // First try to get a cached version. We cache by thread.
        DomainGripper gripper = (DomainGripper) m_cache.get();
        if (gripper == null)
        {
            // there is none so create one and cache it
            gripper = new DomainGripper();
            m_cache.set(gripper);
        }
        else
        {
            // This thread has a cached version so prepare it
            gripper.m_domains.clear();
        }

        // Get the current context.
        gripper.m_system = AccessController.getContext();

        // and merge it with the current combiner (i.e., gripper)
        AccessControlContext context = (AccessControlContext) AccessController
            .doPrivileged(gripper);

        gripper.m_system = null;

        // now get the protection domains
        AccessController.doPrivileged(gripper, context);

        // and return them
        return gripper.m_domains;
    }

    public Object run()
    {
        // this is a call to merge with the current context.
        if (m_system != null)
        {
            return new AccessControlContext(m_system, this);
        }

        // this is a call to get the protection domains.
        AccessController.checkPermission(ALL_PERMISSION);
        return null;
    }
}
