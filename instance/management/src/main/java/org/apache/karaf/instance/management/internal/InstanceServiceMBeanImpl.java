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
package org.apache.karaf.instance.management.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.TabularData;

import org.apache.karaf.instance.management.InstanceServiceMBean;
import org.apache.karaf.instance.InstanceService;
import org.apache.karaf.instance.Instance;
import org.apache.karaf.instance.InstanceSettings;
import org.apache.karaf.instance.management.codec.JmxInstance;

public class InstanceServiceMBeanImpl extends StandardMBean implements InstanceServiceMBean {

    private InstanceService instanceService;

    public InstanceServiceMBeanImpl() throws NotCompliantMBeanException {
        super(InstanceServiceMBean.class);
    }

    public InstanceService getInstanceService() {
        return instanceService;
    }

    public void setInstanceService(InstanceService instanceService) {
        this.instanceService = instanceService;
    }

    public int createInstance(String name, int sshPort, int rmiRegistryPort, int rmiServerPort, String location, String javaOpts, String features, String featureURLs)
            throws Exception {
        if ("".equals(location)) {
            location = null;
        }
        if ("".equals(javaOpts)) {
            javaOpts = null;
        }

        InstanceSettings settings = new InstanceSettings(sshPort, rmiRegistryPort, rmiServerPort, location, javaOpts,
                parseStringList(featureURLs), parseStringList(features));

        Instance inst = instanceService.createInstance(name, settings, false);
        if (inst != null) {
            return inst.getPid();
        } else {
            return -1;
        }
    }

    public void changeSshPort(String name, int port) throws Exception {
        getExistingInstance(name).changeSshPort(port);
    }

    public void changeRmiRegistryPort(String name, int port) throws Exception {
        getExistingInstance(name).changeRmiRegistryPort(port);
    }

    public void changeRmiServerPort(String name, int port) throws Exception {
        getExistingInstance(name).changeRmiServerPort(port);
    }

    public void changeJavaOpts(String name, String javaOpts) throws Exception {
        getExistingInstance(name).changeJavaOpts(javaOpts);
    }

    public void destroyInstance(String name) throws Exception {
        getExistingInstance(name).destroy();
    }

    public void startInstance(String name, String opts) throws Exception {
        getExistingInstance(name).start(opts);
    }

    public void stopInstance(String name) throws Exception {
        getExistingInstance(name).stop();
    }

    public void renameInstance(String originalName, String newName) throws Exception {
        instanceService.renameInstance(originalName, newName, false);
    }

    public TabularData getInstances() throws Exception {
        List<Instance> allInstances = Arrays.asList(instanceService.getInstances());
        List<JmxInstance> instances = new ArrayList<JmxInstance>();
        for (Instance instance : allInstances) {
            instances.add(new JmxInstance(instance));
        }
        TabularData table = JmxInstance.tableFrom(instances);
        return table;
    }

    private Instance getExistingInstance(String name) {
        Instance i = instanceService.getInstance(name);
        if (i == null) {
            throw new IllegalArgumentException("Instance '" + name + "' does not exist");
        }
        return i;
    }

    private List<String> parseStringList(String value) {
        List<String> list = new ArrayList<String>();
        if (value != null) {
            for (String el : value.split(",")) {
                String trimmed = el.trim();
                if (trimmed.length() == 0) {
                    continue;
                }
                list.add(trimmed);
            }
        }
        return list;
    }
}
