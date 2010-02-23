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
import java.util.Hashtable;

import junit.framework.TestCase;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.internal.matchers.Captures;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceListener;
import org.osgi.service.obr.Repository;
import org.osgi.service.obr.Resolver;
import org.osgi.service.obr.Resource;

public class StaxParserTest extends TestCase
{
    public void testStaxParser() throws Exception
    {
        URL url = getClass().getResource("/repo_for_resolvertest.xml");
        RepositoryAdminImpl repoAdmin = createRepositoryAdmin(StaxParser.class);
        RepositoryImpl repo = (RepositoryImpl) repoAdmin.addRepository(url);

        Resolver resolver = repoAdmin.resolver();

        Resource[] discoverResources = repoAdmin.discoverResources("(symbolicname=org.apache.felix.test*)");
        assertNotNull(discoverResources);
        assertEquals(1, discoverResources.length);

        resolver.add(discoverResources[0]);
        assertTrue(resolver.resolve());
    }

    public void testPerfs() throws Exception
    {
//        for (int i = 0; i < 10; i++) {
//            testPerfs(new File(System.getProperty("user.home"), ".m2/repository/repository.xml").toURI().toURL(), 0, 100);
//        }
    }

    protected void testPerfs(URL url, int nbWarm, int nbTest) throws Exception
    {
        long t0, t1;

        StaxParser.factory = null;
        System.setProperty("javax.xml.stream.XMLInputFactory", "com.ctc.wstx.stax.WstxInputFactory");
        for (int i = 0; i < nbWarm; i++)
        {
            RepositoryAdminImpl repoAdmin = createRepositoryAdmin(StaxParser.class);
            RepositoryImpl repo = (RepositoryImpl) repoAdmin.addRepository(url);
        }
        t0 = System.currentTimeMillis();
        for (int i = 0; i < nbTest; i++)
        {
            RepositoryAdminImpl repoAdmin = createRepositoryAdmin(StaxParser.class);
            RepositoryImpl repo = (RepositoryImpl) repoAdmin.addRepository(url);
        }
        t1 = System.currentTimeMillis();
        System.err.println("Woodstox: " + (t1 - t0) + " ms");


        StaxParser.factory = null;
        System.setProperty("javax.xml.stream.XMLInputFactory", "com.sun.xml.internal.stream.XMLInputFactoryImpl");
        for (int i = 0; i < nbWarm; i++)
        {
            RepositoryAdminImpl repoAdmin = createRepositoryAdmin(StaxParser.class);
            RepositoryImpl repo = (RepositoryImpl) repoAdmin.addRepository(url);
        }
        t0 = System.currentTimeMillis();
        for (int i = 0; i < nbTest; i++)
        {
            RepositoryAdminImpl repoAdmin = createRepositoryAdmin(StaxParser.class);
            RepositoryImpl repo = (RepositoryImpl) repoAdmin.addRepository(url);
        }
        t1 = System.currentTimeMillis();
        System.err.println("DefStax: " + (t1 - t0) + " ms");

        for (int i = 0; i < nbWarm; i++)
        {
            RepositoryAdminImpl repoAdmin = createRepositoryAdmin(RepositoryImpl.KXml2Parser.class);
            RepositoryImpl repo = (RepositoryImpl) repoAdmin.addRepository(url);
        }
        t0 = System.currentTimeMillis();
        for (int i = 0; i < nbTest; i++)
        {
            RepositoryAdminImpl repoAdmin = createRepositoryAdmin(RepositoryImpl.KXml2Parser.class);
            RepositoryImpl repo = (RepositoryImpl) repoAdmin.addRepository(url);
        }
        t1 = System.currentTimeMillis();
        System.err.println("KXmlParser: " + (t1 - t0) + " ms");
    }

    public static void main(String[] args) throws Exception
    {
        new StaxParserTest().testStaxParser();
    }

    private RepositoryAdminImpl createRepositoryAdmin(Class repositoryParser) throws Exception
    {
        BundleContext bundleContext = (BundleContext) EasyMock.createMock(BundleContext.class);
        Bundle systemBundle = (Bundle) EasyMock.createMock(Bundle.class);

        EasyMock.expect(bundleContext.getProperty(RepositoryAdminImpl.REPOSITORY_URL_PROP))
                    .andReturn(getClass().getResource("/referral1_repository.xml").toExternalForm());
        EasyMock.expect(bundleContext.getProperty(RepositoryImpl.OBR_PARSER_CLASS))
                    .andReturn(repositoryParser.getName());
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
                return new FilterImpl((String) c.getValue());
            }
        }).anyTimes();
        EasyMock.replay(new Object[] { bundleContext, systemBundle });

        RepositoryAdminImpl repoAdmin = new RepositoryAdminImpl(bundleContext, new Logger(bundleContext));

        // force initialization && remove all initial repositories
        Repository[] repos = repoAdmin.listRepositories();
        for (int i = 0; repos != null && i < repos.length; i++)
        {
            repoAdmin.removeRepository(repos[i].getURL());
        }

        return repoAdmin;
    }

    static Object capture(Capture capture) {
        EasyMock.reportMatcher(new Captures(capture));
        return null;
    }

}