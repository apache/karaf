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
package org.apache.felix.karaf.shell.dev.util;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.felix.karaf.commons.osgi.VersionRange;
import org.junit.Test;

/**
 * Test cases for {@link org.apache.felix.karaf.shell.dev.util.Import}
 */
public class ImportTest {

    @Test
    public void createWithPackageName() {
        Import i = new Import("org.wip.foo");
        assertEquals("org.wip.foo", i.getPackage());
    }

    @Test
    public void createWithPackageNameAndVersion() {
        Import i = new Import("org.wip.bar;version=\"2.0.0\"");
        assertEquals("org.wip.bar", i.getPackage());
        assertEquals(VersionRange.parse("2.0.0"), i.getVersion());
    }

    @Test
    public void createListOfImports() {
        List<Import> imports = Import.parse("org.wip.bar;version=\"2.0.0\",org.wip.foo");
        assertNotNull(imports);
        assertEquals(2, imports.size());
        assertEquals("org.wip.bar", imports.get(0).getPackage());
        assertEquals("org.wip.foo", imports.get(1).getPackage());
    }

    @Test
    public void createListOfImportsWithVersionRanges() {
        List<Import> imports =
                Import.parse("javax.activation;version=\"[0.0,2)\",javax.annotation;version=\"[0.0,2)\"");
        assertNotNull(imports);
        assertEquals(2, imports.size());
        assertEquals("javax.activation", imports.get(0).getPackage());
        assertEquals("javax.annotation", imports.get(1).getPackage());
    }

    @Test
    public void createListOfImportsWithExports() {
        List<Import> imports = Import.parse("org.wip.bar;version=\"2.0.0\",org.wip.foo", "org.wip.bar;version=\"2.0.0\"");
        assertNotNull(imports);
        assertEquals(1, imports.size());
        assertEquals("org.wip.foo", imports.get(0).getPackage());
    }
}
