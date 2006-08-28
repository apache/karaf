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

import java.lang.reflect.Method;
import java.security.*;
import java.util.*;

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
                        return null;
                    }
                });
            }
            else
            {
                m_bundleDict.put("location", m_bundle.getLocation());
            }

            m_bundleDict.put("signer", new Signer(m_bundle));
        }
        return m_bundleDict;
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

    public static final class Signer implements PrivilegedAction
    {
        private Bundle m_bundleImpl = null;
        private String m_filter = null;
        private Method m_getSubjectDNs = null;
        private boolean m_init = false;

        public Signer(String filter)
        {
            m_filter = filter;
        }

        Signer(Bundle bundle)
        {
            m_bundleImpl = bundle;
        }

        public boolean equals(Object o)
        {
            if (!(o instanceof Signer))
            {
                return false;
            }

            String pattern = ((Signer) o).m_filter;

            if (pattern == null)
            {
                return true;
            }

            String[] dns = getSubjectDNs();

            if (dns == null)
            {
                return pattern.trim().equals("\\*");
            }

            for (int i = 0;i < dns.length;i++)
            {
                if (match(pattern, dns[i]))
                {
                    return true;
                }
            }

            return false;
        }

        private String[] getSubjectDNs()
        {
            if (System.getSecurityManager() != null)
            {
                return (String[]) AccessController.doPrivileged(this);
            }
            else
            {
                return null;
            }
        }

        public Object run()
        {
            try
            {
                if (!m_init)
                {
                    m_getSubjectDNs =
                        m_bundleImpl.getClass().getDeclaredMethod("getSubjectDNs", null);

                    m_init = true;
                }

                m_getSubjectDNs.setAccessible(true);

                return m_getSubjectDNs.invoke(m_bundleImpl, null);

            }
            catch (Exception ex)
            {
                // TODO: log this or something
                ex.printStackTrace();
            }

            return null;
        }

        private static boolean match(String pattern, String dn)
        {
            try
            {
                return ((pattern != null) && (dn != null)) ?
                    matchDN(pattern.toCharArray(), 0, dn.toCharArray(), 0) : false;
            }
            catch (Exception ex)
            {
                // TODO: log this or something
                ex.printStackTrace();
            }

            return false;
        }

        private static boolean matchDN(char[] pattern, int pPos, char[] dn, int dPos)
        {
            pPos = skip(pattern, pPos, ' ');

            if (pPos >= pattern.length)
            {
                return true;
            }

            int befor = pPos;

            if ((pPos < pattern.length -1) && (pattern[pPos] == '\\') && (pattern[pPos + 1] == '*'))
            {
                pPos = pPos + 1;
            }

            switch (pattern[pPos++])
            {
                case '*':
                    pPos = skip(pattern, pPos, ' ');
                    if ((pPos < pattern.length) && (pattern[pPos] == ';'))
                    {
                        if (matchDN(pattern, ++pPos, dn, dPos))
                        {
                            return true;
                        }
                        return matchDN(pattern, pPos, dn, skipEscapedUntil(dn, dPos, ';') + 1);
                    }
                    if (pPos >= pattern.length)
                    {
                        return true;
                    }
                    return matchRDN(pattern, befor, dn, dPos);
                case '-':
                    pPos = skip(pattern, pPos, ' ');
                    if ((pPos < pattern.length) && (pattern[pPos] == ';'))
                    {
                        int next = dPos;
                        pPos++;
                        do
                        {
                            if (matchDN(pattern, pPos, dn, next))
                            {
                                return true;
                            }
                            next = skipEscapedUntil(dn, next, ';') + 1;
                        } while (next < dn.length);

                        return false;
                    }
                    if (pPos >= pattern.length)
                    {
                        return true;
                    }
                    throw new IllegalArgumentException("[" + pPos + "]" + new String(pattern));
                default:
                    break;
            }

            return matchRDN(pattern, befor, dn, dPos);
        }

        private static boolean matchRDN(char[] pattern, int pPos, char[] dn, int dPos)
        {
            pPos = skip(pattern, pPos, ' ');

            if (pPos >= pattern.length)
            {
                return true;
            }

            if ((pPos < pattern.length -1) && (pattern[pPos] == '\\') && (pattern[pPos + 1] == '*'))
            {
                pPos = pPos + 1;
            }

            switch (pattern[pPos++])
            {
                case '*':
                    pPos = skip(pattern, pPos, ' ');
                    if ((pPos < pattern.length) && (pattern[pPos] == ','))
                    {
                        pPos++;
                        do
                        {
                            if (matchKV(pattern, pPos, dn, dPos))
                            {
                                return true;
                            }

                            int comma = skipEscapedUntil(dn, dPos, ',');
                            int colon = skipEscapedUntil(dn, dPos, ';');

                            dPos = (comma > colon) ? colon : comma;
                        } while ((dPos < dn.length) && (dn[dPos++] == ','));
                        return false;
                    }
                    throw new IllegalArgumentException("[" + pPos + "]" + new String(pattern));
                default:
                    break;
            }

            return matchKV(pattern, pPos - 1, dn, dPos);
        }

        private static boolean matchKV(char[] pattern, int pPos, char[] dn, int dPos)
        {
            pPos = skip(pattern, pPos, ' ');

            if (pPos >= pattern.length)
            {
                return false;
            }

            int equals = skipEscapedUntil(pattern, pPos, '=');
            int comma = skipEscapedUntil(pattern, pPos, ',');
            int colon = skipEscapedUntil(pattern, pPos, ';');
            if (((colon < pattern.length) && (colon < equals)) ||
                ((comma < pattern.length) && (comma < equals)) ||
                (equals >= pattern.length))
            {
                return false;
            }

            String key = (String) KEY2OIDSTRING.get(
                new String(pattern, pPos, equals - pPos).toLowerCase(Locale.US).trim());

            if (key == null)
            {
                throw new IllegalArgumentException("Bad key [" +
                    new String(pattern, pPos, equals - pPos) + "] in [" +
                    new String(pattern) + "]");
            }

            pPos = equals + 1;
            int keylength = key.length();
            for (int i = 0;i < keylength;i++)
            {
                if ((dPos >= dn.length) || (key.charAt(i) != dn[dPos++]))
                {
                    return false;
                }
            }

            if ((dPos >= dn.length) || (dn[dPos++] != '='))
            {
                return false;
            }

            pPos = skip(pattern, pPos, ' ');
            if ((pPos < pattern.length -1) && (pattern[pPos] == '\\') && (pattern[pPos + 1] == '*'))
            {
                pPos = skip(pattern, pPos + 2, ' ');
                if (pPos >= pattern.length)
                {
                    return true;
                }
                comma = skipEscapedUntil(dn, dPos, ',');
                colon = skipEscapedUntil(dn, dPos, ';');
                if ((pattern[pPos] == ',') && (colon > comma))
                {
                    return matchKV(pattern, ++pPos, dn, comma + 1);
                }

                if (pattern[pPos] == ';' )
                {
                    return matchDN(pattern, ++pPos, dn, colon + 1);
                }

                return false;
            }
            boolean escaped = false;
            while ((pPos < pattern.length) && (dPos < dn.length))
            {
                switch (Character.toLowerCase(pattern[pPos++]))
                {
                    case ' ':
                        if ((pattern[pPos - 2] != ' ') && ((dn[dPos++] != ' ') &&
                            (dn[--dPos] != ';') && (dn[dPos] != ',')))
                        {
                            return false;
                        }
                        break;
                    case '\\':
                        escaped = !escaped;
                        break;

                    case '(':
                    case ')':
                        if (escaped)
                        {
                            if (dn[dPos++] != pattern[pPos - 1])
                            {
                                return false;
                            }
                            escaped = false;
                            break;
                        }
                        return false;
                    case ';':
                        if (!escaped)
                        {
                            if ((dPos < dn.length) && ((dn[dPos] == ',') || (dn[dPos] == ';')))
                            {
                                return matchDN(pattern, pPos, dn, skipEscapedUntil(dn, dPos, ';') + 1);
                            }
                            return false;
                        }
                    case ',':
                        if (!escaped)
                        {
                            if ((dPos < dn.length) && (dn[dPos] == ','))
                            {
                                return matchKV(pattern, pPos, dn, dPos + 1);
                            }
                            return false;
                        }
                    default:
                        if (escaped)
                        {
                            if (dn[dPos++] != '\\')
                            {
                                return false;
                            }
                            escaped = false;
                        }
                        if (dn[dPos++] != Character.toLowerCase(pattern[pPos - 1]))
                        {
                            return false;
                        }
                        break;
                }
            }

            pPos = skip(pattern, pPos, ' ');
            if (pPos >= pattern.length)
            {
                if ((dPos >= dn.length) || (dn[dPos] == ',') || (dn[dPos] == ';'))
                {
                    return true;
                }
            }
            else
            {
                switch (pattern[pPos++])
                {
                    case ',':
                        return matchKV(pattern, pPos, dn, dPos);
                    case ';':
                        return matchDN(pattern, pPos, dn, dPos);
                    default:
                        break;
                }
            }

            return false;
        }

        private static final Map KEY2OIDSTRING = new HashMap();

        static {
            KEY2OIDSTRING.put("2.5.4.3", "cn");
            KEY2OIDSTRING.put("cn", "cn");
            KEY2OIDSTRING.put("commonname", "cn");
            KEY2OIDSTRING.put("2.5.4.4", "sn");
            KEY2OIDSTRING.put("sn", "sn");
            KEY2OIDSTRING.put("surname", "sn");
            KEY2OIDSTRING.put("2.5.4.6", "c");
            KEY2OIDSTRING.put("c", "c");
            KEY2OIDSTRING.put("countryname", "c");
            KEY2OIDSTRING.put("2.5.4.7", "l");
            KEY2OIDSTRING.put("l", "l");
            KEY2OIDSTRING.put("localityname", "l");
            KEY2OIDSTRING.put("2.5.4.8", "st");
            KEY2OIDSTRING.put("st", "st");
            KEY2OIDSTRING.put("stateorprovincename", "st");
            KEY2OIDSTRING.put("2.5.4.10", "o");
            KEY2OIDSTRING.put("o", "o");
            KEY2OIDSTRING.put("organizationname", "o");
            KEY2OIDSTRING.put("2.5.4.11", "ou");
            KEY2OIDSTRING.put("ou", "ou");
            KEY2OIDSTRING.put("organizationalunitname", "ou");
            KEY2OIDSTRING.put("2.5.4.12", "title");
            KEY2OIDSTRING.put("t", "title");
            KEY2OIDSTRING.put("title", "title");
            KEY2OIDSTRING.put("2.5.4.42", "givenname");
            KEY2OIDSTRING.put("givenname", "givenname");
            KEY2OIDSTRING.put("2.5.4.43", "initials");
            KEY2OIDSTRING.put("initials", "initials");
            KEY2OIDSTRING.put("2.5.4.44", "generationqualifier");
            KEY2OIDSTRING.put("generationqualifier", "generationqualifier");
            KEY2OIDSTRING.put("2.5.4.46", "dnqualifier");
            KEY2OIDSTRING.put("dnqualifier", "dnqualifier");
            KEY2OIDSTRING.put("2.5.4.9", "street");
            KEY2OIDSTRING.put("street", "street");
            KEY2OIDSTRING.put("streetaddress", "street");
            KEY2OIDSTRING.put("0.9.2342.19200300.100.1.25", "dc");
            KEY2OIDSTRING.put("dc", "dc");
            KEY2OIDSTRING.put("domaincomponent", "dc");
            KEY2OIDSTRING.put("0.9.2342.19200300.100.1.1", "uid");
            KEY2OIDSTRING.put("uid", "uid");
            KEY2OIDSTRING.put("userid", "uid");
            KEY2OIDSTRING.put("1.2.840.113549.1.9.1", "emailaddress");
            KEY2OIDSTRING.put("emailaddress", "emailaddress");
            KEY2OIDSTRING.put("2.5.4.5", "serialnumber");
            KEY2OIDSTRING.put("serialnumber", "serialnumber");
        }

        private static int skipEscapedUntil(char[] string, int pos, char value)
        {
            boolean escaped = false;

            while (pos < string.length)
            {
                switch (string[pos++])
                {
                    case '\\':
                        escaped = true;
                        break;
                    default:
                        if (!escaped)
                        {
                            if (string[pos - 1] == value)
                            {
                                return pos - 1;
                            }
                        }
                        escaped = false;
                        break;
                }
            }

            return pos;
        }

        private static int skip(char[] string, int pos, char value)
        {
            while (pos < string.length)
            {
                if (string[pos] != value)
                {
                    break;
                }
                pos++;
            }

            return pos;
        }
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
