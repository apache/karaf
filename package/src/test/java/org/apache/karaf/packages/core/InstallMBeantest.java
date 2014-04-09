/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.packages.core;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.karaf.packages.core.internal.PackagesMBeanImpl;
import org.junit.Test;

/**
 * Checks that the PackagesMBean is valid and can be installed in the MBeanServer
 *
 */
public class InstallMBeantest {

    @Test
    public void test() throws Exception {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        PackagesMBeanImpl pack = new PackagesMBeanImpl(null);
        ObjectName oName = new ObjectName("org.apache.karaf:type=package,name=root");
        server.registerMBean(pack,  oName);
        server.unregisterMBean(oName);
    }

}
