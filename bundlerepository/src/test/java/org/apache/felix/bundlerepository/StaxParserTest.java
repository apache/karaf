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
import java.net.URL;

import junit.framework.TestCase;
import org.junit.Ignore;
import org.junit.Test;
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
        assertNotNull(r);

        resolver.add(r);
        assertTrue(resolver.resolve());

    }

    public void testPerfs() throws Exception {
//        for (int i = 0; i < 10; i++) {
//            testPerfs(new File(System.getProperty("user.home"), ".m2/repository/repository.xml").toURI().toURL(), 0, 100);
//        }
    }

    protected void testPerfs(URL url, int nbWarm, int nbTest) throws Exception
    {
        long t0, t1;

        StaxParser.factory = null;
        System.setProperty("javax.xml.stream.XMLInputFactory", "com.ctc.wstx.stax.WstxInputFactory");
        for (int i = 0; i < nbWarm; i++) {
            RepositoryAdminImpl repoAdmin = createRepositoryAdmin(StaxParser.class);
            RepositoryImpl repo = (RepositoryImpl) repoAdmin.addRepository(url);
        }
        t0 = System.currentTimeMillis();
        for (int i = 0; i < nbTest; i++) {
            RepositoryAdminImpl repoAdmin = createRepositoryAdmin(StaxParser.class);
            RepositoryImpl repo = (RepositoryImpl) repoAdmin.addRepository(url);
        }
        t1 = System.currentTimeMillis();
        System.err.println("Woodstox: " + (t1 - t0) + " ms");


        StaxParser.factory = null;
        System.setProperty("javax.xml.stream.XMLInputFactory", "com.sun.xml.internal.stream.XMLInputFactoryImpl");
        for (int i = 0; i < nbWarm; i++) {
            RepositoryAdminImpl repoAdmin = createRepositoryAdmin(StaxParser.class);
            RepositoryImpl repo = (RepositoryImpl) repoAdmin.addRepository(url);
        }
        t0 = System.currentTimeMillis();
        for (int i = 0; i < nbTest; i++) {
            RepositoryAdminImpl repoAdmin = createRepositoryAdmin(StaxParser.class);
            RepositoryImpl repo = (RepositoryImpl) repoAdmin.addRepository(url);
        }
        t1 = System.currentTimeMillis();
        System.err.println("DefStax: " + (t1 - t0) + " ms");

        for (int i = 0; i < nbWarm; i++) {
            RepositoryAdminImpl repoAdmin = createRepositoryAdmin(RepositoryImpl.KXml2Parser.class);
            RepositoryImpl repo = (RepositoryImpl) repoAdmin.addRepository(url);
        }
        t0 = System.currentTimeMillis();
        for (int i = 0; i < nbTest; i++) {
            RepositoryAdminImpl repoAdmin = createRepositoryAdmin(RepositoryImpl.KXml2Parser.class);
            RepositoryImpl repo = (RepositoryImpl) repoAdmin.addRepository(url);
        }
        t1 = System.currentTimeMillis();
        System.err.println("KXmlParser: " + (t1 - t0) + " ms");


    }

    public static void main(String[] args) throws Exception {

        new StaxParserTest().testStaxParser();

    }

    private RepositoryAdminImpl createRepositoryAdmin(Class repositoryParser)
    {
        final MockBundleContext bundleContext = new MockBundleContext();
        bundleContext.setProperty(RepositoryAdminImpl.REPOSITORY_URL_PROP,
                                  getClass().getResource("/referral1_repository.xml").toExternalForm());
        bundleContext.setProperty(RepositoryImpl.OBR_PARSER_CLASS,
                                  repositoryParser.getName());
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