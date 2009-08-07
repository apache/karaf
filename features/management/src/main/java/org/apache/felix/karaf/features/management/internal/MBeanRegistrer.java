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
package org.apache.felix.karaf.features.management.internal;

import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.MBeanRegistrationException;
import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.JMException;

public class MBeanRegistrer {

    private MBeanServer mbeanServer;

    private Map<Object, String> mbeans;

    public void setMbeans(Map<Object, String> mbeans) {
        this.mbeans = mbeans;
    }

    public void registerMBeanServer(MBeanServer mbeanServer) throws JMException {
        if (this.mbeanServer != mbeanServer) {
            unregisterMBeans();
        }
        this.mbeanServer = mbeanServer;
        registerMBeans();
    }

    public void unregisterMBeanServer(MBeanServer mbeanServer) throws JMException {
        unregisterMBeans();
        this.mbeanServer = null;
    }

    public void init() throws Exception {
        registerMBeans();
    }

    protected void registerMBeans() throws JMException {
        if (mbeanServer != null && mbeans != null) {
            for (Map.Entry<Object, String> entry : mbeans.entrySet()) {
                mbeanServer.registerMBean(entry.getKey(), new ObjectName(entry.getValue()));
            }
        }
    }

    protected void unregisterMBeans() throws JMException {
        if (mbeanServer != null && mbeans != null) {
            for (Map.Entry<Object, String> entry : mbeans.entrySet()) {
                mbeanServer.unregisterMBean(new ObjectName(entry.getValue()));
            }
        }
    }
}
