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
package org.apache.felix.karaf.admin.management.internal;

import java.util.Arrays;
import java.util.Collections;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import junit.framework.TestCase;
import org.apache.felix.karaf.admin.AdminService;
import org.apache.felix.karaf.admin.Instance;
import org.apache.felix.karaf.admin.InstanceSettings;
import org.easymock.EasyMock;
import org.junit.Assert;

public class AdminServiceMBeanImplTest extends TestCase {
    public void testCreateInstance() throws Exception {
        final InstanceSettings is = new InstanceSettings(123, "somewhere",
                Collections.<String>emptyList(), Arrays.asList("webconsole", "funfeat"));
        
        final Instance inst = EasyMock.createMock(Instance.class);
        EasyMock.expect(inst.getPid()).andReturn(42);
        EasyMock.replay(inst);

        AdminService as = EasyMock.createMock(AdminService.class);
        EasyMock.expect(as.createInstance("t1", is)).andReturn(inst);
        EasyMock.replay(as);
        
        AdminServiceMBeanImpl ab = new AdminServiceMBeanImpl();
        ab.setAdminService(as);
        Assert.assertSame(as, ab.getAdminService());
        
        assertEquals(42, ab.createInstance("t1", 123, "somewhere", " webconsole,  funfeat", ""));
    }
    
    public void testCreateInstance2() throws Exception {
        final InstanceSettings is = new InstanceSettings(0, null, 
                Collections.<String>emptyList(), Collections.<String>emptyList());
        
        AdminService as = EasyMock.createMock(AdminService.class);
        EasyMock.expect(as.createInstance("t1", is)).andReturn(null);
        EasyMock.replay(as);
        
        AdminServiceMBeanImpl ab = new AdminServiceMBeanImpl();
        ab.setAdminService(as);
        Assert.assertSame(as, ab.getAdminService());
        
        assertEquals(-1, ab.createInstance("t1", 0, "", "", ""));
    }
    
    public void testGetInstances() throws Exception {       
        Instance i1 = EasyMock.createMock(Instance.class);
        EasyMock.expect(i1.getPid()).andReturn(1234);
        EasyMock.expect(i1.getPort()).andReturn(8818);
        EasyMock.expect(i1.getName()).andReturn("i1");
        EasyMock.expect(i1.isRoot()).andReturn(true);
        EasyMock.expect(i1.getLocation()).andReturn("somewhere");
        EasyMock.expect(i1.getState()).andReturn("Stopped");
        EasyMock.replay(i1);
        Instance i2 = EasyMock.createNiceMock(Instance.class);
        EasyMock.expect(i2.getName()).andReturn("i2");
        EasyMock.replay(i2);
        
        AdminService as = EasyMock.createMock(AdminService.class);
        EasyMock.expect(as.getInstances()).andReturn(new Instance [] {i1, i2});
        EasyMock.replay(as);

        AdminServiceMBeanImpl ab = new AdminServiceMBeanImpl();
        ab.setAdminService(as);
        
        TabularData td = ab.getInstances();
        Assert.assertEquals(2, td.size());
        CompositeData cd1 = td.get(new Object [] {"i1"});
        Assert.assertTrue(cd1.containsValue("i1"));
        Assert.assertTrue(cd1.containsValue(true));
        Assert.assertTrue(cd1.containsValue(1234));
        Assert.assertTrue(cd1.containsValue(8818));
        Assert.assertTrue(cd1.containsValue("somewhere"));
        Assert.assertTrue(cd1.containsValue("Stopped"));

        CompositeData cd2 = td.get(new Object [] {"i2"});
        Assert.assertTrue(cd2.containsValue("i2"));
    }
    
    public void testStartInstance() throws Exception {
        Instance inst = EasyMock.createMock(Instance.class);
        inst.start("-x -y -z");
        EasyMock.expectLastCall();
        EasyMock.replay(inst);

        AdminService as = EasyMock.createMock(AdminService.class);
        EasyMock.expect(as.getInstance("test instance")).andReturn(inst);
        EasyMock.replay(as);
        
        AdminServiceMBeanImpl ab = new AdminServiceMBeanImpl();
        ab.setAdminService(as);
        Assert.assertSame(as, ab.getAdminService());

        ab.startInstance("test instance", "-x -y -z");
        EasyMock.verify(as);
        EasyMock.verify(inst);
    }
    
    public void testStopInstance() throws Exception {
        Instance inst = EasyMock.createMock(Instance.class);
        inst.stop();
        EasyMock.expectLastCall();
        EasyMock.replay(inst);

        AdminService as = EasyMock.createMock(AdminService.class);
        EasyMock.expect(as.getInstance("test instance")).andReturn(inst);
        EasyMock.replay(as);
        
        AdminServiceMBeanImpl ab = new AdminServiceMBeanImpl();
        ab.setAdminService(as);
        Assert.assertSame(as, ab.getAdminService());

        ab.stopInstance("test instance");
        EasyMock.verify(as);
        EasyMock.verify(inst);
    }

    public void testDestroyInstance() throws Exception {
        Instance inst = EasyMock.createMock(Instance.class);
        inst.destroy();
        EasyMock.expectLastCall();
        EasyMock.replay(inst);

        AdminService as = EasyMock.createMock(AdminService.class);
        EasyMock.expect(as.getInstance("test instance")).andReturn(inst);
        EasyMock.replay(as);
        
        AdminServiceMBeanImpl ab = new AdminServiceMBeanImpl();
        ab.setAdminService(as);
        Assert.assertSame(as, ab.getAdminService());

        ab.destroyInstance("test instance");
        EasyMock.verify(as);
        EasyMock.verify(inst);
    }

    public void testChangePort() throws Exception {
        Instance inst = EasyMock.createMock(Instance.class);
        inst.changePort(7788);
        EasyMock.expectLastCall();
        EasyMock.replay(inst);

        AdminService as = EasyMock.createMock(AdminService.class);
        EasyMock.expect(as.getInstance("test instance")).andReturn(inst);
        EasyMock.replay(as);
        
        AdminServiceMBeanImpl ab = new AdminServiceMBeanImpl();
        ab.setAdminService(as);
        Assert.assertSame(as, ab.getAdminService());

        ab.changePort("test instance", 7788);
        EasyMock.verify(as);
        EasyMock.verify(inst);
    }
}
