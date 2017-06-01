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

import java.util.Arrays;
import java.util.Collections;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import junit.framework.TestCase;

import org.apache.karaf.instance.core.Instance;
import org.apache.karaf.instance.core.InstanceService;
import org.apache.karaf.instance.core.InstanceSettings;
import org.apache.karaf.instance.core.InstancesMBean;
import org.apache.karaf.instance.core.internal.InstancesMBeanImpl;
import org.easymock.EasyMock;
import org.junit.Assert;

public class InstanceServiceMBeanImplTest extends TestCase {

    public void testCreateInstance() throws Exception {
        final InstanceSettings instanceSettings = new InstanceSettings(123, 456,789, "somewhere", "someopts",
                Collections.emptyList(), Arrays.asList("webconsole", "funfeat"), "localhost");
        
        final Instance inst = EasyMock.createMock(Instance.class);
        EasyMock.expect(inst.getPid()).andReturn(42);
        EasyMock.replay(inst);

        org.apache.karaf.instance.core.InstanceService instanceService = EasyMock.createMock(org.apache.karaf.instance.core.InstanceService.class);
        EasyMock.expect(instanceService.createInstance("t1", instanceSettings, false)).andReturn(inst);
        EasyMock.replay(instanceService);
        
        InstancesMBeanImpl ab = new InstancesMBeanImpl(instanceService);
        assertEquals(42, ab.createInstance("t1", 123, 456, 789, "somewhere", "someopts", " webconsole,  funfeat", ""));
    }
    
    public void testCreateInstance2() throws Exception {
        final InstanceSettings instanceSettings = new InstanceSettings(0, 0, 0, null, null,
                Collections.emptyList(), Collections.emptyList(), "localhost");
        
        InstanceService instanceService = EasyMock.createMock(InstanceService.class);
        EasyMock.expect(instanceService.createInstance("t1", instanceSettings, false)).andReturn(null);
        EasyMock.replay(instanceService);
        
        InstancesMBean ab = new InstancesMBeanImpl(instanceService);
        assertEquals(-1, ab.createInstance("t1", 0, 0, 0, "", "", "", ""));
    }
    
    public void testGetInstances() throws Exception {       
        Instance i1 = EasyMock.createMock(Instance.class);
        EasyMock.expect(i1.getPid()).andReturn(1234);
        EasyMock.expect(i1.getSshPort()).andReturn(8818);
        EasyMock.expect(i1.getSshHost()).andReturn("0.0.0.0");
        EasyMock.expect(i1.getRmiRegistryPort()).andReturn(1122);
        EasyMock.expect(i1.getRmiRegistryHost()).andReturn("0.0.0.0");
        EasyMock.expect(i1.getRmiServerPort()).andReturn(44444);
        EasyMock.expect(i1.getRmiServerHost()).andReturn("0.0.0.0");
        EasyMock.expect(i1.getName()).andReturn("i1");
        EasyMock.expect(i1.isRoot()).andReturn(true);
        EasyMock.expect(i1.getLocation()).andReturn("somewhere");
        EasyMock.expect(i1.getJavaOpts()).andReturn("someopts");
        EasyMock.expect(i1.getState()).andReturn("Stopped");
        EasyMock.replay(i1);
        Instance i2 = EasyMock.createNiceMock(Instance.class);
        EasyMock.expect(i2.getName()).andReturn("i2");
        EasyMock.replay(i2);
        
        InstanceService instanceService = EasyMock.createMock(InstanceService.class);
        EasyMock.expect(instanceService.getInstances()).andReturn(new Instance[]{i1, i2});
        EasyMock.replay(instanceService);

        InstancesMBeanImpl instanceServiceMBean = new InstancesMBeanImpl(instanceService);
        TabularData tabularData = instanceServiceMBean.getInstances();
        Assert.assertEquals(2, tabularData.size());
        CompositeData cd1 = tabularData.get(new Object[]{"i1"});
        Assert.assertTrue(cd1.containsValue("i1"));
        Assert.assertTrue(cd1.containsValue(true));
        Assert.assertTrue(cd1.containsValue(1234));
        Assert.assertTrue(cd1.containsValue(8818));
        Assert.assertTrue(cd1.containsValue(1122));
        Assert.assertTrue(cd1.containsValue(44444));
        Assert.assertTrue(cd1.containsValue("0.0.0.0"));
        Assert.assertTrue(cd1.containsValue("somewhere"));
        Assert.assertTrue(cd1.containsValue("someopts"));
        Assert.assertTrue(cd1.containsValue("Stopped"));

        CompositeData cd2 = tabularData.get(new Object [] {"i2"});
        Assert.assertTrue(cd2.containsValue("i2"));
    }
    
    public void testStartInstanceWithJavaOpts() throws Exception {
        Instance inst = EasyMock.createMock(Instance.class);
        inst.start("-x -y -z");
        EasyMock.expectLastCall();
        EasyMock.replay(inst);

        InstanceService instanceService = EasyMock.createMock(InstanceService.class);
        EasyMock.expect(instanceService.getInstance("test instance")).andReturn(inst);
        EasyMock.replay(instanceService);

        InstancesMBean instanceServiceMBean = new InstancesMBeanImpl(instanceService);

        instanceServiceMBean.startInstance("test instance", "-x -y -z");
        EasyMock.verify(instanceService);
        EasyMock.verify(inst);
    }

    public void testStartInstanceWithNoJavaOpts() throws Exception {
        Instance inst = EasyMock.createMock(Instance.class);
        inst.start(null);
        EasyMock.expectLastCall();
        EasyMock.replay(inst);

        InstanceService instanceService = EasyMock.createMock(InstanceService.class);
        EasyMock.expect(instanceService.getInstance("test instance")).andReturn(inst);
        EasyMock.replay(instanceService);

        InstancesMBean instanceServiceMBean = new InstancesMBeanImpl(instanceService);

        instanceServiceMBean.startInstance("test instance", null);
        EasyMock.verify(instanceService);
        EasyMock.verify(inst);
    }

    public void testStopInstance() throws Exception {
        Instance inst = EasyMock.createMock(Instance.class);
        inst.stop();
        EasyMock.expectLastCall();
        EasyMock.replay(inst);

        InstanceService instanceService = EasyMock.createMock(InstanceService.class);
        EasyMock.expect(instanceService.getInstance("test instance")).andReturn(inst);
        EasyMock.replay(instanceService);
        
        InstancesMBean instanceServiceMBean = new InstancesMBeanImpl(instanceService);

        instanceServiceMBean.stopInstance("test instance");
        EasyMock.verify(instanceService);
        EasyMock.verify(inst);
    }

    public void testDestroyInstance() throws Exception {
        Instance inst = EasyMock.createMock(Instance.class);
        inst.destroy();
        EasyMock.expectLastCall();
        EasyMock.replay(inst);

        InstanceService instanceService = EasyMock.createMock(InstanceService.class);
        EasyMock.expect(instanceService.getInstance("test instance")).andReturn(inst);
        EasyMock.replay(instanceService);
        
        InstancesMBean instanceServiceMBean = new InstancesMBeanImpl(instanceService);

        instanceServiceMBean.destroyInstance("test instance");
        EasyMock.verify(instanceService);
        EasyMock.verify(inst);
    }

    public void testSshChangePort() throws Exception {
        Instance inst = EasyMock.createMock(Instance.class);
        inst.changeSshPort(7788);
        EasyMock.expectLastCall();
        EasyMock.replay(inst);

        InstanceService instanceService = EasyMock.createMock(InstanceService.class);
        EasyMock.expect(instanceService.getInstance("test instance")).andReturn(inst);
        EasyMock.replay(instanceService);
        
        InstancesMBean instanceServiceMBean = new InstancesMBeanImpl(instanceService);

        instanceServiceMBean.changeSshPort("test instance", 7788);
        EasyMock.verify(instanceService);
        EasyMock.verify(inst);
    }
    
    public void testRmiRegistryChangePort() throws Exception {
        Instance inst = EasyMock.createMock(Instance.class);
        inst.changeRmiRegistryPort(1123);
        EasyMock.expectLastCall();
        EasyMock.replay(inst);
        
        InstanceService instanceService = EasyMock.createMock(InstanceService.class);
        EasyMock.expect(instanceService.getInstance("test instance")).andReturn(inst);
        EasyMock.replay(instanceService);
        
        InstancesMBean instanceServiceMBean = new InstancesMBeanImpl(instanceService);
        
        instanceServiceMBean.changeRmiRegistryPort("test instance", 1123);
        EasyMock.verify(instanceService);
        EasyMock.verify(inst);
    }

    public void testRmiServerChangePort() throws Exception {
        Instance inst = EasyMock.createMock(Instance.class);
        inst.changeRmiServerPort(44444);
        EasyMock.expectLastCall();
        EasyMock.replay(inst);

        InstanceService instanceService = EasyMock.createMock(InstanceService.class);
        EasyMock.expect(instanceService.getInstance("test instance")).andReturn(inst);
        EasyMock.replay(instanceService);

        InstancesMBean instanceServiceMBean = new InstancesMBeanImpl(instanceService);

        instanceServiceMBean.changeRmiServerPort("test instance", 44444);
        EasyMock.verify(instanceService);
        EasyMock.verify(inst);
    }

    public void testChangeOptions() throws Exception {
        Instance inst = EasyMock.createMock(Instance.class);
        inst.changeJavaOpts("new opts");
        EasyMock.expectLastCall();
        EasyMock.replay(inst);

        InstanceService instanceService = EasyMock.createMock(InstanceService.class);
        EasyMock.expect(instanceService.getInstance("test instance")).andReturn(inst);
        EasyMock.replay(instanceService);

        InstancesMBean instanceServiceMBean = new InstancesMBeanImpl(instanceService);

        instanceServiceMBean.changeJavaOpts("test instance", "new opts");
        EasyMock.verify(instanceService);
        EasyMock.verify(inst);
    }

}
