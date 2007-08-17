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
package org.osgi.framework;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.BasicPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.felix.framework.FilterImpl;

/**
 * <p>
 * This class is a replacement for the version that ships with the
 * standard OSGi JAR file.
 * </p>
**/
public final class AdminPermission extends BasicPermission
{
    static final long serialVersionUID = 307051004521261705L;

    public static final String CLASS = "class";
    public static final String EXECUTE = "execute";
    public static final String EXTENSIONLIFECYCLE = "extensionLifecycle";
    public static final String LIFECYCLE = "lifecycle";
    public static final String LISTENER = "listener";
    public static final String METADATA = "metadata";
    public static final String RESOLVE = "resolve";
    public static final String RESOURCE = "resource";
    public static final String STARTLEVEL = "startlevel";

    private static final int CLASS_MASK = 1;
    private static final int EXECUTE_MASK = 2;
    private static final int EXTENSIONLIFECYCLE_MASK = 4;
    private static final int LIFECYCLE_MASK = 8;
    private static final int LISTENER_MASK = 16;
    private static final int METADATA_MASK = 32;
    private static final int RESOLVE_MASK = 64;
    private static final int RESOURCE_MASK = 128;
    private static final int STARTLEVEL_MASK = 256;
    private static final int ALL_MASK =
        CLASS_MASK | EXECUTE_MASK | EXTENSIONLIFECYCLE_MASK |
        LIFECYCLE_MASK | LISTENER_MASK | METADATA_MASK |
        RESOLVE_MASK | RESOURCE_MASK | STARTLEVEL_MASK;

    private String m_actions = null;
    int m_actionMask = 0;

    // Cached filter for permissions created with a filter when
    // granting admin permissions.
    private FilterImpl m_filterImpl = null;

    // Bundle associated with the permission when checking
    // admin permissions.
    private Bundle m_bundle = null;
    // Cached bundle property dictionary when checking
    // admin permissions.
    private Dictionary m_bundleDict = null;

    // This constructor is only used when granting an admin permission.
	public AdminPermission()
    {
		this("*", ALL_MASK);
    }

    // This constructor is only used when checking a granted admin permission.
    public AdminPermission(Bundle bundle, String actions)
    {
        this(createName(bundle), actions);
        m_bundle = bundle;
    }

    // This constructor is only used when granting an admin permission.
	public AdminPermission(String filter, String actions)
    {
		super((filter == null) || (filter.equals("*")) ? "(id=*)" : filter);
        m_actionMask = parseActions(actions);
    }

    // This constructor is only used by the admin permission collection
    // when combining admin permissions or by the default constructor when granting
    // an admin permission
    AdminPermission(String filter, int actionMask)
    {
        super((filter == null) || (filter.equals("*")) ? "(id=*)" : filter);
        m_actionMask = actionMask;
    }

    public boolean equals(Object obj)
    {
	if (obj == this)
        {
	    return true;
	}

	if (!(obj instanceof AdminPermission))
        {
	    return false;
	}

	AdminPermission p = (AdminPermission) obj;

	return getName().equals(p.getName()) && (m_actionMask == p.m_actionMask);
    }

    public int hashCode()
    {
	return getName().hashCode() ^ getActions().hashCode();
    }

    public String getActions()
    {
        if (m_actions == null)
        {
            m_actions = createActionString(m_actionMask);
        }
	return m_actions;
    }

    public boolean implies(Permission p)
    {
	if (!(p instanceof AdminPermission))
        {
	    return false;
	}

        AdminPermission admin = (AdminPermission) p;

        // Make sure that the permission was create with a bundle or a "*".
        // Otherwise, throw an Exception - as per spec.
        if ((admin.m_bundle == null) && !(admin.getName().equals("(id=*)")))
        {
            throw new RuntimeException(
                "The specified permission was not constructed with a bundle or *!");
        }

        // Make sure the action mask is a subset.
        if ((m_actionMask & admin.m_actionMask) != admin.m_actionMask)
        {
            return false;
        }

        // Special case: if the specified permission was constructed with "*"
        // filter, then this method returns <code>true</code> if this object's
        // filter is "*".
        if (admin.getName().equals("(id=*)"))
        {
            return getName().equals("(id=*)");
        }

        // Next, if this object was create with a "*" we can return true
        // (This way we avoid creating and matching a filter).
        if (getName().equals("(id=*)"))
        {
            return true;
        }

        // Otherwise, see if this permission's filter matches the
        // dictionary of the passed in permission.
        if (m_filterImpl == null)
        {
            try
            {
                m_filterImpl = new FilterImpl(getName());
            }
            catch (InvalidSyntaxException ex)
            {
                return false;
            }
        }

        return m_filterImpl.match(admin.getBundleDictionary());
    }

    public PermissionCollection newPermissionCollection()
    {
	return new AdminPermissionCollection();
    }

    private Dictionary getBundleDictionary()
    {
        if (m_bundleDict == null)
        {
            // Add bundle properties to dictionary.
            m_bundleDict = new Hashtable();
            m_bundleDict.put("id", new Long(m_bundle.getBundleId()));

            String symbolicName = m_bundle.getSymbolicName();
            if (symbolicName != null)
            {
                m_bundleDict.put("name", symbolicName);
            }
            // Add location in privileged block since it performs a security check.
            if (System.getSecurityManager() != null)
            {
                AccessController.doPrivileged(new PrivilegedAction()
                {
                    public Object run()
                    {
                        m_bundleDict.put("location", m_bundle.getLocation());

                        createSigner(m_bundle, m_bundleDict);
                        return null;
                    }
                });
            }
            else
            {
                m_bundleDict.put("location", m_bundle.getLocation());
                createSigner(m_bundle, m_bundleDict);
            }
        }
        return m_bundleDict;
    }

    private static void createSigner(Bundle bundle, Dictionary dict)
    {
        try
        {
            Method method = bundle.getClass().getDeclaredMethod(
                "getSignerMatcher", null);
            method.setAccessible(true);

            Object signer = method.invoke(bundle, null);

            if (signer != null)
            {
                dict.put("signer", signer);
            }
        }
        catch (Exception ex)
        {
// TODO: log this or something
            ex.printStackTrace();
        }
    }

    private static int parseActions(String actions)
    {
        if (actions == null)
        {
            return ALL_MASK;
        }

        int mask = 0;

        StringTokenizer st = new StringTokenizer(actions, ", ");
        while (st.hasMoreTokens())
        {
            String s = st.nextToken();
            if (s.equals("*"))
            {
                mask = ALL_MASK;
                break;
            }
            else if (s.equalsIgnoreCase(CLASS))
            {
                mask |= CLASS_MASK;
            }
            else if (s.equalsIgnoreCase(EXECUTE))
            {
                mask |= EXECUTE_MASK;
            }
            else if (s.equalsIgnoreCase(EXTENSIONLIFECYCLE))
            {
                mask |= EXTENSIONLIFECYCLE_MASK;
            }
            else if (s.equalsIgnoreCase(LIFECYCLE))
            {
                mask |= LIFECYCLE_MASK;
            }
            else if (s.equalsIgnoreCase(LISTENER))
            {
                mask |= LISTENER_MASK;
            }
            else if (s.equalsIgnoreCase(METADATA))
            {
                mask |= METADATA_MASK;
            }
            else if (s.equalsIgnoreCase(RESOLVE))
            {
                mask |= RESOLVE_MASK;
            }
            else if (s.equalsIgnoreCase(RESOURCE))
            {
                mask |= RESOURCE_MASK;
            }
            else if (s.equalsIgnoreCase(STARTLEVEL))
            {
                mask |= STARTLEVEL_MASK;
            }
        }

        return mask;
    }

    private static String createActionString(int mask)
    {
        StringBuffer sb = new StringBuffer();

        if ((mask & CLASS_MASK) > 0)
        {
            sb.append(CLASS);
            sb.append(",");
        }

        if ((mask & EXECUTE_MASK) > 0)
        {
            sb.append(EXECUTE);
            sb.append(",");
        }

        if ((mask & EXTENSIONLIFECYCLE_MASK) > 0)
        {
            sb.append(EXTENSIONLIFECYCLE);
            sb.append(",");
        }

        if ((mask & LIFECYCLE_MASK) > 0)
        {
            sb.append(LIFECYCLE);
            sb.append(",");
        }

        if ((mask & LISTENER_MASK) > 0)
        {
            sb.append(LISTENER);
            sb.append(",");
        }

        if ((mask & METADATA_MASK) > 0)
        {
            sb.append(METADATA);
            sb.append(",");
        }

        if ((mask & RESOLVE_MASK) > 0)
        {
            sb.append(RESOLVE);
            sb.append(",");
        }

        if ((mask & RESOURCE_MASK) > 0)
        {
            sb.append(RESOURCE);
            sb.append(",");
        }

        if ((mask & STARTLEVEL_MASK) > 0)
        {
            sb.append(STARTLEVEL);
            sb.append(",");
        }

        // Remove trailing comma.
        if (sb.length() > 0)
        {
            sb.setLength(sb.length() - 1);
        }

        return sb.toString();
    }

    private static String createName(Bundle bundle)
    {
        StringBuffer sb = new StringBuffer();
        sb.append("(id=");
        sb.append(bundle.getBundleId());
        sb.append(")");
        return sb.toString();
    }
}

final class AdminPermissionCollection extends PermissionCollection
{
    private static final long serialVersionUID = 3747361397420496672L;
    private HashMap m_map = new HashMap();

    public void add(Permission permission)
    {
        if (!(permission instanceof AdminPermission))
        {
            throw new IllegalArgumentException("Invalid permission: " + permission);
        }
        else if (isReadOnly())
        {
            throw new SecurityException(
                "Cannot add to read-only permission collection.");
        }

        AdminPermission admin = (AdminPermission) permission;
        AdminPermission current = (AdminPermission) m_map.get(admin.getName());
        if (current != null)
        {
            if (admin.m_actionMask != current.m_actionMask)
            {
                m_map.put(admin.getName(),
                    new AdminPermission(admin.getName(),
                        admin.m_actionMask | current.m_actionMask));
            }
        }
        else
        {
            m_map.put(admin.getName(), admin);
        }
    }

    public boolean implies(Permission permission)
    {
        if (!(permission instanceof AdminPermission))
        {
            return false;
        }

        for (Iterator iter = m_map.values().iterator(); iter.hasNext(); )
        {
            if (((AdminPermission) iter.next()).implies(permission))
            {
                return true;
            }
        }

        return false;
    }

    public Enumeration elements()
    {
        return Collections.enumeration(m_map.values());
    }
}
