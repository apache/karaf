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

import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.obr.Repository;
import org.osgi.service.obr.Resource;

public class RepositoryImplTest extends TestCase
{
    public void testReferral1() throws Exception
    {
        URL url = getClass().getResource("/referral1_repository.xml");

        RepositoryAdminImpl repoAdmin = createRepositoryAdmin();
        RepositoryImpl repo = (RepositoryImpl) repoAdmin.addRepository(url);
        Referral[] refs = repo.getReferrals();

        assertNotNull("Expect referrals", refs);
        assertTrue("Expect one referral", refs.length == 1);

        // <referral depth="1" url="referred.xml" />
        assertEquals(1, refs[0].getDepth());
        assertEquals("referred.xml", refs[0].getUrl());

        // expect two resources
        Resource[] res = repoAdmin.discoverResources(null);
        assertNotNull("Expect Resource", res);
        assertEquals("Expect two resources", 2, res.length);

        // first resource is from the referral1_repository.xml
        assertEquals("6", res[0].getId());
        assertEquals("referral1_repository", res[0].getRepository().getName());

        // second resource is from the referred.xml
        assertEquals("99", res[1].getId());
        assertEquals("referred", res[1].getRepository().getName());
    }

    public void testReferral2() throws Exception
    {
        URL url = getClass().getResource("/referral1_repository.xml");

        RepositoryAdminImpl repoAdmin = createRepositoryAdmin();
        RepositoryImpl repo = (RepositoryImpl) repoAdmin.addRepository(url, 1);
        Referral[] refs = repo.getReferrals();

        assertNotNull("Expect referrals", refs);
        assertTrue("Expect one referral", refs.length == 1);

        // <referral depth="1" url="referred.xml" />
        assertEquals(1, refs[0].getDepth());
        assertEquals("referred.xml", refs[0].getUrl());

        // expect one resource (referral is not followed
        Resource[] res = repoAdmin.discoverResources(null);
        assertNotNull("Expect Resource", res);
        assertEquals("Expect one resource", 1, res.length);

        // first resource is from the referral1_repository.xml
        assertEquals("6", res[0].getId());
        assertEquals("referral1_repository", res[0].getRepository().getName());
    }

    private RepositoryAdminImpl createRepositoryAdmin()
    {
        final MockBundleContext bundleContext = new MockBundleContext();
        RepositoryAdminImpl repoAdmin = new RepositoryAdminImpl(bundleContext, new Logger(bundleContext));

        // force initialization && remove all initial repositories
        Repository[] repos = repoAdmin.listRepositories();
        for (int i = 0; repos != null && i < repos.length; i++)
        {
            repoAdmin.removeRepository(repos[i].getURL());
        }

        return repoAdmin;
    }

    private static class MockBundleContext implements BundleContext
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
}