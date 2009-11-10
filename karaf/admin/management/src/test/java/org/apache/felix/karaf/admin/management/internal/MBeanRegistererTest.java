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

import java.util.Map;
import java.util.HashMap;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.felix.karaf.admin.management.AdminServiceMBean;

import static org.easymock.classextension.EasyMock.*;
import org.easymock.classextension.IMocksControl;

import junit.framework.TestCase;
import org.junit.Assert;

public class MBeanRegistererTest extends TestCase {
    public void testRegistration() throws Exception {
        IMocksControl control = createNiceControl();
        MBeanServer mbeanServer = control.createMock(MBeanServer.class);
        String rawName = "org.apache.felix.karaf:type=admin,name=${karaf.name}";
        AdminServiceMBean mbean = control.createMock(AdminServiceMBean.class);
        ObjectName name = new ObjectName("org.apache.felix.karaf:type=admin,name=foobar");
        expect(mbeanServer.registerMBean(isA(AdminServiceMBean.class), eq(name))).andReturn(null);
        mbeanServer.unregisterMBean(eq(name));
        expectLastCall();
        control.replay();

        String old = System.getProperty("karaf.name");
        System.setProperty("karaf.name", "foobar");
        MBeanRegistrer registerer = new MBeanRegistrer();
        Map<Object, String> mbeans = new HashMap<Object, String>();
        mbeans.put(mbean, rawName);
        registerer.setMbeans(mbeans);
        registerer.registerMBeanServer(mbeanServer);
        registerer.unregisterMBeanServer(mbeanServer);

        restoreProperties(old);
        control.verify();
    }

    private void restoreProperties(String old) {
        if (old != null) {
           System.setProperty("karaf.name", old);
        } else { 
           System.getProperties().remove("karaf.name");
        }
    }
}
