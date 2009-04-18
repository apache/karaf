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
 * Test about {@link ExportedService}.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ExportedServiceTest extends TestCase {

    //
    // <comp:composite name="composite.export.5" architecture="true">
    // <subservice action="import"
    // specification="org.apache.felix.ipojo.test.composite.service.BazService"
    // aggregate="true" optional="true" filter="(!(instance.name=export))"
    // scope="composite" />
    // <comp:provides action="export"
    // specification="org.apache.felix.ipojo.test.composite.service.BazService"
    // filter="(instance.name=foo1)" />
    // </comp:composite>

    // <comp:provides action="export"
    // specification="org.apache.felix.ipojo.test.composite.service.BazService"
    // />
    /**
     * Tests simple export.
     */
    public void testSimple() {
        ExportedService svc = new ExportedService()
                .setSpecification("org.apache.felix.ipojo.test.composite.service.BazService");

        Element elem = svc.getElement();
        String spec = elem.getAttribute("specification");
        assertEquals("spec",
                "org.apache.felix.ipojo.test.composite.service.BazService",
                spec);
    }

    /**
     * Tests a malformed export.
     */
    public void testBad() {
        ExportedService svc = new ExportedService()
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

    // <comp:provides action="export"
    // specification="org.apache.felix.ipojo.test.composite.service.BazService"
    // aggregate="true" />
    /**
     * Tests aggregate export.
     */
    public void testAggregate() {
        ExportedService svc = new ExportedService().setSpecification(
                "org.apache.felix.ipojo.test.composite.service.BazService")
                .setAggregate(true);

        Element elem = svc.getElement();
        String spec = elem.getAttribute("specification");
        String agg = elem.getAttribute("aggregate");

        assertEquals("spec",
                "org.apache.felix.ipojo.test.composite.service.BazService",
                spec);
        assertEquals("aggregate", "true", agg);
    }

    // <comp:provides action="export"
    // specification="org.apache.felix.ipojo.test.composite.service.BazService"
    // optional="true" />
    /**
     * Tests optional export.
     */
    public void testOptional() {
        ExportedService svc = new ExportedService().setSpecification(
                "org.apache.felix.ipojo.test.composite.service.BazService")
                .setOptional(true);

        Element elem = svc.getElement();
        String spec = elem.getAttribute("specification");
        String opt = elem.getAttribute("optional");

        assertEquals("spec",
                "org.apache.felix.ipojo.test.composite.service.BazService",
                spec);
        assertEquals("optional", "true", opt);
    }

    // <comp:provides action="export"
    // specification="org.apache.felix.ipojo.test.composite.service.BazService"
    // aggregate="true" optional="true" />
    /**
     * Tests optional and aggregate export.
     */
    public void testOptionalAndAggregate() {
        ExportedService svc = new ExportedService().setSpecification(
                "org.apache.felix.ipojo.test.composite.service.FooService")
                .setOptional(true).setAggregate(true);

        Element elem = svc.getElement();
        String spec = elem.getAttribute("specification");
        String opt = elem.getAttribute("optional");
        String agg = elem.getAttribute("aggregate");

        assertEquals("spec",
                "org.apache.felix.ipojo.test.composite.service.FooService",
                spec);
        assertEquals("optional", "true", opt);
        assertEquals("aggregate", "true", agg);
    }

    // <comp:provides action="export"
    // specification="org.apache.felix.ipojo.test.composite.service.BazService"
    // filter="(instance.name=foo1)" />
    /**
     * Tests filtered export.
     */
    public void testFilter() {
        ExportedService svc = new ExportedService().setSpecification(
                "org.apache.felix.ipojo.test.composite.service.FooService")
                .setFilter("(&(int=2)(long=40))");

        Element elem = svc.getElement();
        String spec = elem.getAttribute("specification");
        String filter = elem.getAttribute("filter");

        assertEquals("spec",
                "org.apache.felix.ipojo.test.composite.service.FooService",
                spec);
        assertEquals("filter", "(&(int=2)(long=40))", filter);
    }

}
