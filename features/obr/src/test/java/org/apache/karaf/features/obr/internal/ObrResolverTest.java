/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.features.obr.internal;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.bundlerepository.impl.DataModelHelperImpl;
import org.apache.felix.bundlerepository.impl.ReasonImpl;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.internal.FeatureImpl;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Test;

public class ObrResolverTest {

    @Test
    public void testResolver() throws Exception {
        final String requirement = "bundle:(&(symbolicname=org.apache.camel.camel-blueprint)(version>=2.4.0)(version<2.4.1))";

        final FeatureImpl f = new FeatureImpl("f1", "1.0");
        f.setResolver("obr");
        f.addBundle(new BundleInfoImpl(requirement));
        final RepositoryAdmin admin = createMock(RepositoryAdmin.class);
        final Resolver resolver = createMock(Resolver.class);
        final Resource resource = createMock(Resource.class);
        final ObrResolver obrResolver = new ObrResolver();
        obrResolver.setRepositoryAdmin(admin);

        final Capture<Requirement> captureReq = new Capture<Requirement>();

        expect(admin.getHelper()).andReturn(new DataModelHelperImpl()).anyTimes();
        expect(admin.getSystemRepository()).andReturn(createMock(org.apache.felix.bundlerepository.Repository.class));
        expect(admin.getLocalRepository()).andReturn(createMock(org.apache.felix.bundlerepository.Repository.class));
        expect(admin.listRepositories()).andReturn(new org.apache.felix.bundlerepository.Repository[0]);
        expect(admin.resolver(EasyMock.<org.apache.felix.bundlerepository.Repository[]>anyObject())).andReturn(resolver);
        resolver.add(EasyMock.capture(captureReq));
        expect(resolver.resolve(Resolver.NO_OPTIONAL_RESOURCES)).andReturn(true);
        expect(resolver.getAddedResources()).andReturn(new Resource[] { });
        expect(resolver.getRequiredResources()).andReturn(new Resource[] { resource });
        expect(resolver.getReason(resource)).andAnswer(new IAnswer<Reason[]>() {
            public Reason[] answer() throws Throwable {
                return new Reason[] { new ReasonImpl( resource, captureReq.getValue()) };
            }
        });
        expect(resource.getURI()).andReturn("foo:bar");
        replay(admin, resolver, resource);

        List<BundleInfo> bundles = obrResolver.resolve(f);
        assertNotNull(bundles);
        assertEquals(1, bundles.size());
        assertEquals("foo:bar", bundles.get(0).getLocation());
        assertEquals(obrResolver.parseRequirement(requirement).toString(), captureReq.getValue().toString());
        verify(admin, resolver, resource);
    }

    @Test
    public void testResolverWithOptionalImports() throws Exception {
        final String requirement = "bundle:(&(symbolicname=org.apache.camel.camel-blueprint)(version>=2.4.0)(version<2.4.1))";

        final FeatureImpl f = new FeatureImpl("f1", "1.0");
        f.setResolver("obr");
        f.getBundles().add(new BundleInfoImpl(requirement));
        final RepositoryAdmin admin = createMock(RepositoryAdmin.class);
        final Resolver resolver = createMock(Resolver.class);
        final Resource resource = createMock(Resource.class);
        final Resource optionalResource = createMock(Resource.class);
        final ObrResolver obrResolver = new ObrResolver();
        obrResolver.setRepositoryAdmin(admin);
        obrResolver.setResolveOptionalImports(true);

        final Capture<Requirement> captureReq = new Capture<Requirement>();

        expect(admin.getHelper()).andReturn(new DataModelHelperImpl()).anyTimes();
        expect(admin.getSystemRepository()).andReturn(createMock(org.apache.felix.bundlerepository.Repository.class));
        expect(admin.getLocalRepository()).andReturn(createMock(org.apache.felix.bundlerepository.Repository.class));
        expect(admin.listRepositories()).andReturn(new org.apache.felix.bundlerepository.Repository[0]);
        expect(admin.resolver(EasyMock.<org.apache.felix.bundlerepository.Repository[]>anyObject())).andReturn(resolver);
        resolver.add(EasyMock.capture(captureReq));
        expect(resolver.resolve()).andReturn(true);
        expect(resolver.getAddedResources()).andReturn(new Resource[] { });
        expect(resolver.getRequiredResources()).andReturn(new Resource[] { resource });
        expect(resolver.getOptionalResources()).andReturn(new Resource[] { optionalResource});
        expect(resolver.getReason(resource)).andAnswer(new IAnswer<Reason[]>() {
            public Reason[] answer() throws Throwable {
                return new Reason[] { new ReasonImpl( resource, captureReq.getValue()) };
            }
        });
        expect(resolver.getReason(optionalResource)).andAnswer(new IAnswer<Reason[]>() {
            public Reason[] answer() throws Throwable {
                return new Reason[] { new ReasonImpl( optionalResource, captureReq.getValue()) };
            }
        });
        expect(resource.getURI()).andReturn("foo:bar");
        expect(optionalResource.getURI()).andReturn("foo:optional:baz");
        replay(admin, resolver, resource, optionalResource);

        List<BundleInfo> bundles = obrResolver.resolve(f);
        assertNotNull(bundles);
        assertEquals(2, bundles.size());
        assertEquals("foo:bar", bundles.get(0).getLocation());
        assertEquals("foo:optional:baz", bundles.get(1).getLocation());
        assertEquals(obrResolver.parseRequirement(requirement).toString(), captureReq.getValue().toString());
        verify(admin, resolver, resource);
    }
    
    /**
     * Test resolving a mvn url when pax url is configured with a repo that contains no protocol like: "test"
     * We expect to get a MalFormedUrlException not a IllegalArgumentException as in the issue 
     * @throws Exception
     */
    @Test(expected=MalformedURLException.class)
    public void testResolverWithInvalidMvnRepoIssueKaraf667() throws Exception {
        final FeatureImpl f = new FeatureImpl("f1", "1.0");
        f.setResolver("obr");
        // Using file instead of mvn: as we do not want to mess with URL handlers
        f.addBundle(new BundleInfoImpl("file:org.foo/foo/1.0"));

        final RepositoryAdmin admin = createMock(RepositoryAdmin.class);
        final Resolver resolver = createMock(Resolver.class);
        final Resource resource = createMock(Resource.class);
        final ObrResolver obrResolver = new ObrResolver();
        obrResolver.setRepositoryAdmin(admin);

        expect(admin.getHelper()).andReturn(new DummyDataModelHelper()).anyTimes();
        replay(admin, resolver, resource);

       	obrResolver.resolve(f);
    }
    
    /**
     * Recreates behaviour of DataModelHelper when an invalid maven url is set for pax url repos
     * TODO Remove as soon as DataModelHelper does not throw this exception any more  
     */
    class DummyDataModelHelper extends DataModelHelperImpl {

		@Override
		public Resource createResource(URL bundleUrl) throws IOException {
			throw new MalformedURLException("Missing protocol");
		}

    	
    }
}
