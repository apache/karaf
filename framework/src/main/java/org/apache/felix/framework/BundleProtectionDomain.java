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

import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.Permission;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;

public class BundleProtectionDomain extends ProtectionDomain
{
    private final Felix m_felix;
    private final BundleImpl m_bundle;

    // TODO: SECURITY - This should probably take a module, not a bundle.
    BundleProtectionDomain(Felix felix, BundleImpl bundle)
        throws MalformedURLException
    {
        super(new CodeSource(new URL(new URL(null, "location:", 
            new FakeURLStreamHandler()), bundle._getLocation(),
            new FakeURLStreamHandler()), (Certificate[]) null), null);
        m_felix = felix;
        m_bundle = bundle;
    }

    public boolean implies(Permission permission)
    {
        return m_felix.impliesBundlePermission(this, permission, false);
    }

    public boolean impliesDirect(Permission permission)
    {
        return m_felix.impliesBundlePermission(this, permission, true);
    }

    BundleImpl getBundle()
    {
        return m_bundle;
    }

    public int hashCode()
    {
        return m_bundle.hashCode();
    }

    public boolean equals(Object other)
    {
        if ((other == null) || other.getClass() != BundleProtectionDomain.class)
        {
            return false;
        }
        return m_bundle == ((BundleProtectionDomain) other).m_bundle;
    }

    public String toString()
    {
        return "[" + m_bundle + "]";
    }
}