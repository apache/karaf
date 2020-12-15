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
package org.apache.karaf.instance.core.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.management.MBeanException;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.TabularData;

import org.apache.karaf.instance.core.Instance;
import org.apache.karaf.instance.core.InstanceSettings;
import org.apache.karaf.instance.core.InstancesMBean;

public class InstancesMBeanImpl extends StandardMBean implements InstancesMBean {

    static final String DEBUG_OPTS = " -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005";
    static final String DEFAULT_OPTS = "-server -Xmx512m";

    private org.apache.karaf.instance.core.InstanceService instanceService;

    public InstancesMBeanImpl(org.apache.karaf.instance.core.InstanceService instanceService) throws NotCompliantMBeanException {
        super(InstancesMBean.class);
        this.instanceService = instanceService;
    }

    public int createInstance(String name,
                              int sshPort,
                              int rmiRegistryPort,
                              int rmiServerPort,
                              String location,
                              String javaOpts,
                              String features,
                              String featuresURLs)
        throws MBeanException {
        return this.createInstance(name, sshPort, rmiRegistryPort, rmiServerPort, location, javaOpts, features, featuresURLs, "localhost");
    }

    public int createInstance(String name, int sshPort, int rmiRegistryPort, int rmiServerPort, String location, String javaOpts, String features, String featureURLs, String address)
            throws MBeanException {
        try {
            if ("".equals(location)) {
                location = null;
            }
            if ("".equals(javaOpts)) {
                javaOpts = null;
            }

            InstanceSettings settings = new InstanceSettings(sshPort, rmiRegistryPort, rmiServerPort, location, javaOpts,
                    parseStringList(featureURLs), parseStringList(features), address);

            Instance inst = instanceService.createInstance(name, settings, false);
            if (inst != null) {
                return inst.getPid();
            } else {
                return -1;
            }
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public void changeSshPort(String name, int port) throws MBeanException {
        try {
            getExistingInstance(name).changeSshPort(port);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public void changeSshHost(String name, String host) throws MBeanException {
        try {
            getExistingInstance(name).changeSshHost(host);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public void changeRmiRegistryPort(String name, int port) throws MBeanException {
        try {
            getExistingInstance(name).changeRmiRegistryPort(port);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public void changeRmiServerPort(String name, int port) throws MBeanException {
        try {
            getExistingInstance(name).changeRmiServerPort(port);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public void changeJavaOpts(String name, String javaOpts) throws MBeanException {
        try {
            getExistingInstance(name).changeJavaOpts(javaOpts);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public void destroyInstance(String name) throws MBeanException {
        try {
            getExistingInstance(name).destroy();
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public void startInstance(String name) throws MBeanException {
        try {
            getExistingInstance(name).start(null);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public void startInstance(String name, String opts) throws MBeanException {
        try {
            getExistingInstance(name).start(opts);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public void startInstance(String name, String opts, boolean wait, boolean debug) throws MBeanException {
        try {
            Instance child = getExistingInstance(name);
            String options = opts;
            if (options == null) {
                options = child.getJavaOpts();
            }
            if (options == null) {
                options = DEFAULT_OPTS;
            }
            if (debug) {
                options += DEBUG_OPTS;
            }
            if (wait) {
                String state = child.getState();
                if (Instance.STOPPED.equals(state)) {
                    child.start(opts);
                }
                if (!Instance.STARTED.equals(state)) {
                    do {
                        Thread.sleep(500);
                        state = child.getState();
                    } while (Instance.STARTING.equals(state));
                }
            } else {
                child.start(opts);
            }
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public void stopInstance(String name) throws MBeanException {
        try {
            getExistingInstance(name).stop();
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public void renameInstance(String originalName, String newName) throws MBeanException {
        try {
            instanceService.renameInstance(originalName, newName, false);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public void renameInstance(String originalName, String newName, boolean verbose) throws MBeanException {
        try {
            instanceService.renameInstance(originalName, newName, verbose);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public void cloneInstance(String name, String cloneName, int sshPort, int rmiRegistryPort, int rmiServerPort, String location, String javaOpts) throws MBeanException {
        try {
            if ("".equals(location)) {
                location = null;
            }
            if ("".equals(javaOpts)) {
                javaOpts = null;
            }

            InstanceSettings settings = new InstanceSettings(sshPort, rmiRegistryPort, rmiServerPort, location, javaOpts, null, null);

            instanceService.cloneInstance(name, cloneName, settings, false);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public TabularData getInstances() throws MBeanException {
        List<Instance> instances = Arrays.asList(instanceService.getInstances());
        TabularData table = InstanceToTableMapper.tableFrom(instances);
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
        List<String> list = new ArrayList<>();
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
