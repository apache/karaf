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
package org.apache.felix.ipojo.api.composite;

import junit.framework.TestCase;

import org.apache.felix.ipojo.metadata.Element;

/**
 * Tests about {@link ImportedService}.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ImportedServiceTest extends TestCase {

    // <comp:composite name="composite.requires.1" architecture="true">
    // <subservice action="import"
    // specification="org.apache.felix.ipojo.test.composite.service.FooService"
    // scope="composite" />
    // </comp:composite>
    /**
     * Simple test.
     */
    public void testSimple() {
        ImportedService svc = new ImportedService()
                .setSpecification("org.apache.felix.ipojo.test.composite.service.FooService");

        Element elem = svc.getElement();
        String spec = elem.getAttribute("specification");
        assertEquals("spec",
                "org.apache.felix.ipojo.test.composite.service.FooService",
                spec);
    }

    /**
     * Malformed import.
     */
    public void testBad() {
        ImportedService svc = new ImportedService()
        // .setSpecification("org.apache.felix.ipojo.test.composite.service.BarService")
        // NO SPEC
        ;
        try {
            svc.getElement();
            fail("Invalid element accepted");
        } catch (IllegalStateException e) {
            // OK
        }
    }

    /**
     * Tests scopes.
     */
    public void testScope() {
        ImportedService svc = new ImportedService().setSpecification(
                "org.apache.felix.ipojo.test.composite.service.FooService")
                .setScope(ImportedService.COMPOSITE_SCOPE);

        Element elem = svc.getElement();
        String spec = elem.getAttribute("specification");
        String scope = elem.getAttribute("scope");
        assertEquals("spec",
                "org.apache.felix.ipojo.test.composite.service.FooService",
                spec);
        assertEquals("scope", "composite", scope);

    }

    //
    // <comp:composite name="composite.requires.2" architecture="true">
    // <subservice action="import"
    // specification="org.apache.felix.ipojo.test.composite.service.FooService"
    // aggregate="true" scope="composite" />
    // </comp:composite>
    /**
     * Tests aggregate.
     */
    public void testAggregate() {
        ImportedService svc = new ImportedService().setSpecification(
                "org.apache.felix.ipojo.test.composite.service.FooService")
                .setScope(ImportedService.COMPOSITE_SCOPE).setAggregate(true);

        Element elem = svc.getElement();
        String spec = elem.getAttribute("specification");
        String scope = elem.getAttribute("scope");
        String agg = elem.getAttribute("aggregate");

        assertEquals("spec",
                "org.apache.felix.ipojo.test.composite.service.FooService",
                spec);
        assertEquals("scope", "composite", scope);
        assertEquals("aggregate", "true", agg);
    }

    //
    // <comp:composite name="composite.requires.3" architecture="true">
    // <subservice action="import"
    // specification="org.apache.felix.ipojo.test.composite.service.FooService"
    // optional="true" scope="composite" />
    // </comp:composite>
    /**
     * Tests optional.
     */
    public void testOptional() {
        ImportedService svc = new ImportedService().setSpecification(
                "org.apache.felix.ipojo.test.composite.service.FooService")
                .setScope(ImportedService.COMPOSITE_SCOPE).setOptional(true);

        Element elem = svc.getElement();
        String spec = elem.getAttribute("specification");
        String scope = elem.getAttribute("scope");
        String opt = elem.getAttribute("optional");

        assertEquals("spec",
                "org.apache.felix.ipojo.test.composite.service.FooService",
                spec);
        assertEquals("scope", "composite", scope);
        assertEquals("optional", "true", opt);
    }

    //
    // <comp:composite name="composite.requires.4" architecture="true">
    // <subservice action="import"
    // specification="org.apache.felix.ipojo.test.composite.service.FooService"
    // optional="true" aggregate="true" scope="comp:composite" />
    // </comp:composite>
    /**
     * Tests optional and aggregate.
     */
    public void testOptionalAndAggregate() {
        ImportedService svc = new ImportedService().setSpecification(
                "org.apache.felix.ipojo.test.composite.service.FooService")
                .setScope(ImportedService.COMPOSITE_SCOPE).setOptional(true)
                .setAggregate(true);

        Element elem = svc.getElement();
        String spec = elem.getAttribute("specification");
        String scope = elem.getAttribute("scope");
        String opt = elem.getAttribute("optional");
        String agg = elem.getAttribute("aggregate");

        assertEquals("spec",
                "org.apache.felix.ipojo.test.composite.service.FooService",
                spec);
        assertEquals("scope", "composite", scope);
        assertEquals("optional", "true", opt);
        assertEquals("aggregate", "true", agg);
    }

    //
    // <comp:composite name="composite.requires.5" architecture="true">
    // <subservice action="import"
    // specification="org.apache.felix.ipojo.test.composite.service.FooService"
    // filter="(&amp;(int=2)(long=40))" scope="composite" />
    // </comp:composite>
    /**
     * Tests filter.
     */
    public void testFilter() {
        ImportedService svc = new ImportedService().setSpecification(
                "org.apache.felix.ipojo.test.composite.service.FooService")
                .setScope(ImportedService.COMPOSITE_SCOPE).setFilter(
                        "(&(int=2)(long=40))");

        Element elem = svc.getElement();
        String spec = elem.getAttribute("specification");
        String scope = elem.getAttribute("scope");
        String filter = elem.getAttribute("filter");

        assertEquals("spec",
                "org.apache.felix.ipojo.test.composite.service.FooService",
                spec);
        assertEquals("scope", "composite", scope);
        assertEquals("filter", "(&(int=2)(long=40))", filter);
    }

}
