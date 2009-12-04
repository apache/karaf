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

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * Test cases for {@link org.apache.felix.karaf.shell.dev.util.Bundles}
 */
public class BundlesTest {

    @Test
    public void testToString() {
        assertEquals("UNINSTALLED", Bundles.toString(Bundle.UNINSTALLED));
        assertEquals("INSTALLED", Bundles.toString(Bundle.INSTALLED));
        assertEquals("RESOLVED", Bundles.toString(Bundle.RESOLVED));
        assertEquals("STARTING", Bundles.toString(Bundle.STARTING));
        assertEquals("STOPPING", Bundles.toString(Bundle.STOPPING));
        assertEquals("ACTIVE", Bundles.toString(Bundle.ACTIVE));
    }
}
