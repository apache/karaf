/*
 *   Copyright 2006 The Apache Software Foundation
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
package org.osgi.framework;

import java.security.*;
import java.util.Enumeration;
import java.util.StringTokenizer;

import sun.net.www.MeteredStream;

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

    private String m_bundleFilter = null;
    private String m_actions = null;
    private int m_actionMask = 0;

	public AdminPermission()
    {
		this("*", "*");
	}

    public AdminPermission(Bundle bundle, String actions)
    {
        this(createNameFilter(bundle), actions);
    }

	public AdminPermission(String filter, String actions)
    {
		super((filter == null) ? "*" : filter);
        m_actionMask = parseActions(actions);
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

		return m_bundleFilter.equals(p.m_bundleFilter) && (m_actionMask == p.m_actionMask);
	}

	public int hashCode()
    {
		return m_bundleFilter.hashCode() ^ getActions().hashCode();
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

        if (m_actionMask != admin.m_actionMask)
        {
            return false;
        }

        // TODO: SECURITY - Still need check that the "name" filter.

        return false;
	}

	public PermissionCollection newPermissionCollection()
    {
		return new AdminPermissionCollection();
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
        else if ((mask & EXECUTE_MASK) > 0)
        {
            sb.append(EXECUTE);
            sb.append(",");
        }
        else if ((mask & EXTENSIONLIFECYCLE_MASK) > 0)
        {
            sb.append(EXTENSIONLIFECYCLE);
            sb.append(",");
        }
        else if ((mask & LIFECYCLE_MASK) > 0)
        {
            sb.append(LIFECYCLE);
            sb.append(",");
        }
        else if ((mask & LISTENER_MASK) > 0)
        {
            sb.append(LISTENER);
            sb.append(",");
        }
        else if ((mask & METADATA_MASK) > 0)
        {
            sb.append(METADATA);
            sb.append(",");
        }
        else if ((mask & RESOLVE_MASK) > 0)
        {
            sb.append(RESOLVE);
            sb.append(",");
        }
        else if ((mask & RESOURCE_MASK) > 0)
        {
            sb.append(RESOURCE);
            sb.append(",");
        }
        else if ((mask & STARTLEVEL_MASK) > 0)
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

    private static String createNameFilter(Bundle bundle)
    {
        StringBuffer sb = new StringBuffer();
        sb.append("(id=");
        sb.append(bundle.getBundleId());
        sb.append(")");
        return sb.toString();
    }

    final class AdminPermissionCollection extends PermissionCollection
    {
        public void add(Permission permission)
        {
            // TODO: SECURITY - AdminPermissionCollection.add()
        }

        public boolean implies(Permission permission)
        {
            // TODO: SECURITY - AdminPermissionCollection.implies()
            return false;
        }

        public Enumeration elements()
        {
            // TODO: SECURITY - AdminPermissionCollection.elements()
            return null;
        }
    }
}