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

import org.osgi.service.obr.Repository;
import org.osgi.service.obr.Resolver;
import org.osgi.service.obr.Resource;

public class ResolverImplTest extends TestCase
{
    public void testReferral1() throws Exception
    {

        URL url = getClass().getResource("/repo_for_resolvertest.xml");
        RepositoryAdminImpl repoAdmin = createRepositoryAdmin();
        RepositoryImpl repo = (RepositoryImpl) repoAdmin.addRepository(url);

        Resolver resolver = repoAdmin.resolver();

        Resource r = null;
        //MockContext doesn't support filtering!
        Resource[] discoverResources = repoAdmin.discoverResources("");
        for (int i = 0; i < discoverResources.length; i++) {
            Resource resource = discoverResources[i];
            if (resource.getSymbolicName().contains("org.apache.felix.test"))
            {
                r = resource;
            }
        }

        resolver.add(r);
        assertTrue(resolver.resolve());

    }

    public static void main(String[] args) throws Exception {

        new ResolverImplTest().testReferral1();

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

}