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
package org.apache.karaf.system.management.internal;

import org.apache.karaf.system.FrameworkType;
import org.apache.karaf.system.SystemService;
import org.apache.karaf.system.management.SystemMBean;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

/**
 * System MBean implementation.
 */
public class System extends StandardMBean implements SystemMBean {

    private SystemService systemService;

    public System() throws NotCompliantMBeanException {
        super(SystemMBean.class);
    }

    public void setSystemService(SystemService systemService) {
        this.systemService = systemService;
    }

    public SystemService getSystemService() {
        return this.systemService;
    }

    public void halt() throws Exception {
        systemService.halt();
    }

    public void halt(String time) throws Exception {
        systemService.halt(time);
    }

    public void reboot() throws Exception {
        systemService.reboot();
    }

    public void reboot(String time) throws Exception {
        systemService.reboot(time, SystemService.Swipe.NONE);
    }

    public void rebootCleanCache(String time) throws Exception {
        systemService.reboot(time, SystemService.Swipe.CACHE);
    }

    public void rebootCleanAll(String time) throws Exception {
        systemService.reboot(time, SystemService.Swipe.ALL);
    }

    public void setStartLevel(int startLevel) throws Exception {
        systemService.setStartLevel(startLevel);
    }

    public int getStartLevel() throws Exception {
        return systemService.getStartLevel();
    }

    @Override
    public String getFramework() {
        return this.systemService.getFramework().toString();
    }
    
    @Override
    public void setFramework(String framework) {
        this.systemService.setFramework(FrameworkType.valueOf(framework.toLowerCase()));
    }

    @Override
    public void setFrameworkDebug(boolean debug) {
        this.systemService.setFrameworkDebug(debug);
    }

    @Override
    public String getName() {
        return this.systemService.getName();
    }

    @Override
    public void setName(String name) throws Exception {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Instance name can't be null or empty");
        }
        this.systemService.setName(name);
    }

    @Override
    public String getVersion() {
        return this.systemService.getVersion();
    }

}
