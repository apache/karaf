package org.apache.felix.framework;

import java.security.Permission;
import java.security.ProtectionDomain;

class BundleProtectionDomain extends ProtectionDomain
{
    private final Felix m_felix;
    private final FelixBundle m_bundle;

    public BundleProtectionDomain(Felix felix, FelixBundle bundle)
    {
        super(null, null);
        m_felix = felix;
        m_bundle = bundle;
    }

    public boolean implies(Permission permission)
    {
        return m_felix.impliesBundlePermission(this, permission, false);
    }

    FelixBundle getBundle()
    {
        return m_bundle;
    }

    public int hashCode()
    {
        return m_bundle.hashCode();
    }

    public boolean equals(Object other)
    {
        return m_bundle.equals(other);
    }
}
