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
package org.apache.felix.karaf.shell.admin.internal;

import org.apache.felix.karaf.shell.admin.AdminService;
import org.apache.felix.karaf.shell.admin.AdminServiceMBean;
import org.apache.felix.karaf.shell.admin.Instance;

public class AdminServiceMBeanImpl implements AdminServiceMBean {

    private AdminService adminService;

    public AdminService getAdminService() {
        return adminService;
    }

    public void setAdminService(AdminService adminService) {
        this.adminService = adminService;
    }

    public void createInstance(String name, int port, String location) throws Exception {
        adminService.createInstance(name, port, location);
    }

    public String[] getInstances() {
        Instance[] instances = adminService.getInstances();
        String[] names = new String[instances.length];
        for (int i = 0; i < instances.length; i++) {
            names[i] = instances[i].getName();
        }
        return names;
    }

    public int getPort(String name) {
        return getExistingInstance(name).getPort();
    }

    public void changePort(String name, int port) throws Exception {
        getExistingInstance(name).changePort(port);
    }

    public String getState(String name) throws Exception {
        return getExistingInstance(name).getState();
    }

    public void start(String name, String javaOpts) throws Exception {
        getExistingInstance(name).start(javaOpts);
    }

    public void stop(String name) throws Exception {
        getExistingInstance(name).stop();
    }

    public void destroy(String name) throws Exception {
        getExistingInstance(name).destroy();
    }


    private Instance getExistingInstance(String name) {
        Instance i = adminService.getInstance(name);
        if (i == null) {
            throw new IllegalArgumentException("Instance '" + name + "' does not exist");
        }
        return i;
    }

}
