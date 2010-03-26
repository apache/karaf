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

import java.util.jar.Attributes;

import junit.framework.TestCase;
import org.apache.felix.bundlerepository.DataModelHelper;
import org.apache.felix.bundlerepository.Resource;

public class DataModelHelperTest extends TestCase
{

    private DataModelHelper dmh = new DataModelHelperImpl();

    public void testResource() throws Exception
    {
        Attributes attr = new Attributes();
        attr.putValue("Manifest-Version", "1.0");
        attr.putValue("Bundle-Name", "Apache Felix Utils");
        attr.putValue("Bundle-Version", "0.1.0.SNAPSHOT");
        attr.putValue("Bundle-ManifestVersion", "2");
        attr.putValue("Bundle-License", "http://www.apache.org/licenses/LICENSE-2.0.txt");
        attr.putValue("Bundle-Description", "Utility classes for OSGi.");
        attr.putValue("Import-Package", "org.osgi.framework;version=\"[1.4,2)\"");
        attr.putValue("Bundle-SymbolicName", "org.apache.felix.utils");

        Resource resource = dmh.createResource(attr);

        String xml = dmh.writeResource(resource);
        System.out.println(xml);

        Resource resource2 = dmh.readResource(xml);
        String xml2 = dmh.writeResource(resource2);
        System.out.println(xml2);

        assertEquals(xml, xml2);
    }

    public void testRequirementFilter() throws Exception
    {
        RequirementImpl r = new RequirementImpl();
        r.setFilter("(&(package=foo.bar)(version>=0.0.0)(version<3.0.0))");
        assertEquals("(&(package=foo.bar)(!(version>=3.0.0)))", r.getFilter());

        r.setFilter("(&(package=javax.transaction)(partial=true)(mandatory:<*partial))");
        assertEquals("(&(package=javax.transaction)(partial=true)(mandatory:<*partial))", r.getFilter());
    }
}
