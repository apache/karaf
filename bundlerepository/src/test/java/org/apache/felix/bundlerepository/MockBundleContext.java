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
package org.apache.felix.bundlerepository;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class MockBundleContext implements BundleContext
{
    public void addBundleListener(BundleListener arg0)
    {
    }

    public void addFrameworkListener(FrameworkListener arg0)
    {
    }

    public void addServiceListener(ServiceListener arg0)
    {
    }

    public void addServiceListener(ServiceListener arg0, String arg1)
    {
    }

    public Filter createFilter(String arg0)
    {
        // returns a match-all filter always
        return new Filter()
        {
            public boolean matchCase(Dictionary arg0)
            {
                return true;
            }

            public boolean match(Dictionary arg0)
            {
                return true;
            }

            public boolean match(ServiceReference arg0)
            {
                return true;
            }
        };
    }

    public ServiceReference[] getAllServiceReferences(String arg0, String arg1)
    {
        return null;
    }

    public Bundle getBundle()
    {
        return null;
    }

    public Bundle getBundle(long arg0)
    {
        return null;
    }

    public Bundle[] getBundles()
    {
        return null;
    }

    public File getDataFile(String arg0)
    {
        return null;
    }

    public String getProperty(String name)
    {
        if (RepositoryAdminImpl.REPOSITORY_URL_PROP.equals(name))
        {
            URL url = getClass().getResource("/referred.xml");
            if (url != null)
            {
                return url.toExternalForm();
            }
        }

        return null;
    }

    public Object getService(ServiceReference arg0)
    {
        return null;
    }

    public ServiceReference getServiceReference(String arg0)
    {
        return null;
    }

    public ServiceReference[] getServiceReferences(String arg0, String arg1)
    {
        return null;
    }

    public Bundle installBundle(String arg0)
    {
        return null;
    }

    public Bundle installBundle(String arg0, InputStream arg1)
    {
        return null;
    }

    public ServiceRegistration registerService(String[] arg0, Object arg1, Dictionary arg2)
    {
        return null;
    }

    public ServiceRegistration registerService(String arg0, Object arg1, Dictionary arg2)
    {
        return null;
    }

    public void removeBundleListener(BundleListener arg0)
    {
    }

    public void removeFrameworkListener(FrameworkListener arg0)
    {
    }

    public void removeServiceListener(ServiceListener arg0)
    {
    }

    public boolean ungetService(ServiceReference arg0)
    {
        return false;
    }
}