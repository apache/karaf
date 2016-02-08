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
package org.apache.karaf.instance.core.management.internal;

import java.util.Collection;
import java.util.Collections;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import junit.framework.TestCase;

import org.apache.karaf.instance.core.Instance;
import org.apache.karaf.instance.core.internal.InstanceToTableMapper;
import org.easymock.EasyMock;
import org.junit.Assert;

public class InstanceToTableMapperTest extends TestCase {
    public void testJMXInstance() throws Exception {
        Instance instance = EasyMock.createMock(Instance.class);
        EasyMock.expect(instance.getPid()).andReturn(1712);
        EasyMock.expect(instance.getName()).andReturn("MyInstance");
        EasyMock.expect(instance.isRoot()).andReturn(false);
        EasyMock.expect(instance.getSshPort()).andReturn(0);
        EasyMock.expect(instance.getSshHost()).andReturn("0.0.0.0");
        EasyMock.expect(instance.getRmiRegistryPort()).andReturn(0);
        EasyMock.expect(instance.getRmiRegistryHost()).andReturn("0.0.0.0");
        EasyMock.expect(instance.getRmiServerPort()).andReturn(0);
        EasyMock.expect(instance.getRmiServerHost()).andReturn("0.0.0.0");
        EasyMock.expect(instance.getState()).andThrow(new Exception("gotcha"));
        EasyMock.expect(instance.getLocation()).andReturn("somewhere");
        EasyMock.expect(instance.getJavaOpts()).andReturn("someopts");
        EasyMock.replay(instance);
        
        TabularData td = InstanceToTableMapper.tableFrom(Collections.singletonList(instance));
        Collection<?> keys = (Collection<?>) td.keySet().iterator().next();
        Assert.assertEquals("MyInstance", keys.iterator().next());
        
        CompositeData cd = td.get(keys.toArray());
        Assert.assertEquals(1712, cd.get("Pid"));
        Assert.assertEquals("MyInstance", cd.get("Name"));
        Assert.assertEquals(false, cd.get("Is Root"));
        Assert.assertEquals(0, cd.get("SSH Port"));
        Assert.assertEquals("0.0.0.0", cd.get("SSH Host"));
        Assert.assertEquals(0, cd.get("RMI Registry Port"));
        Assert.assertEquals("0.0.0.0", cd.get("RMI Registry Host"));
        Assert.assertEquals(0, cd.get("RMI Server Port"));
        Assert.assertEquals("0.0.0.0", cd.get("RMI Server Host"));
        Assert.assertEquals("Error", cd.get("State"));
        Assert.assertEquals("somewhere", cd.get("Location"));
        Assert.assertEquals("someopts", cd.get("JavaOpts"));
    }

    public void testJMXInstance2() throws Exception {
        Instance instance = EasyMock.createMock(Instance.class);
        EasyMock.expect(instance.getPid()).andReturn(1712);
        EasyMock.expect(instance.getName()).andReturn("MyInstance");
        EasyMock.expect(instance.isRoot()).andReturn(true);
        EasyMock.expect(instance.getSshPort()).andReturn(0);
        EasyMock.expect(instance.getSshHost()).andReturn("0.0.0.0");
        EasyMock.expect(instance.getRmiRegistryPort()).andReturn(0);
        EasyMock.expect(instance.getRmiRegistryHost()).andReturn("0.0.0.0");
        EasyMock.expect(instance.getRmiServerPort()).andReturn(0);
        EasyMock.expect(instance.getRmiServerHost()).andReturn("0.0.0.0");
        EasyMock.expect(instance.getState()).andReturn("Started");
        EasyMock.expect(instance.getLocation()).andReturn(null);
        EasyMock.expect(instance.getJavaOpts()).andReturn(null);
        EasyMock.replay(instance);
        
        TabularData td = InstanceToTableMapper.tableFrom(Collections.singletonList(instance));        
        Collection<?> keys = (Collection<?>) td.keySet().iterator().next();
        Assert.assertEquals("MyInstance", keys.iterator().next());
        
        CompositeData cd = td.get(keys.toArray());
        Assert.assertEquals(1712, cd.get("Pid"));
        Assert.assertEquals("MyInstance", cd.get("Name"));
        Assert.assertEquals(true, cd.get("Is Root"));
        Assert.assertEquals(0, cd.get("SSH Port"));
        Assert.assertEquals("0.0.0.0", cd.get("SSH Host"));
        Assert.assertEquals(0, cd.get("RMI Registry Port"));
        Assert.assertEquals("0.0.0.0", cd.get("RMI Registry Host"));
        Assert.assertEquals(0, cd.get("RMI Server Port"));
        Assert.assertEquals("0.0.0.0", cd.get("RMI Server Host"));
        Assert.assertEquals("Started", cd.get("State"));
        Assert.assertNull(cd.get("Location"));
        Assert.assertNull(cd.get("JavaOpts"));
    }
}
