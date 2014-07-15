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
package org.apache.karaf.admin.management.internal;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import junit.framework.TestCase;
import org.apache.karaf.admin.AdminService;
import org.apache.karaf.admin.Instance;
import org.apache.karaf.admin.InstanceSettings;
import org.easymock.EasyMock;
import org.junit.Assert;

public class AdminServiceMBeanImplTest extends TestCase {
    public void testCreateInstance() throws Exception {
        final InstanceSettings is = new InstanceSettings(123, 456, 789, "somewhere", "someopts",
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
        
        assertEquals(42, ab.createInstance("t1", 123, 456, 789, "somewhere", "someopts", " webconsole,  funfeat", ""));
    }
    
    public void testCreateInstance2() throws Exception {
        final InstanceSettings is = new InstanceSettings(0, 0, 0, null, null,
                Collections.<String>emptyList(), Collections.<String>emptyList());
        
        AdminService as = EasyMock.createMock(AdminService.class);
        EasyMock.expect(as.createInstance("t1", is)).andReturn(null);
        EasyMock.replay(as);
        
        AdminServiceMBeanImpl ab = new AdminServiceMBeanImpl();
        ab.setAdminService(as);
        Assert.assertSame(as, ab.getAdminService());
        
        assertEquals(-1, ab.createInstance("t1", 0, 0, 0, "", "", "", ""));
    }
    
    public void testGetInstances() throws Exception {       
        Instance i1 = EasyMock.createMock(Instance.class);
        EasyMock.expect(i1.getPid()).andReturn(1234);
        EasyMock.expect(i1.getSshPort()).andReturn(8818);
        EasyMock.expect(i1.getRmiRegistryPort()).andReturn(1122);
        EasyMock.expect(i1.getRmiServerPort()).andReturn(44444);
        EasyMock.expect(i1.getName()).andReturn("i1");
        EasyMock.expect(i1.isRoot()).andReturn(true);
        EasyMock.expect(i1.getLocation()).andReturn("somewhere");
        EasyMock.expect(i1.getJavaOpts()).andReturn("someopts");
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
        Assert.assertTrue(cd1.containsValue(1122));
        Assert.assertTrue(cd1.containsValue(44444));
        Assert.assertTrue(cd1.containsValue("somewhere"));
        Assert.assertTrue(cd1.containsValue("someopts"));
        Assert.assertTrue(cd1.containsValue("Stopped"));

        CompositeData cd2 = td.get(new Object [] {"i2"});
        Assert.assertTrue(cd2.containsValue("i2"));
    }
    
    public void testStartInstanceWithJavaOpts() throws Exception {
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

    public void testStartInstanceWithNoJavaOpts() throws Exception {
        Instance inst = EasyMock.createMock(Instance.class);
        inst.start(null);
        EasyMock.expectLastCall();
        EasyMock.replay(inst);

        AdminService as = EasyMock.createMock(AdminService.class);
        EasyMock.expect(as.getInstance("test instance")).andReturn(inst);
        EasyMock.replay(as);

        AdminServiceMBeanImpl ab = new AdminServiceMBeanImpl();
        ab.setAdminService(as);
        Assert.assertSame(as, ab.getAdminService());

        ab.startInstance("test instance", null);
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

    public void testSshChangePort() throws Exception {
        Instance inst = EasyMock.createMock(Instance.class);
        inst.changeSshPort(7788);
        EasyMock.expectLastCall();
        EasyMock.replay(inst);

        AdminService as = EasyMock.createMock(AdminService.class);
        EasyMock.expect(as.getInstance("test instance")).andReturn(inst);
        EasyMock.replay(as);
        
        AdminServiceMBeanImpl ab = new AdminServiceMBeanImpl();
        ab.setAdminService(as);
        Assert.assertSame(as, ab.getAdminService());

        ab.changeSshPort("test instance", 7788);
        EasyMock.verify(as);
        EasyMock.verify(inst);
    }
    
    public void testRmiRegistryChangePort() throws Exception {
        Instance inst = EasyMock.createMock(Instance.class);
        inst.changeRmiRegistryPort(1123);
        EasyMock.expectLastCall();
        EasyMock.replay(inst);
        
        AdminService as = EasyMock.createMock(AdminService.class);
        EasyMock.expect(as.getInstance("test instance")).andReturn(inst);
        EasyMock.replay(as);
        
        AdminServiceMBeanImpl ab = new AdminServiceMBeanImpl();
        ab.setAdminService(as);
        Assert.assertSame(as, ab.getAdminService());
        
        ab.changeRmiRegistryPort("test instance", 1123);
        EasyMock.verify(as);
        EasyMock.verify(inst);
    }

    public void testRmiServerChangePort() throws Exception {
        Instance inst = EasyMock.createMock(Instance.class);
        inst.changeRmiServerPort(44444);
        EasyMock.expectLastCall();
        EasyMock.replay(inst);

        AdminService as = EasyMock.createMock(AdminService.class);
        EasyMock.expect(as.getInstance("test instance")).andReturn(inst);
        EasyMock.replay(as);

        AdminServiceMBeanImpl ab = new AdminServiceMBeanImpl();
        ab.setAdminService(as);
        Assert.assertSame(as, ab.getAdminService());

        ab.changeRmiServerPort("test instance", 44444);
        EasyMock.verify(as);
        EasyMock.verify(inst);
    }

    public void testChangeOptions() throws Exception {
        Instance inst = EasyMock.createMock(Instance.class);
        inst.changeJavaOpts("new opts");
        EasyMock.expectLastCall();
        EasyMock.replay(inst);

        AdminService as = EasyMock.createMock(AdminService.class);
        EasyMock.expect(as.getInstance("test instance")).andReturn(inst);
        EasyMock.replay(as);

        AdminServiceMBeanImpl ab = new AdminServiceMBeanImpl();
        ab.setAdminService(as);
        Assert.assertSame(as, ab.getAdminService());

        ab.changeJavaOpts("test instance", "new opts");
        EasyMock.verify(as);
        EasyMock.verify(inst);
    }
}
