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
package org.apache.felix.bundlerepository.impl;

import java.net.URL;
import java.util.Hashtable;

import junit.framework.TestCase;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.utils.filter.FilterImpl;
import org.apache.felix.utils.log.Logger;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.internal.matchers.Captures;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceListener;

public class RepositoryAdminTest extends TestCase
{
    public void testResourceFilterOnCapabilities() throws Exception
    {
        URL url = getClass().getResource("/repo_for_resolvertest.xml");

        RepositoryAdminImpl repoAdmin = createRepositoryAdmin();
        RepositoryImpl repo = (RepositoryImpl) repoAdmin.addRepository(url);

        Resource[] resources = repoAdmin.discoverResources("(category<*dummy)");
        assertNotNull(resources);
        assertEquals(1, resources.length);

        resources = repoAdmin.discoverResources("(category*>dummy)");
        assertNotNull(resources);
        assertEquals(1, resources.length);
    }

    private RepositoryAdminImpl createRepositoryAdmin() throws Exception
    {
        BundleContext bundleContext = (BundleContext) EasyMock.createMock(BundleContext.class);
        Bundle systemBundle = (Bundle) EasyMock.createMock(Bundle.class);

        Activator.setContext(bundleContext);
        EasyMock.expect(bundleContext.getProperty((String) EasyMock.anyObject())).andReturn(null).anyTimes();
        EasyMock.expect(bundleContext.getBundle(0)).andReturn(systemBundle);
        EasyMock.expect(systemBundle.getHeaders()).andReturn(new Hashtable());
        EasyMock.expect(systemBundle.getRegisteredServices()).andReturn(null);
        EasyMock.expect(new Long(systemBundle.getBundleId())).andReturn(new Long(0)).anyTimes();
        EasyMock.expect(systemBundle.getBundleContext()).andReturn(bundleContext);
        bundleContext.addBundleListener((BundleListener) EasyMock.anyObject());
        bundleContext.addServiceListener((ServiceListener) EasyMock.anyObject());
        EasyMock.expect(bundleContext.getBundles()).andReturn(new Bundle[] { systemBundle });
        final Capture c = new Capture();
        EasyMock.expect(bundleContext.createFilter((String) capture(c))).andAnswer(new IAnswer() {
            public Object answer() throws Throwable {
                return FilterImpl.newInstance((String) c.getValue());
            }
        }).anyTimes();
        EasyMock.replay(new Object[] { bundleContext, systemBundle });

        RepositoryAdminImpl repoAdmin = new RepositoryAdminImpl(bundleContext, new Logger(bundleContext));

        // force initialization && remove all initial repositories
        org.apache.felix.bundlerepository.Repository[] repos = repoAdmin.listRepositories();
        for (int i = 0; repos != null && i < repos.length; i++)
        {
            repoAdmin.removeRepository(repos[i].getURI());
        }

        return repoAdmin;
    }

    static Object capture(Capture capture) {
        EasyMock.reportMatcher(new Captures(capture));
        return null;
    }

}
