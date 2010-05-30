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
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Permission;
import java.security.Principal;
import java.security.ProtectionDomain;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import org.apache.felix.framework.BundleProtectionDomain;
import org.apache.felix.framework.security.permissionadmin.PermissionAdminImpl;
import org.apache.felix.framework.security.util.Conditions;
import org.apache.felix.framework.security.util.LocalPermissions;
import org.apache.felix.framework.security.util.Permissions;
import org.apache.felix.framework.security.util.PropertiesCache;
import org.apache.felix.framework.util.IteratorToEnumeration;
import org.apache.felix.framework.util.manifestparser.R4Library;

/*
import org.apache.felix.moduleloader.ICapability;
import org.apache.felix.moduleloader.IContent;
import org.apache.felix.moduleloader.IModule;
import org.apache.felix.moduleloader.IRequirement;
import org.apache.felix.moduleloader.IWire;
*/
import org.apache.felix.framework.capabilityset.Capability;
import org.apache.felix.framework.capabilityset.Requirement;
import org.apache.felix.framework.resolver.Content;
import org.apache.felix.framework.resolver.Module;
import org.apache.felix.framework.resolver.Wire;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;
import org.osgi.service.permissionadmin.PermissionInfo;

/**
 * An implementation of the ConditionalPermissionAdmin service that doesn't need
 * to have a framework specific security manager set. It use the DomainGripper
 * to know what bundleprotectiondomains are expected.
 */
public final class ConditionalPermissionAdminImpl implements
    ConditionalPermissionAdmin
{
    private static class OrderedHashMap extends HashMap
    {
        private final List m_order = new ArrayList();

        public Object put(Object key, Object value)
        {
            Object result = super.put(key, value);
            if (result != value)
            {
                m_order.remove(key);
                m_order.add(key);
            }
            return result;
        };

        public void putAll(Map map)
        {
            for (Iterator iter = map.entrySet().iterator(); iter.hasNext();)
            {
                Entry entry = (Entry) iter.next();
                put(entry.getKey(), entry.getValue());
            }
        };

        public Set keySet()
        {
            return new AbstractSet()
            {
                public Iterator iterator()
                {
                    return m_order.iterator();
                }

                public int size()
                {
                    return m_order.size();
                }

            };
        };

        public Set entrySet()
        {
            return new AbstractSet()
            {

                public Iterator iterator()
                {
                    return new Iterator()
                    {
                        Iterator m_iter = m_order.iterator();

                        public boolean hasNext()
                        {
                            return m_iter.hasNext();
                        }

                        public Object next()
                        {
                            final Object key = m_iter.next();
                            return new Entry()
                            {

                                public Object getKey()
                                {
                                    return key;
                                }

                                public Object getValue()
                                {
                                    return get(key);
                                }

                                public Object setValue(Object arg0)
                                {
                                    throw new IllegalStateException(
                                        "Not Implemented");
                                }
                            };
                        }

                        public void remove()
                        {
                            throw new IllegalStateException("Not Implemented");
                        }

                    };
                }

                public int size()
                {
                    return m_order.size();
                }

            };
        };

        public Collection values()
        {
            List result = new ArrayList();
            for (Iterator iter = m_order.iterator(); iter.hasNext();)
            {
                result.add(super.get(iter.next()));
            }
            return result;
        };

        public Object remove(Object key)
        {
            Object result = super.remove(key);
            if (result != null)
            {
                m_order.remove(key);
            }
            return result;
        };

        public void clear()
        {
            super.clear();
            m_order.clear();
        };
    };

    private static final ConditionInfo[] EMPTY_CONDITION_INFO = new ConditionInfo[0];
    private static final PermissionInfo[] EMPTY_PERMISSION_INFO = new PermissionInfo[0];
    private final Map m_condPermInfos = new OrderedHashMap();
    private final PropertiesCache m_propertiesCache;
    private final Permissions m_permissions;
    private final Conditions m_conditions;
    private final LocalPermissions m_localPermissions;
    private final PermissionAdminImpl m_pai;

    public ConditionalPermissionAdminImpl(Permissions permissions,
        Conditions condtions, LocalPermissions localPermissions,
        PropertiesCache cache, PermissionAdminImpl pai) throws IOException
    {
        m_propertiesCache = cache;
        m_permissions = permissions;
        m_conditions = condtions;
        m_localPermissions = localPermissions;
        Map old = new OrderedHashMap();
        // Now try to restore the cache.
        m_propertiesCache.read(ConditionalPermissionInfoImpl.class, old);
        for (Iterator iter = old.entrySet().iterator(); iter.hasNext();)
        {
            Entry entry = (Entry) iter.next();
            String name = (String) entry.getKey();
            ConditionalPermissionInfoImpl cpi = ((ConditionalPermissionInfoImpl) entry
                .getValue());
            m_condPermInfos.put(name, new ConditionalPermissionInfoImpl(name,
                cpi._getConditionInfos(), cpi._getPermissionInfos(), this, cpi
                    .isAllow()));
        }
        m_pai = pai;
    }

    public ConditionalPermissionInfo addConditionalPermissionInfo(
        ConditionInfo[] conditions, PermissionInfo[] permissions)
    {
        Object sm = System.getSecurityManager();
        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(Permissions.ALL_PERMISSION);
        }
        ConditionalPermissionInfoImpl result = new ConditionalPermissionInfoImpl(
            notNull(conditions), notNull(permissions), this, true);

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
                tmp = new OrderedHashMap();
                tmp.putAll(m_condPermInfos);

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
                    if (tmp != null)
                    {
                        m_condPermInfos.clear();
                        m_condPermInfos.putAll(tmp);
                    }
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

    private static class FakeBundle implements Bundle
    {
        private final Map m_certs;

        public FakeBundle(Map certs)
        {
            m_certs = Collections.unmodifiableMap(certs);
        }

        public Enumeration findEntries(String arg0, String arg1, boolean arg2)
        {
            return null;
        }

        public BundleContext getBundleContext()
        {
            return null;
        }

        public long getBundleId()
        {
            return -1;
        }

        public URL getEntry(String arg0)
        {
            return null;
        }

        public Enumeration getEntryPaths(String arg0)
        {
            return null;
        }

        public Dictionary getHeaders()
        {
            return new Hashtable();
        }

        public Dictionary getHeaders(String arg0)
        {
            return new Hashtable();
        }

        public long getLastModified()
        {
            return 0;
        }

        public String getLocation()
        {
            return "";
        }

        public ServiceReference[] getRegisteredServices()
        {
            return null;
        }

        public URL getResource(String arg0)
        {
            return null;
        }

        public Enumeration getResources(String arg0) throws IOException
        {
            return null;
        }

        public ServiceReference[] getServicesInUse()
        {
            return null;
        }

        public Map getSignerCertificates(int arg0)
        {
            return m_certs;
        }

        public int getState()
        {
            return Bundle.UNINSTALLED;
        }

        public String getSymbolicName()
        {
            return null;
        }

        public Version getVersion()
        {
            return Version.emptyVersion;
        }

        public boolean hasPermission(Object arg0)
        {
            return false;
        }

        public Class loadClass(String arg0) throws ClassNotFoundException
        {
            return null;
        }

        public void start() throws BundleException
        {
            throw new IllegalStateException();
        }

        public void start(int arg0) throws BundleException
        {
            throw new IllegalStateException();
        }

        public void stop() throws BundleException
        {
            throw new IllegalStateException();
        }

        public void stop(int arg0) throws BundleException
        {
            throw new IllegalStateException();
        }

        public void uninstall() throws BundleException
        {
            throw new IllegalStateException();
        }

        public void update() throws BundleException
        {
            throw new IllegalStateException();
        }

        public void update(InputStream arg0) throws BundleException
        {
            throw new IllegalStateException();
        }

        public boolean equals(Object o)
        {
            return this == o;
        }

        public int hashCode()
        {
            return System.identityHashCode(this);
        }
    }

    private static class FakeCert extends X509Certificate
    {
        private final Principal m_principal;

        public FakeCert(final String principal)
        {
            m_principal = new Principal()
            {
                public String getName()
                {
                    return principal;
                }
            };
        }

        public void checkValidity()
            throws java.security.cert.CertificateExpiredException,
            java.security.cert.CertificateNotYetValidException
        {

        }

        public void checkValidity(Date date)
            throws java.security.cert.CertificateExpiredException,
            java.security.cert.CertificateNotYetValidException
        {
        }

        public int getBasicConstraints()
        {
            return 0;
        }

        public Principal getIssuerDN()
        {
            return null;
        }

        public boolean[] getIssuerUniqueID()
        {
            return null;
        }

        public boolean[] getKeyUsage()
        {
            return null;
        }

        public Date getNotAfter()
        {
            return null;
        }

        public Date getNotBefore()
        {
            return null;
        }

        public BigInteger getSerialNumber()
        {
            return null;
        }

        public String getSigAlgName()
        {
            return null;
        }

        public String getSigAlgOID()
        {
            return null;
        }

        public byte[] getSigAlgParams()
        {
            return null;
        }

        public byte[] getSignature()
        {
            return null;
        }

        public Principal getSubjectDN()
        {
            return m_principal;
        }

        public boolean[] getSubjectUniqueID()
        {
            return null;
        }

        public byte[] getTBSCertificate()
            throws java.security.cert.CertificateEncodingException
        {
            return null;
        }

        public int getVersion()
        {
            return 0;
        }

        public byte[] getEncoded()
            throws java.security.cert.CertificateEncodingException
        {
            return null;
        }

        public PublicKey getPublicKey()
        {
            return null;
        }

        public String toString()
        {
            return m_principal.getName();
        }

        public void verify(PublicKey key)
            throws java.security.cert.CertificateException,
            NoSuchAlgorithmException, InvalidKeyException,
            NoSuchProviderException, SignatureException
        {

        }

        public void verify(PublicKey key, String sigProvider)
            throws java.security.cert.CertificateException,
            NoSuchAlgorithmException, InvalidKeyException,
            NoSuchProviderException, SignatureException
        {

        }

        public Set getCriticalExtensionOIDs()
        {
            return null;
        }

        public byte[] getExtensionValue(String arg0)
        {
            return null;
        }

        public Set getNonCriticalExtensionOIDs()
        {
            return null;
        }

        public boolean hasUnsupportedCriticalExtension()
        {
            return false;
        }

        public boolean equals(Object o)
        {
            return this == o;
        }

        public int hashCode()
        {
            return System.identityHashCode(this);
        }

    }

    public AccessControlContext getAccessControlContext(final String[] signers)
    {
        Map certificates = new HashMap();
        for (int i = 0; i < signers.length; i++)
        {
            StringTokenizer tok = new StringTokenizer(signers[i], ";");
            List certsList = new ArrayList();
            while (tok.hasMoreTokens())
            {
                certsList.add(tok.nextToken());
            }
            String[] certs = (String[]) certsList.toArray(new String[certsList
                .size()]);

            X509Certificate key = new FakeCert(certs[0]);
            List certList = new ArrayList();
            certificates.put(key, certList);
            certList.add(key);
            for (int j = 1; j < certs.length; j++)
            {
                certList.add(new FakeCert(certs[j]));
            }
        }
        final Bundle fake = new FakeBundle(certificates);
        ProtectionDomain domain = new ProtectionDomain(null, null)
        {
            public boolean implies(Permission permission)
            {
                List posts = new ArrayList();
                Boolean result = m_pai.hasPermission("", fake, permission,
                    ConditionalPermissionAdminImpl.this, this, null);
                if (result != null)
                {
                    return result.booleanValue();
                }
                if (eval(posts, new Module()
                {

                    public Bundle getBundle()
                    {
                        return fake;
                    }

                    public List<Capability> getCapabilities()
                    {
                        return null;
                    }

                    public Class getClassByDelegation(String arg0)
                        throws ClassNotFoundException
                    {
                        return null;
                    }

                    public Content getContent()
                    {
                        return null;
                    }

                    public int getDeclaredActivationPolicy()
                    {
                        return 0;
                    }

                    public List<Requirement> getDynamicRequirements()
                    {
                        return null;
                    }

                    public URL getEntry(String arg0)
                    {
                        return null;
                    }

                    public Map getHeaders()
                    {
                        return null;
                    }

                    public String getId()
                    {
                        return null;
                    }

                    public InputStream getInputStream(int arg0, String arg1)
                        throws IOException
                    {
                        return null;
                    }

                    public List<R4Library> getNativeLibraries()
                    {
                        return null;
                    }

                    public List<Requirement> getRequirements()
                    {
                        return null;
                    }

                    public URL getResourceByDelegation(String arg0)
                    {
                        return null;
                    }

                    public Enumeration getResourcesByDelegation(String arg0)
                    {
                        return null;
                    }

                    public Object getSecurityContext()
                    {
                        return null;
                    }

                    public String getSymbolicName()
                    {
                        return null;
                    }

                    public Version getVersion()
                    {
                        return null;
                    }

                    public List<Wire> getWires()
                    {
                        return null;
                    }

                    public boolean hasInputStream(int arg0, String arg1)
                        throws IOException
                    {
                        return false;
                    }

                    public boolean isExtension()
                    {
                        return false;
                    }

                    public boolean isResolved()
                    {
                        return false;
                    }

                    public void setSecurityContext(Object arg0)
                    {
                    }
                }, permission, m_pai))
                {
                    if (!posts.isEmpty())
                    {
                        return m_conditions.evalRecursive(posts);
                    }
                    return true;
                }
                return false;
            }
        };
        return new AccessControlContext(new ProtectionDomain[] { domain });
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
            result = new ConditionalPermissionInfoImpl(this, name, true);

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
                result = (ConditionalPermissionInfoImpl) m_condPermInfos
                    .get(name);

                if (result == null)
                {
                    result = new ConditionalPermissionInfoImpl(name,
                        conditions, permissions, this, true);
                }
                else
                {
                    result.setConditionsAndPermissions(conditions, permissions);
                }
            }
        }
        else
        {
            result = new ConditionalPermissionInfoImpl(conditions, permissions,
                this, true);
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
     * automatically in case this is the first check in one permission check. If
     * not then it will keep track of which domains we have already see. While
     * it keeps track it builds up a list of postponed tuples which it will
     * evaluate at the last domain. See the core spec 9.5.1 and following for a
     * general description.
     * 
     * @param felixBundle
     *            the bundle in question.
     * @param loader
     *            the content loader of the bundle to get access to the jar to
     *            check for local permissions.
     * @param root
     *            the bundle id.
     * @param signers
     *            the signers (this is to support the ACC based on signers)
     * @param pd
     *            the bundle protection domain
     * @param permission
     *            the permission currently checked
     * @param direct
     *            whether this is a direct check or not. direct check will not
     *            expect any further bundle domains on the stack
     * @return true in case the permission is granted or there are postponed
     *         tuples false if not. Again, see the spec for more explanations.
     */
    public boolean hasPermission(Module module, Content content,
        ProtectionDomain pd, Permission permission, boolean direct, Object admin)
    {
        // System.out.println(felixBundle + "-" + permission);
        List domains = null;
        List tuples = null;
        Object[] entry = null;
        // first see whether this is the normal case (the special case is for
        // the ACC based on signers).
        // In case of a direct call we don't need to look for other pds
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
                entry = new Object[] { new ArrayList(DomainGripper.grab()),
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

        // check the local permissions. they need to all the permission if there
        // are any
        if (!impliesLocal(module.getBundle(), content, permission))
        {
            return false;
        }

        List posts = new ArrayList();

        boolean result = eval(posts, module, permission, admin);

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
            // Now eval the postponed tupels. if the previous eval did return
            // false
            // tuples will be empty so we don't return from here.
            if (!tuples.isEmpty())
            {
                return m_conditions.evalRecursive(tuples);
            }
        }
        else
        {
            // this is to support recursive permission checks. In case we
            // trigger
            // a permission check while eval the stack is null until this point
            m_stack.set(entry);
        }

        return result;
    }

    public boolean impliesLocal(Bundle felixBundle, Content content,
        Permission permission)
    {
        return m_localPermissions.implies(content, felixBundle, permission);
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
    private boolean eval(List posts, Module module, Permission permission,
        Object admin)
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
        if (m_permissions.getPermissions(
            m_permissions.getImplicit(module.getBundle())).implies(permission,
            module.getBundle()))
        {
            return true;
        }
        List pls = new ArrayList();
        // now do the real thing
        for (Iterator iter = condPermInfos.iterator(); iter.hasNext();)
        {
            ConditionalPermissionInfoImpl cpi = (ConditionalPermissionInfoImpl) iter
                .next();

            ConditionInfo[] conditions = cpi._getConditionInfos();

            List currentPosts = new ArrayList();

            Conditions conds = m_conditions.getConditions(module, conditions);
            if (!conds.isSatisfied(currentPosts, m_permissions
                .getPermissions(cpi._getPermissionInfos()), permission))
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
                pls.add(new Object[] { cpi, null });
                break;
            }
            pls.add(new Object[] { cpi, currentPosts, conds });
        }
        while (pls.size() > 1)
        {
            if (!((ConditionalPermissionInfoImpl) ((Object[]) pls.get(pls
                .size() - 1))[0]).isAllow())
            {
                pls.remove(pls.size() - 1);
            }
            else
            {
                break;
            }
        }
        if (pls.size() == 1)
        {
            if (((Object[]) pls.get(0))[1] != null)
            {
                posts.add(pls.get(0));
            }
            return ((ConditionalPermissionInfoImpl) ((Object[]) pls.get(0))[0])
                .isAllow();
        }
        for (Iterator iter = pls.iterator(); iter.hasNext();)
        {
            posts.add(iter.next());
        }
        return !posts.isEmpty();
    }

    public ConditionalPermissionInfo newConditionalPermissionInfo(
        String encodedConditionalPermissionInfo)
    {
        return new ConditionalPermissionInfoImpl(
            encodedConditionalPermissionInfo);
    }

    public ConditionalPermissionInfo newConditionalPermissionInfo(String name,
        ConditionInfo[] conditions, PermissionInfo[] permissions, String access)
    {
        return new ConditionalPermissionInfoImpl(name, conditions, permissions,
            ConditionalPermissionAdminImpl.this, access
                .equals(ConditionalPermissionInfo.ALLOW));
    }

    public ConditionalPermissionUpdate newConditionalPermissionUpdate()
    {
        return new ConditionalPermissionUpdate()
        {
            List current = null;
            List out = null;
            {
                synchronized (m_condPermInfos)
                {
                    current = new ArrayList(m_condPermInfos.values());
                    out = new ArrayList(m_condPermInfos.values());
                }
            }

            public boolean commit()
            {
                synchronized (m_condPermInfos)
                {
                    if (current.equals(new ArrayList(m_condPermInfos.values())))
                    {
                        m_condPermInfos.clear();
                        write(null, null);
                        for (Iterator iter = out.iterator(); iter.hasNext();)
                        {
                            ConditionalPermissionInfoImpl cpii = (ConditionalPermissionInfoImpl) iter
                                .next();
                            write(cpii.getName(), cpii);
                        }
                    }
                    else
                    {
                        return false;
                    }
                }
                return true;
            }

            public List getConditionalPermissionInfos()
            {
                return out;
            }
        };
    }

    public boolean handlePAHandle(BundleProtectionDomain pd)
    {
        Object[] entry = (Object[]) m_stack.get();

        if (entry == null)
        {
            entry = new Object[] { new ArrayList(DomainGripper.grab()),
                new ArrayList() };
        }

        ((List) entry[0]).remove(pd);
        if (((List) entry[0]).isEmpty())
        {
            m_stack.set(null);
            if (!((List) entry[1]).isEmpty())
            {
                return m_conditions.evalRecursive(((List) entry[1]));
            }
        }
        else
        {
            m_stack.set(entry);
        }

        return true;
    }

    public void clearPD()
    {
        m_stack.set(null);
    }
}
