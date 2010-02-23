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

import java.net.URL;

import junit.framework.TestCase;
import org.osgi.framework.Filter;
import org.osgi.service.obr.Repository;
import org.osgi.service.obr.Resource;

public class RepositoryAdminTest extends TestCase
{
    public void testResourceFilterOnCapabilities() throws Exception
    {
        URL url = getClass().getResource("/repo_for_resolvertest.xml");

        RepositoryAdminImpl repoAdmin = createRepositoryAdmin();
        RepositoryImpl repo = (RepositoryImpl) repoAdmin.addRepository(url);

        Resource[] resources = repoAdmin.discoverResources("(category:<*dummy)");
        assertNotNull(resources);
        assertEquals(1, resources.length);

        resources = repoAdmin.discoverResources("(category:*>dummy)");
        assertNotNull(resources);
        assertEquals(1, resources.length);
    }

    private RepositoryAdminImpl createRepositoryAdmin()
    {
        final MockBundleContext bundleContext = new MockBundleContext() {
            public Filter createFilter(String arg0) {
                return new FilterImpl(arg0);
            }
        };
        RepositoryAdminImpl repoAdmin = new RepositoryAdminImpl(bundleContext, new Logger(bundleContext));

        // force initialization && remove all initial repositories
        Repository[] repos = repoAdmin.listRepositories();
        for (int i = 0; repos != null && i < repos.length; i++)
        {
            repoAdmin.removeRepository(repos[i].getURL());
        }

        return repoAdmin;
    }
}