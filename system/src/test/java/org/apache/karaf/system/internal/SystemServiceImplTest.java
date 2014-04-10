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
package org.apache.karaf.system.internal;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.felix.utils.properties.Properties;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.BundleContext;

/**
 * Implementation of the system service.
 */

public class SystemServiceImplTest {

    private static final String NEW_NAME = "newName";

    @Test
    public void testSetName() throws URISyntaxException, IOException {
        SystemServiceImpl system = new SystemServiceImpl();
        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        URL propUrl = this.getClass().getClassLoader().getResource("etc/system.properties");
        File propfile = new File(propUrl.toURI());        
        EasyMock.expect(bundleContext.getProperty("karaf.etc")).andReturn(propfile.getParentFile().getParent() + "/etc");
        EasyMock.replay(bundleContext);
        system.setBundleContext(bundleContext);
        system.setName(NEW_NAME);
        EasyMock.verify(bundleContext);
        Properties props = new Properties(propfile);
        String nameAfter = props.getProperty("karaf.name");
        Assert.assertEquals(NEW_NAME, nameAfter);
    }
}
