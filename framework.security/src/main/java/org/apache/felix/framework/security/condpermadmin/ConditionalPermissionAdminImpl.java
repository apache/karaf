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

import java.io.IOException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.DomainCombiner;
import java.security.Permission;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.felix.framework.security.util.Conditions;
import org.apache.felix.framework.security.util.LocalPermissions;
import org.apache.felix.framework.security.util.Permissions;
import org.apache.felix.framework.security.util.PropertiesCache;
import org.apache.felix.framework.util.IteratorToEnumeration;
import org.apache.felix.moduleloader.IContent;
import org.osgi.framework.Bundle;
import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.permissionadmin.PermissionInfo;

/**
 * An implementation of the ConditionalPermissionAdmin service that doesn't need
 * to have a framework specific security manager set. It use the DomainGripper
 * to know what bundleprotectiondomains are expected.  
 */
public final class ConditionalPermissionAdminImpl implements
    ConditionalPermissionAdmin
{
    private static final ConditionInfo[] EMPTY_CONDITION_INFO = new ConditionInfo[0];
    private static final PermissionInfo[] EMPTY_PERMISSION_INFO = new PermissionInfo[0];
    private final Map m_condPermInfos = new HashMap();
    private final PropertiesCache m_propertiesCache;
    private final Permissions m_permissions;
    private final Conditions m_conditions;
    private final LocalPermissions m_localPermissions;

    public ConditionalPermissionAdminImpl(Permissions permissions,
        Conditions condtions, LocalPermissions localPermissions,
        PropertiesCache cache) throws IOException
    {
        m_propertiesCache = cache;
        m_permissions = permissions;
        m_conditions = condtions;
        m_localPermissions = localPermissions;
        // Now try to restore the cache.
        Map old = m_propertiesCache.read(ConditionalPermissionInfoImpl.class);
        if (old != null)
        {
            for (Iterator iter = old.entrySet().iterator(); iter.hasNext();)
            {
                Entry entry = (Entry) iter.next();
                String name = (String) entry.getKey();
                ConditionalPermissionInfoImpl cpi =
                    ((ConditionalPermissionInfoImpl) entry.getValue());
                m_condPermInfos.put(name, new ConditionalPermissionInfoImpl(
                    name, cpi._getConditionInfos(), cpi._getPermissionInfos(),
                    this));
            }
        }
    }

    public ConditionalPermissionInfo addConditionalPermissionInfo(
        ConditionInfo[] conditions, PermissionInfo[] permissions)
    {
        Object sm = System.getSecurityManager();
        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(Permissions.ALL_PERMISSION);
        }
        ConditionalPermissionInfoImpl result =
            new ConditionalPermissionInfoImpl(notNull(conditions),
                notNull(permissions), this);

        return write(result.getName(), result);
    }

    ConditionalPermissionInfoImpl write(String name,
        ConditionalPermissionInfoImpl cpi)
    {
        synchronized (m_propertiesCache)
        {
            Map tmp = null;

            synchronized (m_condPermInfos)
            {
                tmp = new HashMap(m_condPermInfos);

                if ((name != null) && (cpi != null))
                {
                    m_condPermInfos.put(name, cpi);
                }
                else if (name != null)
                {
                    m_condPermInfos.remove(name);
                }
                else
                {
                    tmp = null;
                }
            }

            try
            {
                m_propertiesCache.write(m_condPermInfos);
            }
            catch (IOException ex)
            {
                synchronized (m_condPermInfos)
                {
                    m_condPermInfos.clear();
                    m_condPermInfos.putAll(tmp);
                }
                ex.printStackTrace();
                throw new IllegalStateException(ex.getMessage());
            }
        }
        synchronized (m_condPermInfos)
        {
            return (ConditionalPermissionInfoImpl) m_condPermInfos.get(name);
        }
    }

    // TODO: this is pretty much untested so it might not work like this
    public AccessControlContext getAccessControlContext(String[] signers)
    {
        final String[] finalSigners =
            (String[]) notNull(signers).toArray(new String[0]);
        return new AccessControlContext(AccessController.getContext(),
            new DomainCombiner()
            {
                public ProtectionDomain[] combine(ProtectionDomain[] arg0,
                    ProtectionDomain[] arg1)
                {
                    return new ProtectionDomain[] { new ProtectionDomain(null,
                        null)
                    {
                        public boolean implies(Permission permission)
                        {
                            return hasPermission(null, null, null, finalSigners,
                                this, permission, true, null);
                        }
                    } };
                }
            });
    }

    public ConditionalPermissionInfo getConditionalPermissionInfo(String name)
    {
        if (name == null)
        {
            throw new IllegalArgumentException("Name may not be null");
        }
        ConditionalPermissionInfoImpl result = null;

        synchronized (m_condPermInfos)
        {
            result = (ConditionalPermissionInfoImpl) m_condPermInfos.get(name);
        }

        if (result == null)
        {
            result = new ConditionalPermissionInfoImpl(this, name);

            result = write(result.getName(), result);
        }

        return result;
    }

    public Enumeration getConditionalPermissionInfos()
    {
        synchronized (m_condPermInfos)
        {
            return new IteratorToEnumeration((new ArrayList(m_condPermInfos
                .values())).iterator());
        }
    }

    public ConditionalPermissionInfo setConditionalPermissionInfo(String name,
        ConditionInfo[] conditions, PermissionInfo[] permissions)
    {
        Object sm = System.getSecurityManager();
        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(Permissions.ALL_PERMISSION);
        }

        ConditionalPermissionInfoImpl result = null;
        conditions = notNull(conditions);
        permissions = notNull(permissions);

        if (name != null)
        {
            synchronized (m_condPermInfos)
            {
                result =
                    (ConditionalPermissionInfoImpl) m_condPermInfos.get(name);

                if (result == null)
                {
                    result =
                        new ConditionalPermissionInfoImpl(name, conditions,
                            permissions, this);
                }
                else
                {
                    result.setConditionsAndPermissions(conditions, permissions);
                }
            }
        }
        else
        {
            result =
                new ConditionalPermissionInfoImpl(conditions, permissions, this);
        }

        return write(result.getName(), result);
    }

    private PermissionInfo[] notNull(PermissionInfo[] permissions)
    {
        if (permissions == null)
        {
            return ConditionalPermissionInfoImpl.PERMISSION_INFO;
        }
        return (PermissionInfo[]) notNull((Object[]) permissions).toArray(
            EMPTY_PERMISSION_INFO);
    }

    private ConditionInfo[] notNull(ConditionInfo[] conditions)
    {
        if (conditions == null)
        {
            return ConditionalPermissionInfoImpl.CONDITION_INFO;
        }
        return (ConditionInfo[]) notNull((Object[]) conditions).toArray(
            EMPTY_CONDITION_INFO);
    }

    private List notNull(Object[] elements)
    {
        List result = new ArrayList();

        for (int i = 0; i < elements.length; i++)
        {
            if (elements[i] != null)
            {
                result.add(elements[i]);
            }
        }

        return result;
    }

    // The thread local stack used to keep track of bundle protection domains we
    // still expect to see.
    private final ThreadLocal m_stack = new ThreadLocal();

    /**
     * This method does the actual permission check. If it is not a direct check
     * it will try to determine the other bundle domains that will follow 
     * automatically in case this is the first check in one permission check.
     * If not then it will keep track of which domains we have already see.
     * While it keeps track it builds up a list of postponed tuples which
     * it will evaluate at the last domain. See the core spec 9.5.1 and following
     * for a general description.
     * 
     * @param felixBundle the bundle in question.
     * @param loader the content loader of the bundle to get access to the jar 
     *    to check for local permissions.
     * @param root the bundle id.
     * @param signers the signers (this is to support the ACC based on signers) 
     * @param pd the bundle protection domain
     * @param permission the permission currently checked
     * @param direct whether this is a direct check or not. direct check will not
     *     expect any further bundle domains on the stack
     * @return true in case the permission is granted or there are postponed tuples
     *     false if not. Again, see the spec for more explanations.
     */
    public boolean hasPermission(Bundle felixBundle, IContent content, String root, 
        String[] signers, ProtectionDomain pd, Permission permission,
        boolean direct, Object admin)
    {
        // System.out.println(felixBundle + "-" + permission);
        List domains = null;
        List tuples = null;
        Object[] entry = null;
        // first see whether this is the normal case (the special case is for 
        // the ACC based on signers).
        if (signers == null)
        {
            // In case of  a direct call we don't need to look for other pds
            if (direct)
            {
                domains = new ArrayList();
                tuples = new ArrayList();
                domains.add(pd);
            }
            else
            {
                // Get the other pds from the stck
                entry = (Object[]) m_stack.get();

                // if there are none then get them from the gripper
                if (entry == null)
                {
                    entry =
                        new Object[] { new ArrayList(DomainGripper.grab()),
                            new ArrayList() };
                }
                else
                {
                    m_stack.set(null);
                }

                domains = (List) entry[0];
                tuples = (List) entry[1];
                if (!domains.contains(pd))
                {
                    // We have been called directly without the direct flag
                    domains.clear();
                    domains.add(pd);
                }
            }
        }

        // check the local permissions. they need to all the permission if there
        // are any
        if (!m_localPermissions.implies(root, content, felixBundle, permission))
        {
            return false;
        }

        List posts = new ArrayList();

        boolean result = eval(posts, felixBundle, signers, permission, admin);

        if (signers != null)
        {
            return result;
        }

        domains.remove(pd);

        // We postponed tuples
        if (!posts.isEmpty())
        {
            tuples.add(posts);
        }

        // Are we at the end or this was a direct call?
        if (domains.isEmpty())
        {
            m_stack.set(null);
            // Now eval the postponed tupels. if the previous eval did return false
            // tuples will be empty so we don't return from here.
            if (!tuples.isEmpty())
            {
                return m_conditions.evalRecursive(tuples);
            }
        }
        else
        {
            // this is to support recursive permission checks. In case we trigger
            // a permission check while eval the stack is null until this point
            m_stack.set(entry);
        }

        return result;
    }

    public boolean isEmpty()
    {
        synchronized (m_condPermInfos)
        {
            return m_condPermInfos.isEmpty();
        }
    }

    // we need to find all conditions that apply and then check whether they
    // de note the permission in question unless the conditions are postponed
    // then we make sure their permissions imply the permission and add them
    // to the list of posts. Return true in case we pass or have posts
    // else falls and clear the posts first.
    private boolean eval(List posts, Bundle bundle, String[] signers,
        Permission permission, Object admin)
    {
        List condPermInfos = null;

        synchronized (m_condPermInfos)
        {
            if (isEmpty() && (admin == null))
            {
                return true;
            }
            condPermInfos = new ArrayList(m_condPermInfos.values());
        }

        // Check for implicit permissions like access to file area
        if ((bundle != null)
            && m_permissions.getPermissions(m_permissions.getImplicit(bundle))
                .implies(permission, bundle))
        {
            return true;
        }

        // now do the real thing
        for (Iterator iter = condPermInfos.iterator(); iter.hasNext();)
        {
            ConditionalPermissionInfoImpl cpi =
                (ConditionalPermissionInfoImpl) iter.next();

            ConditionInfo[] conditions = cpi._getConditionInfos();

            List currentPosts = new ArrayList();

            if (!m_conditions.getConditions(bundle, signers, conditions)
                .isSatisfied(currentPosts))
            {
                continue;
            }

            if (!m_permissions.getPermissions(cpi._getPermissionInfos())
                .implies(permission, null))
            {
                continue;
            }

            if (currentPosts.isEmpty())
            {
                posts.clear();
                return true;
            }

            posts.add(currentPosts);
        }

        return !posts.isEmpty();
    }
}
