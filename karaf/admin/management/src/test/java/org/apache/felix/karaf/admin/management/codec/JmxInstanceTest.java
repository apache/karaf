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
package org.apache.felix.karaf.admin.management.codec;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularType;

import junit.framework.TestCase;
import org.apache.felix.karaf.admin.Instance;
import org.apache.felix.karaf.admin.management.AdminServiceMBean;
import org.easymock.EasyMock;
import org.junit.Assert;

public class JmxInstanceTest extends TestCase {
    public void testJMXInstanceStatics() {
        CompositeType it = JmxInstance.INSTANCE;
        Assert.assertEquals(
            new HashSet<String>(Arrays.asList(AdminServiceMBean.INSTANCE)),
            it.keySet());

        TabularType tt = JmxInstance.INSTANCE_TABLE;
        Assert.assertEquals("Instances", tt.getTypeName());
    }

    public void testJMXInstance() throws Exception {
        Instance i = EasyMock.createMock(Instance.class);
        EasyMock.expect(i.getPid()).andReturn(1712);
        EasyMock.expect(i.getName()).andReturn("MyInstance");
        EasyMock.expect(i.isRoot()).andReturn(false);
        EasyMock.expect(i.getPort()).andReturn(0);
        EasyMock.expect(i.getState()).andThrow(new Exception("gotcha"));
        EasyMock.expect(i.getLocation()).andReturn("somewhere");
        EasyMock.replay(i);
        
        JmxInstance ji = new JmxInstance(i);
        TabularData td = JmxInstance.tableFrom(Collections.singletonList(ji));        
        Collection<?> keys = (Collection<?>) td.keySet().iterator().next();
        Assert.assertEquals("MyInstance", keys.iterator().next());
        
        CompositeData cd = td.get(keys.toArray());
        Assert.assertEquals(1712, cd.get("Pid"));
        Assert.assertEquals("MyInstance", cd.get("Name"));
        Assert.assertEquals(false, cd.get("Is Root"));
        Assert.assertEquals(0, cd.get("Port"));
        Assert.assertEquals("Error", cd.get("State"));
        Assert.assertEquals("somewhere", cd.get("Location"));
    }

    public void testJMXInstance2() throws Exception {
        Instance i = EasyMock.createMock(Instance.class);
        EasyMock.expect(i.getPid()).andReturn(1712);
        EasyMock.expect(i.getName()).andReturn("MyInstance");
        EasyMock.expect(i.isRoot()).andReturn(true);
        EasyMock.expect(i.getPort()).andReturn(0);
        EasyMock.expect(i.getState()).andReturn("Started");
        EasyMock.expect(i.getLocation()).andReturn(null);
        EasyMock.replay(i);
        
        JmxInstance ji = new JmxInstance(i);
        TabularData td = JmxInstance.tableFrom(Collections.singletonList(ji));        
        Collection<?> keys = (Collection<?>) td.keySet().iterator().next();
        Assert.assertEquals("MyInstance", keys.iterator().next());
        
        CompositeData cd = td.get(keys.toArray());
        Assert.assertEquals(1712, cd.get("Pid"));
        Assert.assertEquals("MyInstance", cd.get("Name"));
        Assert.assertEquals(true, cd.get("Is Root"));
        Assert.assertEquals(0, cd.get("Port"));
        Assert.assertEquals("Started", cd.get("State"));
        Assert.assertNull(cd.get("Location"));
    }
}
