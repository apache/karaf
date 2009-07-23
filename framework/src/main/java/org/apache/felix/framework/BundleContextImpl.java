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

import java.io.File;
import java.io.InputStream;
import java.util.Dictionary;

import org.apache.felix.framework.ext.FelixBundleContext;
import org.osgi.framework.*;

class BundleContextImpl implements FelixBundleContext
{
    private Logger m_logger = null;
    private Felix m_felix = null;
    private BundleImpl m_bundle = null;
    private boolean m_valid = true;

    protected BundleContextImpl(Logger logger, Felix felix, BundleImpl bundle)
    {
        m_logger = logger;
        m_felix = felix;
        m_bundle = bundle;
    }

    protected void invalidate()
    {
        m_valid = false;
    }

    public void addRequirement(String s) throws BundleException
    {
        throw new BundleException("Not implemented yet.");
    }

    public void removeRequirement() throws BundleException
    {
        throw new BundleException("Not implemented yet.");
    }

    public void addCapability() throws BundleException
    {
        throw new BundleException("Not implemented yet.");
    }

    public void removeCapability() throws BundleException
    {
        throw new BundleException("Not implemented yet.");
    }

    public String getProperty(String name)
    {
        checkValidity();

        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            if (!(Constants.FRAMEWORK_VERSION.equals(name) ||
                Constants.FRAMEWORK_VENDOR.equals(name) ||
                Constants.FRAMEWORK_LANGUAGE.equals(name)||
                Constants.FRAMEWORK_OS_NAME.equals(name) ||
                Constants.FRAMEWORK_OS_VERSION.equals(name) ||
                Constants.FRAMEWORK_PROCESSOR.equals(name)))
            {
                ((SecurityManager) sm).checkPermission(
                    new java.util.PropertyPermission(name, "read"));
            }
        }

        return m_felix.getProperty(name);
    }

    public Bundle getBundle()
    {
        checkValidity();

        return m_bundle;
    }

    public Filter createFilter(String expr)
        throws InvalidSyntaxException
    {
        checkValidity();

        return FrameworkUtil.createFilter(expr);
    }

    public Bundle installBundle(String location)
        throws BundleException
    {
        return installBundle(location, null);
    }

    public Bundle installBundle(String location, InputStream is)
        throws BundleException
    {
        checkValidity();

        Bundle result = null;

        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            result = m_felix.installBundle(location, is);
            // Do check the bundle again in case that is was installed
            // already.
            ((SecurityManager) sm).checkPermission(
                new AdminPermission(result, AdminPermission.LIFECYCLE));
        }
        else
        {
            result = m_felix.installBundle(location, is);
        }

        return result;
    }

    public Bundle getBundle(long id)
    {
        checkValidity();

        return m_felix.getBundle(id);
    }

    public Bundle[] getBundles()
    {
        checkValidity();

        return m_felix.getBundles();
    }

    public void addBundleListener(BundleListener l)
    {
        checkValidity();

        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            if (l instanceof SynchronousBundleListener)
            {
                ((SecurityManager) sm).checkPermission(new AdminPermission(m_bundle,
                    AdminPermission.LISTENER));
            }
        }

        m_felix.addBundleListener(m_bundle, l);
    }

    public void removeBundleListener(BundleListener l)
    {
        checkValidity();

        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            if (l instanceof SynchronousBundleListener)
            {
                ((SecurityManager) sm).checkPermission(new AdminPermission(m_bundle,
                    AdminPermission.LISTENER));
            }
        }

        m_felix.removeBundleListener(m_bundle, l);
    }

    public void addServiceListener(ServiceListener l)
    {
        try
        {
            addServiceListener(l, null);
        }
        catch (InvalidSyntaxException ex)
        {
            // This will not happen since the filter is null.
        }
    }

    public void addServiceListener(ServiceListener l, String s)
        throws InvalidSyntaxException
    {
        checkValidity();

        m_felix.addServiceListener(m_bundle, l, s);
    }

    public void removeServiceListener(ServiceListener l)
    {
        checkValidity();

        m_felix.removeServiceListener(m_bundle, l);
    }

    public void addFrameworkListener(FrameworkListener l)
    {
        checkValidity();

        m_felix.addFrameworkListener(m_bundle, l);
    }

    public void removeFrameworkListener(FrameworkListener l)
    {
        checkValidity();

        m_felix.removeFrameworkListener(m_bundle, l);
    }

    public ServiceRegistration registerService(
        String clazz, Object svcObj, Dictionary dict)
    {
        return registerService(new String[] { clazz }, svcObj, dict);
    }

    public ServiceRegistration registerService(
        String[] clazzes, Object svcObj, Dictionary dict)
    {
        checkValidity();

        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            if (clazzes != null)
            {
                for (int i = 0;i < clazzes.length;i++)
                {
                    ((SecurityManager) sm).checkPermission(
                        new ServicePermission(clazzes[i], ServicePermission.REGISTER));
                }
            }
        }

        return m_felix.registerService(m_bundle, clazzes, svcObj, dict);
    }

    public ServiceReference getServiceReference(String clazz)
    {
        checkValidity();

        try
        {
            ServiceReference[] refs = getServiceReferences(clazz, null);
            return getBestServiceReference(refs);
        }
        catch (InvalidSyntaxException ex)
        {
            m_logger.log(Logger.LOG_ERROR, "BundleContextImpl: " + ex);
        }
        return null;
    }

    private ServiceReference getBestServiceReference(ServiceReference[] refs)
    {
        if (refs == null)
        {
            return null;
        }

        if (refs.length == 1)
        {
            return refs[0];
        }

        // Loop through all service references and return
        // the "best" one according to its rank and ID.
        ServiceReference bestRef = refs[0];
        for (int i = 1; i < refs.length; i++)
        {
            if (bestRef.compareTo(refs[i]) < 0)
            {
                bestRef = refs[i];
            }
        }

        return bestRef;
    }

    public ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException
    {
        checkValidity();

        return m_felix.getAllowedServiceReferences(m_bundle, clazz, filter, false);

    }

    public ServiceReference[] getServiceReferences(String clazz, String filter)
        throws InvalidSyntaxException
    {
        checkValidity();

        return m_felix.getAllowedServiceReferences(m_bundle, clazz, filter, true);

    }

    public Object getService(ServiceReference ref)
    {
        checkValidity();

        if (ref == null)
        {
            throw new NullPointerException("Specified service reference cannot be null.");
        }

        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            String[] objectClass = (String[]) ref.getProperty(Constants.OBJECTCLASS);

            if (objectClass == null)
            {
                return null;
            }

            boolean hasPermission = false;

            for (int i = 0;(i < objectClass.length) && !hasPermission;i++)
            {
                try
                {
                    ((SecurityManager) sm).checkPermission(
                        new ServicePermission(objectClass[i], ServicePermission.GET));

                    hasPermission = true;
                }
                catch (Exception ex)
                {

                }
            }

            if (!hasPermission)
            {
                throw new SecurityException("No permission");
            }
        }

        return m_felix.getService(m_bundle, ref);
    }

    public boolean ungetService(ServiceReference ref)
    {
        checkValidity();

        if (ref == null)
        {
            throw new NullPointerException("Specified service reference cannot be null.");
        }

        // Unget the specified service.
        return m_felix.ungetService(m_bundle, ref);
    }

    public File getDataFile(String s)
    {
        checkValidity();

        return m_felix.getDataFile(m_bundle, s);
    }

    private void checkValidity()
    {
        if (m_valid)
        {
            switch (m_bundle.getState())
            {
                case Bundle.ACTIVE:
                case Bundle.STARTING:
                case Bundle.STOPPING:
                    return;
            }
        }

        throw new IllegalStateException("Invalid BundleContext.");
    }
}