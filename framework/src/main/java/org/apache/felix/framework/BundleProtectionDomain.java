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
package org.apache.felix.framework;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.Permission;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;

import org.apache.felix.moduleloader.IModule;

public class BundleProtectionDomain extends ProtectionDomain
{
    private final WeakReference m_felix;
    private final WeakReference m_bundle;
    private final int m_hashCode;
    private final String m_toString;
    private final WeakReference m_module;

    // TODO: SECURITY - This should probably take a module, not a bundle.
    BundleProtectionDomain(Felix felix, BundleImpl bundle)
        throws MalformedURLException
    {
        super(
            new CodeSource(
                Felix.m_secureAction.createURL(
                    Felix.m_secureAction.createURL(null, "location:", new FakeURLStreamHandler()), 
                    bundle._getLocation(),
                    new FakeURLStreamHandler()
                    ), 
                (Certificate[]) null), 
            null);
        m_felix = new WeakReference(felix);
        m_bundle = new WeakReference(bundle);
        m_module = new WeakReference(bundle.getCurrentModule());
        m_hashCode = bundle.hashCode();
        m_toString = "[" + bundle + "]";
    }

    IModule getModule() 
    {
        return (IModule) m_module.get();
    }

    public boolean implies(Permission permission)
    {
        Felix felix = (Felix) m_felix.get();
        return (felix != null) ? 
            felix.impliesBundlePermission(this, permission, false) : false;
    }

    public boolean impliesDirect(Permission permission)
    {
        Felix felix = (Felix) m_felix.get();
        return (felix != null) ? 
            felix.impliesBundlePermission(this, permission, true) : false;
    }

    BundleImpl getBundle()
    {
        return (BundleImpl) m_bundle.get();
    }

    public int hashCode()
    {
        return m_hashCode;
    }

    public boolean equals(Object other)
    {
        if ((other == null) || (other.getClass() != BundleProtectionDomain.class))
        {
            return false;
        }
        if (m_hashCode != other.hashCode())
        {
            return false;
        }
        return m_bundle.get() == ((BundleProtectionDomain) other).m_bundle.get();
    }

    public String toString()
    {
        return m_toString;
    }
}