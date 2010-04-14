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
package org.apache.felix.framework.security.util;

import java.io.File;
import java.io.FilePermission;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.felix.framework.util.SecureAction;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.permissionadmin.PermissionInfo;

/**
 * A permission cache that uses permisssion infos as keys. Permission are
 * created from the parent classloader or any exported package.
 */
// TODO: maybe use bundle events instead of soft/weak references
public final class Permissions
{
    private static final ClassLoader m_classLoader = Permissions.class
        .getClassLoader();

    private static final Map m_permissionCache = new HashMap();
    private static final Map m_permissions = new HashMap();
    private static final ReferenceQueue m_permissionsQueue = new ReferenceQueue();

    private static final ThreadLocal m_stack = new ThreadLocal();

    private final Map m_cache;
    private final ReferenceQueue m_queue;
    private final BundleContext m_context;
    private final PermissionInfo[] m_permissionInfos;
    private final boolean m_allPermission;
    private final SecureAction m_action;

    public static final AllPermission ALL_PERMISSION = new AllPermission();

    private static final PermissionInfo[] IMPLICIT = new PermissionInfo[] { new PermissionInfo(
        FilePermission.class.getName(), "-", "read,write,delete") };

    Permissions(PermissionInfo[] permissionInfos, BundleContext context,
        SecureAction action)
    {
        m_context = context;
        m_permissionInfos = permissionInfos;
        m_cache = new HashMap();
        m_queue = new ReferenceQueue();
        m_action = action;
        for (int i = 0; i < m_permissionInfos.length; i++)
        {
            if (m_permissionInfos[i].getType().equals(
                AllPermission.class.getName()))
            {
                m_allPermission = true;
                return;
            }
        }
        m_allPermission = false;
    }

    public Permissions(BundleContext context, SecureAction action)
    {
        m_context = context;
        m_permissionInfos = null;
        m_cache = null;
        m_queue = null;
        m_allPermission = true;
        m_action = action;
    }

    public PermissionInfo[] getImplicit(Bundle bundle)
    {
        return new PermissionInfo[] {
            IMPLICIT[0],
            new PermissionInfo(AdminPermission.class.getName(), "(id="
                + bundle.getBundleId() + ")", AdminPermission.METADATA),
            new PermissionInfo(AdminPermission.class.getName(), "(id="
                + bundle.getBundleId() + ")", AdminPermission.RESOURCE),
            new PermissionInfo(AdminPermission.class.getName(), "(id="
                + bundle.getBundleId() + ")", AdminPermission.CONTEXT) };
    }

    public Permissions getPermissions(PermissionInfo[] permissionInfos)
    {
        cleanUp(m_permissionsQueue, m_permissions);

        Permissions result = null;
        synchronized (m_permissions)
        {
            result = (Permissions) m_permissions.get(new Entry(permissionInfos));
        }
        if (result == null)
        {
			//permissionInfos may not be referenced by the new Permissions, as
			//otherwise the reference in m_permissions prevents the key from
			//being garbage collectable.
            PermissionInfo[] permissionInfosClone = new PermissionInfo[permissionInfos.length];
            System.arraycopy(permissionInfos, 0, permissionInfosClone, 0, permissionInfos.length);
            result = new Permissions(permissionInfosClone, m_context, m_action);
            synchronized (m_permissions)
            {
                m_permissions.put(
                    new Entry(permissionInfos, m_permissionsQueue), result);
            }
        }
        return result;
    }

    private static final class Entry extends WeakReference
    {
        private final int m_hashCode;

        Entry(Object entry, ReferenceQueue queue)
        {
            super(entry, queue);
            m_hashCode = entry.hashCode();
        }

        Entry(Object entry)
        {
            super(entry);
            m_hashCode = entry.hashCode();
        }

        public Object get()
        {
            return super.get();
        }

        public int hashCode()
        {
            return m_hashCode;
        }

        public boolean equals(Object o)
        {
            if (o == null)
            {
                return false;
            }

            if (o == this)
            {
                return true;
            }

            Object entry = super.get();

            if (o instanceof Entry)
            {

                Object otherEntry = ((Entry) o).get();
				if (entry == null)
				{
					return otherEntry == null;
				}
				if (otherEntry == null)
				{
					return false;
				}
				if (!entry.getClass().equals(otherEntry.getClass()))
				{
					return false;
				}
				if (entry instanceof Object[])
				{
					return Arrays.equals((Object[])entry, (Object[])otherEntry);
				}		
                return entry.equals(((Entry) o).get());
            }
            else
            {
                return false;
            }
        }
    }

    private static final class DefaultPermissionCollection extends
        PermissionCollection
    {
        private final Map m_perms = new HashMap();

        public void add(Permission perm)
        {
            synchronized (m_perms)
            {
                m_perms.put(perm, perm);
            }
        }

        public Enumeration elements()
        {
            throw new IllegalStateException("Not implemented");
        }

        public boolean implies(Permission perm)
        {
            Map perms = null;

            synchronized (m_perms)
            {
                perms = m_perms;
            }

            Permission permission = (Permission) perms.get(perm);

            if ((permission != null) && permission.implies(perm))
            {
                return true;
            }

            for (Iterator iter = perms.values().iterator(); iter.hasNext();)
            {
                Permission current = (Permission) iter.next();
                if ((current != null) && (current != permission)
                    && current.implies(perm))
                {
                    return true;
                }
            }
            return false;
        }
    }

    private void cleanUp(ReferenceQueue queue, Map cache)
    {
        for (Entry entry = (Entry) queue.poll(); entry != null; entry = (Entry) queue
            .poll())
        {
            synchronized (cache)
            {
                cache.remove(entry);
            }
        }
    }

    /**
     * @param target
     *            the permission to be implied
     * @param bundle
     *            if not null then allow implicit permissions like file access
     *            to local data area
     * @return true if the permission is implied by this permissions object.
     */
    public boolean implies(Permission target, final Bundle bundle)
    {
        if (m_allPermission)
        {
            return true;
        }

        Class targetClass = target.getClass();

        cleanUp(m_queue, m_cache);

        if ((bundle != null) && targetClass == FilePermission.class)
        {
            for (int i = 0; i < m_permissionInfos.length; i++)
            {
                if (m_permissionInfos[i].getType().equals(
                    FilePermission.class.getName()))
                {
                    String postfix = "";
                    String name = m_permissionInfos[i].getName();
                    if (!"<<ALL FILES>>".equals(name))
                    {
                        if (name.endsWith("*") || name.endsWith("-"))
                        {
                            postfix = name.substring(name.length() - 1);
                            name = name.substring(0, name.length() - 1);
                        }
                        if (!(new File(name)).isAbsolute())
                        {
                            BundleContext context = (BundleContext) AccessController
                                .doPrivileged(new PrivilegedAction()
                                {
                                    public Object run()
                                    {
                                        return bundle.getBundleContext();
                                    }
                                });
                            if (context == null)
                            {
                                break;
                            }
                            name = m_action.getAbsolutePath(new File(context
                                .getDataFile(""), name));
                        }
                        if (postfix.length() > 0)
                        {
                            if ((name.length() > 0) && !name.endsWith("/"))
                            {
                                name += "/" + postfix;
                            }
                            else
                            {
                                name += postfix;
                            }
                        }
                    }
                    Permission source = createPermission(new PermissionInfo(
                        FilePermission.class.getName(), name,
                        m_permissionInfos[i].getActions()), targetClass);
                    postfix = "";
                    name = target.getName();
                    if (!"<<ALL FILES>>".equals(name))
                    {
                        if (name.endsWith("*") || name.endsWith("-"))
                        {
                            postfix = name.substring(name.length() - 1);
                            name = name.substring(0, name.length() - 1);
                        }
                        if (!(new File(name)).isAbsolute())
                        {
                            BundleContext context = (BundleContext) AccessController
                                .doPrivileged(new PrivilegedAction()
                                {
                                    public Object run()
                                    {
                                        return bundle.getBundleContext();
                                    }
                                });
                            if (context == null)
                            {
                                break;
                            }
                            name = m_action.getAbsolutePath(new File(context
                                .getDataFile(""), name));
                        }
                        if (postfix.length() > 0)
                        {
                            if ((name.length() > 0) && !name.endsWith("/"))
                            {
                                name += "/" + postfix;
                            }
                            else
                            {
                                name += postfix;
                            }
                        }
                    }
                    Permission realTarget = createPermission(
                        new PermissionInfo(FilePermission.class.getName(),
                            name, target.getActions()), targetClass);
                    if (source.implies(realTarget))
                    {
                        return true;
                    }
                }
            }
            return false;
        }

        Object current = m_stack.get();

        if (current == null)
        {
            m_stack.set(targetClass);
        }
        else
        {
            if (current instanceof HashSet)
            {
                if (((HashSet) current).contains(targetClass))
                {
                    return false;
                }
                ((HashSet) current).add(targetClass);
            }
            else
            {
                if (current == targetClass)
                {
                    return false;
                }
                HashSet frame = new HashSet();
                frame.add(current);
                frame.add(targetClass);
                m_stack.set(frame);
                current = frame;
            }
        }

        try
        {
            SoftReference collectionEntry = null;

            PermissionCollection collection = null;

            synchronized (m_cache)
            {
                collectionEntry = (SoftReference) m_cache.get(targetClass);
            }

            if (collectionEntry != null)
            {
                collection = (PermissionCollection) collectionEntry.get();
            }

            if (collection == null)
            {
                collection = target.newPermissionCollection();

                if (collection == null)
                {
                    collection = new DefaultPermissionCollection();
                }

                for (int i = 0; i < m_permissionInfos.length; i++)
                {
                    PermissionInfo permissionInfo = m_permissionInfos[i];
                    String infoType = permissionInfo.getType();
                    String permissionType = targetClass.getName();

                    if (infoType.equals(permissionType))
                    {
                        Permission permission = createPermission(
                            permissionInfo, targetClass);

                        if (permission != null)
                        {
                            collection.add(permission);
                        }
                    }
                }

                synchronized (m_cache)
                {
                    m_cache.put(new Entry(target.getClass(), m_queue),
                        new SoftReference(collection));
                }
            }

            return collection.implies(target);
        }
        finally
        {
            if (current == null)
            {
                m_stack.set(null);
            }
            else
            {
                ((HashSet) current).remove(targetClass);
                if (((HashSet) current).isEmpty())
                {
                    m_stack.set(null);
                }
            }
        }
    }

    private Permission addToCache(String encoded, Permission permission)
    {
        if (permission == null)
        {
            return null;
        }

        synchronized (m_permissionCache)
        {
            Map inner = null;

            SoftReference ref = (SoftReference) m_permissionCache.get(encoded);
            if (ref != null)
            {
                inner = (Map) ref.get();
            }
            if (inner == null)
            {
                inner = new HashMap();
                m_permissionCache.put(encoded,
                    new SoftReference(inner, m_queue));
            }

            inner.put(new Entry(permission.getClass()), new Entry(permission));
        }

        return permission;
    }

    private Permission getFromCache(String encoded, Class target)
    {
        synchronized (m_permissionCache)
        {
            SoftReference ref = (SoftReference) m_permissionCache.get(encoded);
            if (ref != null)
            {
                Map inner = (Map) ref.get();
                if (inner != null)
                {
                    Entry entry = (Entry) inner.get(target);
                    if (entry != null)
                    {
                        Permission result = (Permission) entry.get();
                        if (result != null)
                        {
                            return result;
                        }
                        inner.remove(entry);
                    }
                    if (inner.isEmpty())
                    {
                        m_permissionCache.remove(encoded);
                    }
                }
                else
                {
                    m_permissionCache.remove(encoded);
                }
            }

        }

        return null;
    }

    private Permission createPermission(final PermissionInfo permissionInfo,
        final Class target)
    {
        return (Permission) AccessController
            .doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    Permission cached = getFromCache(permissionInfo
                        .getEncoded(), target);

                    if (cached != null)
                    {
                        return cached;
                    }

                    try
                    {
                        if (m_classLoader.loadClass(target.getName()) == target)
                        {
                            return addToCache(permissionInfo.getEncoded(),
                                createPermission(permissionInfo.getName(),
                                    permissionInfo.getActions(), target));
                        }
                    }
                    catch (ClassNotFoundException e1)
                    {
                    }

                    ServiceReference[] refs = null;
                    try
                    {
                        refs = m_context.getServiceReferences(
                            PackageAdmin.class.getName(), null);
                    }
                    catch (InvalidSyntaxException e)
                    {
                    }
                    if (refs != null)
                    {
                        for (int i = 0; i < refs.length; i++)
                        {
                            PackageAdmin admin = (PackageAdmin) m_context
                                .getService(refs[i]);

                            if (admin != null)
                            {
                                Permission result = null;
                                Bundle bundle = admin.getBundle(target);
                                if (bundle != null)
                                {
                                    ExportedPackage[] exports = admin
                                        .getExportedPackages(bundle);
                                    if (exports != null)
                                    {
                                        String name = target.getName();
                                        name = name.substring(0, name
                                            .lastIndexOf('.'));

                                        for (int j = 0; j < exports.length; j++)
                                        {
                                            if (exports[j].getName().equals(
                                                name))
                                            {
                                                result = createPermission(
                                                    permissionInfo.getName(),
                                                    permissionInfo.getActions(),
                                                    target);
                                                break;
                                            }
                                        }
                                    }
                                }

                                m_context.ungetService(refs[i]);

                                return addToCache(permissionInfo.getEncoded(),
                                    result);
                            }
                        }
                    }

                    return null;
                }
            });
    }

    private Permission createPermission(String name, String action, Class target)
    {
        // System.out.println("\n\n|" + name + "|\n--\n|" + action + "|\n--\n" +
        // target + "\n\n");
        try
        {
            return (Permission) m_action.getConstructor(target,
                new Class[] { String.class, String.class }).newInstance(
                new Object[] { name, action });
        }
        catch (Exception ex)
        {
            // TODO: log this or something
        }

        return null;
    }
}
